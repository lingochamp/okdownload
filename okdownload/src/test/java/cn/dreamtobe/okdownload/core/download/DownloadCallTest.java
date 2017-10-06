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
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collection;

import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.breakpoint.DownloadStrategy;
import cn.dreamtobe.okdownload.task.DownloadTask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    public static void setupClass() {
        OkDownload.setSingletonInstance(new OkDownload.Builder()
                .breakpointStore(mock(BreakpointStore.class))
                .downloadStrategy(mock(DownloadStrategy.class))
                .build());
    }

    @Before
    public void setUp() throws InterruptedException {
        initMocks(this);
        call = spy(DownloadCall.create(mockTask));
        doNothing().when(call).startBlocks(any(Collection.class));
    }


    @Test
    public void start_getBeforeCreate() throws IOException, InterruptedException {
        final BreakpointStore mockStore = OkDownload.with().breakpointStore;

        when(mockStore.get(anyInt())).thenReturn(mockInfo);

        call.start();

        verify(mockStore).get(anyInt());
        verify(mockStore, never()).createAndInsert(null);
        verify(mockStore, never()).createAndInsert(any(DownloadTask.class));
    }

    @Test
    public void start_createIfNon() throws IOException, InterruptedException {
        final BreakpointStore mockStore = OkDownload.with().breakpointStore;

        when(mockStore.get(anyInt())).thenReturn(null);
        when(mockStore.createAndInsert(mockTask)).thenReturn(mockInfo);

        call.start();

        verify(mockStore).createAndInsert(mockTask);
    }

}