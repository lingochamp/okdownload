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

package com.liulishuo.okdownload.core.file;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.dispatcher.CallbackDispatcher;
import com.liulishuo.okdownload.core.download.DownloadStrategy;

import java.io.File;
import java.io.IOException;

import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.FILE_NOT_EXIST;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.INFO_DIRTY;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.OUTPUT_STREAM_NOT_SUPPORT;

public class ProcessFileStrategy {
    @NonNull public MultiPointOutputStream createProcessStream(@NonNull DownloadTask task,
                                                               @NonNull BreakpointInfo info,
                                                               @NonNull DownloadStore store) {
        return new MultiPointOutputStream(task, info, store);
    }

    public void completeProcessStream(@NonNull MultiPointOutputStream processOutputStream,
                                      @NonNull DownloadTask task) {
    }

    public void discardProcess(@NonNull DownloadTask task) throws IOException {
        // Remove target file.
        final String path = task.getPath();
        // Do nothing, because the filename hasn't found yet.
        if (path == null) return;

        final File file = new File(path);
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Delete file failed!");
            }
        }
    }

    public void discardOldFile(@NonNull File oldFile) {
        oldFile.delete();
    }

    /**
     * @see DownloadStrategy#resumeAvailableResponseCheck
     */
    public ResumeAvailableLocalCheck resumeAvailableLocalCheck(DownloadTask task,
                                                               BreakpointInfo info) {
        return new ResumeAvailableLocalCheck(task, info);
    }

    public boolean isPreAllocateLength() {
        // if support seek, enable pre-allocate length.
        return OkDownload.with().outputStreamFactory().supportSeek();
    }

    public static class ResumeAvailableLocalCheck {
        Boolean isAvailable;
        boolean fileExist;
        boolean infoRight;
        boolean outputStreamSupport;
        private final DownloadTask task;
        private final BreakpointInfo info;

        protected ResumeAvailableLocalCheck(DownloadTask task, BreakpointInfo info) {
            this.task = task;
            this.info = info;
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

        public boolean isAvailable() {
            checkIfNeed();
            return this.isAvailable;
        }

        private void checkIfNeed() {
            if (isAvailable == null) {
                fileExist = isFileExistToResume();
                infoRight = isInfoRightToResume();
                outputStreamSupport = isOutputStreamSupportResume();
                isAvailable = infoRight && fileExist && outputStreamSupport;
            }
        }

        @Nullable public ResumeFailedCause getCause() {
            checkIfNeed();

            if (!infoRight) {
                return INFO_DIRTY;
            } else if (!fileExist) {
                return FILE_NOT_EXIST;
            } else if (!outputStreamSupport) {
                return OUTPUT_STREAM_NOT_SUPPORT;
            }

            return null;
        }

        public void callbackCause() {
            checkIfNeed();

            final CallbackDispatcher dispatcher = OkDownload.with().callbackDispatcher();
            if (isAvailable) {
                dispatcher.dispatch().downloadFromBreakpoint(task, info);
            } else {
                final ResumeFailedCause failedCause = getCause();
                if (failedCause == null) {
                    throw new IllegalStateException();
                } else {
                    dispatcher.dispatch().downloadFromBeginning(task, info, failedCause);
                }

            }
        }
    }
}
