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

import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.breakpoint.DownloadStrategy;
import cn.dreamtobe.okdownload.core.file.MultiPointOutputStream;
import cn.dreamtobe.okdownload.core.util.ThreadUtil;
import cn.dreamtobe.okdownload.task.DownloadTask;

/**
 * Created by Jacksgong on 28/09/2017.
 */

public class DownloadCall {
    static final ExecutorService EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            ThreadUtil.threadFactory("OkDownload Block", false));

    private DownloadTask task;

    private DownloadCall(DownloadTask task) {
        this.task = task;
    }

    public static DownloadCall create(DownloadTask task) {
        return new DownloadCall(task);
    }

    public void start() throws InterruptedException {
        // get store
        final BreakpointStore store = OkDownload.with().breakpointStore;
        BreakpointInfo info = store.get(task.getId());
        if (info == null) {
            info = store.createAndInsert(task);
        }

        // init cache
        final DownloadCache cache = new DownloadCache(task);

        final DownloadStrategy breakpointStrategy = OkDownload.with().downloadStrategy;
        if (breakpointStrategy.isAvailable(task, info)) {
            // resume task
            final int blockCount = info.getBlockCount();
            final List<Callable<Object>> blockChainList = new ArrayList<>(info.getBlockCount());
            for (int i = 0; i < blockCount; i++) {
                blockChainList.add(Executors.callable(DownloadChain.createChain(i, task, info, cache)));
            }

            startBlocks(blockChainList);
        } else {
            // new task
            info.resetBlockInfos();
            // add 0 task first.
            info.addBlock(new BlockInfo(0, 0, 0));
            // block until first block get response.
            final Thread parkThread = Thread.currentThread();
            final DownloadChain firstChain = DownloadChain.createFirstBlockChain(parkThread, task, info, cache);
            final Future firstBlockFuture = startFirstBlock(firstChain);
            if (!firstChain.isFinished()) {
                LockSupport.park();
            }

            // start after unpark on BreakpointInterceptor#interceptConnect
            final int blockCount = info.getBlockCount();
            final List<Callable<Object>> blockChainList = new ArrayList<>(info.getBlockCount());
            for (int i = 1; i < blockCount; i++) {
                blockChainList.add(Executors.callable(DownloadChain.createChain(i, task, info, cache)));
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

    void startBlocks(Collection<? extends Callable<Object>> tasks) throws InterruptedException {
        EXECUTOR.invokeAll(tasks);
    }

    Future<?> startFirstBlock(DownloadChain firstChain) {
        return EXECUTOR.submit(firstChain);
    }


    static class DownloadCache {
        private String redirectLocation;
        final MultiPointOutputStream outputStream;

        DownloadCache(DownloadTask task) {
            this.outputStream = new MultiPointOutputStream(task.getId(), task.getReadBufferSize());
        }

        MultiPointOutputStream getOutputStream() {
            return outputStream;
        }

        public void setRedirectLocation(String redirectLocation) {
            this.redirectLocation = redirectLocation;
        }

        public String getRedirectLocation() {
            return redirectLocation;
        }
    }
}
