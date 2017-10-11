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

import android.text.TextUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.DownloadStrategy;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.download.DownloadChain;
import cn.dreamtobe.okdownload.core.exception.CanceledException;
import cn.dreamtobe.okdownload.core.interceptor.Interceptor;
import cn.dreamtobe.okdownload.core.util.LogUtil;

import static cn.dreamtobe.okdownload.core.download.DownloadChain.CHUNKED_CONTENT_LENGTH;

public class HeaderInterceptor implements Interceptor.Connect {
    private static final String TAG = "HeaderInterceptor";

    @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final BreakpointInfo info = chain.getInfo();
        final DownloadConnection connection = chain.getConnectionOrCreate();

        // add user customize header
        final Map<String, List<String>> userRequestHeaderField = chain.task.getHeaderMapFields();
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
        final int blockIndex = chain.blockIndex;
        final BlockInfo blockInfo = info.getBlock(blockIndex);
        if (blockInfo == null) {
            throw new IOException("No block-info found on " + blockIndex);
        }

        String range = "bytes=" + blockInfo.getCurrentOffset() + "-";
        if (blockIndex > 0 && blockIndex < info.getBlockCount() - 1) {
            range += blockInfo.contentLength;
        }

        connection.addHeader("Range", range);

        // add etag if exist
        final String etag = info.getEtag();
        if (!TextUtils.isEmpty(etag)) {
            connection.addHeader("If-Match", etag);
        }

        if (chain.getCache().isInterrupt()) {
            throw CanceledException.SIGNAL;
        }

        DownloadConnection.Connected connected = chain.processConnect();

        if (chain.getCache().isInterrupt()) {
            throw CanceledException.SIGNAL;
        }

        // if precondition failed.
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck = OkDownload.with()
                .downloadStrategy().resumeAvailableResponseCheck(connected, blockIndex, info);
        responseCheck.inspect();

        info.setEtag(etag);
        // content-length
        final String contentLengthStr = connected.getResponseHeaderField("Content-Length");
        long contentLength;
        if (TextUtils.isEmpty(contentLengthStr)) contentLength = CHUNKED_CONTENT_LENGTH;
        else contentLength = Long.parseLong(contentLengthStr);
        if (contentLength < 0) {
            // no content length
            final String transferEncoding = connected.getResponseHeaderField("Transfer-Encoding");
            final boolean isEncodingChunked =
                    transferEncoding != null && transferEncoding.equals("chunked");
            if (!isEncodingChunked) {
                LogUtil.w(TAG, "Transfer-Encoding isn't chunked but there is no "
                        + "valid Content-Length either!");
                if (contentLength != CHUNKED_CONTENT_LENGTH) {
                    LogUtil.w(TAG, "Content-Length[" + contentLength + " is not be "
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

}
