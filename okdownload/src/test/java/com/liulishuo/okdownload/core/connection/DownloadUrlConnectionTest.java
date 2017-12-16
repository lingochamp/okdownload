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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class DownloadUrlConnectionTest {

    @Mock
    private Proxy mockProxy;

    @Mock
    private URLConnection mockConnection;

    @Mock
    private URL mockURL;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        Mockito.when(mockURL.openConnection()).thenReturn(mockConnection);
        Mockito.when(mockURL.openConnection(mockProxy)).thenReturn(mockConnection);
    }

    @Test
    public void construct_noConfiguration_noAssigned() throws IOException {
        DownloadUrlConnection.Factory factory = new DownloadUrlConnection.Factory();

        factory.create("http://blog.dreamtobe.cn");

        verify(mockConnection, never()).setConnectTimeout(anyInt());
        verify(mockConnection, never()).setReadTimeout(anyInt());
    }

    @Test
    public void construct_validConfiguration_Assigned() throws IOException {


        DownloadUrlConnection.Factory factory = new DownloadUrlConnection.Factory(
                new DownloadUrlConnection.Configuration()
                        .proxy(mockProxy)
                        .connectTimeout(123)
                        .readTimeout(456)
        );

        factory.create(mockURL);

        verify(mockURL).openConnection(mockProxy);
        verify(mockConnection).setConnectTimeout(123);
        verify(mockConnection).setReadTimeout(456);
    }
}