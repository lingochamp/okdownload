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
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener2;
import com.liulishuo.okdownload.core.listener.DownloadListenerBunch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    @Nullable final DownloadContextListener contextListener;
    private final QueueSet set;

    DownloadContext(@NonNull DownloadTask[] tasks,
                    @Nullable DownloadContextListener contextListener,
                    @NonNull QueueSet set) {
        this.tasks = tasks;
        this.contextListener = contextListener;
        this.set = set;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public DownloadTask[] getTasks() {
        return tasks;
    }

    public void startOnSerial(DownloadListener listener) {
        start(listener, true);
    }

    public void startOnParallel(DownloadListener listener) {
        start(listener, false);
    }

    public void start(final DownloadListener listener, boolean isSerial) {
        final long startTime = SystemClock.uptimeMillis();
        Util.d(TAG, "start " + isSerial);
        isStarted = true;
        final DownloadListener targetListener;
        if (contextListener != null) {
            targetListener = new DownloadListenerBunch.Builder()
                    .append(listener)
                    .append(new QueueAttachListener(this, contextListener, tasks.length))
                    .build();
        } else {
            targetListener = listener;
        }

        if (isSerial) {
            final List<DownloadTask> scheduleTaskList = new ArrayList<>();
            Collections.addAll(scheduleTaskList, tasks);
            Collections.sort(scheduleTaskList);
            executeOnSerialExecutor(new Runnable() {
                @Override public void run() {
                    for (DownloadTask task : scheduleTaskList) {
                        if (!isStarted) {
                            callbackQueueEndOnSerialLoop(task.isAutoCallbackToUIThread());
                            break;
                        }
                        task.execute(targetListener);
                    }
                }
            });
        } else {
            DownloadTask.enqueue(tasks, targetListener);
        }
        Util.d(TAG,
                "start finish " + isSerial + " " + (SystemClock.uptimeMillis() - startTime) + "ms");
    }

    public AlterContext alter() {
        return new AlterContext(this);
    }

    public void stop() {
        if (isStarted) OkDownload.with().downloadDispatcher().cancel(tasks);
        isStarted = false;
    }

    private void callbackQueueEndOnSerialLoop(boolean isAutoCallbackToUIThread) {
        if (contextListener == null) return;

        if (isAutoCallbackToUIThread) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override public void run() {
                    contextListener.queueEnd(DownloadContext.this);
                }
            });
        } else {
            contextListener.queueEnd(this);
        }
    }

    void executeOnSerialExecutor(Runnable runnable) {
        SERIAL_EXECUTOR.execute(runnable);
    }

    public Builder toBuilder() {
        return new Builder(set, new ArrayList<>(Arrays.asList(tasks)))
                .setListener(this.contextListener);
    }

    public static class Builder {
        final ArrayList<DownloadTask> boundTaskList;

        private final QueueSet set;
        private DownloadContextListener listener;

        public Builder() {
            this(new QueueSet());
        }

        public Builder(QueueSet set) {
            this(set, new ArrayList<DownloadTask>());
        }

        public Builder(QueueSet set, ArrayList<DownloadTask> taskArrayList) {
            this.set = set;
            this.boundTaskList = taskArrayList;
        }

        public Builder setListener(DownloadContextListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder bindSetTask(@NonNull DownloadTask task) {
            final int index = boundTaskList.indexOf(task);
            if (index >= 0) {
                // replace
                boundTaskList.set(index, task);
            } else {
                boundTaskList.add(task);
            }

            return this;
        }

        public DownloadTask bind(@NonNull String url) {
            if (set.uri == null) {
                throw new IllegalArgumentException("If you want to bind only with url, you have to"
                        + " provide parentPath on QueueSet!");
            }

            return bind(new DownloadTask.Builder(url, set.uri).setFilenameFromResponse(true));
        }

        public DownloadTask bind(@NonNull DownloadTask.Builder taskBuilder) {
            if (set.headerMapFields != null) taskBuilder.setHeaderMapFields(set.headerMapFields);
            if (set.readBufferSize != null) taskBuilder.setReadBufferSize(set.readBufferSize);
            if (set.flushBufferSize != null) taskBuilder.setFlushBufferSize(set.flushBufferSize);
            if (set.syncBufferSize != null) taskBuilder.setSyncBufferSize(set.syncBufferSize);
            if (set.wifiRequired != null) taskBuilder.setWifiRequired(set.wifiRequired);
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

            if (set.passIfAlreadyCompleted != null) {
                taskBuilder.setPassIfAlreadyCompleted(set.passIfAlreadyCompleted);
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
            return new DownloadContext(boundTaskList.toArray(tasks), listener, set);
        }
    }

    public static class QueueSet {
        private Map<String, List<String>> headerMapFields;
        private Uri uri;
        private Integer readBufferSize;
        private Integer flushBufferSize;
        private Integer syncBufferSize;
        private Integer syncBufferIntervalMillis;

        private Boolean autoCallbackToUIThread;
        private Integer minIntervalMillisCallbackProcess;

        private Boolean passIfAlreadyCompleted;
        private Boolean wifiRequired;

        private Object tag;

        public Map<String, List<String>> getHeaderMapFields() {
            return headerMapFields;
        }

        public void setHeaderMapFields(Map<String, List<String>> headerMapFields) {
            this.headerMapFields = headerMapFields;
        }

        public Uri getDirUri() {
            return uri;
        }

        public QueueSet setParentPathUri(@NonNull Uri uri) {
            this.uri = uri;
            return this;
        }

        public QueueSet setParentPathFile(@NonNull File parentPathFile) {
            if (parentPathFile.isFile()) {
                throw new IllegalArgumentException("parent path only accept directory path");
            }

            this.uri = Uri.fromFile(parentPathFile);
            return this;
        }

        public QueueSet setParentPath(@NonNull String parentPath) {
            return setParentPathFile(new File(parentPath));
        }

        public int getReadBufferSize() {
            return readBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_READ_BUFFER_SIZE : readBufferSize;
        }

        public QueueSet setReadBufferSize(int readBufferSize) {
            this.readBufferSize = readBufferSize;
            return this;
        }

        public QueueSet setWifiRequired(Boolean wifiRequired) {
            this.wifiRequired = wifiRequired;
            return this;
        }

        public Boolean isWifiRequired() {
            return wifiRequired == null
                    ? DownloadTask.Builder.DEFAULT_IS_WIFI_REQUIRED : wifiRequired;
        }

        public int getFlushBufferSize() {
            return flushBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_FLUSH_BUFFER_SIZE : flushBufferSize;
        }

        public QueueSet setFlushBufferSize(int flushBufferSize) {
            this.flushBufferSize = flushBufferSize;
            return this;
        }

        public int getSyncBufferSize() {
            return syncBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_SYNC_BUFFER_SIZE : syncBufferSize;
        }

        public QueueSet setSyncBufferSize(int syncBufferSize) {
            this.syncBufferSize = syncBufferSize;
            return this;
        }

        public int getSyncBufferIntervalMillis() {
            return syncBufferIntervalMillis == null
                    ? DownloadTask.Builder.DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS
                    : syncBufferIntervalMillis;
        }

        public QueueSet setSyncBufferIntervalMillis(int syncBufferIntervalMillis) {
            this.syncBufferIntervalMillis = syncBufferIntervalMillis;
            return this;
        }

        public boolean getAutoCallbackToUIThread() {
            return autoCallbackToUIThread == null
                    ? DownloadTask.Builder.DEFAULT_AUTO_CALLBACK_TO_UI_THREAD
                    : autoCallbackToUIThread;
        }

        public QueueSet setAutoCallbackToUIThread(Boolean autoCallbackToUIThread) {
            this.autoCallbackToUIThread = autoCallbackToUIThread;
            return this;
        }

        public int getMinIntervalMillisCallbackProcess() {
            return minIntervalMillisCallbackProcess == null
                    ? DownloadTask.Builder.DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS
                    : minIntervalMillisCallbackProcess;
        }

        public QueueSet setMinIntervalMillisCallbackProcess(
                Integer minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            return this;
        }

        public Object getTag() {
            return tag;
        }

        public QueueSet setTag(Object tag) {
            this.tag = tag;
            return this;
        }

        public boolean isPassIfAlreadyCompleted() {
            return passIfAlreadyCompleted == null
                    ? DownloadTask.Builder.DEFAULT_PASS_IF_ALREADY_COMPLETED
                    : passIfAlreadyCompleted;
        }

        public QueueSet setPassIfAlreadyCompleted(boolean passIfAlreadyCompleted) {
            this.passIfAlreadyCompleted = passIfAlreadyCompleted;
            return this;
        }

        public Builder commit() {
            return new DownloadContext.Builder(this);
        }
    }

    private static class QueueAttachListener extends DownloadListener2 {
        private final AtomicInteger remainCount;
        @NonNull private final DownloadContextListener queueListener;
        @NonNull private final DownloadContext hostContext;

        QueueAttachListener(@NonNull DownloadContext context,
                            @NonNull DownloadContextListener queueListener, int taskCount) {
            remainCount = new AtomicInteger(taskCount);
            this.queueListener = queueListener;
            this.hostContext = context;
        }

        @Override public void taskStart(@NonNull DownloadTask task) {
        }

        @Override
        public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                            @Nullable Exception realCause) {
            if (remainCount.decrementAndGet() <= 0) {
                queueListener.queueEnd(hostContext);
                // only log the last one
                Util.d(TAG, "taskEnd and remainCount " + remainCount);
            }
        }
    }

    /**
     * The Alter helper for the {@link DownloadContext}.
     */
    public static class AlterContext {
        private final DownloadContext context;

        AlterContext(DownloadContext context) {
            this.context = context;
        }

        /**
         * Replace the {@code oldTask} to the {@code newTask}
         *
         * @param oldTask the old task which has been added to the context.
         * @param newTask the new task which will be replace the {@code oldTask} on the
         *                {@code context}.
         */
        public AlterContext replaceTask(DownloadTask oldTask, DownloadTask newTask) {
            final DownloadTask[] tasks = context.tasks;
            for (int i = 0; i < tasks.length; i++) {
                final DownloadTask task = tasks[i];
                if (task == oldTask) {
                    tasks[i] = newTask;
                }
            }

            return this;
        }
    }
}
