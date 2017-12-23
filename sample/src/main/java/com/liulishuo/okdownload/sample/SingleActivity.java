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

package com.liulishuo.okdownload.sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;
import com.liulishuo.okdownload.sample.base.BaseSampleActivity;
import com.liulishuo.okdownload.sample.util.DemoUtil;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * On this demo you can see the simplest way to download a task.
 */
public class SingleActivity extends BaseSampleActivity {

    private static final String TAG = "SingleActivity";
    private DownloadTask task;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);
        initSingleDownload(
                (TextView) findViewById(R.id.statusTv),
                (ProgressBar) findViewById(R.id.progressBar),
                findViewById(R.id.actionView),
                (TextView) findViewById(R.id.actionTv));
    }

    @Override public int titleRes() {
        return R.string.single_download_title;
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (task != null) task.cancel();
    }

    private void initSingleDownload(TextView statusTv, ProgressBar progressBar, View actionView,
                                    TextView actionTv) {
        initTask();
        initStatus(statusTv, progressBar);
        initAction(actionView, actionTv, statusTv, progressBar);
    }

    private void initTask() {
        final String filename = "single-test";
        final int progressIntervalMillis = 16;
        task = DemoUtil.createTask(this, filename, progressIntervalMillis);
    }

    private void initStatus(TextView statusTv, ProgressBar progressBar) {
        final StatusUtil.Status status = StatusUtil.getStatus(task);
        statusTv.setText(status.toString());
        final BreakpointInfo info = StatusUtil.getCurrentInfo(task);
        if (info != null) {
            Log.d(TAG, "init status with: " + info.toString());

            DemoUtil.calcProgressToView(progressBar, info.getTotalOffset(), info.getTotalLength());
        }
    }

    private void initAction(final View actionView, final TextView actionTv, final TextView statusTv,
                            final ProgressBar progressBar) {
        actionTv.setText(R.string.start);
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final boolean started = task.getTag() != null;

                if (started) {
                    // to cancel
                    task.cancel();
                } else {
                    actionTv.setText(R.string.cancel);

                    // to start
                    startTask(statusTv, progressBar, actionTv);
                    // mark
                    task.setTag("mark-task-started");
                }
            }
        });
    }

    private void startTask(final TextView statusTv, final ProgressBar progressBar,
                           final TextView actionTv) {

        task.enqueue(new DownloadListener4WithSpeed() {
            private long totalLength;
            private String readableTotalLength;

            @Override public void taskStart(DownloadTask task) {
                super.taskStart(task);

                statusTv.setText(R.string.task_start);
            }

            @Override
            protected void taskEnd(DownloadTask task, EndCause cause,
                                   @android.support.annotation.Nullable Exception realCause,
                                   @NonNull String averageSpeed) {
                final String statusWithSpeed = cause.toString() + " " + averageSpeed;
                statusTv.setText(statusWithSpeed);

                actionTv.setText(R.string.start);
                // mark
                task.setTag(null);
            }

            @Override protected void infoReady(DownloadTask task, @NonNull BreakpointInfo info,
                                               boolean fromBreakpoint) {
                statusTv.setText(R.string.info_ready);

                totalLength = info.getTotalLength();
                readableTotalLength = Util.humanReadableBytes(totalLength, true);
                DemoUtil.calcProgressToView(progressBar, info.getTotalOffset(), totalLength);
            }

            @Override
            public void progressBlock(DownloadTask task, int blockIndex,
                                         long currentBlockOffset) {
            }

            @Override public void progress(DownloadTask task, long currentOffset) {
                final String readableOffset = Util.humanReadableBytes(currentOffset, true);
                final String progressStatus = readableOffset + "/" + readableTotalLength;
                final String speed = taskSpeed().speed();
                final String progressStatusWithSpeed = progressStatus + "(" + speed + ")";

                statusTv.setText(progressStatusWithSpeed);
                DemoUtil.calcProgressToView(progressBar, currentOffset, totalLength);
            }

            @Override protected void blockEnd(DownloadTask task, int blockIndex, BlockInfo info) {
            }


            @Override public void connectStart(DownloadTask task, int blockIndex,
                                               @NonNull Map<String, List<String>> requestHeaders) {
                final String status = "Connect Start " + blockIndex;
                statusTv.setText(status);
            }

            @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                             @NonNull Map<String, List<String>> responseHeaders) {
                final String status = "Connect End " + blockIndex;
                statusTv.setText(status);
            }
        });
    }

    private boolean isTaskRunning() {
        final StatusUtil.Status status = StatusUtil.getStatus(task);
        return status == StatusUtil.Status.PENDING || status == StatusUtil.Status.RUNNING;
    }
}
