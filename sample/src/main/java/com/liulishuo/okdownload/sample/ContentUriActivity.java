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

package com.liulishuo.okdownload.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener2;
import com.liulishuo.okdownload.sample.base.BaseSampleActivity;
import com.liulishuo.okdownload.sample.util.DemoUtil;
import com.liulishuo.okdownload.sample.util.ProgressUtil;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

public class ContentUriActivity extends BaseSampleActivity {
    @Override public int titleRes() {
        return R.string.title_content_uri;
    }

    private static final int WRITE_REQUEST_CODE = 43;

    private TextView statusTv;
    private ProgressBar progressBar;
    private View actionView;
    private TextView actionTv;
    private TextView filenameTv;

    private DownloadTask task;
    private Uri uri;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_uri);

        initView();

        initAction();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WRITE_REQUEST_CODE && data != null && data.getData() != null) {
            uri = data.getData();
            handleUri(uri);
        } else {
            Snackbar.make(actionView, "data of activity result is not valid", Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (task != null) task.cancel();
    }

    private void initView() {
        statusTv = findViewById(R.id.statusTv);
        progressBar = findViewById(R.id.progressBar);
        actionView = findViewById(R.id.actionView);
        actionTv = findViewById(R.id.actionTv);
        filenameTv = findViewById(R.id.filenameTv);

        statusTv.setText("-");
        actionTv.setText(R.string.choose_file);
    }

    private void initAction() {
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (actionView.getTag() == null) {
                    // choose file
                    if (uri == null) {
                        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);

                        intent.setType("application/apk");
                        intent.putExtra(Intent.EXTRA_TITLE, "liulishuo.apk");
                        startActivityForResult(intent, WRITE_REQUEST_CODE);
                    } else {
                        handleUri(uri);
                    }
                } else {
                    task.cancel();
                }
            }
        });
    }

    private void handleUri(Uri uri) {
        DownloadTask task = new DownloadTask.Builder(DemoUtil.URL, uri)
                .setMinIntervalMillisCallbackProcess(300)
                .build();
        this.task = task;
        filenameTv.setText(task.getFilename());
        task.enqueue(new SampleListener());
    }

    private class SampleListener extends DownloadListener2 {

        private final AtomicLong progress = new AtomicLong();
        private SpeedCalculator speedCalculator;

        @Override
        public void taskStart(@NonNull DownloadTask task) {
            actionTv.setText(R.string.cancel);
            actionView.setTag(new Object());

            statusTv.setText(R.string.start);

            speedCalculator = new SpeedCalculator();
        }

        @Override
        public void downloadFromBeginning(@NonNull DownloadTask task,
                                          @NonNull BreakpointInfo info,
                                          @NonNull ResumeFailedCause cause) {
            super.downloadFromBeginning(task, info, cause);

            progress.set(0);
            ProgressUtil.calcProgressToViewAndMark(progressBar, 0, info.getTotalLength());
        }

        @Override
        public void downloadFromBreakpoint(@NonNull DownloadTask task,
                                           @NonNull BreakpointInfo info) {
            super.downloadFromBreakpoint(task, info);

            progress.set(info.getTotalOffset());
            ProgressUtil.calcProgressToViewAndMark(progressBar, progress.get(),
                    info.getTotalLength());
        }

        @Override
        public void fetchProgress(@NonNull DownloadTask task, int blockIndex,
                                  long increaseBytes) {
            super.fetchProgress(task, blockIndex, increaseBytes);

            speedCalculator.downloading(increaseBytes);
            final String status = "progress " + speedCalculator.speed();
            statusTv.setText(status);

            final long offset = progress.addAndGet(increaseBytes);
            ProgressUtil.updateProgressToViewWithMark(progressBar, offset);
        }

        @Override
        public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                            @Nullable Exception realCause) {
            if (realCause != null) {
                Log.e("ContentUriActivity", "taskEnd with realCause", realCause);
            }

            final String status = "taskEnd " + cause + " " + speedCalculator.averageSpeed();
            statusTv.setText(status);

            actionView.setTag(null);
            actionTv.setText(R.string.start);
        }
    }
}
