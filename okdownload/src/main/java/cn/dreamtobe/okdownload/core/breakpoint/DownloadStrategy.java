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

package cn.dreamtobe.okdownload.core.breakpoint;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.cause.ResumeFailedCause;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.dispatcher.CallbackDispatcher;
import cn.dreamtobe.okdownload.core.exception.ResumeFailedException;
import cn.dreamtobe.okdownload.core.exception.ServerCancelledException;

import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.FILE_NOT_EXIST;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.INFO_DIRTY;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.RESPONSE_CREATED_RANGE_NOT_FROM_0;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.RESPONSE_ETAG_CHANGED;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.RESPONSE_PRECONDITION_FAILED;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.RESPONSE_RESET_RANGE_NOT_FROM_0;

public class DownloadStrategy {

    // 1 connection: [0, 1MB)
    private static final long ONE_CONNECTION_UPPER_LIMIT = 1024 * 1024; // 1MB
    // 2 connection: [1MB, 5MB)
    private static final long TWO_CONNECTION_UPPER_LIMIT = 5 * 1024 * 1024; // 5MB
    // 3 connection: [5MB, 50MB)
    private static final long THREE_CONNECTION_UPPER_LIMIT = 50 * 1024 * 1024; // 50MB
    // 4 connection: [50MB, 100MB)
    private static final long FOUR_CONNECTION_UPPER_LIMIT = 100 * 1024 * 1024; // 100MB

    public ResumeAvailableLocalCheck resumeAvailableLocalCheck(DownloadTask task,
                                                               BreakpointInfo info) {
        return new ResumeAvailableLocalCheck(task, info);
    }

    public ResumeAvailableResponseCheck resumeAvailableResponseCheck(
            DownloadConnection.Connected connected,
            int blockIndex,
            BreakpointInfo info) {
        return new ResumeAvailableResponseCheck(connected, blockIndex, info);
    }

    public int determineBlockCount(@NonNull DownloadTask task, long totalLength,
                                   @NonNull DownloadConnection.Connected connected) {
        if (totalLength < ONE_CONNECTION_UPPER_LIMIT) {
            return 1;
        }

        if (totalLength < TWO_CONNECTION_UPPER_LIMIT) {
            return 2;
        }

        if (totalLength < THREE_CONNECTION_UPPER_LIMIT) {
            return 3;
        }

        if (totalLength < FOUR_CONNECTION_UPPER_LIMIT) {
            return 4;
        }

        return 5;
    }

    public static class ResumeAvailableLocalCheck {
        private final boolean isAvailable;
        private final boolean fileExist;
        private final boolean infoRight;
        private final DownloadTask task;
        private final BreakpointInfo info;

        protected ResumeAvailableLocalCheck(DownloadTask task, BreakpointInfo info) {
            this.fileExist = new File(task.getPath()).exists();
            this.infoRight = info.getBlockCount() > 0;
            this.isAvailable = infoRight && fileExist;

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
            } else if (!fileExist) {
                dispatcher.dispatch().downloadFromBeginning(task, info, FILE_NOT_EXIST);
            } else if (!infoRight) {
                dispatcher.dispatch().downloadFromBeginning(task, info, INFO_DIRTY);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public static class ResumeAvailableResponseCheck {
        private DownloadConnection.Connected connected;
        private BreakpointInfo info;
        private int blockIndex;

        protected ResumeAvailableResponseCheck(DownloadConnection.Connected connected,
                                               int blockIndex, BreakpointInfo info) {
            this.connected = connected;
            this.info = info;
            this.blockIndex = blockIndex;
        }

        public void inspect() throws IOException {
            final BlockInfo blockInfo = info.getBlock(blockIndex);
            boolean isServerCancelled = false;
            ResumeFailedCause resumeFailedCause = null;

            final int code = connected.getResponseCode();
            final String etag = info.getEtag();
            final String newEtag = connected.getResponseHeaderField("Etag");

            do {
                if (code == HttpURLConnection.HTTP_PRECON_FAILED) {
                    resumeFailedCause = RESPONSE_PRECONDITION_FAILED;
                    break;
                }

                if (!TextUtils.isEmpty(newEtag) && !newEtag.equals(etag)) {
                    // etag changed.
                    resumeFailedCause = RESPONSE_ETAG_CHANGED;
                    break;
                }

                if (code == HttpURLConnection.HTTP_CREATED && blockInfo.getCurrentOffset() != 0) {
                    // The request has been fulfilled and has resulted in one or more new resources
                    // being created.
                    // mark this case is precondition failed for
                    // 1. checkout whether accept partial
                    // 2. 201 means new resources so range must be from beginning otherwise it can't
                    // match local range.
                    resumeFailedCause = RESPONSE_CREATED_RANGE_NOT_FROM_0;
                    break;
                }

                if (code == HttpURLConnection.HTTP_RESET && blockInfo.getCurrentOffset() != 0) {
                    resumeFailedCause = RESPONSE_RESET_RANGE_NOT_FROM_0;
                    break;
                }

                if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
                    isServerCancelled = true;
                    break;
                }

                if (code == HttpURLConnection.HTTP_OK && blockInfo.getCurrentOffset() != 0) {
                    isServerCancelled = true;
                    break;
                }
            } while (false);

            if (resumeFailedCause != null) {
                // resume failed, relaunch from beginning.
                throw new ResumeFailedException(resumeFailedCause);
            }

            if (isServerCancelled) {
                // server cancelled, end task.
                throw new ServerCancelledException(code);
            }
        }
    }
}
