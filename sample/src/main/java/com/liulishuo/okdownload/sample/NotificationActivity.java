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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.TextView;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.UnifiedListenerManager;
import com.liulishuo.okdownload.sample.base.BaseSampleActivity;
import com.liulishuo.okdownload.sample.util.DemoUtil;
import com.liulishuo.okdownload.sample.util.NotificationSampleListener;

import org.jetbrains.annotations.Nullable;

public class NotificationActivity extends BaseSampleActivity {

    private CancelReceiver cancelReceiver;

    private DownloadTask task;
    private NotificationSampleListener listener;

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

        initListener();
        initTask();
        initAction();

        // for cancel action on notification.
        IntentFilter filter = new IntentFilter(CancelReceiver.ACTION);
        cancelReceiver = new CancelReceiver(task);
        registerReceiver(cancelReceiver, filter);

        GlobalTaskManager.getImpl().attachListener(task, listener);
        GlobalTaskManager.getImpl().addAutoRemoveListenersWhenTaskEnd(task.getId());
        if (StatusUtil.isSameTaskPendingOrRunning(task)) {
            actionTv.setText(R.string.cancel);
            actionView.setTag(new Object());
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cancelReceiver);
        listener.releaseTaskEndRunnable();
    }

    private void initAction() {
        actionTv.setText(R.string.start);
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (v.getTag() == null) {
                    // need to start
                    GlobalTaskManager.getImpl().enqueueTask(task, listener);

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
                .Builder(DemoUtil.URL, DemoUtil.getParentFile(this))
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


    private void initListener() {
        listener = new NotificationSampleListener(this);
        listener.attachTaskEndRunnable(new Runnable() {
            @Override public void run() {
                actionTv.setText(R.string.start);
                actionView.setTag(null);
            }
        });

        final Intent intent = new Intent(CancelReceiver.ACTION);
        final PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        listener.setAction(new NotificationCompat.Action(0, "Cancel", cancelPendingIntent));
        listener.initNotification();
    }

    static class CancelReceiver extends BroadcastReceiver {
        static final String ACTION = "cancelOkdownload";

        private DownloadTask task;

        CancelReceiver(@NonNull DownloadTask task) {
            this.task = task;
        }

        @Override public void onReceive(Context context, Intent intent) {
            this.task.cancel();
        }
    }

    static class GlobalTaskManager {
        private UnifiedListenerManager manager;

        private GlobalTaskManager() {
            manager = new UnifiedListenerManager();
        }

        private static class ClassHolder {
            private static final GlobalTaskManager INSTANCE = new GlobalTaskManager();
        }

        static GlobalTaskManager getImpl() {
            return ClassHolder.INSTANCE;
        }

        void addAutoRemoveListenersWhenTaskEnd(int id) {
            manager.addAutoRemoveListenersWhenTaskEnd(id);
        }

        void attachListener(@NonNull DownloadTask task, @NonNull DownloadListener listener) {
            manager.attachListener(task, listener);
        }

        void enqueueTask(@NonNull DownloadTask task,
                         @NonNull DownloadListener listener) {
            manager.enqueueTaskWithUnifiedListener(task, listener);
        }
    }
}
