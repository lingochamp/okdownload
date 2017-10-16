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

package cn.dreamtobe.okdownload.core.interceptor.connect;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.download.DownloadChain;

import static cn.dreamtobe.okdownload.TestUtils.mockDownloadChain;
import static cn.dreamtobe.okdownload.TestUtils.mockOkDownload;
import static cn.dreamtobe.okdownload.core.interceptor.connect.RedirectInterceptor.MAX_REDIRECT_TIMES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RedirectInterceptorTest {

    private RedirectInterceptor interceptor;

    private DownloadChain mockChain;

    @Mock private DownloadConnection.Connected mockConnected;


    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() throws IOException {
        interceptor = new RedirectInterceptor();

        initMocks(this);

        mockChain = mockDownloadChain();
        when(mockChain.processConnect()).thenReturn(mockConnected);
    }


    @Test(expected = ProtocolException.class)
    public void interceptConnect_redirectMaxTimes_exception() throws IOException {

        when(mockConnected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_MOVED_PERM);
        when(mockConnected.getResponseHeaderField("Location")).thenReturn("mock");

        interceptor.interceptConnect(mockChain);
    }


    @Test(expected = ProtocolException.class)
    public void interceptConnect_redirectNoLocation_exception() throws IOException {
        when(mockConnected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_MOVED_PERM);

        interceptor.interceptConnect(mockChain);
    }

    @Test
    public void interceptConnect_redirect_processEachTime() throws IOException {
        final String mockLocation = "mock location";
        when(mockConnected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_MOVED_PERM);
        when(mockConnected.getResponseHeaderField("Location")).thenReturn(mockLocation);

        try {
            interceptor.interceptConnect(mockChain);
        } catch (ProtocolException e) {
            // ignore
        }

        verify(mockChain, times(MAX_REDIRECT_TIMES)).processConnect();
        verify(mockChain, times(MAX_REDIRECT_TIMES - 1))
                .setRedirectLocation(mockLocation);
        verify(mockChain, times(MAX_REDIRECT_TIMES - 1))
                .setConnection(any(DownloadConnection.class));
    }

    @Test
    public void interceptConnect_noRedirect_success() throws IOException {
        when(mockConnected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        interceptor.interceptConnect(mockChain);

        verify(mockChain).processConnect();
    }
}