/*
 * Copyright (C) 2017 Jacksgong(jacksgong.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.dreamtobe.okdownload.core.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.NamedRunnable;
import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.cause.EndCause;
import cn.dreamtobe.okdownload.core.dispatcher.CallbackDispatcher;
import cn.dreamtobe.okdownload.core.file.MultiPointOutputStream;
import cn.dreamtobe.okdownload.core.file.ProcessFileStrategy;

public class DownloadCall extends NamedRunnable implements Comparable<DownloadCall> {
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload Block", false));

    static final int MAX_COUNT_RETRY_FOR_PRECONDITION_FAILED = 1;
    public final DownloadTask task;
    public final boolean asyncExecuted;

    @Nullable private volatile DownloadCache cache;

    private DownloadCall(DownloadTask task, boolean asyncExecuted) {
        super("download call: " + task.getId());
        this.task = task;
        this.asyncExecuted = asyncExecuted;
    }

    public static DownloadCall create(DownloadTask task, boolean asyncExecuted) {
        return new DownloadCall(task, asyncExecuted);
    }

    public void cancel() {
        final DownloadCache cache = this.cache;
        if (cache != null) cache.setUserCanceled();
    }

    @Override
    public void execute() throws InterruptedException {
        boolean retry = false;
        int retryCount = 0;

        final OkDownload okDownload = OkDownload.with();
        final CallbackDispatcher dispatcher = okDownload.callbackDispatcher();
        final BreakpointStore store = okDownload.breakpointStore();
        final ProcessFileStrategy fileStrategy = okDownload.processFileStrategy();

        dispatcher.dispatch().taskStart(task);
        while (true) {

            // get store
            BreakpointInfo info = store.get(task.getId());
            dispatcher.dispatch().breakpointData(task, info);
            if (info == null) {
                info = store.createAndInsert(task);
            }

            final MultiPointOutputStream outputStream = fileStrategy.createProcessStream(task,
                    info);
            final DownloadCache cache = createCache(outputStream);
            this.cache = cache;

            if (retry) {
                try {
                    fileStrategy.discardProcess(task);
                    start(cache, info, false);
                } catch (IOException e) {
                    cache.setUnknownError(e);
                }
            } else {
                final String filenameOnStore = info.getFilename();
                if (!Util.isEmpty(filenameOnStore)) {
                    okDownload.downloadStrategy().validFilenameFromResume(filenameOnStore, task);
                }

                final ProcessFileStrategy.ResumeAvailableLocalCheck localCheck =
                        okDownload.processFileStrategy().resumeAvailableLocalCheck(task, info);

                localCheck.callbackCause();

                start(cache, info, localCheck.isAvailable());
            }

            if (cache.isPreconditionFailed()
                    && retryCount++ < MAX_COUNT_RETRY_FOR_PRECONDITION_FAILED) {
                store.discard(task.getId());
                // try again from beginning.
                dispatcher.dispatch().downloadFromBeginning(task, info,
                        cache.getResumeFailedCause());
                retry = true;
                continue;
            }

            if (cache.isServerCanceled() || cache.isUnknownError()
                    || cache.isPreconditionFailed()) {
                // error
                dispatcher.dispatch().taskEnd(task, EndCause.ERROR, cache.getRealCause());
            } else if (cache.isUserCanceled()) {
                // user cancel
                dispatcher.dispatch().taskEnd(task, EndCause.CANCELED, null);
            } else if (cache.isFileBusyAfterRun()) {
                dispatcher.dispatch().taskEnd(task, EndCause.FILE_BUSY, null);
            } else {
                dispatcher.dispatch().taskEnd(task, EndCause.COMPLETE, null);
                store.completeDownload(task.getId());
                fileStrategy.completeProcessStream(outputStream, task);
            }
            break;
        }
    }

    // this method is convenient for unit-test.
    DownloadCache createCache(MultiPointOutputStream outputStream) {
        return new DownloadCache(outputStream);
    }

    // this method is convenient for unit-test.
    int getPriority() {
        return task.getPriority();
    }

    void start(final DownloadCache cache, BreakpointInfo info,
               boolean isResumeAvailableFromLocalCheck) throws InterruptedException {
        if (isResumeAvailableFromLocalCheck) {
            // resume task
            final int blockCount = info.getBlockCount();
            final List<Callable<Object>> blockChainList = new ArrayList<>(info.getBlockCount());
            final long totalLength = info.getTotalLength();
            for (int i = 0; i < blockCount; i++) {
                final BlockInfo blockInfo = info.getBlock(i);
                if (Util.isBlockComplete(i, blockCount, blockInfo)) continue;

                Util.resetBlockIfDirty(i, blockCount, totalLength, blockInfo);
                blockChainList.add(
                        Executors.callable(DownloadChain.createChain(i, task, info, cache)));
            }

            if (cache.isInterrupt()) {
                return;
            }

            startBlocks(blockChainList);
        } else {
            // new task
            info.resetInfo();

            // add 0 task first.
            info.addBlock(new BlockInfo(0, 0));
            // block until first block get response.
            final Thread parkThread = Thread.currentThread();
            final DownloadChain firstChain = DownloadChain.createFirstBlockChain(parkThread, task,
                    info, cache);
            if (cache.isInterrupt()) {
                return;
            }
            final Future firstBlockFuture = startFirstBlock(firstChain);
            if (!firstChain.isFinished()) {
                parkForFirstConnection();
            }

            if (cache.isInterrupt()) {
                return;
            }

            // start other blocks after unpark on BreakpointInterceptor#interceptConnect
            final int blockCount = info.getBlockCount();
            final List<Callable<Object>> blockChainList = new ArrayList<>(info.getBlockCount() - 1);
            for (int i = 1; i < blockCount; i++) {
                blockChainList.add(
                        Executors.callable(DownloadChain.createChain(i, task, info, cache)));
            }
            startBlocks(blockChainList);
            if (!firstBlockFuture.isDone()) {
                try {
                    firstBlockFuture.get();
                } catch (CancellationException | ExecutionException ignore) {
                }
            }
        }
    }

    @Override
    protected void canceled(InterruptedException e) {
    }

    @Override
    protected void finished() {
        OkDownload.with().downloadDispatcher().finish(this);
    }

    void parkForFirstConnection() {
        LockSupport.park();
    }

    void startBlocks(Collection<? extends Callable<Object>> tasks) throws InterruptedException {
        EXECUTOR.invokeAll(tasks);
    }

    Future<?> startFirstBlock(DownloadChain firstChain) {
        return EXECUTOR.submit(firstChain);
    }

    @Override
    public int compareTo(@NonNull DownloadCall o) {
        return o.getPriority() - getPriority();
    }
}
