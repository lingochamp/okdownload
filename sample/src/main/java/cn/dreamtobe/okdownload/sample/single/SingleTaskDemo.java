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

package cn.dreamtobe.okdownload.sample.single;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.StatusUtil;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.cause.EndCause;

public class SingleTaskDemo {

    private SingleTaskListener downloadListener;
    private DownloadTask task;
    private FinishListener finishListener;

    private final String demoUrl = "https://t.alipayobjects.com/L1/71/100/and/alipay_wap_main.apk";
    @NonNull private final File parentFile;
    @NonNull private final String filename;

    private SingleTaskViewAdapter viewAdapter;

    public SingleTaskDemo(Context context) {

        final File externalSaveDir = context.getExternalCacheDir();
        if (externalSaveDir == null) {
            this.parentFile = context.getCacheDir();
        } else {
            this.parentFile = externalSaveDir;
        }
        this.filename = "alipay_wap_main.apk";
    }

    public void detachViews() {
        this.viewAdapter.invalidate();
    }

    public void attachViews(SingleTaskViewAdapter viewAdapter, FinishListener finishListener) {
        this.viewAdapter = viewAdapter;
        this.finishListener = finishListener;

        final SingleTaskListener listener = downloadListener;
        final BreakpointInfo info = StatusUtil
                .getCurrentInfo(demoUrl, parentFile.getAbsolutePath(),
                        filename);
        if (info != null) {
            viewAdapter.refreshData(info,
                    listener != null ? listener.getBlockInstantOffsetMap() : null);
        }

        if (listener != null) listener.reattach(viewAdapter);
    }

    public void updateStatus() {
        viewAdapter.setExtInfo(
                StatusUtil.getStatus(demoUrl, parentFile.getAbsolutePath(), filename).name());
    }

    public boolean isTaskExist() {
        return task != null;
    }

    public void startAsync() {
        if (task != null) return;

        DownloadTask.Builder builder = new DownloadTask.Builder(demoUrl,
                Uri.fromFile(parentFile));
        task = builder
                .setMinIntervalMillisCallbackProcess(150)
                .build();

        this.downloadListener = new SingleTaskListener(viewAdapter, true) {
            @Override public void taskEnd(DownloadTask task, EndCause cause,
                                          @Nullable Exception realCause) {
                super.taskEnd(task, cause, realCause);
                SingleTaskDemo.this.task = null;
                downloadListener = null;
                finishListener.finish();
            }
        };

        task.enqueue(downloadListener);
    }

    public void startSamePathTask_fileBusy() {
        final String otherUrl = "http://dldir1.qq.com/weixin/android/seixin6516android1120.apk";
        DownloadTask.Builder builder = new DownloadTask.Builder(otherUrl, Uri.fromFile(parentFile));
        final DownloadTask task = builder
                .setAutoCallbackToUIThread(false)
                // same filename to #startAsync
                .setFilename(filename)
                .build();

        task.enqueue(new SingleTaskListener(this.viewAdapter));

    }

    public void startSameTask_sameTaskBusy() {
        DownloadTask.Builder builder = new DownloadTask.Builder(demoUrl, Uri.fromFile(parentFile));
        DownloadTask task = builder
                .setAutoCallbackToUIThread(false)
                .build();

        task.enqueue(new SingleTaskListener(this.viewAdapter));
    }

    public void cancelTask() {
        final DownloadTask task = this.task;
        if (task == null) return;

        task.cancel();
    }

    interface FinishListener {
        void finish();
    }
}
