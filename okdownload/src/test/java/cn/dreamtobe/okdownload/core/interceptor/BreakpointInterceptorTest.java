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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.download.DownloadChain;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BreakpointInterceptorTest {

    private BreakpointInterceptor interceptor;

    @Mock
    private BreakpointInfo mockInfo;


    @BeforeClass
    public static void setupClass() {
        OkDownload.setSingletonInstance(new OkDownload.Builder()
                .breakpointStore(mock(BreakpointStore.class))
                .build());
    }

    @Before
    public void setup() {
        initMocks(this);
        interceptor = new BreakpointInterceptor();
    }

    @Test
    public void interceptConnect_process() throws IOException {
        final DownloadChain mockChain = mock(DownloadChain.class);
        interceptor.interceptConnect(mockChain);

        verify(mockChain).processConnect();
    }

    @Test
    public void interceptFetch_finish() throws IOException {
        final DownloadChain mockChain = mock(DownloadChain.class);

        when(mockChain.getInfo()).thenReturn(mockInfo);
        when(mockInfo.getBlock(anyInt())).thenReturn(mock(BlockInfo.class));
        when(mockChain.loopFetch()).thenReturn(new Long(-1));

        final long contentLength = interceptor.interceptFetch(mockChain);
        verify(mockChain).loopFetch();

        assertThat(contentLength).isEqualTo(0);
    }

    @Test(expected = IOException.class)
    public void interceptFetch_contentLengthNotMatch_exception() throws IOException {
        final DownloadChain mockChain = mock(DownloadChain.class);
        final BlockInfo blockInfo = new BlockInfo(0, 1, 0);

        when(mockChain.getInfo()).thenReturn(mockInfo);
        when(mockInfo.getBlock(anyInt())).thenReturn(blockInfo);
        when(mockChain.loopFetch()).thenReturn(new Long(-1));

        interceptor.interceptFetch(mockChain);
    }

}