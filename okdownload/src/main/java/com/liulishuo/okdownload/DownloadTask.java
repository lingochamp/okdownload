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
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.download.DownloadStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadTask implements Cloneable, Comparable<DownloadTask> {
    private final int id;
    @NonNull private final String url;
    private final Uri uri;
    private final boolean isUriIsDirectory;
    private final Map<String, List<String>> headerMapFields;


    /**
     * This value more larger the priority more high.
     */
    private final int priority;


    // optimize ------------------
    // no progress callback
    // no request callback
    // no response callback

    private final int readBufferSize;
    private final int flushBufferSize;

    private final int syncBufferSize;
    private final int syncBufferIntervalMills;

    /**
     * if this task has already completed with
     */
    private final boolean passIfAlreadyCompleted;

    private final boolean autoCallbackToUIThread;
    private final int minIntervalMillisCallbackProcess;
    // end optimize ------------------

    private DownloadListener listener;
    private volatile SparseArray<Object> keyTagMap;
    private Object tag;
    private final boolean isWifiRequired;

    private final AtomicLong lastCallbackProcessTimestamp;

    @NonNull private final DownloadStrategy.FilenameHolder filenameHolder;
    @NonNull private final File providedPathFile;

    public DownloadTask(String url, Uri uri, int priority, int readBufferSize, int flushBufferSize,
                        int syncBufferSize, int syncBufferIntervalMills,
                        boolean autoCallbackToUIThread, int minIntervalMillisCallbackProcess,
                        Map<String, List<String>> headerMapFields, @Nullable String filename,
                        boolean passIfAlreadyCompleted, boolean isWifiRequired) {
        try {
            this.url = url;
            this.uri = uri;
            this.priority = priority;
            this.readBufferSize = readBufferSize;
            this.flushBufferSize = flushBufferSize;
            this.syncBufferSize = syncBufferSize;
            this.syncBufferIntervalMills = syncBufferIntervalMills;
            this.autoCallbackToUIThread = autoCallbackToUIThread;
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            this.headerMapFields = headerMapFields;
            this.lastCallbackProcessTimestamp = new AtomicLong();
            this.passIfAlreadyCompleted = passIfAlreadyCompleted;
            this.isWifiRequired = isWifiRequired;

            final File file = new File(uri.getPath());
            if (file.isFile()) {
                if (!Util.isEmpty(filename) && !file.getName().equals(filename)) {
                    throw new IllegalArgumentException("Uri already provided filename!");
                }

                filename = file.getName();
                isUriIsDirectory = false;
            } else {
                isUriIsDirectory = true;
            }

            if (Util.isEmpty(filename)) {
                filenameHolder = new DownloadStrategy.FilenameHolder();
                providedPathFile = new File(uri.getPath());
            } else {
                filenameHolder = new DownloadStrategy.FilenameHolder(filename);

                if (isUriIsDirectory) providedPathFile = new File(uri.getPath(), filename);
                else providedPathFile = new File(uri.getPath());
            }
        } finally {
            this.id = OkDownload.with().breakpointStore().findOrCreateId(this);
        }
    }

    @Nullable
    public Map<String, List<String>> getHeaderMapFields() {
        return this.headerMapFields;
    }

    /**
     * This id can be used on {@link BreakpointStore}
     */
    public int getId() {
        return this.id;
    }

    @Nullable public String getFilename() {
        return filenameHolder.get();
    }

    public boolean isPassIfAlreadyCompleted() {
        return passIfAlreadyCompleted;
    }

    public boolean isWifiRequired() {
        return isWifiRequired;
    }

    public DownloadStrategy.FilenameHolder getFilenameHolder() {
        return filenameHolder;
    }

    public Uri getUri() {
        return uri;
    }

    public boolean isUriIsDirectory() {
        return isUriIsDirectory;
    }

    @NonNull public String getUrl() {
        return url;
    }

    @NonNull public String getParentPath() {
        if (isUriIsDirectory) return uri.getPath();

        return new File(uri.getPath()).getParentFile().getAbsolutePath();
    }

    @Nullable public String getPath() {
        final String filename = filenameHolder.get();
        if (filename == null) return null;

        return isUriIsDirectory
                ? new File(uri.getPath(), filename).getAbsolutePath() : uri.getPath();
    }

    public int getReadBufferSize() {
        return this.readBufferSize;
    }

    public int getFlushBufferSize() {
        return this.flushBufferSize;
    }

    public int getSyncBufferSize() {
        return syncBufferSize;
    }

    public int getSyncBufferIntervalMills() {
        return syncBufferIntervalMills;
    }

    public boolean isAutoCallbackToUIThread() {
        return autoCallbackToUIThread;
    }

    public int getMinIntervalMillisCallbackProcess() {
        return minIntervalMillisCallbackProcess;
    }

    public Object getTag(int key) {
        return keyTagMap == null ? null : keyTagMap.get(key);
    }

    public Object getTag() {
        return tag;
    }

    long getLastCallbackProcessTs() {
        return lastCallbackProcessTimestamp.get();
    }

    void setLastCallbackProcessTs(long lastCallbackProcessTimestamp) {
        this.lastCallbackProcessTimestamp.set(lastCallbackProcessTimestamp);
    }

    public synchronized DownloadTask addTag(int key, Object value) {
        if (keyTagMap == null) {
            synchronized (this) {
                if (keyTagMap == null) keyTagMap = new SparseArray<>();
            }
        }

        keyTagMap.put(key, value);
        return this;
    }

    public synchronized void removeTag(int key) {
        if (keyTagMap != null) keyTagMap.remove(key);
    }

    public synchronized void removeTag() {
        this.tag = null;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public static void enqueue(DownloadTask[] tasks, DownloadListener listener) {
        for (DownloadTask task : tasks) {
            task.listener = listener;
        }
        OkDownload.with().downloadDispatcher().enqueue(tasks);
    }

    public void enqueue(DownloadListener listener) {
        this.listener = listener;
        OkDownload.with().downloadDispatcher().enqueue(this);
    }

    public void execute(DownloadListener listener) {
        this.listener = listener;
        OkDownload.with().downloadDispatcher().execute(this);
    }

    public void cancel() {
        OkDownload.with().downloadDispatcher().cancel(this);
    }

    public DownloadListener getListener() {
        return this.listener;
    }

    public int getPriority() {
        return priority;
    }

    public Builder toBuilder(String anotherUrl, Uri anotherUri) {
        final Builder builder = new Builder(anotherUrl, anotherUri)
                .setPriority(priority)
                .setReadBufferSize(readBufferSize)
                .setFlushBufferSize(flushBufferSize)
                .setSyncBufferSize(syncBufferSize)
                .setSyncBufferIntervalMillis(syncBufferIntervalMills)
                .setAutoCallbackToUIThread(autoCallbackToUIThread)
                .setMinIntervalMillisCallbackProcess(minIntervalMillisCallbackProcess)
                .setHeaderMapFields(headerMapFields)
                .setPassIfAlreadyCompleted(passIfAlreadyCompleted);

        final File anotherFile = new File(anotherUri.getPath());
        if (!anotherFile.isFile()) {
            builder.setFilename(getFilename());
        }

        return builder;
    }

    public Builder toBuilder() {
        return toBuilder(this.url, this.uri);
    }

    public void setTags(DownloadTask oldTask) {
        this.tag = oldTask.tag;
        this.keyTagMap = oldTask.keyTagMap;
    }

    @Override public int compareTo(@NonNull DownloadTask o) {
        return o.getPriority() - getPriority();
    }

    public static class Builder {
        @NonNull final String url;
        @NonNull final Uri uri;
        private volatile Map<String, List<String>> headerMapFields;

        public Builder(@NonNull String url, @NonNull String parentPath, @Nullable String filename) {
            this(url, new File(parentPath));
            this.filename = filename;
        }

        public Builder(@NonNull String url, @NonNull File file) {
            this.url = url;
            if (file.exists() || file.getParent() == null) {
                // file exist or parent not exist
                this.uri = Uri.fromFile(file);
            } else {
                this.uri = Uri.fromFile(file.getParentFile());
                this.filename = file.getName();
            }
        }

        public Builder(@NonNull String url, @NonNull Uri uri) {
            this.url = url;
            this.uri = uri;
        }

        // More larger more high.
        private int priority;

        public static final int DEFAULT_READ_BUFFER_SIZE = 4096/* byte **/;
        private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;
        public static final int DEFAULT_FLUSH_BUFFER_SIZE = 16384/* byte **/;
        private int flushBufferSize = DEFAULT_FLUSH_BUFFER_SIZE;

        /**
         * Make sure sync to physical filesystem.
         */
        public static final int DEFAULT_SYNC_BUFFER_SIZE = 65536/* byte **/;
        private int syncBufferSize = DEFAULT_SYNC_BUFFER_SIZE;
        public static final int DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS = 3000/* millis **/;
        private int syncBufferIntervalMillis = DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS;

        public static final boolean DEFAULT_AUTO_CALLBACK_TO_UI_THREAD = true;
        private boolean autoCallbackToUIThread = DEFAULT_AUTO_CALLBACK_TO_UI_THREAD;

        public static final int DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS = 3000/* millis **/;
        private int minIntervalMillisCallbackProcess = DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS;

        private String filename;

        public static final boolean DEFAULT_PASS_IF_ALREADY_COMPLETED = true;
        /**
         * if this task has already completed judged by
         * {@link StatusUtil.Status#isCompleted(DownloadTask)}, callback completed directly instead
         * of start download.
         */
        private boolean passIfAlreadyCompleted = DEFAULT_PASS_IF_ALREADY_COMPLETED;

        public static final boolean DEFAULT_IS_WIFI_REQUIRED = false;

        private boolean isWifiRequired = DEFAULT_IS_WIFI_REQUIRED;

        public Builder setAutoCallbackToUIThread(boolean autoCallbackToUIThread) {
            this.autoCallbackToUIThread = autoCallbackToUIThread;
            return this;
        }

        public Builder setMinIntervalMillisCallbackProcess(int minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            return this;
        }

        public Builder setHeaderMapFields(Map<String, List<String>> headerMapFields) {
            this.headerMapFields = headerMapFields;
            return this;
        }

        public synchronized void addHeader(String key, String value) {
            if (headerMapFields == null) headerMapFields = new HashMap<>();
            List<String> valueList = headerMapFields.get(key);
            if (valueList == null) {
                valueList = new ArrayList<>();
                headerMapFields.put(key, valueList);
            }
            valueList.add(value);
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder setReadBufferSize(int readBufferSize) {
            if (readBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.readBufferSize = readBufferSize;
            return this;
        }

        public Builder setFlushBufferSize(int flushBufferSize) {
            if (flushBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.flushBufferSize = flushBufferSize;
            return this;
        }

        public Builder setSyncBufferSize(int syncBufferSize) {
            if (syncBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.syncBufferSize = syncBufferSize;
            return this;
        }

        public Builder setSyncBufferIntervalMillis(int syncBufferIntervalMillis) {
            if (syncBufferIntervalMillis < 0) {
                throw new IllegalArgumentException("Value must be positive!");
            }

            this.syncBufferIntervalMillis = syncBufferIntervalMillis;
            return this;
        }

        public Builder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder setPassIfAlreadyCompleted(boolean passIfAlreadyCompleted) {
            this.passIfAlreadyCompleted = passIfAlreadyCompleted;
            return this;
        }

        public Builder setWifiRequired(boolean wifiRequired) {
            this.isWifiRequired = wifiRequired;
            return this;
        }

        public DownloadTask build() {
            return new DownloadTask(url, uri, priority, readBufferSize, flushBufferSize,
                    syncBufferSize, syncBufferIntervalMillis,
                    autoCallbackToUIThread, minIntervalMillisCallbackProcess,
                    headerMapFields, filename, passIfAlreadyCompleted, isWifiRequired);
        }
    }

    @Override public boolean equals(Object obj) {
        if (super.equals(obj)) return true;

        if (obj instanceof DownloadTask) {
            final DownloadTask another = (DownloadTask) obj;
            if (another.id == this.id) return true;
            return compareIgnoreId(another);
        }

        return false;
    }

    public boolean compareIgnoreId(DownloadTask another) {
        if (!url.equals(another.url)) return false;

        if (providedPathFile.equals(another.providedPathFile)) return true;

        // cover the case of filename is provided by response.
        final String filename = getFilename();
        final String anotherFilename = another.getFilename();
        return anotherFilename != null && filename != null && anotherFilename.equals(filename);
    }

    @Override public int hashCode() {
        return (url + providedPathFile.toString() + filenameHolder.get()).hashCode();
    }

    @Override public String toString() {
        return super.toString() + "@" + id + "@" + url + "@" + providedPathFile.toString()
                + "/" + filenameHolder.get();
    }

    public static class TaskCallbackWrapper {
        public static long getLastCallbackProcessTs(DownloadTask task) {
            return task.getLastCallbackProcessTs();
        }

        public static void setLastCallbackProcessTs(DownloadTask task,
                                                    long lastCallbackProcessTimestamp) {
            task.setLastCallbackProcessTs(lastCallbackProcessTimestamp);
        }
    }
}
