/*
 * Copyright (c) 2018 LingoChamp Inc.
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

package com.liulishuo.okdownload.core.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.exception.FileBusyAfterRunException;
import com.liulishuo.okdownload.core.exception.ServerCanceledException;

import java.io.IOException;

/**
 * Check whether the breakpoint is valid to reuse through the backend response.
 */
public class BreakpointRemoteCheck {

    private boolean isAcceptRange;
    private boolean isResumable;
    ResumeFailedCause failedCause;
    private long instanceLength;

    @NonNull private final DownloadTask task;
    @NonNull private final BreakpointInfo info;

    public BreakpointRemoteCheck(@NonNull DownloadTask task,
                                 @NonNull BreakpointInfo info) {
        this.task = task;
        this.info = info;
    }

    /**
     * Get the remote check failed cause.
     *
     * @return {@code null} only if the result of {@link #isResumable()} is {@code false}.
     */
    @Nullable public ResumeFailedCause getCause() {
        return this.failedCause;
    }

    /**
     * Get the remote check failed cause, and if the cause can't be found will thrown the
     * {@link IllegalStateException}.
     *
     * @return the failed cause.
     */
    @NonNull public ResumeFailedCause getCauseOrThrow() {
        if (failedCause == null) {
            throw new IllegalStateException("No cause find with isResumable: " + isResumable);
        }
        return this.failedCause;
    }

    public boolean isResumable() {
        return isResumable;
    }

    public boolean isAcceptRange() {
        return isAcceptRange;
    }

    public long getInstanceLength() {
        return instanceLength;
    }

    public void check() throws IOException {
        // local etag
        final DownloadStrategy downloadStrategy = OkDownload.with().downloadStrategy();

        // execute trial
        ConnectTrial connectTrial = createConnectTrial();
        connectTrial.executeTrial();

        // single/multi
        final boolean isAcceptRange = connectTrial.isAcceptRange();
        final boolean isChunked = connectTrial.isChunked();
        // data
        final long instanceLength = connectTrial.getInstanceLength();
        final String responseEtag = connectTrial.getResponseEtag();
        final String responseFilename = connectTrial.getResponseFilename();
        final int responseCode = connectTrial.getResponseCode();

        // 1. assemble basic data.
        downloadStrategy.validFilenameFromResponse(responseFilename, task, info);
        info.setChunked(isChunked);
        info.setEtag(responseEtag);

        if (OkDownload.with().downloadDispatcher().isFileConflictAfterRun(task)) {
            throw FileBusyAfterRunException.SIGNAL;
        }

        // 2. collect result
        final ResumeFailedCause resumeFailedCause = downloadStrategy
                .getPreconditionFailedCause(responseCode, info.getTotalOffset() != 0, info,
                        responseEtag);

        this.isResumable = resumeFailedCause == null;
        this.failedCause = resumeFailedCause;
        this.instanceLength = instanceLength;
        this.isAcceptRange = isAcceptRange;

        //2. check whether server cancelled.
        if (downloadStrategy.isServerCanceled(responseCode, info.getTotalOffset() != 0)) {
            throw new ServerCanceledException(responseCode, info.getTotalOffset());
        }
    }

    // convenient for unit-test.
    ConnectTrial createConnectTrial() {
        return new ConnectTrial(task, info);
    }

}
