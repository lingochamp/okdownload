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

package cn.dreamtobe.okdownload;

import java.io.IOException;

import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.dispatcher.CallbackDispatcher;
import cn.dreamtobe.okdownload.core.dispatcher.DownloadDispatcher;
import cn.dreamtobe.okdownload.core.download.DownloadCall;
import cn.dreamtobe.okdownload.core.download.DownloadChain;
import cn.dreamtobe.okdownload.core.download.DownloadStrategy;
import cn.dreamtobe.okdownload.core.file.ProcessFileStrategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        when(mockOkDownload.downloadStrategy()).thenReturn(mock(DownloadStrategy.class));
        when(mockOkDownload.downloadDispatcher()).thenReturn(mock(DownloadDispatcher.class));

        final CallbackDispatcher callbackDispatcher = mock(CallbackDispatcher.class);
        doReturn(mock(DownloadListener.class)).when(callbackDispatcher).dispatch();
        when(mockOkDownload.callbackDispatcher()).thenReturn(callbackDispatcher);

        final DownloadConnection.Factory connectionFactory = mock(DownloadConnection.Factory.class);
        doReturn(mock(DownloadConnection.class)).when(connectionFactory).create(anyString());
        when(mockOkDownload.connectionFactory()).thenReturn(connectionFactory);

        final ProcessFileStrategy fileStrategy = spy(new ProcessFileStrategy());
        when(mockOkDownload.processFileStrategy()).thenReturn(fileStrategy);
    }

    public static void initProvider() {
        OkDownloadProvider.context = application;
    }

    public static DownloadChain mockDownloadChain() throws IOException {
        final DownloadChain mockChain = mock(DownloadChain.class);
        doReturn(mock(DownloadConnection.Connected.class)).when(mockChain).processConnect();

        final DownloadCall.DownloadCache mockCache = mock(DownloadCall.DownloadCache.class);
        when(mockCache.isInterrupt()).thenReturn(false);
        when(mockChain.getCache()).thenReturn(mockCache);
        when(mockChain.getInfo()).thenReturn(mock(BreakpointInfo.class));

        return mockChain;
    }
}
