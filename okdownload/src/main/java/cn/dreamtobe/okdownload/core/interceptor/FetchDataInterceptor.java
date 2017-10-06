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

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

import cn.dreamtobe.okdownload.core.download.DownloadChain;
import cn.dreamtobe.okdownload.core.file.MultiPointOutputStream;

public class FetchDataInterceptor implements Interceptor.Fetch {

    private final InputStream inputStream;

    private final byte[] readBuffer;
    private final MultiPointOutputStream outputStream;
    private final int blockIndex;

    public FetchDataInterceptor(int blockIndex,
                                @NonNull InputStream inputStream,
                                @NonNull MultiPointOutputStream outputStream,
                                int readBufferSize) {
        this.blockIndex = blockIndex;
        this.inputStream = inputStream;
        this.readBuffer = new byte[readBufferSize];
        this.outputStream = outputStream;

    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        // fetch
        int fetchLength = inputStream.read(readBuffer);

        // write to file
        outputStream.write(blockIndex, readBuffer, fetchLength);

        return fetchLength;
    }
}
