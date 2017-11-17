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

package com.liulishuo.okdownload.core.interceptor;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.download.DownloadStrategy;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.exception.RetryException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import static com.liulishuo.okdownload.core.download.DownloadChain.CHUNKED_CONTENT_LENGTH;

public class BreakpointInterceptor implements Interceptor.Connect, Interceptor.Fetch {
    private static final String TAG = "BreakpointInterceptor";

    @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final DownloadConnection.Connected connected = chain.processConnect();
        final DownloadStrategy strategy = OkDownload.with().downloadStrategy();
        boolean isReuseAnotherSameInfo = false;
        final BreakpointInfo info = chain.getInfo();

        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        // handle first connect.
        if (chain.isOtherBlockPark()) {
            // only can on the first block.
            if (chain.getBlockIndex() != 0) throw new IOException();

            final long contentLength = chain.getResponseContentLength();

            isReuseAnotherSameInfo = inspectAnotherSameInfo(chain.getTask(), info, contentLength);

            if (!isReuseAnotherSameInfo && strategy.isSplitBlock(contentLength, connected)) {
                discardOldFileIfExist(chain.getInfo().getPath());

                // split
                final int blockCount = strategy.determineBlockCount(chain.getTask(), contentLength,
                        connected);
                splitBlock(blockCount, chain);
            }

            OkDownload.with().callbackDispatcher().dispatch().splitBlockEnd(chain.getTask(), info);

            chain.unparkOtherBlock();
        }

        // update for connected.
        final BreakpointStore store = OkDownload.with().breakpointStore();
        if (!store.update(info)) {
            throw new IOException("Update store failed!");
        }

        final long firstRangeLeft = info.getBlock(0).getRangeLeft();
        if (isReuseAnotherSameInfo
                && firstRangeLeft > strategy.reconnectFirstBlockThresholdBytes()) {
            Util.d(TAG, "Retry the first block since its range left is turn to " + firstRangeLeft);
            throw new RetryException(
                    "Retry since the range left of the fist block is changed larger than 5120byte");
        }

        return connected;
    }

    void splitBlock(int blockCount, DownloadChain chain) throws IOException {
        final long totalLength = chain.getResponseContentLength();
        if (blockCount < 1) {
            throw new IOException("Block Count from strategy determine must be larger than 0, "
                    + "the current one is " + blockCount);
        }

        final BreakpointInfo info = chain.getInfo();

        info.resetBlockInfos();
        final long eachLength = totalLength / blockCount;
        long startOffset = 0;
        long contentLength = 0;
        for (int i = 0; i < blockCount; i++) {
            startOffset = startOffset + contentLength;
            if (i == 0) {
                // first block
                final long remainLength = totalLength % blockCount;
                contentLength = eachLength + remainLength;
            } else {
                contentLength = eachLength;
            }

            final BlockInfo blockInfo = new BlockInfo(startOffset, contentLength);
            info.addBlock(blockInfo);
        }
    }

    // this case meet only if there are another info task is idle and is the same after
    // this task has filename.
    boolean inspectAnotherSameInfo(DownloadTask task, BreakpointInfo info,
                                   long totalLength) throws RetryException {
        if (!task.isUriIsDirectory()) return false;

        final BreakpointStore store = OkDownload.with().breakpointStore();
        final BreakpointInfo anotherInfo = store.findAnotherInfoFromCompare(task, info);
        if (anotherInfo == null) return false;

        store.discard(anotherInfo.getId());

        if (anotherInfo.getTotalOffset()
                <= OkDownload.with().downloadStrategy().reuseIdledSameInfoThresholdBytes()) {
            return false;
        }

        if (anotherInfo.getEtag() != null && !anotherInfo.getEtag().equals(info.getEtag())) {
            return false;
        }

        if (anotherInfo.getTotalLength() != totalLength) {
            return false;
        }

        if (!new File(anotherInfo.getPath()).exists()) return false;

        info.reuseBlocks(anotherInfo);

        Util.d(TAG, "Reuse another same info: " + info);
        return true;
    }

    void discardOldFileIfExist(@NonNull String path) {
        final File oldFile = new File(path);
        if (oldFile.exists()) OkDownload.with().processFileStrategy().discardOldFile(oldFile);
    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        final long contentLength = chain.getResponseContentLength();
        final int blockIndex = chain.getBlockIndex();
        final BreakpointInfo info = chain.getInfo();
        final BlockInfo blockInfo = info.getBlock(blockIndex);
        final long blockLength = blockInfo.getContentLength();
        final boolean isMultiBlock = !info.isSingleBlock();
        final boolean isNotChunked = contentLength != CHUNKED_CONTENT_LENGTH;

        long rangeLeft = blockInfo.getRangeLeft();
        long fetchLength = 0;
        long processFetchLength;
        boolean isFirstBlockLenienceRule = false;

        while (true) {
            processFetchLength = chain.loopFetch();
            if (processFetchLength == -1) {
                break;
            }

            fetchLength += processFetchLength;
            if (isNotChunked && isMultiBlock && blockIndex == 0
                    && Util.isFirstBlockMeetLenienceFull(rangeLeft + fetchLength, blockLength)) {
                isFirstBlockLenienceRule = true;
                break;
            }
        }

        // finish
        chain.flushNoCallbackIncreaseBytes();
        final MultiPointOutputStream outputStream = chain.getOutputStream();
        outputStream.ensureSyncComplete(blockIndex);

        if (!isFirstBlockLenienceRule && isNotChunked) {
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

}
