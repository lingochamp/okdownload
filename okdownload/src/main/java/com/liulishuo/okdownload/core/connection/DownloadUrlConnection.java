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

package com.liulishuo.okdownload.core.connection;

import android.support.annotation.NonNull;

import com.liulishuo.okdownload.RedirectUtil;
import com.liulishuo.okdownload.core.Util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class DownloadUrlConnection implements DownloadConnection, DownloadConnection.Connected {

    protected URLConnection connection;
    private Configuration configuration;
    private URL url;
    private RedirectHandler redirectHandler;

    private static final String TAG = "DownloadUrlConnection";

    DownloadUrlConnection(URLConnection connection) {
        this(connection, new RedirectHandler());
    }

    DownloadUrlConnection(URLConnection connection, RedirectHandler redirectHandler) {
        this.connection = connection;
        this.url = connection.getURL();
        this.redirectHandler = redirectHandler;
    }

    public DownloadUrlConnection(String originUrl, Configuration configuration) throws IOException {
        this(new URL(originUrl), configuration);
    }

    public DownloadUrlConnection(URL url, Configuration configuration) throws IOException {
        this(url, configuration, new RedirectHandler());
    }

    public DownloadUrlConnection(
            URL url,
            Configuration configuration,
            RedirectHandler redirectHandler) throws IOException {
        this.configuration = configuration;
        this.url = url;
        this.redirectHandler = redirectHandler;
        configConnection();
    }

    public DownloadUrlConnection(String originUrl) throws IOException {
        this(originUrl, null);
    }

    void configConnection() throws IOException {
        Util.d(TAG, "config connection for " + url);
        if (configuration != null && configuration.proxy != null) {
            connection = url.openConnection(configuration.proxy);
        } else {
            connection = url.openConnection();
        }

        if (configuration != null) {
            if (configuration.readTimeout != null) {
                connection.setReadTimeout(configuration.readTimeout);
            }

            if (configuration.connectTimeout != null) {
                connection.setConnectTimeout(configuration.connectTimeout);
            }
        }
    }

    @Override
    public void addHeader(String name, String value) {
        connection.addRequestProperty(name, value);
    }

    @Override
    public Connected execute() throws IOException {
        final Map<String, List<String>> headerProperties = getRequestProperties();
        connection.connect();
        redirectHandler.handleRedirect(this, headerProperties);
        return this;
    }

    @Override
    public int getResponseCode() throws IOException {
        if (connection instanceof HttpURLConnection) {
            return ((HttpURLConnection) connection).getResponseCode();
        }

        return DownloadConnection.NO_RESPONSE_CODE;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    @Override
    public boolean setRequestMethod(@NonNull String method) throws ProtocolException {
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setRequestMethod(method);
            return true;
        }

        return false;
    }

    @Override
    public Map<String, List<String>> getResponseHeaderFields() {
        return connection.getHeaderFields();
    }

    @Override
    public String getResponseHeaderField(String name) {
        return connection.getHeaderField(name);
    }

    @Override
    public String getRedirectLocation() {
        return redirectHandler.redirectLocation;
    }

    @Override
    public void release() {
        // the same to response#close on okhttp
        // real execute RealBufferedSource.InputStream#close
        try {
            final InputStream inputStream = connection.getInputStream();
            if (inputStream != null) inputStream.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return connection.getRequestProperties();
    }

    @Override
    public String getRequestProperty(String key) {
        return connection.getRequestProperty(key);
    }

    public static class Factory implements DownloadConnection.Factory {

        private final Configuration configuration;

        public Factory() {
            this(null);
        }

        public Factory(Configuration configuration) {
            this.configuration = configuration;
        }

        DownloadConnection create(URL url) throws IOException {
            return new DownloadUrlConnection(url, configuration);
        }

        @Override
        public DownloadConnection create(String originUrl) throws IOException {
            return new DownloadUrlConnection(originUrl, configuration);
        }
    }

    /**
     * The sample configuration for the {@link DownloadUrlConnection}
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static class Configuration {
        private Proxy proxy;
        private Integer readTimeout;
        private Integer connectTimeout;

        /**
         * The connection will be made through the specified proxy.
         * <p>
         * This {@code proxy} will be used when invoke {@link URL#openConnection(Proxy)} }
         *
         * @param proxy the proxy will be applied to the {@link DownloadUrlConnection}
         */
        public Configuration proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Sets the read timeout to a specified timeout, in milliseconds. A non-zero value specifies
         * the timeout when reading from Input stream when a connection is established to a
         * resource.
         * If the timeout expires before there is data available for read, a
         * java.net.SocketTimeoutException is raised. A timeout of zero is interpreted as an
         * infinite timeout.
         * <p>
         * This {@code readTimeout} will be applied through
         * {@link URLConnection#setReadTimeout(int)}
         *
         * @param readTimeout an <code>int</code> that specifies the timeout value to be used in
         *                    milliseconds
         */
        public Configuration readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        /**
         * Sets a specified timeout value, in milliseconds, to be used when opening a communications
         * link to the resource referenced by this URLConnection.  If the timeout expires before the
         * connection can be established, a java.net.SocketTimeoutException is raised. A timeout of
         * zero is interpreted as an infinite timeout.
         * <p>
         * This {@code connectionTimeout} will be applied through
         * {@link URLConnection#setConnectTimeout(int)}
         *
         * @param connectTimeout an <code>int</code> that specifies the connect timeout value in
         *                       milliseconds
         */
        public Configuration connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

    }

    /**
     * handle redirect connection
     */
    static final class RedirectHandler {

        String redirectLocation;

        void handleRedirect(DownloadUrlConnection downloadUrlConnection,
                            Map<String, List<String>> headerProperties) throws IOException {
            int responseCode = downloadUrlConnection.getResponseCode();
            int redirectCount = 0;
            while (RedirectUtil.isRedirect(responseCode)) {
                // the last connect is useless, so release it
                downloadUrlConnection.release();

                if (++redirectCount > RedirectUtil.MAX_REDIRECT_TIMES) {
                    throw new ProtocolException("Too many redirect requests: " + redirectCount);
                }

                redirectLocation = RedirectUtil
                        .getRedirectedUrl(downloadUrlConnection, responseCode);
                downloadUrlConnection.url = new URL(redirectLocation);
                downloadUrlConnection.configConnection();
                Util.addRequestHeaderFields(headerProperties,
                        downloadUrlConnection);
                downloadUrlConnection.connection.connect();
                responseCode = downloadUrlConnection.getResponseCode();
            }
        }
    }
}
