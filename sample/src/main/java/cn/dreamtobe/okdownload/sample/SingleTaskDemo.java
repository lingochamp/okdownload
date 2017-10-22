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

package cn.dreamtobe.okdownload.sample;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.cause.EndCause;
import cn.dreamtobe.okdownload.core.cause.ResumeFailedCause;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;

public class SingleTaskDemo {

    private static final String TAG = "SingleTaskDemo";

    private DownloadTask task;
    private final String demoUrl = "https://t.alipayobjects.com/L1/71/100/and/alipay_wap_main.apk";

    public void startAsync(Context context, @NonNull final FinishListener listener) {
        if (task != null) return;

        DownloadTask.Builder builder = new DownloadTask.Builder(demoUrl,
                Uri.fromFile(context.getExternalCacheDir()));
        task = builder
                .setAutoCallbackToUIThread(false)
                .build();

        task.enqueue(new SingleTaskListener() {
            @Override public void taskEnd(DownloadTask task, EndCause cause,
                                          @Nullable Exception realCause) {
                super.taskEnd(task, cause, realCause);
                SingleTaskDemo.this.task = null;
                listener.finish();
            }
        });
    }

    public void startSamePathTask_fileBusy(Uri path) {
        final String otherUrl = "http://dldir1.qq.com/weixin/android/seixin6516android1120.apk";
        DownloadTask.Builder builder = new DownloadTask.Builder(otherUrl, path);
        final DownloadTask task = builder
                .setAutoCallbackToUIThread(false)
                // same filename to #startAsync
                .setFilename("alipay_wap_main.apk")
                .build();

        task.enqueue(new SingleTaskListener());

    }

    public void startSameTask_sameTaskBusy(Uri path) {
        DownloadTask.Builder builder = new DownloadTask.Builder(demoUrl, path);
        DownloadTask task = builder
                .setAutoCallbackToUIThread(false)
                .build();

        task.enqueue(new SingleTaskListener());
    }

    public void cancelTask() {
        final DownloadTask task = this.task;
        if (task == null) return;

        task.cancel();
    }

    private class SingleTaskListener implements DownloadListener {
        @Override public void taskStart(DownloadTask task) {
            Log.d(TAG, "taskStart " + task.getId());
        }

        @Override public void breakpointData(DownloadTask task,
                                             @Nullable BreakpointInfo info) {
            Log.d(TAG, "breakpointData " + info);

        }

        @Override public void downloadFromBeginning(DownloadTask task,
                                                    BreakpointInfo info,
                                                    ResumeFailedCause cause) {
            Log.d(TAG, "downloadFromBeginning " + info + " " + cause);
        }

        @Override public void downloadFromBreakpoint(DownloadTask task,
                                                     BreakpointInfo info) {
            Log.d(TAG, "downloadFromBreakpoint " + info);
        }

        @Override public void connectStart(DownloadTask task, int blockIndex,
                                           DownloadConnection connection) {
            Log.d(TAG,
                    "connectStart " + blockIndex + " " + connection.getRequestProperties());

        }

        @Override public void connectEnd(DownloadTask task, int blockIndex,
                                         DownloadConnection.Connected connected) {
            Log.d(TAG,
                    "connectEnd " + blockIndex + " " + connected.getResponseHeaderFields());

        }


        @Override public void fetchStart(DownloadTask task, int blockIndex,
                                         long contentLength) {
            Log.d(TAG, "fetchStart " + blockIndex + " " + contentLength);

        }

        @Override public void fetchProgress(DownloadTask task, int blockIndex,
                                            long fetchedBytes) {
            Log.d(TAG, "fetchProgress " + blockIndex + " " + fetchedBytes);
        }

        @Override public void fetchEnd(DownloadTask task, int blockIndex,
                                       long contentLength) {
            Log.d(TAG, "fetchEnd " + blockIndex + " " + contentLength);
        }

        @Override public void taskEnd(DownloadTask task, EndCause cause,
                                      @Nullable Exception realCause) {
            Log.d(TAG, "taskEnd " + cause + " " + realCause);
        }
    }

    interface FinishListener {
        void finish();
    }
}
