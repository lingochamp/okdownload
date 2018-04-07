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

package com.liulishuo.okdownload.core.interceptor.connect;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadChain;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liulishuo.okdownload.TestUtils.mockDownloadChain;
import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_RANGE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeaderInterceptorTest {

    private HeaderInterceptor interceptor;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
        Util.setLogger(mock(Util.Logger.class));
    }

    @Before
    public void setup() {
        interceptor = new HeaderInterceptor();
    }

    @Test
    public void interceptConnect_range() throws IOException {
        final DownloadChain chain = mockDownloadChain();
        DownloadConnection connection = chain.getConnectionOrCreate();
        final BreakpointInfo info = chain.getInfo();

        when(info.getBlockCount()).thenReturn(3);

        when(info.getBlock(0)).thenReturn(new BlockInfo(0, 10));

        interceptor.interceptConnect(chain);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(connection).addHeader(nameCaptor.capture(), valueCaptor.capture());

        assertThat(nameCaptor.getAllValues()).containsExactly("Range");
        assertThat(valueCaptor.getAllValues()).containsExactly("bytes=0-9");


        when(chain.getBlockIndex()).thenReturn(1);
        when(info.getBlock(1)).thenReturn(new BlockInfo(10, 10));
        // new one.
        connection = mock(DownloadConnection.class);
        when(chain.getConnectionOrCreate()).thenReturn(connection);

        interceptor.interceptConnect(chain);

        nameCaptor = ArgumentCaptor.forClass(String.class);
        valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).addHeader(nameCaptor.capture(), valueCaptor.capture());

        assertThat(nameCaptor.getAllValues()).containsExactly("Range");
        assertThat(valueCaptor.getAllValues()).containsExactly("bytes=10-19");

        when(chain.getBlockIndex()).thenReturn(2);
        when(info.getBlock(2)).thenReturn(new BlockInfo(20, 10));
        // new one.
        connection = mock(DownloadConnection.class);
        when(chain.getConnectionOrCreate()).thenReturn(connection);

        interceptor.interceptConnect(chain);

        nameCaptor = ArgumentCaptor.forClass(String.class);
        valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).addHeader(nameCaptor.capture(), valueCaptor.capture());

        assertThat(nameCaptor.getAllValues()).containsExactly("Range");
        assertThat(valueCaptor.getAllValues()).containsExactly("bytes=20-29");
    }

    @Test
    public void interceptConnect() throws IOException {
        Map<String, List<String>> customHeader = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("header1-value1");
        values.add("header1-value2");
        customHeader.put("header1", values);
        values = new ArrayList<>();
        values.add("header2-value");
        customHeader.put("header2", values);

        final DownloadChain chain = mockDownloadChain();
        when(chain.getInfo().getBlock(0))
                .thenReturn(new BlockInfo(0, 10));
        final DownloadConnection connection = chain.getConnectionOrCreate();
        final DownloadConnection.Connected connected = chain.processConnect();
        final BreakpointInfo info = chain.getInfo();

        final DownloadTask task = chain.getTask();
        when(task.getHeaderMapFields()).thenReturn(customHeader);
        when(info.getEtag()).thenReturn("etag1");

        interceptor.interceptConnect(chain);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(connection, times(5))
                .addHeader(nameCaptor.capture(), valueCaptor.capture());

        assertThat(nameCaptor.getAllValues())
                .containsExactlyInAnyOrder("header1", "header1", "header2", "Range", "If-Match");
        assertThat(valueCaptor.getAllValues())
                .containsExactlyInAnyOrder("header1-value1", "header1-value2", "header2-value",
                        "bytes=0-9", "etag1");

        verify(OkDownload.with().downloadStrategy())
                .resumeAvailableResponseCheck(eq(connected), eq(0), eq(info));
        verify(OkDownload.with().downloadStrategy()
                .resumeAvailableResponseCheck(connected, 0, info)).inspect();

        ArgumentCaptor<Long> contentLengthCaptor = ArgumentCaptor.forClass(Long.class);
        verify(chain).setResponseContentLength(contentLengthCaptor.capture());
        assertThat(contentLengthCaptor.getValue()).isEqualTo(CHUNKED_CONTENT_LENGTH);

        when(connected.getResponseHeaderField(CONTENT_LENGTH)).thenReturn("10");
        interceptor.interceptConnect(chain);
        verify(chain, times(2)).setResponseContentLength(contentLengthCaptor.capture());
        assertThat(contentLengthCaptor.getAllValues()).containsOnly(-1L, 10L);

        when(connected.getResponseHeaderField(CONTENT_LENGTH)).thenReturn(null);
        when(connected.getResponseHeaderField(CONTENT_RANGE)).thenReturn("bytes 2-5/333");
        interceptor.interceptConnect(chain);
        verify(chain, times(3)).setResponseContentLength(contentLengthCaptor.capture());
        assertThat(contentLengthCaptor.getAllValues()).containsOnly(-1L, 10L, 4L);
    }
}