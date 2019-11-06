/*
 * Copyright (c) 2018 LingoChamp Inc.
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

package com.liulishuo.filedownloader.connection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.okdownload.IRedirectHandler;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.RedirectUtil;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.connection.DownloadConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.net.ProtocolException;
import java.util.List;
import java.util.Map;

public class DownloadConnectionAdapter implements DownloadConnection, DownloadConnection.Connected {

    @NonNull
    private final FileDownloadConnection fileDownloadConnection;
    @NonNull
    private final IRedirectHandler redirectHandler;
    @Nullable
    private DownloadConnectionAdapter redirectConnectAdapter;

    private static final String TAG = "DownloadConnectionAdapter";

    public DownloadConnectionAdapter(
            @NonNull FileDownloadConnection fileDownloadConnection,
            @NonNull IRedirectHandler redirectHandler) {
        this.fileDownloadConnection = fileDownloadConnection;
        this.redirectHandler = redirectHandler;
    }

    @Override
    public void addHeader(String name, String value) {
        fileDownloadConnection.addHeader(name, value);
    }

    @Override
    public boolean setRequestMethod(@NonNull String method) throws ProtocolException {
        return fileDownloadConnection.setRequestMethod(method);
    }

    @Override
    public Connected execute() throws IOException {
        final Map<String, List<String>> headerProperties = getRequestProperties();
        fileDownloadConnection.execute();
        redirectHandler.handleRedirect(this, this, headerProperties);
        return this;
    }

    @Override
    public void release() {
        if (redirectConnectAdapter != null) {
            redirectConnectAdapter.release();
        } else {
            fileDownloadConnection.ending();
        }
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return fileDownloadConnection.getRequestHeaderFields();
    }

    /**
     * {@link FileDownloadConnection} don't support this method.
     *
     * @param key property key
     */
    @Override
    public String getRequestProperty(String key) {
        return "unknown";
    }

    @Override
    public int getResponseCode() throws IOException {
        if (redirectConnectAdapter != null) redirectConnectAdapter.getResponseCode();
        return fileDownloadConnection.getResponseCode();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (redirectConnectAdapter != null) redirectConnectAdapter.getInputStream();
        return fileDownloadConnection.getInputStream();
    }

    @Nullable
    @Override
    public Map<String, List<String>> getResponseHeaderFields() {
        if (redirectConnectAdapter != null) redirectConnectAdapter.getResponseHeaderFields();
        return fileDownloadConnection.getResponseHeaderFields();
    }

    @Nullable
    @Override
    public String getResponseHeaderField(String name) {
        if (redirectConnectAdapter != null) redirectConnectAdapter.getResponseHeaderField(name);
        return fileDownloadConnection.getResponseHeaderField(name);
    }

    @Override
    public String getRedirectLocation() {
        return redirectHandler.getRedirectLocation();
    }

    static final class RedirectHandler implements IRedirectHandler {

        String redirectLocation;

        @Override
        public void handleRedirect(
                DownloadConnection originalConnection,
                Connected originalConnected,
                Map<String, List<String>> headerProperties) throws IOException {
            if (!(originalConnection instanceof DownloadConnectionAdapter)) return;
            int responseCode = originalConnected.getResponseCode();
            int redirectCount = 0;
            DownloadConnectionAdapter redirectConnectAdapter = null;
            DownloadConnection checkingDownloadConnection = originalConnection;
            while (RedirectUtil.isRedirect(responseCode)) {
                checkingDownloadConnection.release();
                if (++redirectCount > RedirectUtil.MAX_REDIRECT_TIMES) {
                    throw new ProtocolException("Too many redirect requests: " + redirectCount);
                }

                redirectLocation = RedirectUtil.getRedirectedUrl(originalConnected, responseCode);
                final DownloadConnection redirectConnection = OkDownload.with()
                        .connectionFactory().create(redirectLocation);
                if (redirectConnection instanceof DownloadConnectionAdapter) {
                    Util.addRequestHeaderFields(headerProperties, redirectConnection);
                    redirectConnectAdapter = (DownloadConnectionAdapter) redirectConnection;
                    redirectConnectAdapter.fileDownloadConnection.execute();
                    responseCode = redirectConnectAdapter.getResponseCode();
                    checkingDownloadConnection = redirectConnection;
                } else {
                    redirectLocation = null;
                    throw new InvalidClassException("The connection factory is customized, "
                            + "but now the factory creates a inconsistent connection: "
                            + redirectConnection.getClass().getCanonicalName());
                }
            }
            if (redirectConnectAdapter != null && redirectLocation != null) {
                DownloadConnectionAdapter originalAdapter =
                        ((DownloadConnectionAdapter) originalConnection);
                originalAdapter.redirectConnectAdapter = redirectConnectAdapter;
            }
        }

        @Nullable
        @Override
        public String getRedirectLocation() {
            return redirectLocation;
        }
    }

    public static class Factory implements DownloadConnection.Factory {

        @NonNull
        private final FileDownloadHelper.ConnectionCreator creator;

        public Factory(@NonNull FileDownloadHelper.ConnectionCreator creator) {
            this.creator = creator;
        }

        @Override
        public DownloadConnection create(String url) throws IOException {
            final FileDownloadConnection fileDownloadConnection = creator.create(url);
            return new DownloadConnectionAdapter(fileDownloadConnection, new RedirectHandler());
        }
    }
}
