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

import android.support.annotation.NonNull;

import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import java.io.File;
import java.io.IOException;

import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;

public class BreakpointInterceptor implements Interceptor.Connect, Interceptor.Fetch {

    @NonNull @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final DownloadConnection.Connected connected = chain.processConnect();
        final BreakpointInfo info = chain.getInfo();

        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        // update for connected.
        final BreakpointStore store = OkDownload.with().breakpointStore();
        try {
            if (!store.update(info)) {
                throw new IOException("Update store failed!");
            }
        } catch (Exception e) {
            throw new IOException("Update store failed!", e);
        }

        return connected;
    }

    void discardOldFileIfExist(@NonNull String path) {
        final File oldFile = new File(path);
        if (oldFile.exists()) OkDownload.with().processFileStrategy().discardOldFile(oldFile);
    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        final long contentLength = chain.getResponseContentLength();
        final int blockIndex = chain.getBlockIndex();
        final boolean isNotChunked = contentLength != CHUNKED_CONTENT_LENGTH;

        long fetchLength = 0;
        long processFetchLength;

        while (true) {
            processFetchLength = chain.loopFetch();
            if (processFetchLength == -1) {
                break;
            }

            fetchLength += processFetchLength;
        }

        // finish
        chain.flushNoCallbackIncreaseBytes();
        final MultiPointOutputStream outputStream = chain.getOutputStream();
        outputStream.ensureSyncComplete(blockIndex);

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

}
