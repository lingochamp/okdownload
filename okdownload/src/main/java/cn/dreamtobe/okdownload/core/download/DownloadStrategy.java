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

package cn.dreamtobe.okdownload.core.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.cause.ResumeFailedCause;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.exception.ResumeFailedException;
import cn.dreamtobe.okdownload.core.exception.ServerCancelledException;

import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.RESPONSE_CREATED_RANGE_NOT_FROM_0;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.RESPONSE_ETAG_CHANGED;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.RESPONSE_PRECONDITION_FAILED;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.RESPONSE_RESET_RANGE_NOT_FROM_0;
import static cn.dreamtobe.okdownload.core.download.DownloadChain.CHUNKED_CONTENT_LENGTH;

public class DownloadStrategy {

    // 1 connection: [0, 1MB)
    private static final long ONE_CONNECTION_UPPER_LIMIT = 1024 * 1024; // 1MiB
    // 2 connection: [1MB, 5MB)
    private static final long TWO_CONNECTION_UPPER_LIMIT = 5 * 1024 * 1024; // 5MiB
    // 3 connection: [5MB, 50MB)
    private static final long THREE_CONNECTION_UPPER_LIMIT = 50 * 1024 * 1024; // 50MiB
    // 4 connection: [50MB, 100MB)
    private static final long FOUR_CONNECTION_UPPER_LIMIT = 100 * 1024 * 1024; // 100MiB

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

    public boolean isSplitBlock(final long contentLength,
                                @NonNull DownloadConnection.Connected connected) throws
            IOException {
        // chunked
        if (contentLength == CHUNKED_CONTENT_LENGTH) return false;

        // output stream not support seek
        if (!OkDownload.with().outputStreamFactory().supportSeek()) return false;

        // partial, support range
        return connected.getResponseCode() == HttpURLConnection.HTTP_PARTIAL;
    }

    private static final Pattern TMP_FILE_NAME_PATTERN = Pattern.compile(".*\\\\|/([^\\\\|/|?]*)\\??");

    public void validFilenameFromResume(@NonNull String filenameOnStore,
                                        @NonNull DownloadTask task) {
        final String filename = task.getFilename();
        if (Util.isEmpty(filename)) {
            task.getFilenameHolder().set(filenameOnStore);
        }
    }

    public void validFilenameFromResponse(@Nullable String responseFileName,
                                          @NonNull DownloadTask task,
                                          @NonNull BreakpointInfo info,
                                          @NonNull DownloadConnection.Connected connected) throws
            IOException {
        if (Util.isEmpty(task.getFilename())) {
            final String filename = determineFilename(responseFileName, task, connected);

            // Double check avoid changed by other block.
            if (Util.isEmpty(task.getFilename())) {
                synchronized (task) {
                    if (Util.isEmpty(task.getFilename())) {
                        task.getFilenameHolder().set(filename);
                        info.getFilenameHolder().set(filename);
                    }

                }
            }
        }
    }

    protected String determineFilename(@Nullable String responseFileName,
                                       @NonNull DownloadTask task,
                                       @NonNull DownloadConnection.Connected connected) throws
            IOException {

        if (Util.isEmpty(responseFileName)) {

            final String url = task.getUrl();
            Matcher m = TMP_FILE_NAME_PATTERN.matcher(url);
            String filename = null;
            while (m.find()) {
                filename = m.group(1);
            }

            if (Util.isEmpty(filename)) {
                filename = Util.md5(url);
            }

            if (filename == null) {
                throw new IOException("Can't find valid filename.");
            }

            return filename;
        }

        return responseFileName;
    }

    public static class FilenameHolder {
        private volatile String filename;

        public FilenameHolder() { }

        public FilenameHolder(@NonNull String filename) {
            this.filename = filename;
        }

        void set(@NonNull String filename) {
            this.filename = filename;
        }

        @Nullable public String get() { return filename; }

        @Override public boolean equals(Object obj) {
            if (super.equals(obj)) return true;

            if (obj instanceof FilenameHolder) {
                if (filename == null) return ((FilenameHolder) obj).filename == null;
                else return filename.equals(((FilenameHolder) obj).filename);
            }

            return false;
        }

        @Override public int hashCode() {
            return filename == null ? 0 : filename.hashCode();
        }
    }

    public static class ResumeAvailableResponseCheck {
        @NonNull private DownloadConnection.Connected connected;
        @NonNull private BreakpointInfo info;
        private int blockIndex;

        protected ResumeAvailableResponseCheck(@NonNull DownloadConnection.Connected connected,
                                               int blockIndex, @NonNull BreakpointInfo info) {
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

                if (!Util.isEmpty(etag) && !Util.isEmpty(newEtag) && !newEtag.equals(etag)) {
                    // etag changed.
                    // also etag changed is relate to HTTP_PRECON_FAILED
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
                throw new ServerCancelledException(code, blockInfo.getCurrentOffset());
            }
        }
    }
}
