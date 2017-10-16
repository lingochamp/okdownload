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

package cn.dreamtobe.okdownload.core.download;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.interceptor.BreakpointInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.FetchDataInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.Interceptor;
import cn.dreamtobe.okdownload.core.interceptor.RetryInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.connect.CallServerInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.connect.HeaderInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.connect.RedirectInterceptor;

import static cn.dreamtobe.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownloadChainTest {

    private DownloadChain chain;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() {
        this.chain = spy(DownloadChain.createChain(0,
                mock(DownloadTask.class), mock(BreakpointInfo.class),
                mock(DownloadCache.class)));
    }

    @Test
    public void getConnectionOrCreate() throws IOException {

        final String infoUrl = "infoUrl";
        final DownloadConnection.Factory connectionFactory = OkDownload.with().connectionFactory();
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getUrl()).thenReturn(infoUrl);

        DownloadChain chain = DownloadChain.createChain(0,
                mock(DownloadTask.class), info,
                mock(DownloadCache.class));

        // using info url
        final DownloadConnection connection = chain.getConnectionOrCreate();
        verify(connectionFactory).create(infoUrl);

        // using created one.
        assertThat(chain.getConnectionOrCreate()).isEqualTo(connection);

        final String redirectLocation = "redirectLocation";
        final DownloadCache cache = mock(DownloadCache.class);
        when(cache.getRedirectLocation()).thenReturn(redirectLocation);
        chain = DownloadChain.createChain(0,
                mock(DownloadTask.class), info,
                cache);

        // using redirect location instead of info url.
        chain.getConnectionOrCreate();
        verify(connectionFactory).create(redirectLocation);
    }

    @Test
    public void start() throws IOException {
        final DownloadConnection.Connected connected = mock(DownloadConnection.Connected.class);
        doReturn(connected).when(chain).processConnect();
        doReturn(100L).when(chain).processFetch();
        chain.start();

        final List<Interceptor.Connect> connectInterceptorList = chain.connectInterceptorList;
        assertThat(connectInterceptorList).hasSize(5);

        assertThat(connectInterceptorList.get(0)).isInstanceOf(RetryInterceptor.class);
        assertThat(connectInterceptorList.get(1)).isInstanceOf(BreakpointInterceptor.class);
        assertThat(connectInterceptorList.get(2)).isInstanceOf(RedirectInterceptor.class);
        assertThat(connectInterceptorList.get(3)).isInstanceOf(HeaderInterceptor.class);
        assertThat(connectInterceptorList.get(4)).isInstanceOf(CallServerInterceptor.class);


        final List<Interceptor.Fetch> fetchInterceptorList = chain.fetchInterceptorList;
        assertThat(fetchInterceptorList).hasSize(3);

        assertThat(fetchInterceptorList.get(0)).isInstanceOf(RetryInterceptor.class);
        assertThat(fetchInterceptorList.get(1)).isInstanceOf(BreakpointInterceptor.class);
        assertThat(fetchInterceptorList.get(2)).isInstanceOf(FetchDataInterceptor.class);
    }

    @Test
    public void processConnect() throws IOException {
        final Interceptor.Connect connect = mock(Interceptor.Connect.class);
        chain.connectInterceptorList.add(connect);

        chain.connectIndex = 0;
        chain.processConnect();

        assertThat(chain.connectIndex).isEqualTo(1);
        verify(connect).interceptConnect(chain);
    }

    @Test
    public void processFetch() throws IOException {
        final Interceptor.Fetch fetch = mock(Interceptor.Fetch.class);
        chain.fetchInterceptorList.add(fetch);

        chain.fetchIndex = 0;
        chain.processFetch();

        assertThat(chain.fetchIndex).isEqualTo(1);
        verify(fetch).interceptFetch(chain);
    }

    @Test
    public void loopFetch() throws IOException {
        final Interceptor.Fetch fetch1 = mock(Interceptor.Fetch.class);
        chain.fetchInterceptorList.add(fetch1);
        final Interceptor.Fetch fetch2 = mock(Interceptor.Fetch.class);
        chain.fetchInterceptorList.add(fetch2);
        final Interceptor.Fetch fetch3 = mock(Interceptor.Fetch.class);
        chain.fetchInterceptorList.add(fetch3);

        chain.fetchIndex = 0;
        //1
        chain.loopFetch();
        //2
        chain.loopFetch();
        //3
        chain.loopFetch();
        //3
        chain.loopFetch();
        //3
        chain.loopFetch();

        assertThat(chain.fetchIndex).isEqualTo(3);
        verify(fetch1).interceptFetch(chain);
        verify(fetch2).interceptFetch(chain);
        verify(fetch3, times(3)).interceptFetch(chain);
    }

    @Test(expected = IllegalAccessError.class)
    public void run() throws IOException {
        doNothing().when(chain).start();

        chain.run();
        chain.run();
    }
}