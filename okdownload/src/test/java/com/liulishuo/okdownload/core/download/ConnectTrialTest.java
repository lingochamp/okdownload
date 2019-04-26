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

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.dispatcher.CallbackDispatcher;
import com.liulishuo.okdownload.core.exception.DownloadSecurityException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.Util.ACCEPT_RANGES;
import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_DISPOSITION;
import static com.liulishuo.okdownload.core.Util.CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.CONTENT_RANGE;
import static com.liulishuo.okdownload.core.Util.ETAG;
import static com.liulishuo.okdownload.core.Util.IF_MATCH;
import static com.liulishuo.okdownload.core.Util.METHOD_HEAD;
import static com.liulishuo.okdownload.core.Util.RANGE;
import static com.liulishuo.okdownload.core.Util.TRANSFER_ENCODING;
import static com.liulishuo.okdownload.core.Util.VALUE_CHUNKED;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConnectTrialTest {

    private ConnectTrial connectTrial;

    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;
    @Mock private DownloadConnection connection;
    @Mock private DownloadConnection.Connected connected;
    @Rule public ExpectedException thrown= ExpectedException.none();

    private final String etag = "etag";
    private final String url = "https://jacksgong.com";

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        connectTrial = spy(new ConnectTrial(task, info));

        setupConnection();
        when(connection.execute()).thenReturn(connected);
        when(task.getUrl()).thenReturn(url);
        when(info.getUrl()).thenReturn(url);
        when(info.getEtag()).thenReturn(etag);
    }

    private void setupConnection() throws IOException {
        mockOkDownload();
        final DownloadConnection.Factory factory = OkDownload.with().connectionFactory();
        when(factory.create(url)).thenReturn(connection);
    }

    @Test
    public void executeTrial() throws Exception {
        final String redirectLocation = "http://location";
        when(connected.getRedirectLocation()).thenReturn(redirectLocation);

        connectTrial.executeTrial();

        final DownloadStrategy downloadStrategy = OkDownload.with().downloadStrategy();
        verify(downloadStrategy).inspectNetworkOnWifi(eq(task));
        verify(downloadStrategy).inspectNetworkAvailable();
        verify(connection).addHeader(eq(IF_MATCH), eq(etag));
        verify(connection).addHeader(eq(RANGE), eq("bytes=0-0"));

        verify(task).setRedirectLocation(redirectLocation);
        verify(connection).execute();
        verify(connection).release();
    }

    @Test
    public void executeTrial_userHeader() throws Exception {
        Map<String, List<String>> userHeader = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("header1-value1");
        values.add("header1-value2");
        userHeader.put("header1", values);
        values = new ArrayList<>();
        values.add("header2-value");
        userHeader.put("header2", values);
        when(task.getHeaderMapFields()).thenReturn(userHeader);

        connectTrial.executeTrial();

        verify(connection).addHeader(eq("header1"), eq("header1-value1"));
        verify(connection).addHeader(eq("header1"), eq("header1-value2"));
        verify(connection).addHeader(eq("header2"), eq("header2-value"));
    }

    @Test
    public void executeTrial_needTrialHeadMethod() throws IOException {
        when(connectTrial.isNeedTrialHeadMethodForInstanceLength(anyLong(), eq(connected)))
                .thenReturn(true);
        connectTrial.executeTrial();

        verify(connectTrial).isNeedTrialHeadMethodForInstanceLength(anyLong(), eq(connected));
        verify(connectTrial).trialHeadMethodForInstanceLength();
    }

    @Test
    public void executeTrial_noNeedTrialHeadMethod() throws IOException {
        when(connectTrial.isNeedTrialHeadMethodForInstanceLength(anyLong(), eq(connected)))
                .thenReturn(false);
        connectTrial.executeTrial();

        verify(connectTrial).isNeedTrialHeadMethodForInstanceLength(anyLong(), eq(connected));
        verify(connectTrial, never()).trialHeadMethodForInstanceLength();
    }

    @Test
    public void getInstanceLength() throws Exception {
        when(connected.getResponseHeaderField(CONTENT_RANGE))
                .thenReturn("bytes 21010-47021/47022");
        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_PARTIAL);
        connectTrial.executeTrial();
        assertThat(connectTrial.getInstanceLength()).isEqualTo(47022L);
        assertThat(connectTrial.isChunked()).isFalse();

        when(connected.getResponseHeaderField(CONTENT_RANGE)).thenReturn(null);
        connectTrial.executeTrial();
        assertThat(connectTrial.getInstanceLength()).isEqualTo(CHUNKED_CONTENT_LENGTH);
        assertThat(connectTrial.isChunked()).isTrue();

        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_PARTIAL);
        when(connected.getResponseHeaderField(CONTENT_RANGE)).thenReturn(null);
        when(connected.getResponseHeaderField(CONTENT_LENGTH)).thenReturn("47022");
        connectTrial.executeTrial();
        assertThat(connectTrial.getInstanceLength()).isEqualTo(47022L);
        assertThat(connectTrial.isChunked()).isFalse();
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

        when(connected.getResponseHeaderField(CONTENT_DISPOSITION))
                .thenReturn("attachment; filename=../data/data/genome.png");
        thrown.expect(DownloadSecurityException.class);
        thrown.expectMessage("The filename [../data/data/genome.png] from the response is not "
                + "allowable, because it contains '../', which can raise the directory traversal "
                + "vulnerability");
        connectTrial.executeTrial();

        when(connected.getResponseHeaderField(CONTENT_DISPOSITION))
                .thenReturn("attachment; filename=a/b/../abc");
        thrown.expect(DownloadSecurityException.class);
        thrown.expectMessage("The filename [a/b/../abc] from the response is not "
                + "allowable, because it contains '../', which can raise the directory traversal "
                + "vulnerability");
        connectTrial.executeTrial();
    }

    @Test
    public void getResponseCode() throws Exception {
        when(connected.getResponseCode()).thenReturn(1);
        connectTrial.executeTrial();
        assertThat(connectTrial.getResponseCode()).isEqualTo(1);
    }

    @Test
    public void isNeedTrialHeadMethodForInstanceLength_oldOneIsValid_false() {
        assertThat(connectTrial.isNeedTrialHeadMethodForInstanceLength(1, connected)).isFalse();
    }

    @Test
    public void isNeedTrialHeadMethodForInstanceLength_contentRangeValid_false() {
        when(connected.getResponseHeaderField(CONTENT_RANGE)).thenReturn("has value");
        assertThat(connectTrial
                .isNeedTrialHeadMethodForInstanceLength(CHUNKED_CONTENT_LENGTH, connected))
                .isFalse();
    }


    @Test
    public void isNeedTrialHeadMethodForInstanceLength_chunked_false() {
        when(connected.getResponseHeaderField(TRANSFER_ENCODING)).thenReturn(VALUE_CHUNKED);
        assertThat(connectTrial
                .isNeedTrialHeadMethodForInstanceLength(CHUNKED_CONTENT_LENGTH, connected))
                .isFalse();
    }


    @Test
    public void isNeedTrialHeadMethodForInstanceLength_contentLengthNotResponse_false() {
        when(connected.getResponseHeaderField(CONTENT_LENGTH)).thenReturn(null);
        assertThat(connectTrial
                .isNeedTrialHeadMethodForInstanceLength(CHUNKED_CONTENT_LENGTH, connected))
                .isFalse();
    }

    @Test
    public void isNeedTrialHeadMethodForInstanceLength_true() {
        when(connected.getResponseHeaderField(CONTENT_RANGE)).thenReturn(null);
        when(connected.getResponseHeaderField(TRANSFER_ENCODING)).thenReturn("not chunked");
        when(connected.getResponseHeaderField(CONTENT_LENGTH)).thenReturn("1");
        assertThat(connectTrial
                .isNeedTrialHeadMethodForInstanceLength(CHUNKED_CONTENT_LENGTH, connected))
                .isTrue();
    }

    @Test
    public void trialHeadMethodForInstanceLength() throws IOException {
        final DownloadConnection.Factory factory = OkDownload.with().connectionFactory();
        final DownloadConnection connection = mock(DownloadConnection.class);
        when(factory.create(anyString())).thenReturn(connection);

        final DownloadConnection.Connected connected = mock(DownloadConnection.Connected.class);
        when(connection.execute()).thenReturn(connected);

        when(connected.getResponseHeaderField(CONTENT_LENGTH)).thenReturn("10");

        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();
        final DownloadListener listener = mock(DownloadListener.class);
        when(callbackDispatcher.dispatch()).thenReturn(listener);

        connectTrial.trialHeadMethodForInstanceLength();

        verify(connection).setRequestMethod(eq(METHOD_HEAD));
        verify(listener).connectTrialStart(eq(task), nullable(Map.class));
        verify(listener).connectTrialEnd(eq(task), anyInt(), nullable(Map.class));
        assertThat(connectTrial.getInstanceLength()).isEqualTo(10L);
    }

    @Test
    public void trialHeadMethodForInstanceLength_userHeader() throws Exception {
        Map<String, List<String>> userHeader = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("header1-value1");
        values.add("header1-value2");
        userHeader.put("header1", values);
        values = new ArrayList<>();
        values.add("header2-value");
        userHeader.put("header2", values);
        when(task.getHeaderMapFields()).thenReturn(userHeader);

        connectTrial.trialHeadMethodForInstanceLength();

        verify(connection).addHeader(eq("header1"), eq("header1-value1"));
        verify(connection).addHeader(eq("header1"), eq("header1-value2"));
        verify(connection).addHeader(eq("header2"), eq("header2-value"));
    }
}