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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.liulishuo.filedownloader.exception.FileDownloadNetworkPolicyException;
import com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException;
import com.liulishuo.filedownloader.retry.RetryAssist;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.exception.NetworkPolicyException;
import com.liulishuo.okdownload.core.exception.PreAllocateException;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompatListenerAssist {

    private static final Executor EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload Block Complete", false));
    private static final String TAG = "CompatListenerAssist";

    @NonNull
    private final CompatListenerAssistCallback callback;
    @NonNull
    private final Handler uiHandler;
    private boolean resumable;
    private String etag;
    @NonNull
    final AtomicBoolean taskConnected;
    @Nullable
    private Exception exception;
    private boolean reuseOldFile;

    CompatListenerAssist(@NonNull CompatListenerAssistCallback callback) {
        this(callback, new Handler(Looper.getMainLooper()));
    }

    CompatListenerAssist(@NonNull CompatListenerAssistCallback callback,
                         @NonNull Handler uiHandler) {
        this.callback = callback;
        this.taskConnected = new AtomicBoolean(false);
        this.uiHandler = uiHandler;
    }

    public void taskStart(@NonNull DownloadTask task) {
        final DownloadTaskAdapter downloadTaskAdapter =
                FileDownloadUtils.findDownloadTaskAdapter(task);
        if (downloadTaskAdapter == null) return;
        final int soFarBytes = downloadTaskAdapter.getSoFarBytes();
        final int totalBytes = downloadTaskAdapter.getTotalBytes();
        callback.pending(downloadTaskAdapter, soFarBytes, totalBytes);
        callback.started(downloadTaskAdapter);
    }

    public void setResumable(boolean resumable) {
        this.resumable = resumable;
    }

    public boolean isResumable() {
        return resumable;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getEtag() {
        return etag;
    }

    public void connectStart(DownloadTask task) {
        if (taskConnected.compareAndSet(false, true)) {
            final DownloadTaskAdapter downloadTaskAdapter =
                    FileDownloadUtils.findDownloadTaskAdapter(task);
            if (downloadTaskAdapter == null) return;
            final int soFarBytes = downloadTaskAdapter.getSoFarBytes();
            final int totalBytes = downloadTaskAdapter.getTotalBytes();
            downloadTaskAdapter.getProgressAssist().calculateCallbackMinIntervalBytes(totalBytes);
            callback.connected(downloadTaskAdapter, etag, resumable, soFarBytes, totalBytes);
        }
    }

    public void fetchProgress(@NonNull DownloadTask task, long increaseBytes) {
        final DownloadTaskAdapter downloadTaskAdapter =
                FileDownloadUtils.findDownloadTaskAdapter(task);
        if (downloadTaskAdapter == null) return;
        downloadTaskAdapter.getProgressAssist()
                .onProgress(downloadTaskAdapter, increaseBytes, callback);
    }

    public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                        @Nullable Exception realCause) {
        final DownloadTaskAdapter downloadTaskAdapter =
                FileDownloadUtils.findDownloadTaskAdapter(task);
        if (downloadTaskAdapter == null) return;
        downloadTaskAdapter.getProgressAssist().clearProgress();
        exception = realCause;
        switch (cause) {
            case PRE_ALLOCATE_FAILED:
            case ERROR:
                handleError(downloadTaskAdapter, realCause);
                break;
            case CANCELED:
                handleCanceled(downloadTaskAdapter);
                break;
            case FILE_BUSY:
            case SAME_TASK_BUSY:
                handleWarn(downloadTaskAdapter, cause, realCause);
                break;
            case COMPLETED:
                handleComplete(downloadTaskAdapter);
                break;
            default:
                break;
        }
        onTaskFinish(downloadTaskAdapter);
    }

    void handleWarn(
            @NonNull DownloadTaskAdapter downloadTaskAdapter,
            EndCause cause,
            Exception realCause) {
        Util.w(TAG, "handle warn, cause: " + cause + "real cause: " + realCause);
        callback.warn(downloadTaskAdapter);
    }

    void handleCanceled(@NonNull DownloadTaskAdapter downloadTaskAdapter) {
        callback.paused(
                downloadTaskAdapter,
                (int) downloadTaskAdapter.getProgressAssist().getSofarBytes(),
                downloadTaskAdapter.getTotalBytes());
    }

    void handleError(
            @NonNull DownloadTaskAdapter downloadTaskAdapter,
            @Nullable Exception realCause) {
        final RetryAssist retryAssist = downloadTaskAdapter.getRetryAssist();
        if (retryAssist != null && retryAssist.canRetry()) {
            Log.d(TAG, "handle retry " + Thread.currentThread().getName());
            final int retryingTime = retryAssist.getRetriedTimes() + 1;
            callback.retry(downloadTaskAdapter, realCause, retryingTime,
                    (int) downloadTaskAdapter.getProgressAssist().getSofarBytes());
            retryAssist.doRetry(downloadTaskAdapter.getDownloadTask());
            return;
        }

        Log.d(TAG, "handle error");

        final Throwable throwable;
        if (realCause instanceof NetworkPolicyException) {
            throwable = new FileDownloadNetworkPolicyException();
        } else if (realCause instanceof PreAllocateException) {
            final PreAllocateException preAllocateException = (PreAllocateException) realCause;
            throwable = new FileDownloadOutOfSpaceException(
                    preAllocateException.getFreeSpace(),
                    preAllocateException.getRequireSpace(),
                    downloadTaskAdapter.getProgressAssist().getSofarBytes(),
                    preAllocateException);
        } else {
            throwable = new Throwable(realCause);
        }
        callback.error(downloadTaskAdapter, throwable);
    }

    void onTaskFinish(@NonNull DownloadTaskAdapter downloadTaskAdapter) {
        Util.d(TAG, "on task finish, have finish listener: "
                + downloadTaskAdapter.isContainFinishListener());
        for (BaseDownloadTask.FinishListener listener : downloadTaskAdapter.getFinishListeners()) {
            listener.over(downloadTaskAdapter);
        }
        FileDownloadList.getImpl().remove(downloadTaskAdapter);
    }

    void handleComplete(@NonNull final DownloadTaskAdapter downloadTaskAdapter) {
        reuseOldFile = !taskConnected.get();
        if (downloadTaskAdapter.getDownloadTask().isAutoCallbackToUIThread()) {
            EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    handleBlockComplete(downloadTaskAdapter);
                }
            });
        } else {
            try {
                callback.blockComplete(downloadTaskAdapter);
                callback.completed(downloadTaskAdapter);
            } catch (final Throwable throwable) {
                handleError(downloadTaskAdapter, new Exception(throwable));
            }
        }
    }

    void handleBlockComplete(@NonNull final DownloadTaskAdapter downloadTaskAdapter) {
        try {
            callback.blockComplete(downloadTaskAdapter);
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.completed(downloadTaskAdapter);
                }
            });
        } catch (final Throwable throwable) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleError(downloadTaskAdapter, new Exception(throwable));
                }
            });
        }
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    public boolean isReuseOldFile() {
        return reuseOldFile;
    }

    public interface CompatListenerAssistCallback {

        /**
         * compat with {@link FileDownloadListener#pending(BaseDownloadTask, int, int)}
         */
        void pending(BaseDownloadTask task, int soFarBytes, int totalBytes);

        /**
         * compat with {@link FileDownloadListener#started(BaseDownloadTask)}
         */
        void started(BaseDownloadTask task);

        /**
         * compat with
         * {@link FileDownloadListener#connected(BaseDownloadTask, String, boolean, int, int)}
         */
        void connected(BaseDownloadTask task, String etag, boolean isContinue,
                       int soFarBytes, int totalBytes);

        /**
         * compat with {@link FileDownloadListener#progress(BaseDownloadTask, int, int)}
         */
        void progress(BaseDownloadTask task, int soFarBytes, int totalBytes);

        /**
         * compat with {@link FileDownloadListener#blockComplete(BaseDownloadTask)}
         */
        void blockComplete(BaseDownloadTask task) throws Throwable;

        /**
         * compat with {@link FileDownloadListener#retry(BaseDownloadTask, Throwable, int, int)}
         */
        void retry(BaseDownloadTask task, Throwable ex, int retryingTimes,
                   int soFarBytes);

        /**
         * compat with {@link FileDownloadListener#completed(BaseDownloadTask)}
         */
        void completed(BaseDownloadTask task);

        /**
         * compat with {@link FileDownloadListener#paused(BaseDownloadTask, int, int)}
         */
        void paused(BaseDownloadTask task, int soFarBytes, int totalBytes);

        /**
         * compat with {@link FileDownloadListener#error(BaseDownloadTask, Throwable)}
         */
        void error(BaseDownloadTask task, Throwable e);

        /**
         * compat with {@link FileDownloadListener#warn(BaseDownloadTask)}
         */
        void warn(BaseDownloadTask task);

    }
}
