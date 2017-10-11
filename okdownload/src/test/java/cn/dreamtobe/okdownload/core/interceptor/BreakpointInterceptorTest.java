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

package cn.dreamtobe.okdownload.core.interceptor;

import android.net.Uri;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.download.DownloadChain;

import static cn.dreamtobe.okdownload.TestUtils.mockDownloadChain;
import static cn.dreamtobe.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BreakpointInterceptorTest {

    private BreakpointInterceptor interceptor;

    @Mock private BreakpointInfo mockInfo;

    private DownloadChain mockChain;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() throws IOException {
        initMocks(this);

        mockChain = mockDownloadChain();

        interceptor = spy(new BreakpointInterceptor());
    }

    @Test
    public void interceptConnect_process() throws IOException {
        interceptor.interceptConnect(mockChain);

        final BreakpointStore store = OkDownload.with().breakpointStore();
        verify(store).update(mockChain.getInfo());
        verify(mockChain).processConnect();
    }

    @Test
    public void interceptConnect_otherBlockPark_unpark() throws IOException {
        when(mockChain.isOtherBlockPark()).thenReturn(true);
        when(mockChain.processConnect()).thenReturn(mock(DownloadConnection.Connected.class));
        doNothing().when(interceptor).splitBlock(anyInt(), eq(mockChain));

        interceptor.interceptConnect(mockChain);

        verify(mockChain).unparkOtherBlock();
    }

    @Test
    public void splitBlock() throws IOException {
        when(mockChain.getResponseContentLength()).thenReturn(6666L);

        final BreakpointInfo info = spy(new BreakpointInfo(0, "", mock(Uri.class)));
        when(mockChain.getInfo()).thenReturn(info);

        interceptor.splitBlock(5, mockChain);

        assertThat(info.getBlockCount()).isEqualTo(5);
        long totalLength = 0;
        for (int i = 0; i < 5; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            totalLength += blockInfo.contentLength;
        }
        assertThat(totalLength).isEqualTo(6666L);

        final BlockInfo lastBlockInfo = info.getBlock(4);
        assertThat(lastBlockInfo.startOffset + lastBlockInfo.contentLength).isEqualTo(6666L);
    }

    @Test
    public void interceptFetch_finish() throws IOException {
        final BlockInfo blockInfo = new BlockInfo(0, 10, 0);

        when(mockChain.getResponseContentLength()).thenReturn(new Long(10));
        when(mockChain.getInfo()).thenReturn(mockInfo);
        when(mockInfo.getBlock(anyInt())).thenReturn(blockInfo);
        when(mockChain.loopFetch()).thenReturn(1L, 1L, 2L, 1L, 5L, -1L);

        final long contentLength = interceptor.interceptFetch(mockChain);
        verify(mockChain, times(6)).loopFetch();

        assertThat(contentLength).isEqualTo(10);
    }

    @Test(expected = IOException.class)
    public void interceptFetch_contentLengthNotMatch_exception() throws IOException {
        final BlockInfo blockInfo = new BlockInfo(0, 1, 0);

        when(mockChain.getInfo()).thenReturn(mockInfo);
        when(mockInfo.getBlock(anyInt())).thenReturn(blockInfo);
        when(mockChain.loopFetch()).thenReturn(new Long(-1));

        interceptor.interceptFetch(mockChain);
    }

}