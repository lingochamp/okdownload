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

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.connection.DownloadConnection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.liulishuo.okdownload.core.Util.ACCEPT_RANGES;
import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_DISPOSITION;
import static com.liulishuo.okdownload.core.Util.CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_RANGE;
import static com.liulishuo.okdownload.core.Util.ETAG;
import static com.liulishuo.okdownload.core.Util.IF_MATCH;
import static com.liulishuo.okdownload.core.Util.METHOD_HEAD;
import static com.liulishuo.okdownload.core.Util.RANGE;
import static com.liulishuo.okdownload.core.Util.TRANSFER_ENCODING;
import static com.liulishuo.okdownload.core.Util.VALUE_CHUNKED;

/**
 * The trial connect which is just used for checking the {@code isAcceptRange},
 * {@code instanceLength}, {@code responseEtag}.
 */
public class ConnectTrial {
    private static final String TAG = "ConnectTrial";
    @NonNull private final DownloadTask task;
    @NonNull private final BreakpointInfo info;

    private boolean isAcceptRange;
    @IntRange(from = CHUNKED_CONTENT_LENGTH) private long instanceLength;
    @Nullable private String responseEtag;
    @Nullable private String responseFilename;
    private int responseCode;

    public ConnectTrial(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {
        this.task = task;
        this.info = info;
    }

    public void executeTrial() throws IOException {
        OkDownload.with().downloadStrategy().inspectNetworkOnWifi(task);
        OkDownload.with().downloadStrategy().inspectNetworkAvailable();

        DownloadConnection connection = OkDownload.with().connectionFactory().create(task.getUrl());
        boolean isNeedTrialHeadMethod;
        try {
            if (!Util.isEmpty(info.getEtag())) {
                connection.addHeader(IF_MATCH, info.getEtag());
            }
            connection.addHeader(RANGE, "bytes=0-0");

            final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
            final Map<String, List<String>> requestProperties = connection.getRequestProperties();
            listener.connectTrialStart(task, requestProperties);

            final DownloadConnection.Connected connected = connection.execute();
            this.responseCode = connected.getResponseCode();
            this.isAcceptRange = isAcceptRange(connected);
            this.instanceLength = findInstanceLength(connected);
            this.responseEtag = findEtag(connected);
            this.responseFilename = findFilename(connected);
            final Map<String, List<String>> responseHeader = connected.getResponseHeaderFields();
            listener.connectTrialEnd(task, responseCode, responseHeader);

            isNeedTrialHeadMethod = isNeedTrialHeadMethodForInstanceLength(instanceLength,
                    connected);
        } finally {
            connection.release();
        }

        if (isNeedTrialHeadMethod) {
            trialHeadMethodForInstanceLength();
        }
    }

    /**
     * Get the instance length of the task.
     *
     * @return the instance length of the task.
     */
    public long getInstanceLength() {
        return this.instanceLength;
    }

    /**
     * Check whether the task is accept range request.
     *
     * @return whether the task is accept range request.
     */
    public boolean isAcceptRange() {
        return this.isAcceptRange;
    }

    /**
     * Check whether the response indicate the resource transfer encoding is chunked.
     *
     * @return {@code true} only if the response transfer encoding is chunked.
     */
    public boolean isChunked() {
        return this.instanceLength == CHUNKED_CONTENT_LENGTH;
    }

    /**
     * Get the Etag from the response header.
     *
     * @return the Etag from the response header.
     */
    @Nullable public String getResponseEtag() {
        return responseEtag;
    }

    /**
     * Get the filename from the 'Content-Disposition' field on the response header.
     *
     * @return the filename from the 'Content-Disposition' field on the response header.
     */
    @Nullable public String getResponseFilename() {
        return responseFilename;
    }

    /**
     * Get the response code from the trial connect.
     *
     * @return the response code from the trial connect.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Check whether the response Etag is the same to the local Etag if the local Etag is provided
     * on connect.
     *
     * @return whether the local Etag is overdue.
     */
    public boolean isEtagOverdue() {
        return info.getEtag() != null && !info.getEtag().equals(responseEtag);
    }

    private static boolean isAcceptRange(@NonNull DownloadConnection.Connected connected)
            throws IOException {
        if (connected.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) return true;

        final String acceptRanges = connected.getResponseHeaderField(ACCEPT_RANGES);
        return "bytes".equals(acceptRanges);
    }

    @Nullable private static String findFilename(DownloadConnection.Connected connected) {
        return parseContentDisposition(connected.getResponseHeaderField(CONTENT_DISPOSITION));
    }

    private static final Pattern CONTENT_DISPOSITION_QUOTED_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");
    // no note
    private static final Pattern CONTENT_DISPOSITION_NON_QUOTED_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*(.*)");

    /**
     * The same to com.android.providers.downloads.Helpers#parseContentDisposition.
     * </p>
     * Parse the Content-Disposition HTTP Header. The format of the header
     * is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be
     * downloaded to the file system. We only support the attachment type.
     */
    @Nullable private static String parseContentDisposition(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }

        try {
            Matcher m = CONTENT_DISPOSITION_QUOTED_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }

            m = CONTENT_DISPOSITION_NON_QUOTED_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalStateException ex) {
            // This function is defined as returning null when it can't parse the header
        }
        return null;
    }

    @Nullable private static String findEtag(DownloadConnection.Connected connected) {
        return connected.getResponseHeaderField(ETAG);
    }

    private static long findInstanceLength(DownloadConnection.Connected connected) {
        // Content-Range
        final long instanceLength = parseContentRangeFoInstanceLength(
                connected.getResponseHeaderField(CONTENT_RANGE));
        if (instanceLength != CHUNKED_CONTENT_LENGTH) return instanceLength;

        // chunked on here
        final boolean isChunked = parseTransferEncoding(connected
                .getResponseHeaderField(TRANSFER_ENCODING));
        if (!isChunked) {
            Util.w(TAG, "Transfer-Encoding isn't chunked but there is no "
                    + "valid instance length found either!");
        }

        return CHUNKED_CONTENT_LENGTH;
    }

    boolean isNeedTrialHeadMethodForInstanceLength(
            long oldInstanceLength, @NonNull DownloadConnection.Connected connected) {
        if (oldInstanceLength != CHUNKED_CONTENT_LENGTH) {
            // the instance length already has certain value.
            return false;
        }

        final String contentRange = connected.getResponseHeaderField(CONTENT_RANGE);
        if (contentRange != null && contentRange.length() > 0) {
            // because of the Content-Range can certain the result is right, so pass.
            return false;
        }

        final boolean isChunked = parseTransferEncoding(
                connected.getResponseHeaderField(TRANSFER_ENCODING));
        if (isChunked) {
            // because of the Transfer-Encoding can certain the result is right, so pass.
            return false;
        }

        final String contentLengthField = connected.getResponseHeaderField(CONTENT_LENGTH);
        if (contentLengthField == null || contentLengthField.length() <= 0) {
            // because of the response header isn't contain the Content-Length so the HEAD method
            // request is useless, because we plan to get the right instance-length on the
            // Content-Length field through the response header of non 0-0 Range HEAD method request
            return false;
        }

        // because of the response header contain Content-Length, but because of we using Range: 0-0
        // so we the Content-Length is always 1 now, we can't use it, so we try to use HEAD method
        // request just for get the certain instance-length.
        return true;
    }

    // if instance length is can't certain through transfer-encoding and content-range but the
    // content-length is exist but can't be used, we will request HEAD method request to find out
    // right one.
    void trialHeadMethodForInstanceLength() throws IOException {
        final DownloadConnection connection = OkDownload.with().connectionFactory()
                .create(task.getUrl());
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        try {
            connection.setRequestMethod(METHOD_HEAD);

            listener.connectTrialStart(task, connection.getRequestProperties());
            final DownloadConnection.Connected connectedForContentLength = connection.execute();
            listener.connectTrialEnd(task, connectedForContentLength.getResponseCode(),
                    connectedForContentLength.getResponseHeaderFields());

            this.instanceLength = Util.parseContentLength(
                    connectedForContentLength.getResponseHeaderField(CONTENT_LENGTH));
        } finally {
            connection.release();
        }
    }

    private static boolean parseTransferEncoding(@Nullable String transferEncoding) {
        return transferEncoding != null && transferEncoding.equals(VALUE_CHUNKED);
    }

    private static long parseContentRangeFoInstanceLength(@Nullable String contentRange) {
        if (contentRange == null) return CHUNKED_CONTENT_LENGTH;

        final String[] session = contentRange.split("/");
        if (session.length >= 2) {
            try {
                return Long.parseLong(session[1]);
            } catch (NumberFormatException e) {
                Util.w(TAG, "parse instance length failed with " + contentRange);
            }
        }

        return -1;
    }
}
