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
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
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
    private final ArrayList<DownloadChain> blockChainList;

    @Nullable private volatile DownloadCache cache;
    private volatile boolean canceled;

    private DownloadCall(DownloadTask task, boolean asyncExecuted) {
        super("download call: " + task.getId());
        this.task = task;
        this.asyncExecuted = asyncExecuted;
        this.blockChainList = new ArrayList<>();
    }

    public static DownloadCall create(DownloadTask task, boolean asyncExecuted) {
        return new DownloadCall(task, asyncExecuted);
    }

    public void cancel() {
        this.canceled = true;
        final DownloadCache cache = this.cache;
        if (cache != null) cache.setUserCanceled();

        final List<DownloadChain> chains = (List<DownloadChain>) blockChainList.clone();
        for (DownloadChain chain : chains) {
            chain.cancel();
        }
    }

    public boolean isCanceled() { return canceled; }

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
            if (canceled) break;

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

            if (canceled) break;

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

            // finish
            blockChainList.clear();

            if (cache.isUserCanceled()) break;

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
            } else if (cache.isFileBusyAfterRun()) {
                dispatcher.dispatch().taskEnd(task, EndCause.FILE_BUSY, null);
            } else if (cache.isPreAllocateFailed()) {
                dispatcher.dispatch().taskEnd(task, EndCause.PRE_ALLOCATE_FAILED,
                        cache.getRealCause());
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
            final List<DownloadChain> blockChainList = new ArrayList<>(info.getBlockCount());
            final long totalLength = info.getTotalLength();
            for (int i = 0; i < blockCount; i++) {
                final BlockInfo blockInfo = info.getBlock(i);
                if (Util.isBlockComplete(i, blockCount, blockInfo)) continue;

                Util.resetBlockIfDirty(i, blockCount, totalLength, blockInfo);
                blockChainList.add(DownloadChain.createChain(i, task, info, cache));
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
            blockChainList.add(firstChain);
            if (!firstChain.isFinished()) {
                parkForFirstConnection();
            }

            if (cache.isInterrupt()) {
                return;
            }

            // start other blocks after unpark on BreakpointInterceptor#interceptConnect
            final int blockCount = info.getBlockCount();
            final List<DownloadChain> blockChainList = new ArrayList<>(info.getBlockCount() - 1);
            for (int i = 1; i < blockCount; i++) {
                blockChainList.add(DownloadChain.createChain(i, task, info, cache));
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

    void startBlocks(List<DownloadChain> tasks) throws InterruptedException {
        ArrayList<Future> futures = new ArrayList<>(tasks.size());
        try {
            for (DownloadChain chain : tasks) {
                futures.add(EXECUTOR.submit(chain));
            }

            blockChainList.addAll(tasks);

            for (Future future : futures) {
                if (!future.isDone()) {
                    try {
                        future.get();
                    } catch (CancellationException | ExecutionException ignore) { }
                }
            }
        } catch (Throwable t) {
            for (Future future : futures) {
                future.cancel(true);
            }
            throw t;
        } finally {
            blockChainList.removeAll(tasks);
        }
    }

    Future<?> startFirstBlock(DownloadChain firstChain) {
        return EXECUTOR.submit(firstChain);
    }

    @Override
    public int compareTo(@NonNull DownloadCall o) {
        return o.getPriority() - getPriority();
    }
}
