/*
 * Copyright (c) 2017 LingoChamp Inc.
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

package com.liulishuo.okdownload.core.dispatcher;


import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.download.DownloadCall;

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
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadDispatcher {

    private static final String TAG = "DownloadDispatcher";

    int maxParallelRunningCount = 5;
    // for sort performance(not need to copy one array), using ArrayList instead of deque(for add
    // on top, remove on bottom).
    private final List<DownloadCall> readyAsyncCalls;

    private final List<DownloadCall> runningAsyncCalls;
    private final List<DownloadCall> runningSyncCalls;

    // for the case of tasks has been cancelled but didn't remove from runningAsyncCalls list yet.
    private volatile int flyingCanceledAsyncCallCount;
    private @Nullable
    volatile ExecutorService executorService;

    // for avoiding processCalls when doing enqueue/cancel operation
    private final AtomicInteger skipProceedCallCount = new AtomicInteger();

    private DownloadStore store;

    public DownloadDispatcher() {
        this(new ArrayList<DownloadCall>(), new ArrayList<DownloadCall>(),
                new ArrayList<DownloadCall>());
    }

    DownloadDispatcher(List<DownloadCall> readyAsyncCalls,
                       List<DownloadCall> runningAsyncCalls,
                       List<DownloadCall> runningSyncCalls) {
        this.readyAsyncCalls = readyAsyncCalls;
        this.runningAsyncCalls = runningAsyncCalls;
        this.runningSyncCalls = runningSyncCalls;
    }

    public void setDownloadStore(@NonNull DownloadStore store) {
        this.store = store;
    }

    synchronized ExecutorService executorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                    Util.threadFactory("OkDownload Download", false));
        }

        return executorService;
    }

    public void enqueue(DownloadTask[] tasks) {
        skipProceedCallCount.incrementAndGet();
        enqueueLocked(tasks);
        skipProceedCallCount.decrementAndGet();
    }

    public void enqueue(DownloadTask task) {
        skipProceedCallCount.incrementAndGet();
        enqueueLocked(task);
        skipProceedCallCount.decrementAndGet();
    }

    private synchronized void enqueueLocked(DownloadTask[] tasks) {
        final long startTime = SystemClock.uptimeMillis();
        Util.d(TAG, "start enqueueLocked for bunch task: " + tasks.length);
        final List<DownloadTask> taskList = new ArrayList<>();
        Collections.addAll(taskList, tasks);
        if (taskList.size() > 1) Collections.sort(taskList);
        final int originReadyAsyncCallSize = readyAsyncCalls.size();

        for (DownloadTask task : taskList) {
            enqueueIgnorePriority(task);
        }

        if (originReadyAsyncCallSize != readyAsyncCalls.size()) Collections.sort(readyAsyncCalls);

        Util.d(TAG, "end enqueueLocked for bunch task: " + tasks.length + " consume "
                + (SystemClock.uptimeMillis() - startTime) + "ms");
    }

    private synchronized void enqueueLocked(DownloadTask task) {
        Util.d(TAG, "enqueueLocked for single task: " + task);
        final int originReadyAsyncCallSize = readyAsyncCalls.size();
        enqueueIgnorePriority(task);
        if (originReadyAsyncCallSize != readyAsyncCalls.size()) Collections.sort(readyAsyncCalls);
    }

    private synchronized void enqueueIgnorePriority(DownloadTask task) {
        if (inspectCompleted(task)) return;
        if (inspectForConflict(task)) return;

        final DownloadCall call = DownloadCall.create(task, true, store);
        if (runningAsyncSize() < maxParallelRunningCount) {
            runningAsyncCalls.add(call);
            executorService().execute(call);
        } else {
            // priority
            readyAsyncCalls.add(call);
        }
    }

    public void execute(DownloadTask task) {
        Util.d(TAG, "execute: " + task);
        final DownloadCall call;

        synchronized (this) {
            if (inspectCompleted(task)) return;
            if (inspectForConflict(task)) return;


            call = DownloadCall.create(task, false, store);
            runningSyncCalls.add(call);
        }

        syncRunCall(call);
    }

    public void cancelAll() {
        skipProceedCallCount.incrementAndGet();
        // assemble tasks
        List<DownloadTask> taskList = new ArrayList<>();
        for (DownloadCall call : readyAsyncCalls) taskList.add(call.task);
        for (DownloadCall call : runningAsyncCalls) taskList.add(call.task);
        for (DownloadCall call : runningSyncCalls) taskList.add(call.task);

        if (!taskList.isEmpty()) {
            DownloadTask[] tasks = new DownloadTask[taskList.size()];
            cancelLocked(taskList.toArray(tasks));
        }

        skipProceedCallCount.decrementAndGet();
    }

    public void cancel(DownloadTask[] tasks) {
        skipProceedCallCount.incrementAndGet();
        cancelLocked(tasks);
        skipProceedCallCount.decrementAndGet();
        processCalls();
    }

    public boolean cancel(DownloadTask task) {
        skipProceedCallCount.incrementAndGet();
        final boolean result = cancelLocked(task);
        skipProceedCallCount.decrementAndGet();
        processCalls();
        return result;
    }

    private synchronized void cancelLocked(DownloadTask[] tasks) {
        final long startCancelTime = SystemClock.uptimeMillis();
        Util.d(TAG, "start cancel bunch task manually: " + tasks.length);

        final List<DownloadCall> needCallbackCalls = new ArrayList<>();
        final List<DownloadCall> needCancelCalls = new ArrayList<>();
        try {
            for (DownloadTask task : tasks) {
                filterCanceledCalls(task, needCallbackCalls, needCancelCalls);
            }
        } finally {
            handleCanceledCalls(needCallbackCalls, needCancelCalls);
            Util.d(TAG,
                    "finish cancel bunch task manually: " + tasks.length + " consume "
                            + (SystemClock.uptimeMillis() - startCancelTime) + "ms");
        }
    }

    private synchronized boolean cancelLocked(DownloadTask task) {
        Util.d(TAG, "cancel manually: " + task.getId());
        final List<DownloadCall> needCallbackCalls = new ArrayList<>();
        final List<DownloadCall> needCancelCalls = new ArrayList<>();

        try {
            filterCanceledCalls(task, needCallbackCalls, needCancelCalls);
        } finally {
            handleCanceledCalls(needCallbackCalls, needCancelCalls);
        }

        return needCallbackCalls.size() > 0 || needCancelCalls.size() > 0;
    }

    private synchronized void filterCanceledCalls(@NonNull DownloadTask task,
                                                  @NonNull List<DownloadCall> needCallbackCalls,
                                                  @NonNull List<DownloadCall> needCancelCalls) {
        for (Iterator<DownloadCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            DownloadCall call = i.next();
            if (call.task == task) {
                if (call.isCanceled() || call.isFinishing()) return;

                i.remove();
                needCallbackCalls.add(call);
                return;
            }
        }

        for (DownloadCall call : runningAsyncCalls) {
            if (call.task == task) {
                needCallbackCalls.add(call);
                needCancelCalls.add(call);
                return;
            }
        }

        for (DownloadCall call : runningSyncCalls) {
            if (call.task == task) {
                needCallbackCalls.add(call);
                needCancelCalls.add(call);
                return;
            }
        }
    }

    private synchronized void handleCanceledCalls(@NonNull List<DownloadCall> needCallbackCalls,
                                                  @NonNull List<DownloadCall> needCancelCalls) {
        Util.d(TAG, "handle cancel calls, cancel calls: " + needCancelCalls.size());
        if (!needCancelCalls.isEmpty()) {
            ArrayList<Integer> idList = new ArrayList<>(needCancelCalls.size());

            for (DownloadCall call : needCancelCalls) {
                if (call.cancel()) {
                    idList.add(call.task.getId());
                } else {
                    needCallbackCalls.remove(call);
                }
            }

            // bunch ids of task which need to be canceled
            if (idList.size() == 1) {
                store.onTaskEnd(idList.get(0), EndCause.CANCELED, null);
            } else {
                int[] ids = new int[idList.size()];
                for (int i = 0; i < idList.size(); i++) ids[i] = idList.get(i);
                if (ids.length > 0) store.bunchTaskCanceled(ids);
            }
        }

        Util.d(TAG, "handle cancel calls, callback cancel event: " + needCallbackCalls.size());
        if (!needCallbackCalls.isEmpty()) {
            for (DownloadCall call : needCallbackCalls) {
                OkDownload.with().callbackDispatcher().dispatch().taskEnd(call.task,
                        EndCause.CANCELED,
                        null);
            }
        }
    }

    @Nullable public synchronized DownloadTask findSameTask(DownloadTask task) {
        Util.d(TAG, "findSameTask: " + task.getId());
        for (DownloadCall call : readyAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.task.equals(task)) return call.task;
        }

        for (DownloadCall call : runningAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.task.equals(task)) return call.task;
        }

        for (DownloadCall call : runningSyncCalls) {
            if (call.isCanceled()) continue;
            if (call.task.equals(task)) return call.task;
        }

        return null;
    }

    public synchronized boolean isRunning(DownloadTask task) {
        Util.d(TAG, "isRunning: " + task.getId());
        for (DownloadCall call : runningSyncCalls) {
            if (call.isCanceled()) continue;
            if (call.task.equals(task)) {
                return true;
            }
        }

        for (DownloadCall call : runningAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.task.equals(task)) {
                return true;
            }
        }

        return false;
    }

    public synchronized boolean isPending(DownloadTask task) {
        Util.d(TAG, "isPending: " + task.getId());
        for (DownloadCall call : readyAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.task.equals(task)) return true;
        }

        return false;
    }

    // this method convenient for unit-test.
    void syncRunCall(DownloadCall call) {
        call.run();
    }

    public synchronized void flyingCanceled(DownloadCall call) {
        Util.d(TAG, "flying canceled: " + call.task.getId());
        if (call.asyncExecuted) flyingCanceledAsyncCallCount++;
    }

    public synchronized void finish(DownloadCall call) {
        final boolean asyncExecuted = call.asyncExecuted;
        final Collection<DownloadCall> calls = asyncExecuted ? runningAsyncCalls : runningSyncCalls;
        if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
        if (asyncExecuted && call.isCanceled()) flyingCanceledAsyncCallCount--;

        if (asyncExecuted) processCalls();
    }

    public synchronized boolean isFileConflictAfterRun(@NonNull DownloadTask task) {
        Util.d(TAG, "is file conflict after run: " + task.getId());
        final File file = task.getFile();
        if (file == null) return false;

        // Other one is running, cancel the current task.
        for (DownloadCall syncCall : runningSyncCalls) {
            if (syncCall.isCanceled() || syncCall.task == task) continue;

            final File otherFile = syncCall.task.getFile();
            if (otherFile != null && file.equals(otherFile)) {
                return true;
            }
        }

        for (DownloadCall asyncCall : runningAsyncCalls) {
            if (asyncCall.isCanceled() || asyncCall.task == task) continue;

            final File otherFile = asyncCall.task.getFile();
            if (otherFile != null && file.equals(otherFile)) {
                return true;
            }
        }

        return false;
    }

    private boolean inspectForConflict(DownloadTask task) {
        return inspectForConflict(task, readyAsyncCalls)
                || inspectForConflict(task, runningAsyncCalls)
                || inspectForConflict(task, runningSyncCalls);
    }

    boolean inspectCompleted(DownloadTask task) {
        if (task.isPassIfAlreadyCompleted() && StatusUtil.isCompleted(task)) {
            if (task.getFile() == null && !OkDownload.with().downloadStrategy()
                    .validFilenameFromStore(task)) {
                return false;
            }

            OkDownload.with().callbackDispatcher().dispatch()
                    .taskEnd(task, EndCause.COMPLETED, null);
            return true;
        }

        return false;
    }

    private boolean inspectForConflict(DownloadTask task, Collection<DownloadCall> calls) {
        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();
        for (DownloadCall call : calls) {
            if (call.isCanceled()) continue;

            if (call.task.equals(task)) {
                callbackDispatcher.dispatch()
                        .taskEnd(task, EndCause.SAME_TASK_BUSY, null);
                return true;
            }

            final File file = call.task.getFile();
            final File taskFile = task.getFile();
            if (file != null && taskFile != null && file.equals(taskFile)) {
                callbackDispatcher.dispatch().taskEnd(task, EndCause.FILE_BUSY, null);
                return true;
            }
        }

        return false;
    }

    private synchronized void processCalls() {
        if (skipProceedCallCount.get() > 0) return;
        if (runningAsyncSize() >= maxParallelRunningCount) return;
        if (readyAsyncCalls.isEmpty()) return;

        for (Iterator<DownloadCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            DownloadCall call = i.next();

            i.remove();

            final DownloadTask task = call.task;
            if (isFileConflictAfterRun(task)) {
                OkDownload.with().callbackDispatcher().dispatch().taskEnd(task, EndCause.FILE_BUSY,
                        null);
                continue;
            }

            runningAsyncCalls.add(call);
            executorService().execute(call);

            if (runningAsyncSize() >= maxParallelRunningCount) return;
        }
    }

    private int runningAsyncSize() {
        return runningAsyncCalls.size() - flyingCanceledAsyncCallCount;
    }

    public static void setMaxParallelRunningCount(int maxParallelRunningCount) {
        DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();
        if (dispatcher.getClass() != DownloadDispatcher.class) {
            throw new IllegalStateException(
                    "The current dispatcher is " + dispatcher + " not DownloadDispatcher exactly!");
        }

        maxParallelRunningCount = Math.max(1, maxParallelRunningCount);
        dispatcher.maxParallelRunningCount = maxParallelRunningCount;
    }
}
