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

package com.liulishuo.okdownload;

import android.content.Context;
import android.net.Uri;

import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.dispatcher.CallbackDispatcher;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;
import com.liulishuo.okdownload.core.download.DownloadCache;
import com.liulishuo.okdownload.core.download.DownloadChain;
import com.liulishuo.okdownload.core.download.DownloadStrategy;
import com.liulishuo.okdownload.core.file.DownloadOutputStream;
import com.liulishuo.okdownload.core.file.ProcessFileStrategy;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

public class TestUtils {
    public static void mockOkDownload() throws IOException {
        final OkDownload mockOkDownload = mock(OkDownload.class);
        OkDownload.singleton = mockOkDownload;

        final BreakpointStore store = mock(BreakpointStore.class);
        when(store.update(any(BreakpointInfo.class))).thenReturn(true);
        when(mockOkDownload.breakpointStore()).thenReturn(store);

        final DownloadStrategy strategy = mock(DownloadStrategy.class);
        when(mockOkDownload.downloadStrategy()).thenReturn(strategy);
        when(strategy.resumeAvailableResponseCheck(any(DownloadConnection.Connected.class),
                anyInt(), any(BreakpointInfo.class)))
                .thenReturn(mock(DownloadStrategy.ResumeAvailableResponseCheck.class));

        when(mockOkDownload.downloadDispatcher()).thenReturn(mock(DownloadDispatcher.class));

        final CallbackDispatcher callbackDispatcher = mock(CallbackDispatcher.class);
        doReturn(mock(DownloadListener.class)).when(callbackDispatcher).dispatch();
        when(mockOkDownload.callbackDispatcher()).thenReturn(callbackDispatcher);

        final DownloadConnection.Factory connectionFactory = mock(DownloadConnection.Factory.class);
        doReturn(mock(DownloadConnection.class)).when(connectionFactory).create(anyString());
        when(mockOkDownload.connectionFactory()).thenReturn(connectionFactory);

        final ProcessFileStrategy fileStrategy = spy(new ProcessFileStrategy());
        when(mockOkDownload.processFileStrategy()).thenReturn(fileStrategy);
        doNothing().when(fileStrategy).discardProcess(any(DownloadTask.class));

        final DownloadOutputStream.Factory outputStreamFactory = mock(
                DownloadOutputStream.Factory.class);
        when(mockOkDownload.outputStreamFactory()).thenReturn(outputStreamFactory);
        doReturn(mock(DownloadOutputStream.class)).when(outputStreamFactory).create(
                any(Context.class), any(Uri.class), anyInt());
    }

    public static void initProvider() {
        OkDownloadProvider.context = application;
    }

    public static DownloadChain mockDownloadChain() throws IOException {
        final DownloadChain mockChain = mock(DownloadChain.class);
        doReturn(mock(DownloadConnection.Connected.class)).when(mockChain).processConnect();

        final DownloadCache mockCache = mock(DownloadCache.class);
        when(mockCache.isInterrupt()).thenReturn(false);
        when(mockChain.getCache()).thenReturn(mockCache);
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getBlock(0)).thenReturn(mock(BlockInfo.class));
        when(mockChain.getInfo()).thenReturn(info);
        when(mockChain.getTask()).thenReturn(mock(DownloadTask.class));
        when(mockChain.getConnectionOrCreate()).thenReturn(mock(DownloadConnection.class));

        return mockChain;
    }

    public static AssertFile assertFile(File actual) {
        return new AssertFile(actual);
    }

    public static class AssertFile {
        final File actual;

        public AssertFile(File actual) {
            this.actual = actual;
        }

        public void isEqualTo(File expected) {
            assertThat(actual.getAbsolutePath()).isEqualTo(expected.getAbsolutePath());
        }
    }
}
