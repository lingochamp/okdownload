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

import android.net.Uri;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.TestUtils;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.breakpoint.DownloadStrategy;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DownloadCallTest {

    private DownloadCall call;

    @Mock
    private DownloadTask mockTask;
    @Mock
    private BreakpointInfo mockInfo;

    @BeforeClass
    public static void setupClass() throws IOException {
        TestUtils.mockOkDownload();
    }

    @Before
    public void setup() throws InterruptedException {
        initMocks(this);
        when(mockTask.getUri()).thenReturn(mock(Uri.class));
        when(mockTask.getListener()).thenReturn(mock(DownloadListener.class));
        call = spy(DownloadCall.create(mockTask, false));
        doNothing().when(call).startBlocks(any(Collection.class));

        final Future mockFuture = mock(Future.class);
        doReturn(mockFuture).when(call).startFirstBlock(any(DownloadChain.class));
        when(mockFuture.isDone()).thenReturn(true);

    }

    @Test
    public void execute_createIfNon() throws IOException, InterruptedException {
        mockLocalCheck(true);

        final BreakpointStore mockStore = OkDownload.with().breakpointStore();

        when(mockStore.get(anyInt())).thenReturn(null);
        when(mockStore.createAndInsert(mockTask)).thenReturn(mockInfo);

        call.execute();

        verify(mockStore).createAndInsert(mockTask);
    }

    @Test
    public void execute_availableResume_startAllBlocks() throws InterruptedException {
        mockLocalCheck(true);

        final BreakpointStore mockStore = OkDownload.with().breakpointStore();
        when(mockStore.get(anyInt())).thenReturn(mockInfo);
        when(mockInfo.getBlockCount()).thenReturn(3);

        call.execute();

        ArgumentCaptor<List<Callable<Object>>> captor = ArgumentCaptor.forClass(List.class);

        verify(call).startBlocks(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    public void execute_notAvailableResume_startFirstAndOthers() throws InterruptedException {
        mockLocalCheck(false);

        final BreakpointStore mockStore = OkDownload.with().breakpointStore();
        when(mockStore.get(anyInt())).thenReturn(mockInfo);
        when(mockInfo.getBlockCount()).thenReturn(3);
        doNothing().when(call).parkForFirstConnection();

        call.execute();

        verify(call).startFirstBlock(any(DownloadChain.class));
        ArgumentCaptor<List<Callable<Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(call).startBlocks(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    public void finished_callToDispatch() {
        call.finished();

        verify(OkDownload.with().downloadDispatcher()).finish(call);
    }

    private void mockLocalCheck(boolean isAvailable) {
        final DownloadStrategy downloadStrategy = OkDownload.with().downloadStrategy();
        DownloadStrategy.ResumeAvailableLocalCheck localCheck = mock(DownloadStrategy.ResumeAvailableLocalCheck.class);
        when(localCheck.isAvailable()).thenReturn(isAvailable);
        when(downloadStrategy.resumeAvailableLocalCheck(any(DownloadTask.class), any(BreakpointInfo.class)))
                .thenReturn(localCheck);
    }

}