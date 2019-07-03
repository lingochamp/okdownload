/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.liulishuo.okdownload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStoreOnCache;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener2;
import com.liulishuo.okdownload.core.listener.DownloadListenerBunch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The serial queue, what used to dynamically increase tasks, and tasks in the queue will
 * automatically start download one by one.
 */

public class DownloadSerialQueue extends DownloadListener2 implements Runnable {
    private static final Executor SERIAL_EXECUTOR = new ThreadPoolExecutor(0,
            Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload DynamicSerial", false));

    volatile boolean shutedDown = false;
    volatile boolean looping = false;
    volatile boolean paused = false;

    volatile DownloadTask runningTask;
    private final ArrayList<DownloadTask> taskList;

    static final int ID_INVALID = BreakpointStoreOnCache.FIRST_ID - 1;
    private static final String TAG = "DownloadSerialQueue";

    @NonNull DownloadListenerBunch listenerBunch;

    public DownloadSerialQueue() {
        this(null);
    }

    DownloadSerialQueue(DownloadListener listener, ArrayList<DownloadTask> taskList) {
        listenerBunch = new DownloadListenerBunch.Builder()
                .append(this)
                .append(listener).build();

        this.taskList = taskList;
    }

    public DownloadSerialQueue(DownloadListener listener) {
        this(listener, new ArrayList<DownloadTask>());
    }

    public void setListener(DownloadListener listener) {
        listenerBunch = new DownloadListenerBunch.Builder()
                .append(this)
                .append(listener).build();
    }

    /**
     * Enqueues the given task sometime in the serial queue. If the {@code task} is in the head of
     * the serial queue, the {@code task} will be started automatically.
     */
    public synchronized void enqueue(DownloadTask task) {
        taskList.add(task);
        Collections.sort(taskList);

        if (!paused && !looping) {
            looping = true;
            startNewLooper();
        }
    }

    /**
     * Pause the queue.
     *
     * @see #resume()
     */
    public synchronized void pause() {
        if (paused) {
            Util.w(TAG, "require pause this queue(remain " + taskList.size() + "), but"
                    + "it has already been paused");
            return;
        }
        paused = true;

        if (runningTask != null) {
            runningTask.cancel();
            taskList.add(0, runningTask);
            runningTask = null;
        }
    }

    /**
     * Resume the queue if the queue is paused.
     *
     * @see #pause()
     */
    public synchronized void resume() {
        if (!paused) {
            Util.w(TAG, "require resume this queue(remain " + taskList.size() + "), but it is"
                    + " still running");
            return;
        }
        paused = false;

        if (!taskList.isEmpty() && !looping) {
            looping = true;
            startNewLooper();
        }
    }

    /**
     * Returns the identify of the working task, if there is task is working, you will receive
     * {@link #ID_INVALID}.
     *
     * @return the identify of the working task
     */
    public int getWorkingTaskId() {
        return runningTask != null ? runningTask.getId() : ID_INVALID;
    }

    /**
     * Get the count of tasks which is waiting on this queue.
     *
     * @return the count of waiting tasks on this queue.
     */
    public int getWaitingTaskCount() {
        return taskList.size();
    }

    /**
     * Attempts to stop the working task, halts the processing of waiting tasks, and returns a list
     * of the tasks that were awaiting execution. These tasks are drained (removed) from the task
     * queue upon return from this method.
     */
    public synchronized DownloadTask[] shutdown() {
        shutedDown = true;

        if (runningTask != null) runningTask.cancel();

        final DownloadTask[] tasks = new DownloadTask[taskList.size()];
        taskList.toArray(tasks);
        taskList.clear();

        return tasks;
    }

    @Override public void run() {
        while (!shutedDown) {
            final DownloadTask nextTask;
            synchronized (this) {
                if (taskList.isEmpty() || paused) {
                    runningTask = null;
                    looping = false;
                    break;
                }

                nextTask = taskList.remove(0);
            }

            nextTask.execute(listenerBunch);
        }
    }

    void startNewLooper() {
        SERIAL_EXECUTOR.execute(this);
    }

    @Override public void taskStart(@NonNull DownloadTask task) {
        this.runningTask = task;
    }

    @Override
    public synchronized void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                                     @Nullable Exception realCause) {
        if (cause != EndCause.CANCELED && task == runningTask) runningTask = null;
    }
}
