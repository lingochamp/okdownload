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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend;
import com.liulishuo.okdownload.sample.base.BaseSampleActivity;
import com.liulishuo.okdownload.sample.comprehensive.single.SingleTaskUtil;
import com.liulishuo.okdownload.sample.util.DemoUtil;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class NotificationActivity extends BaseSampleActivity {

    private NotificationCompat.Builder builder;
    private NotificationManager manager;
    private CancelReceiver cancelReciver;

    private DownloadTask task;
    private TextView actionTv;
    private View actionView;

    @Override public int titleRes() {
        return R.string.title_notification;
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        actionTv = findViewById(R.id.actionTv);
        actionView = findViewById(R.id.actionView);


        // for cancel action on notification.
        IntentFilter filter = new IntentFilter(CancelReceiver.ACTION);
        cancelReciver = new CancelReceiver();
        registerReceiver(cancelReciver, filter);

        initTask();
        initNotification();
        initAction();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cancelReciver);
    }

    private void initAction() {
        actionTv.setText(R.string.start);
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (v.getTag() == null) {
                    // need to start
                    enqueueTask();

                    actionTv.setText(R.string.cancel);
                    v.setTag(new Object());
                } else {
                    // need to cancel
                    task.cancel();
                }
            }
        });
    }

    private void initTask() {
        task = new DownloadTask
                .Builder(SingleTaskUtil.URL, DemoUtil.getParentFile(this))
                .setFilename("notification-file.apk")
                // if there is the same task has been completed download, just delete it and
                // re-download automatically.
                .setPassIfAlreadyCompleted(false)
                .setMinIntervalMillisCallbackProcess(80)
                // because for the notification we don't need make sure invoke on the ui thread, so
                // just let callback no need callback to the ui thread.
                .setAutoCallbackToUIThread(false)
                .build();
    }

    private void initNotification() {
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        final String channelId = "okdownload";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "OkDownloadSample",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        builder = new NotificationCompat.Builder(this, channelId);

        final Intent intent = new Intent(CancelReceiver.ACTION);
        final PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setDefaults(Notification.DEFAULT_LIGHTS)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle("OkDownloadSample")
                .setContentText("Download a task showing on notification sample")
                .addAction(new NotificationCompat.Action(0, "Cancel", cancelPendingIntent))
                .setSmallIcon(R.mipmap.ic_launcher);
    }

    private void enqueueTask() {
        task.enqueue(new DownloadListener4WithSpeed() {
            int totalLength;

            @Override public void taskStart(@NonNull DownloadTask task) {
                Log.d("NotificationActivity", "taskStart");
                builder.setTicker("taskStart");
                builder.setOngoing(true);
                builder.setAutoCancel(false);
                builder.setContentText("The task is started");
                builder.setProgress(0, 0, true);
                manager.notify(task.getId(), builder.build());
            }

            @Override
            public void connectStart(@NonNull DownloadTask task, int blockIndex,
                                     @NonNull Map<String, List<String>> requestHeaderFields) {
                builder.setTicker("connectStart");
                builder.setContentText(
                        "The connect of " + blockIndex + " block for this task is connecting");
                builder.setProgress(0, 0, true);
                manager.notify(task.getId(), builder.build());
            }

            @Override
            public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode,
                                   @NonNull Map<String, List<String>> responseHeaderFields) {
                builder.setTicker("connectStart");
                builder.setContentText(
                        "The connect of " + blockIndex + " block for this task is connected");
                builder.setProgress(0, 0, true);
                manager.notify(task.getId(), builder.build());
            }

            @Override
            public void infoReady(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                  boolean fromBreakpoint,
                                  @NonNull Listener4SpeedAssistExtend.Listener4SpeedModel model) {
                Log.d("NotificationActivity", "infoReady " + info + " " + fromBreakpoint);

                if (fromBreakpoint) {
                    builder.setTicker("fromBreakpoint");
                } else {
                    builder.setTicker("fromBeginning");
                }
                builder.setContentText(
                        "This task is download fromBreakpoint[" + fromBreakpoint + "]");
                builder.setProgress((int) info.getTotalLength(), (int) info.getTotalOffset(), true);
                manager.notify(task.getId(), builder.build());

                totalLength = (int) info.getTotalLength();
            }

            @Override
            public void progressBlock(@NonNull DownloadTask task, int blockIndex,
                                      long currentBlockOffset,
                                      @NonNull SpeedCalculator blockSpeed) {
            }

            @Override public void progress(@NonNull DownloadTask task, long currentOffset,
                                           @NonNull SpeedCalculator taskSpeed) {
                Log.d("NotificationActivity", "progress " + currentOffset);

                builder.setContentText("downloading with speed: " + taskSpeed.speed());
                builder.setProgress(totalLength, (int) currentOffset, false);
                manager.notify(task.getId(), builder.build());
            }

            @Override
            public void blockEnd(@NonNull DownloadTask task, int blockIndex, BlockInfo info,
                                 @NonNull SpeedCalculator blockSpeed) {
            }

            @Override public void taskEnd(@NonNull final DownloadTask task, @NonNull EndCause cause,
                                          @android.support.annotation.Nullable Exception realCause,
                                          @NonNull SpeedCalculator taskSpeed) {
                Log.d("NotificationActivity", "taskEnd " + cause + " " + realCause);
                builder.setOngoing(false);
                builder.setAutoCancel(true);

                builder.setTicker("taskEnd " + cause);
                builder.setContentText(
                        "task end " + cause + " average speed: " + taskSpeed.averageSpeed());

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override public void run() {
                        actionTv.setText(R.string.start);
                        actionView.setTag(null);

                        manager.notify(task.getId(), builder.build());
                    }
                    // because of on some android phone too frequency notify for same id would be
                    // ignored.
                }, 100);
            }
        });
    }

    public class CancelReceiver extends BroadcastReceiver {
        static final String ACTION = "cancelOkdownload";

        @Override public void onReceive(Context context, Intent intent) {
            task.cancel();
        }
    }
}
