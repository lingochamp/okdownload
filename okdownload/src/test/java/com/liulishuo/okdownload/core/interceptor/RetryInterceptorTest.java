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

import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadCache;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.exception.RetryException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RetryInterceptorTest {

    private RetryInterceptor interceptor;
    @Mock private DownloadChain chain;
    @Mock private DownloadCache cache;
    @Mock private DownloadConnection.Connected connected;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        initMocks(this);

        interceptor = spy(new RetryInterceptor());

        when(chain.getCache()).thenReturn(cache);
    }

    @Test
    public void interceptConnect_interrupt() throws IOException {
        when(cache.isInterrupt()).thenReturn(true);

        thrown.expect(InterruptException.class);
        interceptor.interceptConnect(chain);
        verify(chain, never()).processConnect();
    }

    @Test
    public void interceptConnect_retry() throws IOException {
        doThrow(RetryException.class).doReturn(connected).when(chain).processConnect();

        interceptor.interceptConnect(chain);

        verify(chain, times(2)).processConnect();
        verify(chain).resetConnectForRetry();
        verify(cache, never()).catchException(any(IOException.class));
    }

    @Test
    public void interceptConnect_userCanceled() throws IOException {
        doThrow(InterruptException.class).when(chain).processConnect();
        when(cache.isUserCanceled()).thenReturn(true);

        thrown.expect(InterruptException.class);
        interceptor.interceptConnect(chain);
        verify(cache).catchException(any(IOException.class));
    }

    @Test
    public void interceptConnect_failedReleaseConnection() throws IOException {
        final DownloadConnection connection = mock(DownloadConnection.class);
        when(chain.getConnection()).thenReturn(connection);
        doThrow(IOException.class).doReturn(connected).when(chain).processConnect();

        thrown.expect(IOException.class);
        interceptor.interceptConnect(chain);

        verify(cache).catchException(any(IOException.class));
        verify(connection).release();
    }

    @Test
    public void interceptFetch_failedRelease() throws IOException {
        final MultiPointOutputStream outputStream = mock(MultiPointOutputStream.class);
        when(chain.getOutputStream()).thenReturn(outputStream);
        doThrow(IOException.class).when(chain).processFetch();

        thrown.expect(IOException.class);
        interceptor.interceptFetch(chain);

        final int blockIndex = chain.getBlockIndex();
        verify(cache).catchException(any(IOException.class));
        verify(outputStream).close(eq(blockIndex));
    }
}