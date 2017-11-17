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

import java.io.IOException;
import java.net.SocketException;

import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadCache;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.exception.FileBusyAfterRunException;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.exception.PreAllocateException;
import com.liulishuo.okdownload.core.exception.ResumeFailedException;
import com.liulishuo.okdownload.core.exception.RetryException;
import com.liulishuo.okdownload.core.exception.ServerCancelledException;

public class RetryInterceptor implements Interceptor.Connect, Interceptor.Fetch {

    @Override
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

                handleException(e, cache);
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
            handleException(e, chain.getCache());
            throw e;
        } finally {
            chain.getOutputStream().ensureSyncComplete(chain.getBlockIndex());
            chain.getOutputStream().close(chain.getBlockIndex());
        }
    }

    private static void handleException(IOException e, DownloadCache cache) {
        if (cache.isUserCanceled()) return; // ignored

        if (e instanceof ResumeFailedException) {
            cache.setPreconditionFailed(e);
        } else if (e instanceof ServerCancelledException) {
            cache.setServerCanceled(e);
        } else if (e == FileBusyAfterRunException.SIGNAL) {
            cache.setFileBusyAfterRun();
        } else if (e instanceof PreAllocateException) {
            cache.setPreAllocateFailed(e);
        } else if (e != InterruptException.SIGNAL) {
            cache.setUnknownError(e);
            if (!(e instanceof SocketException)) { // we know socket exception, so ignore it.
                e.printStackTrace();
            }
        }
    }
}
