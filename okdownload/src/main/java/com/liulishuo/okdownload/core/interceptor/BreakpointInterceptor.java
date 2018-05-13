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

package com.liulishuo.okdownload.core.interceptor;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.exception.RetryException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_RANGE;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.CONTENT_LENGTH_CHANGED;

public class BreakpointInterceptor implements Interceptor.Connect, Interceptor.Fetch {

    private static final String TAG = "BreakpointInterceptor";

    @NonNull @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final DownloadConnection.Connected connected = chain.processConnect();
        final BreakpointInfo info = chain.getInfo();

        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        if (info.getBlockCount() == 1 && !info.isChunked()) {
            // only one block to download this resource
            // use this block response header instead of trial result if they are different.
            final long blockInstanceLength = getExactContentLengthRangeFrom0(connected);
            final long infoInstanceLength = info.getTotalLength();
            if (blockInstanceLength > 0 && blockInstanceLength != infoInstanceLength) {
                Util.d(TAG, "SingleBlock special check: the response instance-length["
                        + blockInstanceLength + "] isn't equal to the instance length from trial-"
                        + "connection[" + infoInstanceLength + "]");
                final BlockInfo blockInfo = info.getBlock(0);
                boolean isFromBreakpoint = blockInfo.getRangeLeft() != 0;

                final BlockInfo newBlockInfo = new BlockInfo(0, blockInstanceLength);
                info.resetBlockInfos();
                info.addBlock(newBlockInfo);

                if (isFromBreakpoint) {
                    final String msg = "Discard breakpoint because of on this special case, we have"
                            + " to download from beginning";
                    Util.w(TAG, msg);
                    throw new RetryException(msg);
                }
                OkDownload.with().callbackDispatcher().dispatch()
                        .downloadFromBeginning(chain.getTask(), info, CONTENT_LENGTH_CHANGED);
            }
        }

        // update for connected.
        final DownloadStore store = chain.getDownloadStore();
        try {
            if (!store.update(info)) {
                throw new IOException("Update store failed!");
            }
        } catch (Exception e) {
            throw new IOException("Update store failed!", e);
        }

        return connected;
    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        final long contentLength = chain.getResponseContentLength();
        final int blockIndex = chain.getBlockIndex();
        final boolean isNotChunked = contentLength != CHUNKED_CONTENT_LENGTH;

        long fetchLength = 0;
        long processFetchLength;

        final MultiPointOutputStream outputStream = chain.getOutputStream();

        try {
            while (true) {
                processFetchLength = chain.loopFetch();
                if (processFetchLength == -1) {
                    break;
                }

                fetchLength += processFetchLength;
            }
        } finally {
            // finish
            chain.flushNoCallbackIncreaseBytes();
            if (!chain.getCache().isUserCanceled()) outputStream.done(blockIndex);
        }

        if (isNotChunked) {
            // local persist data check.
            outputStream.inspectComplete(blockIndex);

            // response content length check.
            if (fetchLength != contentLength) {
                throw new IOException("Fetch-length isn't equal to the response content-length, "
                        + fetchLength + "!= " + contentLength);
            }
        }

        return fetchLength;
    }

    private static final Pattern CONTENT_RANGE_RIGHT_VALUE = Pattern
            .compile(".*\\d+ *- *(\\d+) */ *\\d+");

    /**
     * Get the exactly content-length, on this method we assume the range is from 0.
     */
    @IntRange(from = -1)
    long getExactContentLengthRangeFrom0(@NonNull DownloadConnection.Connected connected) {
        final String contentRangeField = connected.getResponseHeaderField(CONTENT_RANGE);
        long contentLength = -1;
        if (!Util.isEmpty(contentRangeField)) {
            final long rightRange = getRangeRightFromContentRange(contentRangeField);
            // for the range from 0, the contentLength is just right-range +1.
            if (rightRange > 0) contentLength = rightRange + 1;
        }

        if (contentLength < 0) {
            // content-length
            final String contentLengthField = connected.getResponseHeaderField(CONTENT_LENGTH);
            if (!Util.isEmpty(contentLengthField)) {
                contentLength = Long.parseLong(contentLengthField);
            }
        }

        return contentLength;
    }

    @IntRange(from = -1)
    static long getRangeRightFromContentRange(@NonNull String contentRange) {
        Matcher m = CONTENT_RANGE_RIGHT_VALUE.matcher(contentRange);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }

        return -1;
    }
}
