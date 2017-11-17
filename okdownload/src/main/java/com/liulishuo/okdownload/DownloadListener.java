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

package com.liulishuo.okdownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

/**
 * @see com.liulishuo.okdownload.core.listener.DownloadListener1
 * @see com.liulishuo.okdownload.core.listener.DownloadListener2
 * @see com.liulishuo.okdownload.core.listener.DownloadListener3
 * @see com.liulishuo.okdownload.core.listener.DownloadListener4
 * @see com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed
 */
public interface DownloadListener {
    void taskStart(DownloadTask task);

    void breakpointData(DownloadTask task, @Nullable BreakpointInfo info);

    void downloadFromBeginning(DownloadTask task, BreakpointInfo info, ResumeFailedCause cause);

    void downloadFromBreakpoint(DownloadTask task, BreakpointInfo info);

    void connectStart(DownloadTask task, int blockIndex,
                      @NonNull Map<String, List<String>> requestHeaderFields);

    void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                    @NonNull Map<String, List<String>> responseHeaderFields);

    void splitBlockEnd(DownloadTask task, BreakpointInfo info);

    void fetchStart(DownloadTask task, int blockIndex, long contentLength);

    void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes);

    void fetchEnd(DownloadTask task, int blockIndex, long contentLength);

    void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause);
}


