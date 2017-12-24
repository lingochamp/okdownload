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

package com.liulishuo.okdownload.core.listener;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.assist.DownloadListener1Assist;

import java.util.List;
import java.util.Map;

/**
 * taskStart->connect->progress<-->progress(currentOffset)->taskEnd
 */
public abstract class DownloadListener1 implements DownloadListener,
        DownloadListener1Assist.Listener1Callback {
    final DownloadListener1Assist assist;

    DownloadListener1(DownloadListener1Assist assist) {
        this.assist = assist;
        assist.setCallback(this);
    }

    protected long getTotalLength(int id) {
        final DownloadListener1Assist.Listener1Model model = assist.findModel(id);
        return model == null ? 0 : model.getTotalLength();
    }

    /**
     * If you only have one task attach to this listener instance, you can use this method without
     * provide task id, otherwise please use {@link #getTotalLength(int)} instead.
     */
    protected long getTotalLength() {
        final DownloadListener1Assist.Listener1Model model = assist.getSingleTaskModel();
        return model == null ? 0 : model.getTotalLength();
    }

    public DownloadListener1() {
        this(new DownloadListener1Assist());
    }

    @Override public void taskStart(DownloadTask task) {
        assist.taskStart(task.getId());
    }

    @Override
    public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
        assist.taskEnd(task.getId());
    }

    @Override public void downloadFromBeginning(DownloadTask task, BreakpointInfo info,
                                                ResumeFailedCause cause) {
        assist.downloadFromBeginning(task, cause);
    }

    @Override public void downloadFromBreakpoint(DownloadTask task, BreakpointInfo info) {
        assist.downloadFromBreakpoint(task.getId(), info);
    }

    @Override public void connectStart(DownloadTask task, int blockIndex,
                                       @NonNull Map<String, List<String>> requestHeaderFields) {
    }

    @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                     @NonNull Map<String, List<String>> responseHeaderFields) {
        assist.connectEnd(task);
    }

    @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        assist.splitBlockEnd(task, info);
    }

    @Override public void fetchStart(DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        assist.fetchProgress(task, increaseBytes);
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex, long contentLength) {
    }
}

