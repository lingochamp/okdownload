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

import com.liulishuo.okdownload.core.IdentifiedTask;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.download.DownloadStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A DownloadTask is just for download one file from one url, you can customize its profile through
 * {@link Builder} and store temporary data through {@link #setTag(Object)} or
 * {@link #addTag(int, Object)}, also you can get state or its breakpoint data from
 * {@link OkDownload#breakpointStore()} with {@link #getId()}, and it's very welcome to use
 * {@link StatusUtil} to help you more convenient to get its status stored on database with task's
 * {@link #getId()}.
 */
public class DownloadTask extends IdentifiedTask implements Cloneable, Comparable<DownloadTask> {
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

    /**
     * Get you custom request header map files for this task.
     *
     * @return you custom request header map files for this task. {@code null} if you isn't add any
     * header fields for this task.
     * @see Builder#addHeader(String, String)
     * @see Builder#setHeaderMapFields(Map)
     */
    @Nullable
    public Map<String, List<String>> getHeaderMapFields() {
        return this.headerMapFields;
    }

    /**
     * This id can be used on {@link BreakpointStore}
     */
    @Override public int getId() {
        return this.id;
    }

    /**
     * Get the filename of the file to store download data.
     *
     * @return the filename of the file to store download data. {@code null} if you not provided it
     * and okdownload isn't get response yet.
     */
    @Nullable public String getFilename() {
        return filenameHolder.get();
    }

    /**
     * Whether pass this task with completed callback directly if this task has already completed.
     *
     * @return {@code true} pass this task with completed callback directly if this task has already
     * completed.
     */
    public boolean isPassIfAlreadyCompleted() {
        return passIfAlreadyCompleted;
    }

    /**
     * Whether wifi required for proceed this task.
     *
     * @return {@code true} if this task only can download on the Wifi network type.
     */
    public boolean isWifiRequired() {
        return isWifiRequired;
    }

    public DownloadStrategy.FilenameHolder getFilenameHolder() {
        return filenameHolder;
    }

    /**
     * Get the uri to store download data.
     *
     * @return the uri to store download data.
     */
    public Uri getUri() {
        return uri;
    }

    /**
     * Whether you provide {@code uri} or file is directory.
     *
     * @return {@code true} is you provide {@code uri} or file path for this task is directory.
     */
    public boolean isUriIsDirectory() {
        return isUriIsDirectory;
    }

    /**
     * Get the url for this task.
     *
     * @return the url for this task.
     */
    @NonNull public String getUrl() {
        return url;
    }

    @NonNull @Override protected File getProvidedPathFile() {
        return this.providedPathFile;
    }

    /**
     * Get the parent path of the file store downloaded data.
     *
     * @return the parent path of the file store downloaded data.
     */
    @Override @NonNull public String getParentPath() {
        if (isUriIsDirectory) return uri.getPath();

        return new File(uri.getPath()).getParentFile().getAbsolutePath();
    }

    /**
     * Get the path of file store downloaded data.
     *
     * @return the path of file store downloaded data. {@code null} is there isn't filename found
     * yet for the file of this task.
     */
    @Nullable public String getPath() {
        final String filename = filenameHolder.get();
        if (filename == null) return null;

        return isUriIsDirectory
                ? new File(uri.getPath(), filename).getAbsolutePath() : uri.getPath();
    }

    /**
     * Get the bytes of buffer once read from response input-stream.
     *
     * @return the bytes of buffer once read from response input-stream.
     */
    public int getReadBufferSize() {
        return this.readBufferSize;
    }

    /**
     * Get the bytes of the buffer size on BufferedOutputStream.
     *
     * @return the bytes of the buffer size on BufferedOutputStream.
     */
    public int getFlushBufferSize() {
        return this.flushBufferSize;
    }

    /**
     * Get the bytes of the buffer size before sync to the disk.
     *
     * @return the bytes of the buffer size before sync to the disk.
     */
    public int getSyncBufferSize() {
        return syncBufferSize;
    }

    /**
     * Get the interval milliseconds of the sync buffer.
     *
     * @return the interval milliseconds of the sync buffer.
     */
    public int getSyncBufferIntervalMills() {
        return syncBufferIntervalMills;
    }

    /**
     * Whether all callbacks callback to the UI thread automatically.
     *
     * @return {@code true} if all callbacks callback to the UI thread automatically.
     */
    public boolean isAutoCallbackToUIThread() {
        return autoCallbackToUIThread;
    }

    /**
     * Get the minimum interval milliseconds of progress callbacks.
     *
     * @return minimum interval milliseconds of progress callbacks.
     */
    public int getMinIntervalMillisCallbackProcess() {
        return minIntervalMillisCallbackProcess;
    }

    /**
     * Get the tag with its {@code key}, which you set through {@link #addTag(int, Object)}.
     *
     * @param key the key is identify the tag.
     * @return the tag with the {@code key}.
     */
    public Object getTag(int key) {
        return keyTagMap == null ? null : keyTagMap.get(key);
    }

    /**
     * Get the tag save on this task object reference through {@link #setTag(Object)}.
     *
     * @return the tag you set through {@link #setTag(Object)}
     */
    public Object getTag() {
        return tag;
    }

    long getLastCallbackProcessTs() {
        return lastCallbackProcessTimestamp.get();
    }

    void setLastCallbackProcessTs(long lastCallbackProcessTimestamp) {
        this.lastCallbackProcessTimestamp.set(lastCallbackProcessTimestamp);
    }

    /**
     * Add the {@code tag} identify with the {@code key} you can use it through {@link #getTag(int)}
     *
     * @param key   the identify of the tag.
     * @param value the value of the tag.
     */
    public synchronized DownloadTask addTag(int key, Object value) {
        if (keyTagMap == null) {
            synchronized (this) {
                if (keyTagMap == null) keyTagMap = new SparseArray<>();
            }
        }

        keyTagMap.put(key, value);
        return this;
    }

    /**
     * Remove the tag with the {@code key} what you set through {@link #addTag(int, Object)}.
     *
     * @param key the key of the tag.
     */
    public synchronized void removeTag(int key) {
        if (keyTagMap != null) keyTagMap.remove(key);
    }

    /**
     * Remove the tag you set through {@link #setTag(Object)}.
     */
    public synchronized void removeTag() {
        this.tag = null;
    }

    /**
     * Set tag to this task, which you can use it through {@link #getTag()}.
     *
     * @param tag the tag will be store on this task reference.
     */
    public void setTag(Object tag) {
        this.tag = tag;
    }

    /**
     * Enqueue a bunch of {@code tasks} with the listener to the downloader dispatcher.
     *
     * @param tasks    the tasks will be executed when resources is available on the dispatcher
     *                 thread-pool.
     * @param listener the listener is used for listen each {@code tasks} lifecycle.
     */
    public static void enqueue(DownloadTask[] tasks, DownloadListener listener) {
        for (DownloadTask task : tasks) {
            task.listener = listener;
        }
        OkDownload.with().downloadDispatcher().enqueue(tasks);
    }

    /**
     * Enqueue the task with the {@code listener} to the downloader dispatcher, what means it will
     * be run when resource is available and on the dispatcher thread pool.
     *
     * @param listener the listener is used for listen the whole lifecycle of the task.
     */
    public void enqueue(DownloadListener listener) {
        this.listener = listener;
        OkDownload.with().downloadDispatcher().enqueue(this);
    }

    /**
     * Execute the task with the {@code listener} on the invoke thread.
     *
     * @param listener the listener is used for listen the whole lifecycle of the task.
     */
    public void execute(DownloadListener listener) {
        this.listener = listener;
        OkDownload.with().downloadDispatcher().execute(this);
    }

    /**
     * Cancel the current task.
     * <p>
     * If the task is canceled all resourced about this task will be recycled.
     */
    public void cancel() {
        OkDownload.with().downloadDispatcher().cancel(this);
    }

    /**
     * Get the listener of the task.
     *
     * @return the listener is used for listen the whole lifecycle of the task.
     */
    public DownloadListener getListener() {
        return this.listener;
    }

    /**
     * The priority of the task, more larger means less time waiting to download.
     *
     * @return the priority of the task.
     */
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

    /**
     * Copy tags from another task.
     *
     * @param oldTask the task will provide its tags
     */
    public void setTags(DownloadTask oldTask) {
        this.tag = oldTask.tag;
        this.keyTagMap = oldTask.keyTagMap;
    }

    @Override public int compareTo(@NonNull DownloadTask o) {
        return o.getPriority() - getPriority();
    }

    /**
     * The builder of download task.
     */
    public static class Builder {
        @NonNull final String url;
        @NonNull final Uri uri;
        private volatile Map<String, List<String>> headerMapFields;

        /**
         * Create the task builder through {@code url} and the file's parent path and the filename.
         *
         * @param url        the url for the task.
         * @param parentPath the parent path of the file for store download data.
         * @param filename   the filename of the file for store download data.
         */
        public Builder(@NonNull String url, @NonNull String parentPath, @Nullable String filename) {
            this(url, new File(parentPath));
            this.filename = filename;
        }

        /**
         * Create the task builder through {@code url} and the store path {@code file}.
         *
         * @param url  the url for the task.
         * @param file the file is used for store download data of the task.
         */
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

        /**
         * Create the task builder through {@code url} and the store path {@code uri}.
         *
         * @param url the url for the task.
         * @param uri the uri indicate the file path of the task.
         */
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

        /**
         * Set whether callback to UI thread automatically.
         * default is {@link #DEFAULT_AUTO_CALLBACK_TO_UI_THREAD}
         *
         * @param autoCallbackToUIThread whether callback to ui thread automatically.
         */
        public Builder setAutoCallbackToUIThread(boolean autoCallbackToUIThread) {
            this.autoCallbackToUIThread = autoCallbackToUIThread;
            return this;
        }

        /**
         * Set the minimum internal milliseconds of progress callbacks.
         * default is {@link #DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS}
         *
         * @param minIntervalMillisCallbackProcess the minimum interval milliseconds of  progress
         *                                         callbacks.
         */
        public Builder setMinIntervalMillisCallbackProcess(int minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            return this;
        }

        /**
         * Set the request headers for this task.
         *
         * @param headerMapFields the header map fields.
         */
        public Builder setHeaderMapFields(Map<String, List<String>> headerMapFields) {
            this.headerMapFields = headerMapFields;
            return this;
        }

        /**
         * Add the request header for this task.
         *
         * @param key   the key of the field.
         * @param value the value of the field.
         */
        public synchronized void addHeader(String key, String value) {
            if (headerMapFields == null) headerMapFields = new HashMap<>();
            List<String> valueList = headerMapFields.get(key);
            if (valueList == null) {
                valueList = new ArrayList<>();
                headerMapFields.put(key, valueList);
            }
            valueList.add(value);
        }

        /**
         * Set the priority of the task, more larger more higher, more higher means less time to
         * wait to download.
         * default is 0.
         *
         * @param priority the priority of the task.
         */
        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Set the how may bytes of buffer once read from response input-stream.
         * default is {@link #DEFAULT_READ_BUFFER_SIZE}
         *
         * @param readBufferSize the bytes of buffer once read from response input-stream.
         */
        public Builder setReadBufferSize(int readBufferSize) {
            if (readBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.readBufferSize = readBufferSize;
            return this;
        }

        /**
         * Set the hwo many bytes of the buffer size on the BufferedOutputStream.
         * default is {@link #DEFAULT_FLUSH_BUFFER_SIZE}
         *
         * @param flushBufferSize the bytes of buffer size before BufferedOutputStream#flush().
         */
        public Builder setFlushBufferSize(int flushBufferSize) {
            if (flushBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.flushBufferSize = flushBufferSize;
            return this;
        }

        /**
         * Set the how many bytes of the buffer size before sync to the disk.
         * default is {@link #DEFAULT_SYNC_BUFFER_SIZE}
         *
         * @param syncBufferSize the bytes of buffer size before sync.
         */
        public Builder setSyncBufferSize(int syncBufferSize) {
            if (syncBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.syncBufferSize = syncBufferSize;
            return this;
        }

        /**
         * Set the interval milliseconds for sync download-data buffer from the memory to the disk.
         * default is {@link #DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS}
         *
         * @param syncBufferIntervalMillis the interval milliseconds for sync buffer to the disk.
         */
        public Builder setSyncBufferIntervalMillis(int syncBufferIntervalMillis) {
            if (syncBufferIntervalMillis < 0) {
                throw new IllegalArgumentException("Value must be positive!");
            }

            this.syncBufferIntervalMillis = syncBufferIntervalMillis;
            return this;
        }

        /**
         * Set the filename of the file for this task.
         * <p>
         * If you only provided the store directory path, and doesn't provide any filename, the
         * filename will get through response header, and if there isn't filename found on the
         * response header, the file name will be found through the url path.
         *
         * @param filename the filename of the file for this task.
         */
        public Builder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * Set whether the task is completed directly without any further action when check the task
         * has been downloaded.
         * default is {@link #DEFAULT_PASS_IF_ALREADY_COMPLETED}
         *
         * @param passIfAlreadyCompleted whether pass this task with completed callback directly if
         *                               this task has already completed.
         */
        public Builder setPassIfAlreadyCompleted(boolean passIfAlreadyCompleted) {
            this.passIfAlreadyCompleted = passIfAlreadyCompleted;
            return this;
        }

        /**
         * Set the task proceed only on the Wifi network state.
         * default is {@link #DEFAULT_IS_WIFI_REQUIRED}
         *
         * @param wifiRequired whether wifi required for proceed this task.
         */
        public Builder setWifiRequired(boolean wifiRequired) {
            this.isWifiRequired = wifiRequired;
            return this;
        }

        /**
         * Build the task through the builder.
         *
         * @return a new task is built from this builder.
         */
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

    @Override public int hashCode() {
        return (url + providedPathFile.toString() + filenameHolder.get()).hashCode();
    }

    @Override public String toString() {
        return super.toString() + "@" + id + "@" + url + "@" + providedPathFile.toString()
                + "/" + filenameHolder.get();
    }

    @NonNull public MockTaskForCompare mock(int id) {
        return new MockTaskForCompare(id, this);
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

    public static class MockTaskForCompare extends IdentifiedTask {
        final int id;
        @NonNull final String url;
        @NonNull final File providedPathFile;
        @Nullable final String filename;
        @NonNull final String parentPath;

        public MockTaskForCompare(int id, @NonNull DownloadTask task) {
            this.id = id;
            this.url = task.url;
            this.parentPath = task.getParentPath();
            this.providedPathFile = task.providedPathFile;
            this.filename = task.getFilename();
        }

        @Override public int getId() {
            return id;
        }

        @NonNull @Override public String getUrl() {
            return url;
        }

        @NonNull @Override protected File getProvidedPathFile() {
            return providedPathFile;
        }

        @NonNull @Override public String getParentPath() {
            return parentPath;
        }

        @Nullable @Override public String getFilename() {
            return filename;
        }
    }
}
