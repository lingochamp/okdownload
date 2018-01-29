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

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.List;
import java.util.Map;

/**
 * @see com.liulishuo.okdownload.core.listener.DownloadListener1
 * @see com.liulishuo.okdownload.core.listener.DownloadListener2
 * @see com.liulishuo.okdownload.core.listener.DownloadListener3
 * @see com.liulishuo.okdownload.core.listener.DownloadListener4
 * @see com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed
 */
public interface DownloadListener {
    void taskStart(@NonNull DownloadTask task);

    void downloadFromBeginning(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                               @NonNull ResumeFailedCause cause);

    void downloadFromBreakpoint(@NonNull DownloadTask task, @NonNull BreakpointInfo info);

    void connectStart(@NonNull DownloadTask task, @IntRange(from = 0) int blockIndex,
                      @NonNull Map<String, List<String>> requestHeaderFields);

    void connectEnd(@NonNull DownloadTask task, @IntRange(from = 0) int blockIndex,
                    int responseCode,
                    @NonNull Map<String, List<String>> responseHeaderFields);

    void splitBlockEnd(@NonNull DownloadTask task, @NonNull BreakpointInfo info);

    void fetchStart(@NonNull DownloadTask task, @IntRange(from = 0) int blockIndex,
                    @IntRange(from = 0) long contentLength);

    void fetchProgress(@NonNull DownloadTask task, @IntRange(from = 0) int blockIndex,
                       @IntRange(from = 0) long increaseBytes);

    void fetchEnd(@NonNull DownloadTask task, @IntRange(from = 0) int blockIndex,
                  @IntRange(from = 0) long contentLength);

    void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                 @Nullable Exception realCause);
}
