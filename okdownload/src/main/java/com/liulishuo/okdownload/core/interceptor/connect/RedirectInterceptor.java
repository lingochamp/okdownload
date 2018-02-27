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

package com.liulishuo.okdownload.core.interceptor.connect;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.interceptor.Interceptor;

public class RedirectInterceptor implements Interceptor.Connect {

    /**
     * How many redirects and auth challenges should we attempt? Chrome follows 21 redirects;
     * Firefox, curl, and wget follow 20; Safari follows 16; and HTTP/1.0 recommends 5.
     */
    static final int MAX_REDIRECT_TIMES = 10;

    /**
     * The target resource resides temporarily under a different URI and the user agent MUST NOT
     * change the request method if it performs an automatic redirection to that URI.
     */
    private static final int HTTP_TEMPORARY_REDIRECT = 307;
    /**
     * The target resource has been assigned a new permanent URI and any future references to this
     * resource ought to use one of the enclosed URIs.
     */
    private static final int HTTP_PERMANENT_REDIRECT = 308;

    @NonNull @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        int redirectCount = 0;

        String url;
        DownloadConnection connection = chain.getConnectionOrCreate();
        while (true) {

            if (chain.getCache().isInterrupt()) {
                throw InterruptException.SIGNAL;
            }

            final DownloadConnection.Connected connected = chain.processConnect();
            final int code = connected.getResponseCode();

            if (!isRedirect(code)) {
                return connected;
            }

            if (++redirectCount >= MAX_REDIRECT_TIMES) {
                throw new ProtocolException("Too many redirect requests: " + redirectCount);
            }

            url = connected.getResponseHeaderField("Location");
            if (url == null) {
                throw new ProtocolException(
                        "Response code is " + code + " but can't find Location field");
            }

            connection.release();

            connection = OkDownload.with().connectionFactory().create(url);
            chain.setConnection(connection);
            chain.setRedirectLocation(url);

        }
    }

    private static boolean isRedirect(int code) {
        return code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER
                || code == HttpURLConnection.HTTP_MULT_CHOICE
                || code == HTTP_TEMPORARY_REDIRECT
                || code == HTTP_PERMANENT_REDIRECT;
    }
}
