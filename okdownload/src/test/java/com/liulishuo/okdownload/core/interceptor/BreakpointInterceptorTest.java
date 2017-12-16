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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.download.DownloadStrategy;
import com.liulishuo.okdownload.core.exception.RetryException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import static com.liulishuo.okdownload.TestUtils.mockDownloadChain;
import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.download.DownloadChain.CHUNKED_CONTENT_LENGTH;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
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

        interceptor = spy(new BreakpointInterceptor());
        new File(existPath).createNewFile();
    }

    @After
    public void tearDown() {
        new File(existPath).delete();
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
        doNothing().when(interceptor).discardOldFileIfExist(nullable(String.class));

        interceptor.interceptConnect(mockChain);

        verify(mockChain).unparkOtherBlock();
    }

    @Test
    public void interceptConnect_otherBlockPark_split() throws IOException {
        final long contentLength = 66666L;
        final int blockCount = 6;

        when(mockChain.isOtherBlockPark()).thenReturn(true);
        when(mockChain.getResponseContentLength()).thenReturn(contentLength);
        final DownloadStrategy strategy = OkDownload.with().downloadStrategy();
        doReturn(true).when(strategy)
                .isSplitBlock(eq(contentLength), any(DownloadConnection.Connected.class));

        doReturn(blockCount).when(strategy)
                .determineBlockCount(any(DownloadTask.class), eq(contentLength),
                        any(DownloadConnection.Connected.class));
        doNothing().when(interceptor).splitBlock(eq(blockCount), eq(mockChain));
        doNothing().when(interceptor).discardOldFileIfExist(nullable(String.class));

        interceptor.interceptConnect(mockChain);
        verify(interceptor).splitBlock(eq(blockCount), eq(mockChain));
    }

    @Test
    public void splitBlock() throws IOException {
        when(mockChain.getResponseContentLength()).thenReturn(6666L);

        final BreakpointInfo info = spy(new BreakpointInfo(0, "", "", null));
        when(mockChain.getInfo()).thenReturn(info);

        interceptor.splitBlock(5, mockChain);

        assertThat(info.getBlockCount()).isEqualTo(5);
        long totalLength = 0;
        for (int i = 0; i < 5; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            totalLength += blockInfo.getContentLength();
        }
        assertThat(totalLength).isEqualTo(6666L);

        final BlockInfo lastBlockInfo = info.getBlock(4);
        assertThat(lastBlockInfo.getRangeRight()).isEqualTo(6665L);
    }


    @Test
    public void inspectAnotherSameInfo() throws RetryException {
        final BreakpointStore store = OkDownload.with().breakpointStore();
        final BreakpointInfo info = mock(BreakpointInfo.class);

        assertThat(interceptor
                .inspectAnotherSameInfo(mockChain.getTask(), mockInfo, 100L))
                .isFalse();

        when(mockChain.getTask().isUriIsDirectory()).thenReturn(true);
        assertThat(interceptor
                .inspectAnotherSameInfo(mockChain.getTask(), mockInfo, 100L))
                .isFalse();

        when(info.getId()).thenReturn(1);
        when(mockInfo.getId()).thenReturn(2);
        when(store.findAnotherInfoFromCompare(mockChain.getTask(), mockInfo)).thenReturn(info);

        when(info.getTotalOffset()).thenReturn(0L);
        assertThat(interceptor
                .inspectAnotherSameInfo(mockChain.getTask(), mockInfo, 100L))
                .isFalse();
        verify(store).discard(eq(1));

        when(info.getTotalOffset()).thenReturn(10L);
        when(info.getTotalLength()).thenReturn(101L);
        assertThat(interceptor
                .inspectAnotherSameInfo(mockChain.getTask(), mockInfo, 100L))
                .isFalse();

        when(info.getTotalLength()).thenReturn(100L);
        when(info.getEtag()).thenReturn("old-etag");
        when(mockInfo.getEtag()).thenReturn("new-etag");
        assertThat(interceptor
                .inspectAnotherSameInfo(mockChain.getTask(), mockInfo, 100L))
                .isFalse();

        when(info.getPath()).thenReturn("./no-exist-file");
        when(info.getEtag()).thenReturn("new-etag");
        assertThat(interceptor
                .inspectAnotherSameInfo(mockChain.getTask(), mockInfo, 100L))
                .isFalse();

        when(info.getPath()).thenReturn(existPath);
        assertThat(interceptor
                .inspectAnotherSameInfo(mockChain.getTask(), mockInfo, 100L))
                .isTrue();
        verify(mockInfo).reuseBlocks(eq(info));

        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getCurrentOffset()).thenReturn(5121L);
        assertThat(
                interceptor.inspectAnotherSameInfo(mockChain.getTask(), mockInfo, 100L))
                .isTrue();
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