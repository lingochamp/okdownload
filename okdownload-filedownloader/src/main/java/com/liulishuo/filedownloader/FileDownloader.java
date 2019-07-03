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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.liulishuo.okdownload.DownloadContext;
import com.liulishuo.okdownload.DownloadContextListener;
import com.liulishuo.okdownload.DownloadMonitor;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.connection.DownloadOkHttp3Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class FileDownloader {

    private static final String TAG = "FileDownloader";

    private static final class HolderClass {
        private static final FileDownloader INSTANCE = new FileDownloader();
    }

    public static FileDownloader getImpl() {
        return HolderClass.INSTANCE;
    }

    public static void init(@NonNull Context context) {
        init(context, null);
    }

    public static void init(
            @NonNull Context context,
            @Nullable FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker) {
        init(context, okHttpClientCustomMaker, 0);
    }

    public static void init(
            @NonNull Context context,
            @Nullable final FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker,
            int maxNetworkThreadCount) {
        setup(context);
        OkDownload.Builder builder = null;
        final OkHttpClient okHttpClient;
        if (okHttpClientCustomMaker == null) {
            okHttpClient = null;
        } else {
            okHttpClient = okHttpClientCustomMaker.customMake();
        }
        if (okHttpClient != null) {
            builder = new OkDownload.Builder(context);
            builder.connectionFactory(new DownloadConnection.Factory() {
                @Override
                public DownloadConnection create(String url) throws IOException {
                    Util.d(TAG, "create a okhttp connection with " + url);
                    return new DownloadOkHttp3Connection.Factory()
                            .setBuilder(okHttpClient.newBuilder())
                            .create(url);
                }
            });
        }
        final DownloadMonitor downloadMonitor = FileDownloadMonitor.getDownloadMonitor();
        if (downloadMonitor != null) {
            if (builder == null) builder = new OkDownload.Builder(context);
            builder.monitor(downloadMonitor);
        }
        if (builder != null) OkDownload.setSingletonInstance(builder.build());
    }

    /**
     * You can invoke this method anytime before you using the FileDownloader.
     * <p>
     *
     * @param context the context of Application or Activity etc..
     */
    public static void setup(@NonNull Context context) {
        FileDownloadHelper.holdContext(context.getApplicationContext());
    }


    public DownloadTaskAdapter create(String url) {
        return new DownloadTaskAdapter(url);
    }


    /**
     * Start the download queue by the same listener.
     *
     * @param listener Used to assemble tasks which is bound by the same {@code listener}
     * @param isSerial Whether start tasks one by one rather than parallel.
     * @return {@code true} if start tasks successfully.
     */
    public boolean start(final FileDownloadListener listener, final boolean isSerial) {
        if (listener == null) {
            Util.w(TAG, "Tasks with the listener can't start, because the listener "
                    + "provided is null: [null, " + isSerial + "]");
            return false;
        }

        List<DownloadTaskAdapter> originalTasks =
                FileDownloadList.getImpl().assembleTasksToStart(listener);
        if (originalTasks.isEmpty()) {
            Util.w(TAG, "no task for listener: " + listener + " to start");
            return false;
        }

        ArrayList<DownloadTask> downloadTasks = new ArrayList<>();
        for (DownloadTaskAdapter task : originalTasks) {
            downloadTasks.add(task.getDownloadTask());
        }
        final DownloadContext downloadContext =
                new DownloadContext.Builder(new DownloadContext.QueueSet(), downloadTasks)
                        .setListener(new DownloadContextListener() {
                            @Override
                            public void taskEnd(@NonNull DownloadContext context,
                                                @NonNull DownloadTask task,
                                                @NonNull EndCause cause,
                                                @Nullable Exception realCause,
                                                int remainCount) {
                                Util.d(TAG, "task " + task.getId() + "end");
                                final DownloadTaskAdapter downloadTaskAdapter =
                                        FileDownloadUtils.findDownloadTaskAdapter(task);
                                if (downloadTaskAdapter != null) {
                                    FileDownloadList.getImpl().remove(downloadTaskAdapter);
                                }
                            }

                            @Override
                            public void queueEnd(@NonNull DownloadContext context) {
                                Util.d(TAG, "queue end");
                            }
                        })
                        .build();

        final CompatListenerAdapter compatListenerAdapter = CompatListenerAdapter.create(listener);
        downloadContext.start(compatListenerAdapter, isSerial);
        return true;
    }

    @Deprecated
    public void bindService() {
        // do nothing
    }

    @Deprecated
    public void unbindService() {
        // do nothing
    }

    @Deprecated
    public void unBindServiceIfIdle() {
        // do nothing
    }

    @Deprecated
    public boolean isServiceConnected() {
        return true;
    }

    @Deprecated
    public void addServiceConnectListener(FileDownloadConnectListener connectListener) {
        // do nothing
    }

    @Deprecated
    public void removeServiceConnectListener(FileDownloadConnectListener connectListener) {
        // do nothing
    }

    public void pause(final FileDownloadListener listener) {
        final List<DownloadTaskAdapter> taskAdapters =
                FileDownloadList.getImpl().getByFileDownloadListener(listener);
        final DownloadTask[] downloadTasks = new DownloadTask[taskAdapters.size()];
        for (int i = 0; i < taskAdapters.size(); i++) {
            downloadTasks[i] = taskAdapters.get(i).getDownloadTask();
        }
        OkDownload.with().downloadDispatcher().cancel(downloadTasks);
    }

    public void pauseAll() {
        OkDownload.with().downloadDispatcher().cancelAll();
    }

    public int pause(final int id) {
        OkDownload.with().downloadDispatcher().cancel(id);
        return 0;
    }
}
