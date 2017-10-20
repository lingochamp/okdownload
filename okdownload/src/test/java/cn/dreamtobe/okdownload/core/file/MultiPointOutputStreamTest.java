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

package cn.dreamtobe.okdownload.core.file;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;

import static cn.dreamtobe.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class) // for sparseArray.
@Config(manifest = NONE)
public class MultiPointOutputStreamTest {

    private MultiPointOutputStream multiPointOutputStream;

    @Mock private BreakpointInfo info;
    @Mock private DownloadTask task;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() {
        initMocks(this);
        multiPointOutputStream = spy(new MultiPointOutputStream(task, info));
    }

    @Test
    public void write() throws IOException {
        final DownloadOutputStream outputStream = mock(DownloadOutputStream.class);
        doReturn(outputStream).when(multiPointOutputStream).outputStream(anyInt());
        multiPointOutputStream.syncRunning = true;

        final byte[] bytes = new byte[6];
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong());
        multiPointOutputStream.write(1, bytes, 6);

        verify(multiPointOutputStream).write(1, bytes, 6);

        multiPointOutputStream.noSyncLengthMap.put(2, new AtomicLong());
        multiPointOutputStream.write(2, bytes, 16);
        verify(multiPointOutputStream).write(2, bytes, 16);

        assertThat(multiPointOutputStream.allNoSyncLength.get()).isEqualTo(22);
        assertThat(multiPointOutputStream.noSyncLengthMap.get(1).get()).isEqualTo(6);
        assertThat(multiPointOutputStream.noSyncLengthMap.get(2).get()).isEqualTo(16);
    }

    @Test
    public void ensureSyncComplete() throws IOException {
        final DownloadOutputStream outputStream = mock(DownloadOutputStream.class);
        doReturn(outputStream).when(multiPointOutputStream).outputStream(1);
        when(info.getBlock(1)).thenReturn(mock(BlockInfo.class));
        multiPointOutputStream.syncRunning = false;

        final BreakpointStore store = OkDownload.with().breakpointStore();
        multiPointOutputStream.allNoSyncLength.addAndGet(10);
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong(10));
        multiPointOutputStream.outputStreamMap.put(1, mock(DownloadOutputStream.class));

        multiPointOutputStream.ensureSyncComplete(1);

        verify(store).onSyncToFilesystemSuccess(info, 1, 10);
        assertThat(multiPointOutputStream.allNoSyncLength.get()).isZero();
        assertThat(multiPointOutputStream.noSyncLengthMap.get(1).get()).isZero();
    }

    @Test(expected = IOException.class)
    public void inspectComplete_notFull() throws IOException {
        final DownloadOutputStream outputStream = mock(DownloadOutputStream.class);
        doReturn(outputStream).when(multiPointOutputStream).outputStream(1);
        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(1)).thenReturn(blockInfo);
        multiPointOutputStream.syncRunning = false;
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong());

        when(blockInfo.getContentLength()).thenReturn(10L);
        when(blockInfo.getCurrentOffset()).thenReturn(10L);
        when(info.isLastBlock(1)).thenReturn(false);

        multiPointOutputStream.inspectComplete(1);
    }
}