/*
 * Copyright (C) 2017 Jacksgong(jacksgong.com)
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

package cn.dreamtobe.okdownload;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;

public class DownloadTask {
    private final int id;
    private final String url;
    private final Uri uri;
    private final HashMap<String, List<String>> headerMapFields;


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

    private final boolean autoCallbackToUIThread;
    private final int minIntervalMillisCallbackProcess;
    // end optimize ------------------

    private DownloadListener listener;
    private volatile SparseArray<Object> keyTagMap;
    private Object tag;

    private AtomicLong lastCallbackProcessTimestamp;

    public DownloadTask(String url, Uri uri, int priority, int readBufferSize, int flushBufferSize,
                        int syncBufferSize, int syncBufferIntervalMills,
                        boolean autoCallbackToUIThread, int minIntervalMillisCallbackProcess,
                        HashMap<String, List<String>> headerMapFields) {
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
        this.id = OkDownload.with().breakpointStore().createId(this);
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

    public Uri getUri() {
        return uri;
    }

    public String getPath() {
        return uri.getPath();
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

    public long getLastCallbackProcessTs() {
        return lastCallbackProcessTimestamp.get();
    }

    public void setLastCallbackProcessTs(long lastCallbackProcessTimestamp) {
        this.lastCallbackProcessTimestamp.set(lastCallbackProcessTimestamp);
    }

    public synchronized void addTag(int key, Object value) {
        if (keyTagMap == null) {
            synchronized (this) {
                if (keyTagMap == null) keyTagMap = new SparseArray<>();
            }
        }

        keyTagMap.put(key, value);
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public void enqueue(DownloadListener listener) {
        this.listener = listener;
        OkDownload.with().downloadDispatcher().enqueue(this);
    }

    public void execute(DownloadListener listener) {
        this.listener = listener;
        OkDownload.with().downloadDispatcher().execute(this);
    }

    public DownloadListener getListener() {
        return this.listener;
    }

    public int getPriority() {
        return priority;
    }

    public Builder toBuilder() {
        return new Builder(this.url, this.uri)
                .setPriority(priority)
                .setReadBufferSize(readBufferSize)
                .setFlushBufferSize(flushBufferSize)
                .setSyncBufferSize(syncBufferSize)
                .setSyncBufferIntervalMillis(syncBufferIntervalMills)
                .setAutoCallbackToUIThread(autoCallbackToUIThread)
                .setMinIntervalMillisCallbackProcess(minIntervalMillisCallbackProcess)
                .setHeaderMapFields(headerMapFields);
    }

    public static class Builder {
        final String url;
        final Uri uri;
        private volatile HashMap<String, List<String>> headerMapFields;

        public Builder(String url, Uri uri) {
            this.url = url;
            this.uri = uri;
        }

        // More larger more high.
        private int priority;

        private int readBufferSize = 4096/* byte **/;
        private int flushBufferSize = 16384/* byte **/;

        /**
         * Make sure sync to physical filesystem.
         */
        private int syncBufferSize = 65536/* byte **/;
        private int syncBufferIntervalMillis = 3000/* millis **/;

        private boolean autoCallbackToUIThread = true;
        private int minIntervalMillisCallbackProcess = 320/* millis **/;

        public Builder setAutoCallbackToUIThread(boolean autoCallbackToUIThread) {
            this.autoCallbackToUIThread = autoCallbackToUIThread;
            return this;
        }

        public Builder setMinIntervalMillisCallbackProcess(int minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            return this;
        }

        public Builder setHeaderMapFields(HashMap<String, List<String>> headerMapFields) {
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

        public DownloadTask build() {
            return new DownloadTask(url, uri, priority, readBufferSize, flushBufferSize,
                    syncBufferSize, syncBufferIntervalMillis,
                    autoCallbackToUIThread, minIntervalMillisCallbackProcess,
                    headerMapFields);
        }
    }
}
