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

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.io.File;

import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.FILE_NOT_EXIST;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.INFO_DIRTY;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.OUTPUT_STREAM_NOT_SUPPORT;

/**
 * Check whether the breakpoint is valid to reuse through the local data.
 */
public class BreakpointLocalCheck {

    private boolean isDirty;
    boolean fileExist;
    boolean infoRight;
    boolean outputStreamSupport;

    private final DownloadTask task;
    private final BreakpointInfo info;

    public BreakpointLocalCheck(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {
        this.task = task;
        this.info = info;
    }

    /**
     * Get whether the local data is dirty. only if one of the {@code fileExist} or
     * {@code infoRight} or {@code outputStreamSupport} is {@code false}, the result will be
     * {@code false}.
     *
     * @return whether the local data is dirty.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Get cause and if the cause isn't exist  throw the {@link IllegalStateException}.
     *
     * @return the cause of resume failed.
     */
    @NonNull public ResumeFailedCause getCauseOrThrow() {
        if (!infoRight) {
            return INFO_DIRTY;
        } else if (!fileExist) {
            return FILE_NOT_EXIST;
        } else if (!outputStreamSupport) {
            return OUTPUT_STREAM_NOT_SUPPORT;
        }

        throw new IllegalStateException("No cause find with isDirty: " + isDirty);
    }

    public boolean isInfoRightToResume() {
        final int blockCount = info.getBlockCount();

        if (blockCount <= 0) return false;
        if (info.isChunked()) return false;
        if (info.getPath() == null) return false;
        final File fileOnTask = task.getPath() == null ? null : new File(task.getPath());
        if (!new File(info.getPath()).equals(fileOnTask)) return false;

        for (int i = 0; i < blockCount; i++) {
            BlockInfo blockInfo = info.getBlock(i);
            if (blockInfo.getContentLength() <= 0) return false;
        }

        return true;
    }

    public boolean isOutputStreamSupportResume() {
        final boolean supportSeek = OkDownload.with().outputStreamFactory().supportSeek();
        if (supportSeek) return true;

        if (info.getBlockCount() != 1) return false;
        if (OkDownload.with().processFileStrategy().isPreAllocateLength()) return false;

        return true;
    }

    public boolean isFileExistToResume() {
        final String filePath = task.getPath();
        return filePath != null && new File(filePath).exists();
    }

    public void check() {
        fileExist = isFileExistToResume();
        infoRight = isInfoRightToResume();
        outputStreamSupport = isOutputStreamSupportResume();
        isDirty = !infoRight || !fileExist || !outputStreamSupport;
    }
}
