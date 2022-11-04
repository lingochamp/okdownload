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

package com.liulishuo.filedownloader;

import android.support.annotation.Nullable;

import com.liulishuo.filedownloader.message.MessageSnapshot;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Storing all tasks in processing in the Main-Process.
 */
@SuppressWarnings("UnusedReturnValue")
public class FileDownloadList {

    private static final String TAG = "FileDownloadList";

    private static volatile FileDownloadList singleton;

    public static FileDownloadList getImpl() {
        if (singleton == null) {
            synchronized (FileDownloadList.class) {
                if (singleton == null) {
                    singleton = new FileDownloadList();
                }
            }
        }
        return singleton;
    }

    public static void setSingleton(FileDownloadList singleton) {
        FileDownloadList.singleton = singleton;
    }

    final ArrayList<DownloadTaskAdapter> list;

    FileDownloadList() {
        list = new ArrayList<>();
    }

    @Nullable
    public BaseDownloadTask.IRunningTask get(final int id) {
        synchronized (list) {
            for (DownloadTaskAdapter.IRunningTask task : list) {
                if (task.is(id)) {
                    final DownloadTaskAdapter downloadTaskAdapter =
                            (DownloadTaskAdapter) task.getOrigin();
                    if (OkDownload.with().downloadDispatcher()
                            .isRunning(downloadTaskAdapter.getDownloadTask())) {
                        return task;
                    }
                }
            }
        }
        return null;
    }

    List<DownloadTaskAdapter> getByFileDownloadListener(FileDownloadListener
                                                                fileDownloadListener) {
        final List<DownloadTaskAdapter> tasks = new ArrayList<>();
        synchronized (list) {
            for (DownloadTaskAdapter downloadTaskAdapter : list) {
                if (downloadTaskAdapter.getListener() != null
                        && downloadTaskAdapter.getListener() == fileDownloadListener) {
                    tasks.add(downloadTaskAdapter);
                }
            }
        }
        return tasks;
    }

    /**
     * @param willRemoveDownload will be remove
     */
    @Deprecated
    public boolean remove(final BaseDownloadTask.IRunningTask willRemoveDownload,
                          MessageSnapshot snapshot) {
        if (willRemoveDownload == null) return false;
        final DownloadTaskAdapter downloadTaskAdapter =
                (DownloadTaskAdapter) willRemoveDownload.getOrigin();
        return remove(downloadTaskAdapter);
    }

    public boolean remove(DownloadTaskAdapter downloadTaskAdapter) {
        synchronized (list) {
            Util.d(TAG, "remove task: " + downloadTaskAdapter.getId());
            return list.remove(downloadTaskAdapter);
        }
    }

    /**
     * This method generally used for enqueuing the task which will be assembled by a queue.
     *
     * @see BaseDownloadTask.InQueueTask#enqueue()
     */
    void addQueueTask(final DownloadTaskAdapter task) {
        if (task.isMarkedAdded2List()) {
            Util.w(TAG, "queue task: " + task + " has been marked");
            return;
        }

        synchronized (list) {
            task.markAdded2List();
            task.insureAssembleDownloadTask();
            list.add(task);
            Util.d(TAG, "add list in all " + task + " " + list.size());
        }
    }

    public void addIndependentTask(DownloadTaskAdapter task) {
        if (task.isMarkedAdded2List()) {
            Util.w(TAG, "independent task: " + task.getId() + " has been added to queue");
            return;
        }

        synchronized (list) {
            task.setAttachKeyDefault();
            task.markAdded2List();
            list.add(task);
            Util.d(TAG, "add independent task: " + task.getId());
        }
    }

    List<DownloadTaskAdapter> assembleTasksToStart(FileDownloadListener listener) {
        final List<DownloadTaskAdapter> targetList = new ArrayList<>();
        synchronized (list) {
            // Prevent size changing
            for (DownloadTaskAdapter task : list) {
                if (task.getListener() == listener
                        && !task.isAttached()) {
                    task.setAttachKeyByQueue(listener.hashCode());
                    targetList.add(task);
                }
            }
            return targetList;
        }
    }
}
