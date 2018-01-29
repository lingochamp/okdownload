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
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist;

import java.util.List;
import java.util.Map;

/**
 * taskStart->(retry)->connect->progress<-->progress(currentOffset)->taskEnd
 */
public abstract class DownloadListener1 implements DownloadListener,
        Listener1Assist.Listener1Callback {
    final Listener1Assist assist;

    DownloadListener1(Listener1Assist assist) {
        this.assist = assist;
        assist.setCallback(this);
    }

    public DownloadListener1() {
        this(new Listener1Assist());
    }

    @Override public final void taskStart(@NonNull DownloadTask task) {
        assist.taskStart(task);
    }

    @Override
    public void downloadFromBeginning(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                      @NonNull ResumeFailedCause cause) {
        assist.downloadFromBeginning(task, cause);
    }

    @Override
    public void downloadFromBreakpoint(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {
        assist.downloadFromBreakpoint(task.getId(), info);
    }

    @Override public void connectStart(@NonNull DownloadTask task, int blockIndex,
                                       @NonNull Map<String, List<String>> requestHeaderFields) {
    }

    @Override public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode,
                                     @NonNull Map<String, List<String>> responseHeaderFields) {
        assist.connectEnd(task);
    }

    @Override public void splitBlockEnd(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {
        assist.splitBlockEnd(task, info);
    }

    @Override
    public void fetchStart(@NonNull DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override
    public void fetchProgress(@NonNull DownloadTask task, int blockIndex, long increaseBytes) {
        assist.fetchProgress(task, increaseBytes);
    }

    @Override public void fetchEnd(@NonNull DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override
    public final void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                              @Nullable Exception realCause) {
        assist.taskEnd(task, cause, realCause);
    }

}

