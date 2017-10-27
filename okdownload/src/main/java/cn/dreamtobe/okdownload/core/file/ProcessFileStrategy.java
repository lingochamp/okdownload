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

package cn.dreamtobe.okdownload.core.file;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.dispatcher.CallbackDispatcher;
import cn.dreamtobe.okdownload.core.download.DownloadStrategy;

import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.FILE_NOT_EXIST;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.INFO_DIRTY;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.OUTPUT_STREAM_NOT_SUPPORT;

public class ProcessFileStrategy {
    @NonNull public MultiPointOutputStream createProcessStream(@NonNull DownloadTask task,
                                                               @NonNull BreakpointInfo info) {
        return new MultiPointOutputStream(task, info);
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
        private final boolean isAvailable;
        private final boolean fileExist;
        private final boolean infoRight;
        private final boolean outputStreamSupport;
        private final DownloadTask task;
        private final BreakpointInfo info;

        protected ResumeAvailableLocalCheck(DownloadTask task, BreakpointInfo info) {
            final String filePath = task.getPath();

            File fileOnTask = null;
            if (filePath == null) {
                this.fileExist = false;
            } else {
                fileOnTask = new File(filePath);
                this.fileExist = fileOnTask.exists();
            }

            final String pathOnInfo = info.getPath();
            this.infoRight = info.getBlockCount() > 0 && pathOnInfo != null
                    && new File(pathOnInfo).equals(fileOnTask);

            final boolean supportSeek = OkDownload.with().outputStreamFactory().supportSeek();
            this.outputStreamSupport = (info.getBlockCount() > 1 && supportSeek)
                    // pre-allocate but can't support seek, so can't resume, even though one block.
                    || (info.getBlockCount() == 1 && !supportSeek
                    && !OkDownload.with().processFileStrategy().isPreAllocateLength())
                    || (info.getBlockCount() == 1 && supportSeek);

            this.isAvailable = infoRight && fileExist && outputStreamSupport;

            this.task = task;
            this.info = info;
        }

        public boolean isAvailable() {
            return this.isAvailable;
        }

        public void callbackCause() {
            final CallbackDispatcher dispatcher = OkDownload.with().callbackDispatcher();
            if (isAvailable) {
                dispatcher.dispatch().downloadFromBreakpoint(task, info);
            } else if (!infoRight) {
                dispatcher.dispatch().downloadFromBeginning(task, info, INFO_DIRTY);
            } else if (!fileExist) {
                dispatcher.dispatch().downloadFromBeginning(task, info, FILE_NOT_EXIST);
            } else if (!outputStreamSupport) {
                dispatcher.dispatch().downloadFromBeginning(task, info, OUTPUT_STREAM_NOT_SUPPORT);
            } else {
                throw new IllegalStateException();
            }
        }
    }
}
