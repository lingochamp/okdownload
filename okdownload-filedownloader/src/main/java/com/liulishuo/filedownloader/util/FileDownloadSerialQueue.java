/*
 * Copyright (c) 2018 LingoChamp Inc.
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

package com.liulishuo.filedownloader.util;

import android.support.annotation.NonNull;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.DownloadTaskAdapter;
import com.liulishuo.filedownloader.FileDownloadList;
import com.liulishuo.okdownload.DownloadSerialQueue;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.UnifiedListenerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The serial queue, what used to dynamically increase tasks, and tasks in the queue will
 * automatically start download one by one.
 */

public class FileDownloadSerialQueue {

    final DownloadSerialQueue serialQueue;
    final UnifiedListenerManager listenerManager;

    public FileDownloadSerialQueue() {
        this(new DownloadSerialQueue(), new UnifiedListenerManager());
    }

    public FileDownloadSerialQueue(@NonNull DownloadSerialQueue serialQueue,
                                   @NonNull UnifiedListenerManager listenerManager) {
        this.serialQueue = serialQueue;
        this.listenerManager = listenerManager;

        this.serialQueue.setListener(this.listenerManager.getHostListener());
    }

    /**
     * Enqueues the given task sometime in the serial queue. If the {@code task} is in the head of
     * the serial queue, the {@code task} will be started automatically.
     */
    public void enqueue(BaseDownloadTask task) {
        final DownloadTaskAdapter downloadTaskAdapter = (DownloadTaskAdapter) task;
        downloadTaskAdapter.assembleDownloadTask();
        FileDownloadList.getImpl().addIndependentTask(downloadTaskAdapter);
        serialQueue.enqueue(downloadTaskAdapter.getDownloadTask());
        listenerManager.addAutoRemoveListenersWhenTaskEnd(downloadTaskAdapter.getId());
        listenerManager.attachListener(downloadTaskAdapter.getDownloadTask(),
                downloadTaskAdapter.getCompatListener());
    }

    /**
     * Pause the queue.
     *
     * @see #resume()
     */
    public void pause() {
        serialQueue.pause();
    }

    /**
     * Resume the queue if the queue is paused.
     *
     * @see #pause()
     */
    public void resume() {
        serialQueue.resume();
    }

    /**
     * Returns the identify of the working task, if there is task is working, you will receive
     * {@link DownloadSerialQueue#ID_INVALID}.
     *
     * @return the identify of the working task
     */
    public int getWorkingTaskId() {
        return serialQueue.getWorkingTaskId();
    }

    /**
     * Get the count of tasks which is waiting on this queue.
     *
     * @return the count of waiting tasks on this queue.
     */
    public int getWaitingTaskCount() {
        return serialQueue.getWaitingTaskCount();
    }

    /**
     * Attempts to stop the working task, halts the processing of waiting tasks, and returns a list
     * of the tasks that were awaiting execution. These tasks are drained (removed) from the task
     * queue upon return from this method.
     */
    public List<BaseDownloadTask> shutdown() {
        DownloadTask[] tasks = serialQueue.shutdown();
        List<BaseDownloadTask> notRunningTasks = new ArrayList<>();
        for (DownloadTask task : tasks) {
            final DownloadTaskAdapter notRunningTask =
                    FileDownloadUtils.findDownloadTaskAdapter(task);
            if (notRunningTask != null) {
                notRunningTasks.add(notRunningTask);
                FileDownloadList.getImpl().remove(notRunningTask);
            }
        }
        return notRunningTasks;
    }
}
