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

package com.liulishuo.okdownload.core.file;

import android.content.Context;
import android.net.Uri;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.exception.PreAllocateException;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class) // for sparseArray.
@Config(manifest = NONE)
public class MultiPointOutputStreamTest {

    private MultiPointOutputStream multiPointOutputStream;

    private final String parentPath = "./p-path/";
    private final String path = "./p-path/filename";
    @Mock private BreakpointInfo info;
    @Mock private DownloadTask task;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() {
        when(OkDownload.with().context()).thenReturn(application);
        initMocks(this);
        when(task.getPath()).thenReturn(path);
        when(task.getParentPath()).thenReturn(parentPath);
        multiPointOutputStream = spy(new MultiPointOutputStream(task, info));
    }

    @After
    public void tearDown() {
        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        if (file.getParentFile().exists()) {
            file.getParentFile().delete();
        }
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
        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(1)).thenReturn(blockInfo);

        when(blockInfo.getContentLength()).thenReturn(9L);
        when(blockInfo.getCurrentOffset()).thenReturn(10L);

        multiPointOutputStream.inspectComplete(1);
    }

    @Test
    public void outputStream() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(10L);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.isChunked()).thenReturn(false);

        assertThat(multiPointOutputStream.outputStreamMap.get(0)).isNull();
        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);

        assertThat(outputStream).isNotNull();
        assertThat(multiPointOutputStream.outputStreamMap.get(0)).isEqualTo(outputStream);
        verify(outputStream).seek(eq(10L));
        verify(outputStream).setLength(eq(20L));
    }

    @Test
    public void outputStream_rangeLeft0_noSeek() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(0L);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.isChunked()).thenReturn(false);

        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);
        verify(outputStream, never()).seek(anyLong());
        verify(outputStream).setLength(eq(20L));
    }

    @Test
    public void outputStream_chunked_noPreAllocate() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(0L);
        when(info.isChunked()).thenReturn(true);

        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);
        verify(outputStream, never()).seek(anyLong());
        verify(outputStream, never()).setLength(anyLong());
    }

    private void prepareOutputStreamEnv() throws FileNotFoundException, PreAllocateException {
        when(OkDownload.with().outputStreamFactory().supportSeek()).thenReturn(true);
        when(OkDownload.with().processFileStrategy().isPreAllocateLength()).thenReturn(true);
        when(OkDownload.with().outputStreamFactory().create(any(Context.class), any(Uri.class),
                anyInt())).thenReturn(mock(DownloadOutputStream.class));
        // recreate for new values of support-seek and pre-allocate-length.
        multiPointOutputStream = spy(new MultiPointOutputStream(task, info));
        doNothing().when(multiPointOutputStream).inspectFreeSpace(anyString(), anyLong());

        when(task.getUri()).thenReturn(mock(Uri.class));
    }
}