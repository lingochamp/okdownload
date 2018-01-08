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

package com.liulishuo.okdownload;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener2;
import com.liulishuo.okdownload.core.listener.DownloadListenerBunch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadContext {

    private static final String TAG = "DownloadContext";
    private static final Executor SERIAL_EXECUTOR = new ThreadPoolExecutor(0,
            Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload Serial", false));

    private final DownloadTask[] tasks;
    volatile boolean isStarted = false;
    @Nullable private final DownloadQueueListener queueListener;

    public DownloadContext(@NonNull DownloadTask[] tasks,
                           @Nullable DownloadQueueListener queueListener) {
        this.tasks = tasks;
        this.queueListener = queueListener;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public DownloadTask[] getTasks() {
        return tasks;
    }

    public void start(final DownloadListener listener, boolean isSerial) {
        isStarted = true;
        final DownloadListener targetListener;
        if (queueListener != null) {
            targetListener = new DownloadListenerBunch.Builder()
                    .append(listener)
                    .append(new QueueAttachListener(queueListener, tasks.length))
                    .build();
        } else {
            targetListener = listener;
        }

        if (isSerial) {
            executeOnSerialExecutor(new Runnable() {
                @Override public void run() {
                    for (DownloadTask task : tasks) {
                        if (!isStarted) {
                            callbackQueueEndOnSerialLoop(task.isAutoCallbackToUIThread());
                            break;
                        }
                        task.execute(targetListener);
                    }
                }
            });
        } else {
            for (DownloadTask task : tasks) {
                task.enqueue(targetListener);
            }
        }
    }

    public void stop() {
        isStarted = false;
        for (DownloadTask task : tasks) {
            task.cancel();
        }
    }

    private void callbackQueueEndOnSerialLoop(boolean isAutoCallbackToUIThread) {
        if (queueListener == null) return;

        if (isAutoCallbackToUIThread) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override public void run() {
                    queueListener.queueEnd();
                }
            });
        } else {
            queueListener.queueEnd();
        }
    }

    void executeOnSerialExecutor(Runnable runnable) {
        SERIAL_EXECUTOR.execute(runnable);
    }

    public static class Builder {
        ArrayList<DownloadTask> boundTaskList = new ArrayList<>();

        private final QueueSet set;
        private DownloadQueueListener listener;

        public Builder() {
            this(new QueueSet());
        }

        public Builder(QueueSet set) {
            this.set = set;
        }

        public void setListener(DownloadQueueListener listener) {
            this.listener = listener;
        }

        public void bindSetTask(@NonNull DownloadTask task) {
            if (!boundTaskList.contains(task)) boundTaskList.add(task);
        }

        public DownloadTask bind(@NonNull String url) {
            if (set.uri == null) {
                throw new IllegalArgumentException("If you want to bind only with url, you have to"
                        + " provide parentPath on QueueSet!");
            }

            return bind(new DownloadTask.Builder(url, set.uri));
        }

        public DownloadTask bind(@NonNull DownloadTask.Builder taskBuilder) {
            if (set.headerMapFields != null) taskBuilder.setHeaderMapFields(set.headerMapFields);
            if (set.readBufferSize != null) taskBuilder.setReadBufferSize(set.readBufferSize);
            if (set.flushBufferSize != null) taskBuilder.setFlushBufferSize(set.flushBufferSize);
            if (set.syncBufferSize != null) taskBuilder.setSyncBufferSize(set.syncBufferSize);
            if (set.syncBufferIntervalMillis != null) {
                taskBuilder.setSyncBufferIntervalMillis(set.syncBufferIntervalMillis);
            }
            if (set.autoCallbackToUIThread != null) {
                taskBuilder.setAutoCallbackToUIThread(set.autoCallbackToUIThread);
            }
            if (set.minIntervalMillisCallbackProcess != null) {
                taskBuilder
                        .setMinIntervalMillisCallbackProcess(set.minIntervalMillisCallbackProcess);
            }

            final DownloadTask task = taskBuilder.build();
            if (set.tag != null) task.setTag(set.tag);

            boundTaskList.add(task);
            return task;
        }

        public void unbind(@NonNull DownloadTask task) {
            boundTaskList.remove(task);
        }

        public void unbind(int id) {
            List<DownloadTask> list = (List<DownloadTask>) boundTaskList.clone();
            for (DownloadTask task : list) {
                if (task.getId() == id) boundTaskList.remove(task);
            }
        }

        public DownloadContext build() {
            DownloadTask[] tasks = new DownloadTask[boundTaskList.size()];
            return new DownloadContext(boundTaskList.toArray(tasks), listener);
        }
    }

    public static class QueueSet {
        private HashMap<String, List<String>> headerMapFields;
        private Uri uri;
        private Integer readBufferSize;
        private Integer flushBufferSize;
        private Integer syncBufferSize;
        private Integer syncBufferIntervalMillis;

        private Boolean autoCallbackToUIThread;
        private Integer minIntervalMillisCallbackProcess;

        private Object tag;

        public HashMap<String, List<String>> getHeaderMapFields() {
            return headerMapFields;
        }

        public void setHeaderMapFields(
                HashMap<String, List<String>> headerMapFields) {
            this.headerMapFields = headerMapFields;
        }

        public Uri getDirUri() {
            return uri;
        }

        public void setParentPathUri(Uri uri) {
            this.uri = uri;
        }

        public void setParentPathFile(File parentPathFile) {
            this.uri = Uri.fromFile(parentPathFile);
        }

        public void setParentPath(String parentPath) {
            this.uri = Uri.fromFile(new File(parentPath));
        }

        public int getReadBufferSize() {
            return readBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_READ_BUFFER_SIZE : readBufferSize;
        }

        public void setReadBufferSize(int readBufferSize) {
            this.readBufferSize = readBufferSize;
        }

        public int getFlushBufferSize() {
            return flushBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_FLUSH_BUFFER_SIZE : flushBufferSize;
        }

        public void setFlushBufferSize(int flushBufferSize) {
            this.flushBufferSize = flushBufferSize;
        }

        public int getSyncBufferSize() {
            return syncBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_SYNC_BUFFER_SIZE : syncBufferSize;
        }

        public void setSyncBufferSize(int syncBufferSize) {
            this.syncBufferSize = syncBufferSize;
        }

        public int getSyncBufferIntervalMillis() {
            return syncBufferIntervalMillis == null
                    ? DownloadTask.Builder.DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS
                    : syncBufferIntervalMillis;
        }

        public void setSyncBufferIntervalMillis(int syncBufferIntervalMillis) {
            this.syncBufferIntervalMillis = syncBufferIntervalMillis;
        }

        public boolean getAutoCallbackToUIThread() {
            return autoCallbackToUIThread == null
                    ? DownloadTask.Builder.DEFAULT_AUTO_CALLBACK_TO_UI_THREAD
                    : autoCallbackToUIThread;
        }

        public void setAutoCallbackToUIThread(Boolean autoCallbackToUIThread) {
            this.autoCallbackToUIThread = autoCallbackToUIThread;
        }

        public int getMinIntervalMillisCallbackProcess() {
            return minIntervalMillisCallbackProcess == null
                    ? DownloadTask.Builder.DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS
                    : minIntervalMillisCallbackProcess;
        }

        public void setMinIntervalMillisCallbackProcess(
                Integer minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
        }

        public Object getTag() {
            return tag;
        }

        public void setTag(Object tag) {
            this.tag = tag;
        }

        public Builder commit() {
            return new DownloadContext.Builder(this);
        }
    }

    private static class QueueAttachListener extends DownloadListener2 {
        private final AtomicInteger remainCount;
        @NonNull private final DownloadQueueListener queueListener;

        QueueAttachListener(@NonNull DownloadQueueListener queueListener, int taskCount) {
            remainCount = new AtomicInteger(taskCount);
            this.queueListener = queueListener;
        }

        @Override public void taskStart(DownloadTask task) {
        }

        @Override
        public void taskEnd(DownloadTask task, EndCause cause,
                            @Nullable Exception realCause) {
            if (remainCount.decrementAndGet() <= 0) queueListener.queueEnd();
            Util.d(TAG, "taskEnd and remainCount" + remainCount);
        }
    }
}
