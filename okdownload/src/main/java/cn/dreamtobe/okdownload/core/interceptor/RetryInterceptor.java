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

import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.download.DownloadChain;

/**
 * The number 1 interceptor.
 */

public class RetryInterceptor implements Interceptor.Connect, Interceptor.Fetch {

    @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) {
        try {
            return chain.processConnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException();
    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        try {
            return chain.processFetch();
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException();
    }
}
