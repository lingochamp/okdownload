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

import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadCache;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.exception.RetryException;

import java.io.IOException;

public class RetryInterceptor implements Interceptor.Connect, Interceptor.Fetch {

    @NonNull @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final DownloadCache cache = chain.getCache();

        while (true) {
            try {
                if (cache.isInterrupt()) {
                    throw InterruptException.SIGNAL;
                }
                return chain.processConnect();
            } catch (IOException e) {
                if (e instanceof RetryException) {
                    chain.resetConnectForRetry();
                    continue;
                }

                chain.getCache().catchException(e);
                final DownloadConnection connection = chain.getConnection();
                if (connection != null) {
                    connection.release();
                }

                throw e;
            }
        }
    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        try {
            return chain.processFetch();
        } catch (IOException e) {
            chain.getCache().catchException(e);
            throw e;
        }
    }
}
