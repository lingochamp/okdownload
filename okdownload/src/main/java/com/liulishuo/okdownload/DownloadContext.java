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
import android.support.annotation.NonNull;

import com.liulishuo.okdownload.core.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadContext {

    private static final Executor serialExecutor = new ThreadPoolExecutor(0,
            Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload Serial", false));

    private final DownloadTask[] tasks;
    volatile boolean isStarted = false;

    public DownloadContext(@NonNull DownloadTask[] tasks) {
        this.tasks = tasks;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void start(final DownloadListener listener, boolean isSerial) {
        isStarted = true;
        if (isSerial) {
            executeOnSerialExecutor(new Runnable() {
                @Override public void run() {
                    for (DownloadTask task : tasks) {
                        if (!isStarted) break;
                        task.execute(listener);
                    }
                }
            });
        } else {
            for (DownloadTask task : tasks) {
                task.enqueue(listener);
            }
        }
    }

    public void stop() {
        isStarted = false;
        for (DownloadTask task : tasks) {
            task.cancel();
        }
    }

    void executeOnSerialExecutor(Runnable runnable) {
        serialExecutor.execute(runnable);
    }

    public static class Builder {
        ArrayList<DownloadTask> boundTaskList = new ArrayList<>();

        private final QueueSet set;

        public Builder(QueueSet set) {
            this.set = set;
        }

        public void bindSetTask(@NonNull DownloadTask task) {
            if (!boundTaskList.contains(task)) boundTaskList.add(task);
        }

        public void bind(@NonNull String url) {
            if (set.uri == null) {
                throw new IllegalArgumentException("If you want to bind only with url, you have to"
                        + " provide parentPath on QueueSet!");
            }

            bind(new DownloadTask.Builder(url, set.uri));
        }

        public void bind(@NonNull DownloadTask.Builder taskBuilder) {
            if (set.headerMapFields != null) taskBuilder.setHeaderMapFields(set.headerMapFields);
            if (set.readBufferSize != null) taskBuilder.setReadBufferSize(set.readBufferSize);
            if (set.flushBufferSize != null) taskBuilder.setFlushBufferSize(set.flushBufferSize);
            if (set.syncBufferSize != null) taskBuilder.setSyncBufferSize(set.syncBufferSize);
            if (set.syncBufferIntervalMillis != null) {
                taskBuilder.setSyncBufferIntervalMillis(set.syncBufferIntervalMillis);
            }

            final DownloadTask task = taskBuilder.build();
            if (set.tag != null) task.setTag(set.tag);

            boundTaskList.add(task);
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
            return new DownloadContext(boundTaskList.toArray(tasks));
        }
    }

    public static class QueueSet {
        private HashMap<String, List<String>> headerMapFields;
        private Uri uri;
        private Integer readBufferSize;
        private Integer flushBufferSize;
        private Integer syncBufferSize;
        private Integer syncBufferIntervalMillis;
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

        public void setDirUri(Uri uri) {
            this.uri = uri;
        }

        public void setParentPath(String parentPath) {
            this.uri = Uri.fromFile(new File(parentPath));
        }

        public int getReadBufferSize() {
            if (readBufferSize == null) {
                return DownloadTask.Builder.DEFAULT_READ_BUFFER_SIZE;
            } else {
                return readBufferSize;
            }
        }

        public void setReadBufferSize(int readBufferSize) {
            this.readBufferSize = readBufferSize;
        }

        public int getFlushBufferSize() {
            if (flushBufferSize == null) {
                return DownloadTask.Builder.DEFAULT_FLUSH_BUFFER_SIZE;
            } else {
                return flushBufferSize;
            }
        }

        public void setFlushBufferSize(int flushBufferSize) {
            this.flushBufferSize = flushBufferSize;
        }

        public int getSyncBufferSize() {
            if (syncBufferSize == null) {
                return DownloadTask.Builder.DEFAULT_SYNC_BUFFER_SIZE;
            } else {
                return syncBufferSize;
            }
        }

        public void setSyncBufferSize(int syncBufferSize) {
            this.syncBufferSize = syncBufferSize;
        }

        public int getSyncBufferIntervalMillis() {
            if (syncBufferIntervalMillis == null) {
                return DownloadTask.Builder.DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS;
            } else {
                return syncBufferIntervalMillis;
            }
        }

        public void setSyncBufferIntervalMillis(int syncBufferIntervalMillis) {
            this.syncBufferIntervalMillis = syncBufferIntervalMillis;
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
}
