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

package cn.dreamtobe.okdownload.task;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;

public class DownloadTask {
    private Integer id = null;
    private final TaskOptimizer optimizer;

    final String url;
    final Uri uri;
    private volatile HashMap<String, List<String>> headerMapFields;


    private int priority;
    private DownloadListener listener;

    private String redirectLocation;

    public DownloadTask(String url, Uri uri) {
        this.url = url;
        this.uri = uri;
        this.optimizer = new TaskOptimizer();
    }

    @Nullable
    public Map<String, List<String>> getHeaderMapFields() {
        return this.headerMapFields;
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

    public TaskOptimizer optimize() {
        return this.optimizer;
    }

    /**
     * This id can be used on {@link BreakpointStore}
     */
    public int getId() {
        if (id == null) throw new IllegalArgumentException();
        return this.id;
    }

    public Uri getUri() {
        return uri;
    }

    public int getReadBufferSize() {
        return this.optimizer.getReadBufferSize();
    }

    public void enqueue(DownloadListener listener) {
        this.listener = listener;
        OkDownload.with().downloadDispatcher.enqueue(this);
    }

    public void execute(DownloadListener listener) {
        this.listener = listener;
        listener.taskStart(this);
//        try {
//            DownloadCall.create(this).start();
//        } finally {
//            OkDownload.with().downloadDispatcher.finish(this);
//        }
    }

    public DownloadListener getListener() {
        return this.listener;
    }

    void validId() {
    }
}
