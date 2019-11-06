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

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.services.DownloadMgrInitialParams;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.liulishuo.okdownload.DownloadContext;
import com.liulishuo.okdownload.DownloadContextListener;
import com.liulishuo.okdownload.DownloadMonitor;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.connection.DownloadOkHttp3Connection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class FileDownloader {

    private static final String TAG = "FileDownloader";
    private static final String FILEDOWNLOADER_DATABASE_NAME = "filedownloader.db";
    private static final int DEFAULT_INTERVAL = 10;
    private static int globalPost2UIInterval = -1;

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

    /**
     * Initial a OkDownload instance.
     * This method and {@link #setupOnApplicationOnCreate(Application)}
     * cannot be invoked int the same time.
     *
     * @param context                 Android context.
     * @param okHttpClientCustomMaker a maker to build a OkHttpClient.
     * @param maxNetworkThreadCount   the max thread count of thread pool for downloading.
     *                                This parameter is deprecated in OkDownload.
     */
    public static void init(
            @NonNull Context context,
            @Nullable final FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker,
            int maxNetworkThreadCount) {
        setup(context);
        OkDownload.Builder builder = okDownloadBuilder(context, okHttpClientCustomMaker);
        if (builder != null) OkDownload.setSingletonInstance(builder.build());
    }

    @Nullable
    public static OkDownload.Builder okDownloadBuilder(
            @NonNull Context context,
            @Nullable final FileDownloadHelper.OkHttpClientCustomMaker okHttpClientCustomMaker) {
        OkDownload.Builder builder = null;
        final OkHttpClient okHttpClient;
        if (okHttpClientCustomMaker == null) {
            okHttpClient = null;
        } else {
            okHttpClient = okHttpClientCustomMaker.customMake();
        }
        if (okHttpClient != null) {
            builder = new OkDownload.Builder(context);
            builder.connectionFactory(url -> new DownloadOkHttp3Connection.Factory()
                    .setBuilder(okHttpClient.newBuilder())
                    .create(url));
        }
        final DownloadMonitor downloadMonitor = FileDownloadMonitor.getDownloadMonitor();
        if (downloadMonitor != null) {
            if (builder == null) builder = new OkDownload.Builder(context);
            builder.monitor(downloadMonitor);
        }
        return builder;
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

    /**
     * Using this method to setup the FileDownloader only you want to register your own customize
     * components for Filedownloader, otherwise just using {@link #setup(Context)} instead.
     * <p/>
     * Please invoke this method on the {@link Application#onCreate()} because of the customize
     * components must be assigned before FileDownloader is running.
     * <p/>
     * Such as:
     * <p/>
     * class MyApplication extends Application {
     * ...
     * public void onCreate() {
     * ...
     * FileDownloader.setupOnApplicationOnCreate(this)
     * .idGenerator(new MyIdGenerator())
     * .database(new MyDatabase())
     * ...
     * .commit();
     * ...
     * }
     * ...
     * }
     *
     * @param application the application.
     * @return the customize components maker.
     */
    public static DownloadMgrInitialParams.InitCustomMaker setupOnApplicationOnCreate(
            Application application) {
        final Context context = application.getApplicationContext();

        DownloadMgrInitialParams.InitCustomMaker customMaker =
                new DownloadMgrInitialParams.InitCustomMaker(context);
        return customMaker;
    }


    public DownloadTaskAdapter create(String url) {
        final DownloadTaskAdapter taskAdapter = new DownloadTaskAdapter(url);
        if (globalPost2UIInterval > 0) {
            taskAdapter.setCallbackProgressMinInterval(globalPost2UIInterval);
        }
        return taskAdapter;
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

    @Deprecated
    public void startForeground(int id, Notification notification) {
        // do nothing
    }

    @Deprecated
    public void stopForeground(boolean removeNotification) {
        // do nothing
    }

    @Deprecated
    public boolean setMaxNetworkThreadCount(final int count) {
        // do nothing
        return false;
    }



    /**
     * No need to bind any service.
     */
    @Deprecated
    public void bindService(final Runnable runnable) {
        runnable.run();
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

    /**
     * For avoiding missing screen frames.
     * <p/>
     * This mechanism is used for avoid methods in {@link FileDownloadListener} is invoked too
     * frequent in result the system missing screen frames in the main thread.
     * <p>
     */
    public static void setGlobalPost2UIInterval(final int intervalMillisecond) {
        globalPost2UIInterval = intervalMillisecond;
    }

    /**
     * Only use {@link BaseDownloadTask#setCallbackProgressMinInterval(int)} in OkDownload.
     */
    @Deprecated
    public static void setGlobalHandleSubPackageSize(final int packageSize) {
    }

    /**
     * Avoid missing screen frames, this leads to all callbacks in {@link FileDownloadListener} do
     * not be invoked at once when it has already achieved to ensure callbacks don't be too frequent
     */
    public static void enableAvoidDropFrame() {
        setGlobalPost2UIInterval(DEFAULT_INTERVAL);
    }

    /**
     * Disable avoiding missing screen frames, let all callbacks in {@link FileDownloadListener}
     * can be invoked at once when it achieve.
     */
    public static void disableAvoidDropFrame() {
        setGlobalPost2UIInterval(-1);
    }

    /**
     * @return {@code true} if enabled the function of avoiding missing screen frames.
     */
    public static boolean isEnabledAvoidDropFrame() {
        return globalPost2UIInterval > 0;
    }

    /**
     * Get downloaded bytes so far by the downloadId.
     */
    public long getSoFar(final int downloadId) {
        final BreakpointInfo info = OkDownload.with().breakpointStore().get(downloadId);
        if (info == null) return 0;
        return info.getTotalOffset();
    }

    /**
     * Get the total bytes of the target file for the task with the {code id}.
     */
    public long getTotal(final int id) {
        final BreakpointInfo info = OkDownload.with().breakpointStore().get(id);
        if (info == null) return 0;
        return info.getTotalLength();
    }

    /**
     * Clear all data in the filedownloader database.
     * {@link com.liulishuo.okdownload.core.breakpoint.BreakpointStore} doesn't provide clear all
     * data api, and it's no need to clear database manually in fact.
     */
    @Deprecated
    public void clearAllTaskData() {
        pauseAll();
    }

    /**
     * Clear the data with the provided {@code id}.
     * Normally used to deleting the data in OkDownload database, when it is paused or in
     * downloading status. If you want to re-download it clearly.
     * <p/>
     * <strong>Note:</strong> YOU NO NEED to clear the data when it is already completed downloading
     * because the data would be deleted when it completed downloading automatically by
     * OkDownload.
     * <p>
     */
    public boolean clear(final int id, final String targetFilePath) {
        pause(id);
        OkDownload.with().breakpointStore().remove(id);
        final File targetFile = new File(targetFilePath);
        if (targetFile.exists()) {
            //noinspection
            return targetFile.delete();
        }
        return true;
    }

    /**
     * @param id The downloadId.
     * @return The downloading status without cover the completed status (if completed you will
     * receive
     * {@link FileDownloadStatus#INVALID_STATUS} ).
     */
    public byte getStatusIgnoreCompleted(final int id) {
        byte status = getStatus(id, null);
        if (status == FileDownloadStatus.completed) status = FileDownloadStatus.INVALID_STATUS;
        return status;
    }

    /**
     * @param url  The downloading URL.
     * @param path The downloading file's path.
     * @return The downloading status.
     * @see #getStatus(int, String)
     * @see #getStatusIgnoreCompleted(int)
     */
    public byte getStatus(final String url, final String path) {
        final File file = new File(path);
        final StatusUtil.Status okDownloadStatus =
                StatusUtil.getStatus(url, file.getParent(), file.getName());
        return FileDownloadUtils.convertDownloadStatus(okDownloadStatus);
    }

    /**
     * @param id   The downloadId.
     * @param path The target file path.
     * @return the downloading status.
     * @see FileDownloadStatus
     * @see #getStatus(String, String)
     * @see #getStatusIgnoreCompleted(int)
     */
    public byte getStatus(final int id, final String path) {
        byte status;
        BaseDownloadTask.IRunningTask task = FileDownloadList.getImpl().get(id);
        if (task == null) {
            final BreakpointInfo breakpointInfo = OkDownload.with().breakpointStore().get(id);
            if (breakpointInfo == null) {
                status = FileDownloadStatus.INVALID_STATUS;
            } else {
                if (path != null) {
                    final File f = new File(path);
                    final StatusUtil.Status downloadStatus = StatusUtil
                            .getStatus(breakpointInfo.getUrl(), f.getParent(), f.getName());
                    status = FileDownloadUtils.convertDownloadStatus(downloadStatus);
                } else {
                    status = FileDownloadStatus.INVALID_STATUS;
                }
            }
        } else {
            status = task.getOrigin().getStatus();
        }

        return status;
    }

    /**
     * Find the running task by {@code url} and default path, and replace its listener with
     * the new one {@code listener}.
     *
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0
     * @see #replaceListener(int, FileDownloadListener)
     * @see #replaceListener(String, String, FileDownloadListener)
     */
    public int replaceListener(String url, FileDownloadListener listener) {
        return replaceListener(url, FileDownloadUtils.getDefaultSaveFilePath(url), listener);
    }

    /**
     * Find the running task by {@code url} and {@code path}, and replace its listener with
     * the new one {@code listener}.
     *
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0
     * @see #replaceListener(String, FileDownloadListener)
     * @see #replaceListener(int, FileDownloadListener)
     */
    public int replaceListener(String url, String path, FileDownloadListener listener) {
        final File file = new File(path);
        final DownloadTask downloadTask = new DownloadTask.Builder(url, file).build();
        return replaceListener(downloadTask.getId(), listener);
    }

    /**
     * Find the running task by {@code id}, and replace its listener width the new one
     * {@code listener}.
     *
     * @return The target task's DownloadId, if not exist target task, and replace failed, will be 0
     * @see #replaceListener(String, FileDownloadListener)
     * @see #replaceListener(String, String, FileDownloadListener)
     */
    public int replaceListener(int id, FileDownloadListener listener) {
        final BaseDownloadTask.IRunningTask task = FileDownloadList.getImpl().get(id);
        if (task == null) {
            OkDownload.with().breakpointStore().remove(id);
            return 0;
        }

        task.getOrigin().setListener(listener);
        return task.getOrigin().getId();
    }

    /**
     * Because there is no any service, the FileDownloadLine will not do any wait action.
     * @see FileDownloadLine
     * @see #bindService(Runnable)
     */
    public FileDownloadLine insureServiceBind() {
        return new FileDownloadLine();
    }

    @Deprecated
    public FileDownloadLineAsync insureServiceBindAsync() {
        return new FileDownloadLineAsync();
    }

    /**
     * Because the FileDownloader database cannot be compatible with OkDownload database,
     * provide this method to discard the database of FileDownloader. It's better to invoke this
     * method at work thread.
     */
    public void discardFileDownloadDatabase(Context context) {
        context.deleteDatabase(FILEDOWNLOADER_DATABASE_NAME);
    }
}