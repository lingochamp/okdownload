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

package cn.dreamtobe.okdownload.core.interceptor.connect;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.download.DownloadChain;
import cn.dreamtobe.okdownload.core.download.DownloadStrategy;
import cn.dreamtobe.okdownload.core.exception.InterruptException;
import cn.dreamtobe.okdownload.core.exception.FileBusyAfterRunException;
import cn.dreamtobe.okdownload.core.interceptor.Interceptor;

import static cn.dreamtobe.okdownload.core.download.DownloadChain.CHUNKED_CONTENT_LENGTH;

public class HeaderInterceptor implements Interceptor.Connect {
    private static final String TAG = "HeaderInterceptor";

    @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final BreakpointInfo info = chain.getInfo();
        final DownloadConnection connection = chain.getConnectionOrCreate();
        final DownloadTask task = chain.getTask();

        // add user customize header
        final Map<String, List<String>> userRequestHeaderField = task.getHeaderMapFields();
        if (userRequestHeaderField != null) {
            for (Map.Entry<String, List<String>> entry : userRequestHeaderField.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                for (String value : values) {
                    connection.addHeader(key, value);
                }
            }
        }

        // add range header
        final int blockIndex = chain.getBlockIndex();
        final BlockInfo blockInfo = info.getBlock(blockIndex);
        if (blockInfo == null) {
            throw new IOException("No block-info found on " + blockIndex);
        }

        String range = "bytes=" + blockInfo.getRangeLeft() + "-";
        if (blockIndex > 0 && blockIndex < info.getBlockCount() - 1) {
            range += blockInfo.getRangeRight();
        }

        connection.addHeader("Range", range);

        // add etag if exist
        final String etag = info.getEtag();
        if (!Util.isEmpty(etag)) {
            connection.addHeader("If-Match", etag);
        }

        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        OkDownload.with().callbackDispatcher().dispatch().connectStart(task, blockIndex,
                connection);
        DownloadConnection.Connected connected = chain.processConnect();

        OkDownload.with().callbackDispatcher().dispatch().connectEnd(task, blockIndex,
                connected);
        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        // if precondition failed.
        final DownloadStrategy strategy = OkDownload.with().downloadStrategy();
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                strategy.resumeAvailableResponseCheck(connected, blockIndex, info);
        responseCheck.inspect();

        final String contentDisposition = connected.getResponseHeaderField("Content-Disposition");
        final String responseFilename = parseContentDisposition(contentDisposition);
        strategy.validFilenameFromResponse(responseFilename, task, info, connected);
        inspectFileConflictAfterRun(chain);

        info.setEtag(etag);

        // content-length
        final String contentLengthStr = connected.getResponseHeaderField("Content-Length");
        long contentLength;
        if (Util.isEmpty(contentLengthStr)) contentLength = CHUNKED_CONTENT_LENGTH;
        else contentLength = Long.parseLong(contentLengthStr);
        if (contentLength < 0) {
            // no content length
            final String transferEncoding = connected.getResponseHeaderField("Transfer-Encoding");
            final boolean isEncodingChunked =
                    transferEncoding != null && transferEncoding.equals("chunked");
            if (!isEncodingChunked) {
                Util.w(TAG, "Transfer-Encoding isn't chunked but there is no "
                        + "valid Content-Length either!");
                if (contentLength != CHUNKED_CONTENT_LENGTH) {
                    Util.w(TAG, "Content-Length[" + contentLength + " is not be "
                            + "recognized, so we change to chunked mark.");
                    contentLength = CHUNKED_CONTENT_LENGTH;
                }
            } else {
                contentLength = CHUNKED_CONTENT_LENGTH;
            }
        }

        chain.setResponseContentLength(contentLength);
        return connected;
    }

    private void inspectFileConflictAfterRun(DownloadChain chain) throws IOException {
        if (OkDownload.with().downloadDispatcher().isFileConflictAfterRun(chain.getTask())) {
            throw FileBusyAfterRunException.SIGNAL;
        }
    }

    private static final Pattern CONTENT_DISPOSITION_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");

    /**
     * The same to com.android.providers.downloads.Helpers#parseContentDisposition.
     * </p>
     * Parse the Content-Disposition HTTP Header. The format of the header
     * is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be
     * downloaded to the file system. We only support the attachment type.
     */
    @Nullable static String parseContentDisposition(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }

        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalStateException ex) {
            // This function is defined as returning null when it can't parse the header
        }
        return null;
    }

}
