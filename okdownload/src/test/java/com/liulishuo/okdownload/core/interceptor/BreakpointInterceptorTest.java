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

import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static com.liulishuo.okdownload.TestUtils.mockDownloadChain;
import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BreakpointInterceptorTest {

    private BreakpointInterceptor interceptor;

    @Mock private BreakpointInfo mockInfo;
    @Mock private DownloadStore store;

    private DownloadChain mockChain;

    private final String existPath = "./exist-path";

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
        Util.setLogger(mock(Util.Logger.class));
    }

    @Before
    public void setup() throws IOException {
        initMocks(this);

        mockChain = mockDownloadChain();
        when(mockChain.getDownloadStore()).thenReturn(store);

        interceptor = spy(new BreakpointInterceptor());
        new File(existPath).createNewFile();
    }

    @After
    public void tearDown() {
        new File(existPath).delete();
    }

    @Test
    public void interceptConnect_process() throws IOException {
        when(store.update(any(BreakpointInfo.class))).thenReturn(true);

        interceptor.interceptConnect(mockChain);

        verify(store).update(mockChain.getInfo());
        verify(mockChain).processConnect();
    }

    @Test
    public void interceptFetch_finish() throws IOException {
        BlockInfo blockInfo2 = new BlockInfo(10, 20);

        when(mockChain.getResponseContentLength()).thenReturn(new Long(10));
        when(mockChain.getInfo()).thenReturn(mockInfo);
        when(mockChain.getOutputStream()).thenReturn(mock(MultiPointOutputStream.class));
        when(mockChain.getBlockIndex()).thenReturn(1);
        when(mockInfo.getBlock(1)).thenReturn(blockInfo2);
        when(mockInfo.getBlockCount()).thenReturn(2);
        when(mockChain.loopFetch()).thenReturn(1L, 1L, 2L, 1L, 5L, -1L);

        final long contentLength = interceptor.interceptFetch(mockChain);
        verify(mockChain, times(6)).loopFetch();
        verify(mockChain).flushNoCallbackIncreaseBytes();

        assertThat(contentLength).isEqualTo(10);
    }

    @Test
    public void interceptFetch_chunked() throws IOException {
        final BlockInfo blockInfo = new BlockInfo(0, 0);

        when(mockChain.getResponseContentLength()).thenReturn(Long.valueOf(CHUNKED_CONTENT_LENGTH));
        when(mockChain.getInfo()).thenReturn(mockInfo);
        when(mockChain.getOutputStream()).thenReturn(mock(MultiPointOutputStream.class));
        when(mockInfo.getBlock(anyInt())).thenReturn(blockInfo);
        when(mockChain.loopFetch()).thenReturn(1L, 1L, 2L, 1L, 5L, -1L);

        final long contentLength = interceptor.interceptFetch(mockChain);
        verify(mockChain, times(6)).loopFetch();

        assertThat(contentLength).isEqualTo(10);
    }
}