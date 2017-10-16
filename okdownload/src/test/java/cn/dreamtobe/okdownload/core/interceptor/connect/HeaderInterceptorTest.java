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
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.download.DownloadChain;

import static cn.dreamtobe.okdownload.TestUtils.mockDownloadChain;
import static cn.dreamtobe.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
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
        assertThat(valueCaptor.getAllValues()).containsExactly("bytes=0-");


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
        assertThat(valueCaptor.getAllValues()).containsExactly("bytes=10-20");

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
        assertThat(valueCaptor.getAllValues()).containsExactly("bytes=20-");
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
                        "bytes=0-", "etag1");
    }
}