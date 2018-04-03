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

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.dispatcher.CallbackDispatcher;
import com.liulishuo.okdownload.core.download.DownloadCache;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.download.DownloadStrategy;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FetchDataInterceptorTest {
    @Mock private InputStream inputStream;
    @Mock private MultiPointOutputStream outputStream;
    @Mock private DownloadTask task;
    @Mock private DownloadChain chain;

    private FetchDataInterceptor interceptor;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() {
        initMocks(this);

        interceptor = new FetchDataInterceptor(0, inputStream, outputStream, task);
        when(chain.getCache()).thenReturn(mock(DownloadCache.class));
        when(chain.getTask()).thenReturn(task);
    }

    @Test
    public void interceptFetch() throws IOException {
        final CallbackDispatcher dispatcher = OkDownload.with().callbackDispatcher();

        doReturn(10).when(inputStream).read(any(byte[].class));
        doReturn(true).when(dispatcher).isFetchProcessMoment(task);

        interceptor.interceptFetch(chain);

        final DownloadStrategy downloadStrategy = OkDownload.with().downloadStrategy();
        verify(downloadStrategy).inspectNetworkOnWifi(eq(task));
        verify(chain).increaseCallbackBytes(10L);
        verify(chain).flushNoCallbackIncreaseBytes();
        verify(outputStream).write(eq(0), any(byte[].class), eq(10));
    }
}