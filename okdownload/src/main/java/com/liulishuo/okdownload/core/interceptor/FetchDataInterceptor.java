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

import java.io.IOException;
import java.io.InputStream;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.dispatcher.CallbackDispatcher;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

public class FetchDataInterceptor implements Interceptor.Fetch {

    private final InputStream inputStream;

    private final byte[] readBuffer;
    private final MultiPointOutputStream outputStream;
    private final int blockIndex;
    private final DownloadTask task;
    private final CallbackDispatcher dispatcher;

    public FetchDataInterceptor(int blockIndex,
                                @NonNull InputStream inputStream,
                                @NonNull MultiPointOutputStream outputStream,
                                DownloadTask task) {
        this.blockIndex = blockIndex;
        this.inputStream = inputStream;
        this.readBuffer = new byte[task.getReadBufferSize()];
        this.outputStream = outputStream;

        this.task = task;
        this.dispatcher = OkDownload.with().callbackDispatcher();
    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        // fetch
        int fetchLength = inputStream.read(readBuffer);
        if (fetchLength == -1) {
            return fetchLength;
        }

        // write to file
        outputStream.write(blockIndex, readBuffer, fetchLength);

        chain.increaseCallbackBytes(fetchLength);
        if (this.dispatcher.isFetchProcessMoment(task)) {
            chain.flushNoCallbackIncreaseBytes();
        }

        return fetchLength;
    }
}
