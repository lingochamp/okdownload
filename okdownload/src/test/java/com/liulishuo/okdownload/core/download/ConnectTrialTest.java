/*
 * Copyright (c) 2018 LingoChamp Inc.
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

package com.liulishuo.okdownload.core.download;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.connection.DownloadConnection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.Util.ACCEPT_RANGES;
import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_DISPOSITION;
import static com.liulishuo.okdownload.core.Util.CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_RANGE;
import static com.liulishuo.okdownload.core.Util.ETAG;
import static com.liulishuo.okdownload.core.Util.IF_MATCH;
import static com.liulishuo.okdownload.core.Util.RANGE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConnectTrialTest {

    private ConnectTrial connectTrial;

    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;
    @Mock private DownloadConnection connection;
    @Mock private DownloadConnection.Connected connected;

    private final String etag = "etag";

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
        Util.setLogger(mock(Util.Logger.class));
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        connectTrial = new ConnectTrial(task, info);

        final DownloadConnection.Factory factory = OkDownload.with().connectionFactory();
        final String url = "https://jacksgong.com";
        when(factory.create(url)).thenReturn(connection);
        when(connection.execute()).thenReturn(connected);
        when(task.getUrl()).thenReturn(url);
        when(info.getUrl()).thenReturn(url);
        when(info.getEtag()).thenReturn(etag);
    }

    @Test
    public void executeTrial() throws Exception {
        connectTrial.executeTrial();

        final DownloadStrategy downloadStrategy = OkDownload.with().downloadStrategy();
        verify(downloadStrategy).inspectNetwork(eq(task));
        verify(connection).addHeader(eq(IF_MATCH), eq(etag));
        verify(connection).addHeader(eq(RANGE), eq("bytes=0-0"));

        verify(connection).execute();
        verify(connection).release();
    }

    @Test
    public void getInstanceLength() throws Exception {
        when(connected.getResponseHeaderField(CONTENT_RANGE))
                .thenReturn("bytes 21010-47021/47022");
        when(connected.getResponseHeaderField(CONTENT_LENGTH))
                .thenReturn("100");
        connectTrial.executeTrial();
        assertThat(connectTrial.getInstanceLength()).isEqualTo(47022L);
        assertThat(connectTrial.isChunked()).isFalse();

        when(connected.getResponseHeaderField(CONTENT_RANGE))
                .thenReturn(null);
        connectTrial.executeTrial();
        assertThat(connectTrial.getInstanceLength()).isEqualTo(100L);
        assertThat(connectTrial.isChunked()).isFalse();

        when(connected.getResponseHeaderField(CONTENT_LENGTH))
                .thenReturn(null);
        connectTrial.executeTrial();
        assertThat(connectTrial.getInstanceLength()).isEqualTo(CHUNKED_CONTENT_LENGTH);
        assertThat(connectTrial.isChunked()).isTrue();
    }

    @Test
    public void isAcceptRange() throws Exception {
        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_PARTIAL);
        connectTrial.executeTrial();
        assertThat(connectTrial.isAcceptRange()).isTrue();

        when(connected.getResponseCode()).thenReturn(0);
        connectTrial.executeTrial();
        assertThat(connectTrial.isAcceptRange()).isFalse();

        when(connected.getResponseHeaderField(ACCEPT_RANGES)).thenReturn("bytes");
        connectTrial.executeTrial();
        assertThat(connectTrial.isAcceptRange()).isTrue();
    }

    @Test
    public void isEtagOverdue() throws Exception {
        when(connected.getResponseHeaderField(ETAG)).thenReturn(etag);
        connectTrial.executeTrial();
        assertThat(connectTrial.isEtagOverdue()).isFalse();

        when(connected.getResponseHeaderField(ETAG)).thenReturn("newEtag");
        connectTrial.executeTrial();
        assertThat(connectTrial.isEtagOverdue()).isTrue();
        assertThat(connectTrial.getResponseEtag()).isEqualTo("newEtag");
    }

    @Test
    public void getResponseFilename() throws IOException {
        connectTrial.executeTrial();
        assertThat(connectTrial.getResponseFilename()).isNull();

        when(connected.getResponseHeaderField(CONTENT_DISPOSITION))
                .thenReturn("attachment;      filename=\"hello world\"");
        connectTrial.executeTrial();
        assertThat(connectTrial.getResponseFilename()).isEqualTo("hello world");

        when(connected.getResponseHeaderField(CONTENT_DISPOSITION))
                .thenReturn("attachment; filename=\"hello world\"");
        connectTrial.executeTrial();
        assertThat(connectTrial.getResponseFilename()).isEqualTo("hello world");

        when(connected.getResponseHeaderField(CONTENT_DISPOSITION))
                .thenReturn("attachment; filename=genome.jpeg\nabc");
        connectTrial.executeTrial();
        assertThat(connectTrial.getResponseFilename()).isEqualTo("genome.jpeg");
    }

    @Test
    public void getResponseCode() throws Exception {
        when(connected.getResponseCode()).thenReturn(1);
        connectTrial.executeTrial();
        assertThat(connectTrial.getResponseCode()).isEqualTo(1);
    }
}