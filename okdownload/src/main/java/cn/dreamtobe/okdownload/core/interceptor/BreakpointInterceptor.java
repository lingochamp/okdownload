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

package cn.dreamtobe.okdownload.core.interceptor;

import java.io.IOException;

import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.dispatcher.CallbackDispatcher;
import cn.dreamtobe.okdownload.core.download.DownloadChain;

import static cn.dreamtobe.okdownload.core.download.DownloadChain.CHUNKED_CONTENT_LENGTH;

/**
 * The number 2 interceptor.
 */

public class BreakpointInterceptor implements Interceptor.Connect, Interceptor.Fetch {

    private final BreakpointStore store;

    public BreakpointInterceptor() {
        store = OkDownload.with().breakpointStore();
    }

    @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final DownloadConnection.Connected connected = chain.processConnect();

        // handle first connect.
        if (chain.isOtherBlockPark()) {
            // only can on the first block.
            if (chain.blockIndex != 0) throw new IOException();

            final long contentLength = chain.getResponseContentLength();
            if (contentLength != CHUNKED_CONTENT_LENGTH) {
                // split
                final int blockCount = OkDownload.with().downloadStrategy().determineBlockCount(chain.task,
                        contentLength, connected.getResponseHeaderFields());
                splitBlock(blockCount, chain);
            }

            chain.unparkOtherBlock();
        }

        // update for connected.
        final BreakpointStore store = OkDownload.with().breakpointStore();
        store.update(chain.getInfo());

        return connected;
    }

    void splitBlock(int blockCount, DownloadChain chain) throws IOException {
        final long totalLength = chain.getResponseContentLength();
        if (blockCount < 1) {
            throw new IOException("Block Count from strategy determine must be larger than 0, " +
                    "the current one is " + blockCount);
        }

        final BreakpointInfo info = chain.getInfo();
        info.resetBlockInfos();
        final long eachLength = totalLength / blockCount;
        for (int i = 0; i < blockCount; i++) {
            final long startOffset = i * eachLength;
            final long contentLength;
            if (i == blockCount - 1) {
                // last block
                final long remainLength = totalLength % blockCount;
                contentLength = eachLength + remainLength;
            } else {
                contentLength = eachLength;
            }

            final BlockInfo blockInfo = new BlockInfo(startOffset, contentLength, startOffset);
            info.addBlock(blockInfo);
        }
    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        final CallbackDispatcher dispatcher = OkDownload.with().callbackDispatcher();
        final int blockIndex = chain.blockIndex;
        final BreakpointInfo breakpointInfo = chain.getInfo();
        final BlockInfo blockInfo = breakpointInfo.getBlock(blockIndex);

        final long contentLength = chain.getResponseContentLength();

        long fetchLength = 0;
        final long startOffset = blockInfo.getCurrentOffset();
        long processFetchLength;
        while (true) {
            processFetchLength = chain.loopFetch();
            if (processFetchLength == -1) {
                break;
            }

            fetchLength += processFetchLength;
            blockInfo.processCurrentOffset(processFetchLength);
        }

        if (fetchLength != contentLength) {
            throw new IOException("Fetch-length isn't equal to the response content-length, " +
                    fetchLength + "!= " + contentLength);
        }

        final long blockLength = startOffset + fetchLength;
        if (blockLength != blockInfo.contentLength) {
            throw new IOException("Local block length is not match required one, " + blockLength +
                    " != " + blockInfo.contentLength);
        }

        if (blockInfo.getCurrentOffset() != blockInfo.contentLength) {
            throw new IOException("The current offset on block-info isn't update correct, " +
                    blockInfo.getCurrentOffset() + " != " + blockInfo.contentLength);
        }

        return fetchLength;
    }

}
