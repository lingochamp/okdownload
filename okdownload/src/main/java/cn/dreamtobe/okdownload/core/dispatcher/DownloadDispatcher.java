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

package cn.dreamtobe.okdownload.core.dispatcher;


import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.download.DownloadCall;
import cn.dreamtobe.okdownload.core.util.ThreadUtil;
import cn.dreamtobe.okdownload.DownloadTask;

public class DownloadDispatcher {
    // same id will be discard
    // submit task to download server
    // assemble breakpoint and start chain on block dispatcher

    int maxTaskCount = 15;
    // for sort performance(not need to copy one array), using ArrayList instead of deque(for add on top, remove on bottom).
    private final List<DownloadCall> readyAsyncCalls;

    private final List<DownloadCall> runningAsyncCalls;
    private final List<DownloadCall> runningSyncCalls;

    private @Nullable
    ExecutorService executorService;

    public DownloadDispatcher() {
        this(new ArrayList<DownloadCall>(), new ArrayList<DownloadCall>(), new ArrayList<DownloadCall>());
    }

    DownloadDispatcher(List<DownloadCall> readyAsyncCalls, List<DownloadCall> runningAsyncCalls,
                       List<DownloadCall> runningSyncCalls) {
        this.readyAsyncCalls = readyAsyncCalls;
        this.runningAsyncCalls = runningAsyncCalls;
        this.runningSyncCalls = runningSyncCalls;

    }

    synchronized ExecutorService executorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                    ThreadUtil.threadFactory("OkDownload Download", false));
        }

        return executorService;
    }

    public synchronized void enqueue(DownloadTask task) {
        if (inspectForConflict(task)) return;

        final DownloadCall call = DownloadCall.create(task, true);
        if (runningAsyncCalls.size() < maxTaskCount) {
            runningAsyncCalls.add(call);
            executorService().execute(call);
        } else {
            // priority
            readyAsyncCalls.add(call);
            Collections.sort(readyAsyncCalls);
        }
    }

    public synchronized void execute(DownloadTask task) {
        if (inspectForConflict(task)) return;

        final DownloadCall call = DownloadCall.create(task, false);
        runningSyncCalls.add(call);
        syncRunCall(call);
    }

    // this method convenient for unit-test.
    void syncRunCall(DownloadCall call) {
        call.run();
    }

    public synchronized void finish(DownloadCall call) {
        final boolean asyncExecuted = call.asyncExecuted;
        final Collection<DownloadCall> calls = asyncExecuted ? runningAsyncCalls : runningSyncCalls;
        if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");

        if (asyncExecuted) processCalls();
    }

    private boolean inspectForConflict(DownloadTask task) {
        return inspectForConflict(task, readyAsyncCalls)
                || inspectForConflict(task, runningAsyncCalls)
                || inspectForConflict(task, runningSyncCalls);

    }

    private boolean inspectForConflict(DownloadTask task, Collection<DownloadCall> calls) {
        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher;
        for (DownloadCall call : calls) {
            if (call.task == task) {
                callbackDispatcher.dispatch(task)
                        .taskEnd(task, DownloadListener.EndCause.sameTaskBusy, null);
                return true;
            }

            if (new File(call.task.getPath()).equals(new File(task.getPath()))) {
                callbackDispatcher.dispatch(task)
                        .taskEnd(task, DownloadListener.EndCause.fileBusy, null);
                return true;
            }
        }

        return false;
    }

    private void processCalls() {
        if (runningAsyncCalls.size() >= maxTaskCount) return;
        if (readyAsyncCalls.isEmpty()) return;

        for (Iterator<DownloadCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            DownloadCall call = i.next();

            i.remove();
            runningAsyncCalls.add(call);
            executorService().execute(call);

            if (runningAsyncCalls.size() >= maxTaskCount) return;
        }
    }
}
