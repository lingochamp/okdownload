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

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.List;
import java.util.Map;

/**
 * taskStart->taskEnd
 */
public abstract class DownloadListener2 implements DownloadListener {
    @Override
    public void connectTrialStart(@NonNull DownloadTask task,
                                  @NonNull Map<String, List<String>> requestHeaderFields) {
    }

    @Override public void connectTrialEnd(@NonNull DownloadTask task, int responseCode,
                                          @NonNull Map<String, List<String>> responseHeaderFields) {
    }

    @Override
    public void downloadFromBeginning(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                      @NonNull ResumeFailedCause cause) {
    }

    @Override
    public void downloadFromBreakpoint(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {
    }

    @Override public void connectStart(@NonNull DownloadTask task, int blockIndex,
                                       @NonNull Map<String, List<String>> requestHeaderFields) {
    }

    @Override public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode,
                                     @NonNull Map<String, List<String>> responseHeaderFields) {
    }

    @Override
    public void fetchStart(@NonNull DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override
    public void fetchProgress(@NonNull DownloadTask task, int blockIndex, long increaseBytes) {
    }

    @Override public void fetchEnd(@NonNull DownloadTask task, int blockIndex, long contentLength) {
    }
}
