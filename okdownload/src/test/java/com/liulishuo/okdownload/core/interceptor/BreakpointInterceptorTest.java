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

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadCache;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.exception.RetryException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static com.liulishuo.okdownload.TestUtils.mockDownloadChain;
import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_RANGE;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.CONTENT_LENGTH_CHANGED;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BreakpointInterceptorTest {

    private BreakpointInterceptor interceptor;

    @Mock private BreakpointInfo info;
    @Mock private DownloadStore store;
    @Mock private DownloadConnection.Connected connected;
    @Mock private DownloadCache cache;
    @Mock private BlockInfo blockInfo;
    @Mock private DownloadTask task;

    private DownloadChain chain;

    private final String existPath = "./exist-path";

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() throws IOException {
        initMocks(this);

        chain = mockDownloadChain();
        when(chain.getDownloadStore()).thenReturn(store);
        when(store.update(info)).thenReturn(true);
        when(chain.getTask()).thenReturn(task);

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

        interceptor.interceptConnect(chain);

        verify(store).update(chain.getInfo());
        verify(chain).processConnect();
    }

    @Test
    public void interceptFetch_finish() throws IOException {
        BlockInfo blockInfo2 = new BlockInfo(10, 20);

        when(chain.getResponseContentLength()).thenReturn(new Long(10));
        when(chain.getInfo()).thenReturn(info);
        when(chain.getOutputStream()).thenReturn(mock(MultiPointOutputStream.class));
        when(chain.getBlockIndex()).thenReturn(1);
        when(info.getBlock(1)).thenReturn(blockInfo2);
        when(info.getBlockCount()).thenReturn(2);
        when(chain.loopFetch()).thenReturn(1L, 1L, 2L, 1L, 5L, -1L);

        final long contentLength = interceptor.interceptFetch(chain);
        verify(chain, times(6)).loopFetch();
        verify(chain).flushNoCallbackIncreaseBytes();

        assertThat(contentLength).isEqualTo(10);
    }

    @Test
    public void interceptFetch_chunked() throws IOException {
        final BlockInfo blockInfo = new BlockInfo(0, 0);

        when(chain.getResponseContentLength()).thenReturn(Long.valueOf(CHUNKED_CONTENT_LENGTH));
        when(chain.getInfo()).thenReturn(info);
        when(chain.getOutputStream()).thenReturn(mock(MultiPointOutputStream.class));
        when(info.getBlock(anyInt())).thenReturn(blockInfo);
        when(chain.loopFetch()).thenReturn(1L, 1L, 2L, 1L, 5L, -1L);

        final long contentLength = interceptor.interceptFetch(chain);
        verify(chain, times(6)).loopFetch();

        assertThat(contentLength).isEqualTo(10);
    }

    @Test
    public void getRangeRightFromContentRange() {
        assertThat(BreakpointInterceptor.getRangeRightFromContentRange("bytes 1-111/222"))
                .isEqualTo(111L);

        assertThat(BreakpointInterceptor.getRangeRightFromContentRange("bytes 1 -111/222"))
                .isEqualTo(111L);

        assertThat(BreakpointInterceptor.getRangeRightFromContentRange("bytes 1 - 111/222"))
                .isEqualTo(111L);

        assertThat(BreakpointInterceptor.getRangeRightFromContentRange("bytes 1 - 111 /222"))
                .isEqualTo(111L);

        assertThat(BreakpointInterceptor.getRangeRightFromContentRange("bytes 1 - 111 / 222"))
                .isEqualTo(111L);
    }

    @Test
    public void getExactContentLength_contentRange() {
        when(connected.getResponseHeaderField(CONTENT_RANGE)).thenReturn("bytes 0-111/222");
        assertThat(interceptor.getExactContentLengthRangeFrom0(connected)).isEqualTo(112L);
    }

    @Test
    public void getExactContentLength_contentLength() {
        when(connected.getResponseHeaderField(CONTENT_LENGTH)).thenReturn("123");
        assertThat(interceptor.getExactContentLengthRangeFrom0(connected)).isEqualTo(123L);
    }

    @Test
    public void interceptConnect_singleBlockCheck() throws IOException {
        mockOkDownload();

        when(chain.processConnect()).thenReturn(connected);
        when(chain.getInfo()).thenReturn(info);
        when(chain.getCache()).thenReturn(cache);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(info.getBlockCount()).thenReturn(1);
        when(info.isChunked()).thenReturn(false);

        when(info.getTotalLength()).thenReturn(1L);
        doReturn(2L).when(interceptor).getExactContentLengthRangeFrom0(connected);

        interceptor.interceptConnect(chain);

        ArgumentCaptor<BlockInfo> captor = ArgumentCaptor.forClass(BlockInfo.class);

        verify(info).addBlock(captor.capture());
        verify(info).resetBlockInfos();

        final BlockInfo addBlockInfo = captor.getValue();
        assertThat(addBlockInfo.getRangeLeft()).isZero();
        assertThat(addBlockInfo.getContentLength()).isEqualTo(2L);
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(CONTENT_LENGTH_CHANGED));
    }

    @Test(expected = RetryException.class)
    public void interceptConnect_singleBlockCheck_fromBreakpoint() throws IOException {
        when(chain.processConnect()).thenReturn(connected);
        when(chain.getInfo()).thenReturn(info);
        when(chain.getCache()).thenReturn(cache);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(info.getBlockCount()).thenReturn(1);
        when(info.isChunked()).thenReturn(false);

        when(info.getTotalLength()).thenReturn(1L);
        doReturn(2L).when(interceptor).getExactContentLengthRangeFrom0(connected);

        when(blockInfo.getRangeLeft()).thenReturn(1L);

        interceptor.interceptConnect(chain);
    }

    @Test
    public void interceptConnect_singleBlockCheck_expect() throws IOException {
        when(chain.processConnect()).thenReturn(connected);
        when(chain.getInfo()).thenReturn(info);
        when(chain.getCache()).thenReturn(cache);
        when(info.getBlockCount()).thenReturn(1);
        when(info.isChunked()).thenReturn(false);
        when(info.getBlock(0)).thenReturn(blockInfo);

        when(info.getTotalLength()).thenReturn(2L);
        doReturn(2L).when(interceptor).getExactContentLengthRangeFrom0(connected);

        interceptor.interceptConnect(chain);

        verify(info, never()).resetBlockInfos();
        verify(info, never()).addBlock(any(BlockInfo.class));
    }

    @Test
    public void interceptConnect_notSingleBlockOrChunked() throws IOException {
        when(chain.processConnect()).thenReturn(connected);
        when(chain.getInfo()).thenReturn(info);
        when(chain.getCache()).thenReturn(cache);

        // not single block
        when(info.getBlockCount()).thenReturn(2);
        when(info.isChunked()).thenReturn(false);
        interceptor.interceptConnect(chain);
        verify(info, never()).resetBlockInfos();
        verify(info, never()).addBlock(any(BlockInfo.class));

        // chunked
        when(info.getBlockCount()).thenReturn(1);
        when(info.isChunked()).thenReturn(true);
        interceptor.interceptConnect(chain);
        verify(info, never()).resetBlockInfos();
        verify(info, never()).addBlock(any(BlockInfo.class));
    }
}