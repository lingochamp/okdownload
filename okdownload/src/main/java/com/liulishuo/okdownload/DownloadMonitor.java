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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

/**
 * If you set monitor to okdownload, the lifecycle of every task on okdownload will be caught and
 * callback to this monitor.
 *
 * @see OkDownload#setMonitor(DownloadMonitor)
 * @see OkDownload.Builder#setMonitor(DownloadMonitor)
 */
public interface DownloadMonitor {
    void taskStart(DownloadTask task);

    /**
     * Call this monitor function when the {@code task} just end trial connection, and its
     * {@code info} is ready and also certain this task will resume from the past breakpoint.
     *
     * @param task the target task.
     * @param info has certainly total-length and offset-length now.
     */
    void taskDownloadFromBreakpoint(@NonNull DownloadTask task, @NonNull BreakpointInfo info);

    /**
     * Call this monitor function when the {@code task} just end trial connection, and its
     * {@code info} is ready and also certain this task will download from the very beginning.
     *
     * @param task  the target task.
     * @param info  has certainly total-length and offset-length now.
     * @param cause the cause of why download from the very beginning instead of from the past
     *              breakpoint.
     */
    void taskDownloadFromBeginning(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                   @Nullable ResumeFailedCause cause);

    void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause);
}
