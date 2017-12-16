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
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;
import com.liulishuo.okdownload.sample.base.BaseSampleActivity;
import com.liulishuo.okdownload.sample.util.EachBlockProgressUtil;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * On this demo we will download a task and display each block progress for it.
 */
public class EachBlockProgressActivity extends BaseSampleActivity {

    private DownloadTask task;
    private static final String TAG = "EachBlockProgress";

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_each_block_progress);

        initEachBlockProgress((TextView) findViewById(R.id.statusTv),
                (TextView) findViewById(R.id.extInfoTv),
                findViewById(R.id.actionView), (TextView) findViewById(R.id.actionTv),
                findViewById(R.id.startSameTaskView), findViewById(R.id.startSameFileView),
                (ProgressBar) findViewById(R.id.taskPb),
                (ProgressBar) findViewById(R.id.block0Pb),
                (ProgressBar) findViewById(R.id.block1Pb),
                (ProgressBar) findViewById(R.id.block2Pb),
                (ProgressBar) findViewById(R.id.block3Pb),
                (TextView) findViewById(R.id.taskTitleTv),
                (TextView) findViewById(R.id.block0TitleTv),
                (TextView) findViewById(R.id.block1TitleTv),
                (TextView) findViewById(R.id.block2TitleTv),
                (TextView) findViewById(R.id.block3TitleTv),
                (TextView) findViewById(R.id.taskSpeedTv),
                (TextView) findViewById(R.id.block0SpeedTv),
                (TextView) findViewById(R.id.block1SpeedTv),
                (TextView) findViewById(R.id.block2SpeedTv),
                (TextView) findViewById(R.id.block3SpeedTv));
    }

    @Override public int titleRes() {
        return R.string.each_block_progress_title;
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (task != null) task.cancel();
    }

    private void initEachBlockProgress(TextView statusTv, TextView extInfoTv,
                                       View actionView, TextView actionTv,
                                       View startSameTaskView, View startSameFileView,
                                       ProgressBar taskPb,
                                       ProgressBar block0Pb, ProgressBar block1Pb,
                                       ProgressBar block2Pb, ProgressBar block3Pb,
                                       TextView taskTitleTv,
                                       TextView block0TitleTv, TextView block1TitleTv,
                                       TextView block2TitleTv, TextView block3TitleTv,
                                       TextView taskSpeedTv,
                                       TextView block0SpeedTv, TextView block1SpeedTv,
                                       TextView block2SpeedTv, TextView block3SpeedTv) {
        initTask();

        initStatus(statusTv,
                taskPb, block0Pb, block1Pb, block2Pb, block3Pb,
                taskTitleTv, block0TitleTv, block1TitleTv, block2TitleTv, block3TitleTv);

        initAction(statusTv, extInfoTv,
                actionView, actionTv,
                startSameTaskView, startSameFileView,
                taskPb, block0Pb, block1Pb, block2Pb, block3Pb,
                taskTitleTv, block0TitleTv, block1TitleTv, block2TitleTv, block3TitleTv,
                taskSpeedTv, block0SpeedTv, block1SpeedTv, block2SpeedTv, block3SpeedTv);
    }

    private void initTask() {
        task = EachBlockProgressUtil.createTask(this);
    }

    private void initStatus(TextView statusTv,
                            ProgressBar taskPb,
                            ProgressBar block0Pb, ProgressBar block1Pb,
                            ProgressBar block2Pb, ProgressBar block3Pb,
                            TextView taskTitleTv,
                            TextView block0TitleTv, TextView block1TitleTv,
                            TextView block2TitleTv, TextView block3TitleTv) {
        final StatusUtil.Status status = StatusUtil.getStatus(task);
        statusTv.setText(status.toString());

        final BreakpointInfo info = StatusUtil.getCurrentInfo(task);
        if (info != null) {
            Log.d(TAG, "init status with: " + info.toString());

            EachBlockProgressUtil
                    .initTitle(info, taskTitleTv, block0TitleTv, block1TitleTv, block2TitleTv,
                            block3TitleTv);
            EachBlockProgressUtil
                    .initProgress(info, taskPb, block0Pb, block1Pb, block2Pb, block3Pb);
        }
    }

    private void initAction(final TextView statusTv, final TextView extInfoTv,
                            final View actionView, final TextView actionTv,
                            View startSameTaskView, View startSameFileView,
                            final ProgressBar taskPb,
                            final ProgressBar block0Pb, final ProgressBar block1Pb,
                            final ProgressBar block2Pb, final ProgressBar block3Pb,
                            final TextView taskTitleTv,
                            final TextView block0TitleTv, final TextView block1TitleTv,
                            final TextView block2TitleTv, final TextView block3TitleTv,
                            final TextView taskSpeedTv,
                            final TextView block0SpeedTv, final TextView block1SpeedTv,
                            final TextView block2SpeedTv, final TextView block3SpeedTv) {

        actionTv.setText(R.string.start);

        // start or cancel
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final boolean started = task.getTag() != null;

                if (started) {
                    // to cancel
                    task.cancel();
                } else {
                    actionTv.setText(R.string.cancel);

                    // to start
                    startTask(statusTv, actionTv,
                            taskPb,
                            block0Pb, block1Pb, block2Pb, block3Pb,
                            taskTitleTv, block0TitleTv, block1TitleTv, block2TitleTv, block3TitleTv,
                            taskSpeedTv,
                            block0SpeedTv, block1SpeedTv, block2SpeedTv, block3SpeedTv);
                    // mark
                    task.setTag("mark-task-started");
                }
            }
        });

        // start same task -- same-busy
        startSameTaskView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final boolean started = task.getTag() != null;
                if (!started) return;

                final DownloadTask task = EachBlockProgressUtil
                        .createTask(EachBlockProgressActivity.this);
                task.enqueue(EachBlockProgressUtil.createSampleListener(extInfoTv));
            }
        });

        // start same file -- file-busy
        startSameFileView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final boolean started = task.getTag() != null;
                if (!started) return;

                final DownloadTask sameFileAnotherUrlTask = EachBlockProgressUtil
                        .createSameFileAnotherUrlTask(EachBlockProgressActivity.this);
                sameFileAnotherUrlTask
                        .enqueue(EachBlockProgressUtil.createSampleListener(extInfoTv));
            }
        });
    }

    private void startTask(final TextView statusTv, final TextView actionTv,
                           final ProgressBar taskPb,
                           final ProgressBar block0Pb, final ProgressBar block1Pb,
                           final ProgressBar block2Pb, final ProgressBar block3Pb,
                           final TextView taskTitleTv,
                           final TextView block0TitleTv, final TextView block1TitleTv,
                           final TextView block2TitleTv, final TextView block3TitleTv,
                           final TextView taskSpeedTv,
                           final TextView block0SpeedTv, final TextView block1SpeedTv,
                           final TextView block2SpeedTv, final TextView block3SpeedTv) {
        task.enqueue(new DownloadListener4WithSpeed() {
            @Override public void taskStart(DownloadTask task) {
                statusTv.setText(R.string.task_start);
            }

            @Override protected void infoReady(DownloadTask task, @NonNull BreakpointInfo info) {
                EachBlockProgressUtil
                        .initTitle(info, taskTitleTv, block0TitleTv, block1TitleTv, block2TitleTv,
                                block3TitleTv);
                EachBlockProgressUtil
                        .initProgress(info, taskPb, block0Pb, block1Pb, block2Pb, block3Pb);
            }

            @Override
            protected void progressBlock(DownloadTask task, int blockIndex,
                                         long currentBlockOffset) {
                final ProgressBar progressBar = EachBlockProgressUtil
                        .getProgressBar(blockIndex, block0Pb, block1Pb, block2Pb, block3Pb);

                EachBlockProgressUtil.updateProgress(progressBar, currentBlockOffset);

                final TextView speedTv = EachBlockProgressUtil.getSpeedTv(blockIndex,
                        block0SpeedTv, block1SpeedTv, block2SpeedTv, block3SpeedTv);

                if (speedTv != null) speedTv.setText(blockSpeed(blockIndex).speed());
            }

            @Override protected void progress(DownloadTask task, long currentOffset) {
                statusTv.setText(R.string.fetch_progress);

                EachBlockProgressUtil.updateProgress(taskPb, currentOffset);
                taskSpeedTv.setText(taskSpeed().speed());
            }

            @Override protected void blockEnd(DownloadTask task, int blockIndex, BlockInfo info) {
                final TextView speedTv = EachBlockProgressUtil.getSpeedTv(blockIndex,
                        block0SpeedTv, block1SpeedTv, block2SpeedTv, block3SpeedTv);

                if (speedTv != null) speedTv.setText(blockSpeed(blockIndex).speedFromBegin());
            }

            @Override public void connectStart(DownloadTask task, int blockIndex,
                                               @NonNull Map<String, List<String>> requestHeaders) {
                final String status = "connectStart " + blockIndex + " " + requestHeaders;
                statusTv.setText(status);
            }

            @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                             @NonNull Map<String, List<String>> responseHeaders) {
                final String status = "connectEnd " + blockIndex + " " + responseCode + " "
                        + responseHeaders;
                statusTv.setText(status);

            }

            @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
                super.splitBlockEnd(task, info);
                final String status = "splitBlockEnd " + info.getBlockCount() + " " + info
                        .getTotalLength();
                statusTv.setText(status);
            }

            @Override
            public void fetchStart(DownloadTask task, int blockIndex, long contentLength) {
                super.fetchStart(task, blockIndex, contentLength);
                final String status = "fetchStart " + blockIndex + " " + contentLength;
                statusTv.setText(status);
            }

            @Override
            protected void taskEnd(DownloadTask task, EndCause cause,
                                   @android.support.annotation.Nullable Exception realCause,
                                   @NonNull String averageSpeed) {
                statusTv.setText(cause.toString());
                taskSpeedTv.setText(averageSpeed);

                actionTv.setText(R.string.start);
                // mark
                task.setTag(null);
            }
        });
    }

}
