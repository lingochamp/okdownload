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

package com.liulishuo.okdownload.sample.single;

import android.content.Context;
import android.support.annotation.NonNull;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.UnifiedListenerManager;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.sample.DemoUtil;

import java.io.File;

public class SingleTaskDemo {

    @NonNull private final UnifiedListenerManager listenerManager;
    @NonNull private final SingleTaskListener listener;
    @NonNull private DownloadTask task;

    private final String demoUrl;
    @NonNull private final File parentFile;
    @NonNull private final String filename;

    private SingleTaskViewAdapter viewAdapter;

    public SingleTaskDemo(@NonNull Context context,
                          @NonNull UnifiedListenerManager listenerManager) {
        this.listenerManager = listenerManager;
        this.parentFile = DemoUtil.getParentFile(context);
        this.filename = SingleTaskUtil.FILENAME;
        this.demoUrl = SingleTaskUtil.URL;
        this.task = SingleTaskUtil.createTask(demoUrl, parentFile);
        this.listener = new SingleTaskListener(viewAdapter, true);
    }

    public void detachViews() {
        this.listener.detach();
    }

    public void attachViews(@NonNull SingleTaskViewAdapter viewAdapter,
                            SingleTaskListener.FinishListener finishListener) {
        this.viewAdapter = viewAdapter;

        final BreakpointInfo info = StatusUtil.getCurrentInfo(task);

        listener.reattach(viewAdapter, finishListener);
        listenerManager.attachListener(task, listener);

        if (info != null) viewAdapter.refreshData(info, listener.getBlockInstantOffsetMap());
    }

    public void updateStatus() {
        viewAdapter.setExtInfo(
                StatusUtil.getStatus(demoUrl, parentFile.getAbsolutePath(), filename).name());
    }

    public boolean isTaskPendingOrRunning() {
        return StatusUtil.isSameTaskPendingOrRunning(task);
    }

    // start task asynchronously
    public void startAsync() {
        if (StatusUtil.isSameTaskPendingOrRunning(task)) return;

        listenerManager.enqueueTaskWithUnifiedListener(task, listener);
    }

    // start task with the same path
    public void startSamePathTask_fileBusy() {
        final String otherUrl = "http://dldir1.qq.com/weixin/android/seixin6516android1120.apk";
        DownloadTask.Builder builder = new DownloadTask.Builder(otherUrl, parentFile);
        final DownloadTask task = builder
                .setAutoCallbackToUIThread(false)
                // same filename to #startAsync
                .setFilename(filename)
                .build();

        task.enqueue(new SingleTaskListener(this.viewAdapter));

    }

    // start task with the same path and same url
    public void startSameTask_sameTaskBusy() {
        DownloadTask.Builder builder = new DownloadTask.Builder(demoUrl, parentFile);
        DownloadTask task = builder
                .setAutoCallbackToUIThread(false)
                .build();

        task.enqueue(new SingleTaskListener(this.viewAdapter));
    }

    // cancel task
    public void cancelTask() {
        final DownloadTask task = OkDownload.with().downloadDispatcher().findSameTask(this.task);
        if (task == null) return;

        task.cancel();
    }
}
