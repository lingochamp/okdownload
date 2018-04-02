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

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.FILE_NOT_EXIST;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.INFO_DIRTY;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.OUTPUT_STREAM_NOT_SUPPORT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessFileStrategyTest {
    private ProcessFileStrategy strategy;

    private ProcessFileStrategy.ResumeAvailableLocalCheck localCheck;

    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;
    private final File existFile = new File("./exist-path");

    @Before
    public void setup() throws IOException {
        initMocks(this);
        strategy = new ProcessFileStrategy();
        localCheck = spy(strategy.resumeAvailableLocalCheck(task, info));

        existFile.createNewFile();
    }

    @After
    public void tearDown() {
        existFile.delete();
    }

    @Test
    public void discardProcess() throws IOException {
        when(task.getFile()).thenReturn(new File("mock path"));

        strategy.discardProcess(task);
        // nothing need to test.
    }

    @Test
    public void resumeAvailableLocalCheck() throws IOException {
        mockOkDownload();
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();

        localCheck.isAvailable = false;
        localCheck.fileExist = true;
        localCheck.infoRight = true;

        // output stream not support
        localCheck.outputStreamSupport = false;
        localCheck.callbackCause();
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(OUTPUT_STREAM_NOT_SUPPORT));

        // file not exist
        localCheck.fileExist = false;
        localCheck.callbackCause();
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(FILE_NOT_EXIST));

        // info not right
        localCheck.infoRight = false;
        localCheck.callbackCause();
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(INFO_DIRTY));

        // available.
        localCheck.isAvailable = true;
        localCheck.callbackCause();
        verify(listener).downloadFromBreakpoint(eq(task), eq(info));
    }

    @Test
    public void isInfoRightToResume() {
        when(task.getFile()).thenReturn(existFile);
        when(info.getFile()).thenReturn(existFile);
        when(info.getBlockCount()).thenReturn(1);
        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getContentLength()).thenReturn(1L);
        assertThat(localCheck.isInfoRightToResume()).isTrue();

        // there is a block with the value of content-length is 0.
        when(blockInfo.getContentLength()).thenReturn(0L);
        assertThat(localCheck.isInfoRightToResume()).isFalse();
        when(blockInfo.getContentLength()).thenReturn(1L);

        // the filename in task is not equal to the filename in info.
        when(task.getFile()).thenReturn(null);
        assertThat(localCheck.isInfoRightToResume()).isFalse();
        when(task.getFile()).thenReturn(existFile);

        // the file path in info is null.
        when(info.getFile()).thenReturn(null);
        assertThat(localCheck.isInfoRightToResume()).isFalse();
        when(info.getFile()).thenReturn(existFile);

        // is chunked
        when(info.isChunked()).thenReturn(true);
        assertThat(localCheck.isInfoRightToResume()).isFalse();
        when(info.isChunked()).thenReturn(false);

        // there isn't any block in info.
        when(info.getBlockCount()).thenReturn(0);
        assertThat(localCheck.isInfoRightToResume()).isFalse();
    }

    @Test
    public void isOutputStreamSupportResume() throws IOException {
        mockOkDownload();

        final DownloadOutputStream.Factory outputStreamFactory = OkDownload.with()
                .outputStreamFactory();
        final ProcessFileStrategy strategyOnOkDownload = OkDownload.with().processFileStrategy();

        // support seek
        when(outputStreamFactory.supportSeek()).thenReturn(true);
        assertThat(localCheck.isOutputStreamSupportResume()).isTrue();

        // not support seek, but only 1 block and there isn't pre-allocation length.
        when(outputStreamFactory.supportSeek()).thenReturn(false);
        when(info.getBlockCount()).thenReturn(1);
        when(strategyOnOkDownload.isPreAllocateLength()).thenReturn(false);
        assertThat(localCheck.isOutputStreamSupportResume()).isTrue();

        // not support seek, only 1 block, but there is pre-allocation length, so we can't append
        // with it
        when(info.getBlockCount()).thenReturn(1);
        doReturn(true).when(strategyOnOkDownload).isPreAllocateLength();
        doReturn(false).when(outputStreamFactory).supportSeek();
        assertThat(localCheck.isOutputStreamSupportResume()).isFalse();


        // not support seek, there is pre-allocation length, but there is more than on block count.
        doReturn(false).when(strategyOnOkDownload).isPreAllocateLength();
        doReturn(false).when(outputStreamFactory).supportSeek();
        when(info.getBlockCount()).thenReturn(2);
        assertThat(localCheck.isOutputStreamSupportResume()).isFalse();
    }

    @Test
    public void isFileExistToResume() {
        when(task.getFile()).thenReturn(existFile);
        assertThat(localCheck.isFileExistToResume()).isTrue();

        when(task.getFile()).thenReturn(null);
        assertThat(localCheck.isFileExistToResume()).isFalse();

        when(task.getFile()).thenReturn(new File("no-exist"));
        assertThat(localCheck.isFileExistToResume()).isFalse();
    }

    @Test
    public void isAvailable_allTrue() {
        doReturn(true).when(localCheck).isFileExistToResume();
        doReturn(true).when(localCheck).isInfoRightToResume();
        doReturn(true).when(localCheck).isOutputStreamSupportResume();

        assertThat(localCheck.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_fileNotExist() {
        doReturn(false).when(localCheck).isFileExistToResume();
        doReturn(true).when(localCheck).isInfoRightToResume();
        doReturn(true).when(localCheck).isOutputStreamSupportResume();

        assertThat(localCheck.isAvailable()).isFalse();
    }


    @Test
    public void isAvailable_infoNotRight() {
        doReturn(true).when(localCheck).isFileExistToResume();
        doReturn(false).when(localCheck).isInfoRightToResume();
        doReturn(true).when(localCheck).isOutputStreamSupportResume();

        assertThat(localCheck.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_outputStreamNotSupport() {
        doReturn(true).when(localCheck).isFileExistToResume();
        doReturn(true).when(localCheck).isInfoRightToResume();
        doReturn(false).when(localCheck).isOutputStreamSupportResume();

        assertThat(localCheck.isAvailable()).isFalse();
    }
}