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

package com.liulishuo.okdownload.core.download;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.exception.NetworkPolicyException;
import com.liulishuo.okdownload.core.exception.ResumeFailedException;
import com.liulishuo.okdownload.core.exception.ServerCanceledException;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.liulishuo.okdownload.core.Util.ETAG;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.RESPONSE_CREATED_RANGE_NOT_FROM_0;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.RESPONSE_ETAG_CHANGED;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.RESPONSE_PRECONDITION_FAILED;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.RESPONSE_RESET_RANGE_NOT_FROM_0;

public class DownloadStrategy {

    private static final String TAG = "DownloadStrategy";

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

    public int determineBlockCount(@NonNull DownloadTask task, long totalLength) {
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

    public long reuseIdledSameInfoThresholdBytes() {
        return 10240;
    }

    // this case meet only if there are another info task is idle and is the same after
    // this task has filename.
    public boolean inspectAnotherSameInfo(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                          long instanceLength) {
        if (!task.isFilenameFromResponse()) return false;

        final BreakpointStore store = OkDownload.with().breakpointStore();
        final BreakpointInfo anotherInfo = store.findAnotherInfoFromCompare(task, info);
        if (anotherInfo == null) return false;

        store.remove(anotherInfo.getId());

        if (anotherInfo.getTotalOffset()
                <= OkDownload.with().downloadStrategy().reuseIdledSameInfoThresholdBytes()) {
            return false;
        }

        if (anotherInfo.getEtag() != null && !anotherInfo.getEtag().equals(info.getEtag())) {
            return false;
        }

        if (anotherInfo.getTotalLength() != instanceLength) {
            return false;
        }

        if (anotherInfo.getFile() == null || !anotherInfo.getFile().exists()) return false;

        info.reuseBlocks(anotherInfo);

        Util.d(TAG, "Reuse another same info: " + info);
        return true;
    }

    public boolean isUseMultiBlock(final boolean isAcceptRange) {

        // output stream not support seek
        if (!OkDownload.with().outputStreamFactory().supportSeek()) return false;

        //  support range
        return isAcceptRange;
    }

    private static final Pattern TMP_FILE_NAME_PATTERN = Pattern
            .compile(".*\\\\|/([^\\\\|/|?]*)\\??");

    public void inspectFilenameFromResume(@NonNull String filenameOnStore,
                                          @NonNull DownloadTask task) {
        final String filename = task.getFilename();
        if (Util.isEmpty(filename)) {
            task.getFilenameHolder().set(filenameOnStore);
        }
    }

    public void validFilenameFromResponse(@Nullable String responseFileName,
                                          @NonNull DownloadTask task,
                                          @NonNull BreakpointInfo info) throws IOException {
        if (Util.isEmpty(task.getFilename())) {
            final String filename = determineFilename(responseFileName, task);

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
                                       @NonNull DownloadTask task) throws IOException {

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

    /**
     * @return {@code true} success valid filename from store.
     */
    public boolean validFilenameFromStore(@NonNull DownloadTask task) {
        final String filename = OkDownload.with().breakpointStore()
                .getResponseFilename(task.getUrl());
        if (filename == null) return false;

        task.getFilenameHolder().set(filename);
        return true;
    }

    /**
     * Valid info for {@code task} on completed state.
     */
    public void validInfoOnCompleted(@NonNull DownloadTask task, @NonNull DownloadStore store) {
        BreakpointInfo info = store.getAfterCompleted(task.getId());
        if (info == null) {
            info = new BreakpointInfo(task.getId(), task.getUrl(), task.getParentFile(),
                    task.getFilename());
            final long size;
            if (Util.isUriContentScheme(task.getUri())) {
                size = Util.getSizeFromContentUri(task.getUri());
            } else {
                final File file = task.getFile();
                if (file == null) {
                    size = 0;
                    Util.w(TAG, "file is not ready on valid info for task on complete state "
                            + task);
                } else {
                    size = file.length();
                }
            }
            info.addBlock(new BlockInfo(0, size, size));
        }
        DownloadTask.TaskHideWrapper.setBreakpointInfo(task, info);
    }

    public static class FilenameHolder {
        private volatile String filename;
        private final boolean filenameProvidedByConstruct;

        public FilenameHolder() {
            this.filenameProvidedByConstruct = false;
        }

        public FilenameHolder(@NonNull String filename) {
            this.filename = filename;
            this.filenameProvidedByConstruct = true;
        }

        void set(@NonNull String filename) {
            this.filename = filename;
        }

        @Nullable public String get() { return filename; }

        public boolean isFilenameProvidedByConstruct() {
            return filenameProvidedByConstruct;
        }

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
            final int code = connected.getResponseCode();
            final String newEtag = connected.getResponseHeaderField(ETAG);

            final ResumeFailedCause resumeFailedCause = OkDownload.with().downloadStrategy()
                    .getPreconditionFailedCause(code, blockInfo.getCurrentOffset() != 0,
                            info, newEtag);
            if (resumeFailedCause != null) {
                // resume failed, relaunch from beginning.
                throw new ResumeFailedException(resumeFailedCause);
            }

            final boolean isServerCancelled = OkDownload.with().downloadStrategy()
                    .isServerCanceled(code, blockInfo.getCurrentOffset() != 0);
            if (isServerCancelled) {
                // server cancelled, end task.
                throw new ServerCanceledException(code, blockInfo.getCurrentOffset());
            }
        }
    }

    @Nullable public ResumeFailedCause getPreconditionFailedCause(int responseCode,
                                                                  boolean isAlreadyProceed,
                                                                  @NonNull BreakpointInfo info,
                                                                  @Nullable String responseEtag) {
        final String localEtag = info.getEtag();
        if (responseCode == HttpURLConnection.HTTP_PRECON_FAILED) {
            return RESPONSE_PRECONDITION_FAILED;
        }

        if (!Util.isEmpty(localEtag) && !Util.isEmpty(responseEtag) && !responseEtag
                .equals(localEtag)) {
            // etag changed.
            // also etag changed is relate to HTTP_PRECON_FAILED
            return RESPONSE_ETAG_CHANGED;
        }

        if (responseCode == HttpURLConnection.HTTP_CREATED && isAlreadyProceed) {
            // The request has been fulfilled and has resulted in one or more new resources
            // being created.
            // mark this case is precondition failed for
            // 1. checkout whether accept partial
            // 2. 201 means new resources so range must be from beginning otherwise it can't
            // match local range.
            return RESPONSE_CREATED_RANGE_NOT_FROM_0;
        }

        if (responseCode == HttpURLConnection.HTTP_RESET && isAlreadyProceed) {
            return RESPONSE_RESET_RANGE_NOT_FROM_0;
        }

        return null;
    }

    public boolean isServerCanceled(int responseCode, boolean isAlreadyProceed) {
        if (responseCode != HttpURLConnection.HTTP_PARTIAL
                && responseCode != HttpURLConnection.HTTP_OK) {
            return true;
        }

        if (responseCode == HttpURLConnection.HTTP_OK && isAlreadyProceed) {
            return true;
        }

        return false;
    }

    Boolean isHasAccessNetworkStatePermission = null;
    private ConnectivityManager manager = null;

    public void inspectNetworkAvailable() throws UnknownHostException {
        if (isHasAccessNetworkStatePermission == null) {
            isHasAccessNetworkStatePermission = Util
                    .checkPermission(Manifest.permission.ACCESS_NETWORK_STATE);
        }

        // no permission will not check network available case.
        if (!isHasAccessNetworkStatePermission) return;

        if (manager == null) {
            manager = (ConnectivityManager) OkDownload.with().context()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (!Util.isNetworkAvailable(manager)) {
            throw new UnknownHostException("network is not available!");
        }
    }

    public void inspectNetworkOnWifi(@NonNull DownloadTask task) throws IOException {
        if (isHasAccessNetworkStatePermission == null) {
            isHasAccessNetworkStatePermission = Util
                    .checkPermission(Manifest.permission.ACCESS_NETWORK_STATE);
        }

        if (!task.isWifiRequired()) return;

        if (!isHasAccessNetworkStatePermission) {
            throw new IOException("required for access network state but don't have the "
                    + "permission of Manifest.permission.ACCESS_NETWORK_STATE, please declare this "
                    + "permission first on your AndroidManifest, so we can handle the case of "
                    + "downloading required wifi state.");
        }

        if (manager == null) {
            manager = (ConnectivityManager) OkDownload.with().context()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (Util.isNetworkNotOnWifiType(manager)) {
            throw new NetworkPolicyException();
        }
    }
}
