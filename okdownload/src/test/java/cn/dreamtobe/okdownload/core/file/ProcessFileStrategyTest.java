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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;

import static cn.dreamtobe.okdownload.TestUtils.mockOkDownload;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.FILE_NOT_EXIST;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.INFO_DIRTY;
import static cn.dreamtobe.okdownload.core.cause.ResumeFailedCause.OUTPUT_STREAM_NOT_SUPPORT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessFileStrategyTest {
    private ProcessFileStrategy strategy;

    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;
    private final String existPath = "./exist-path/";

    @Before
    public void setup() throws IOException {
        initMocks(this);
        strategy = new ProcessFileStrategy();
        new File(existPath).createNewFile();
    }

    @After
    public void tearDown() {
        new File(existPath).delete();
    }

    @Test
    public void discardProcess() throws IOException {
        when(task.getPath()).thenReturn("mock path");

        strategy.discardProcess(task);
        // nothing need to test.
    }

    @Test
    public void resumeAvailableLocalCheck() throws IOException {
        mockOkDownload();
        ProcessFileStrategy.ResumeAvailableLocalCheck check;
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        final DownloadOutputStream.Factory outputStreamFactory = OkDownload.with()
                .outputStreamFactory();
        final ProcessFileStrategy strategyOnOkDownload = OkDownload.with().processFileStrategy();

        // available.
        final String noExistPath = "./no-exist-path/";
        when(task.getPath()).thenReturn(existPath);
        when(info.getBlockCount()).thenReturn(2);
        when(info.getPath()).thenReturn(existPath);
        when(outputStreamFactory.supportSeek()).thenReturn(true);

        check = strategy.resumeAvailableLocalCheck(task, info);
        assertThat(check.isAvailable()).isTrue();
        check.callbackCause();
        verify(listener).downloadFromBreakpoint(eq(task), eq(info));

        // file not exist
        when(task.getPath()).thenReturn(noExistPath);
        when(info.getPath()).thenReturn(noExistPath);
        check = strategy.resumeAvailableLocalCheck(task, info);
        assertThat(check.isAvailable()).isFalse();
        check.callbackCause();
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(FILE_NOT_EXIST));

        // no filename
        when(task.getPath()).thenReturn(null);
        when(info.getPath()).thenReturn(null);
        check = strategy.resumeAvailableLocalCheck(task, info);
        assertThat(check.isAvailable()).isFalse();
        check.callbackCause();
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(INFO_DIRTY));

        // info not right
        when(task.getPath()).thenReturn(existPath);
        when(info.getPath()).thenReturn(noExistPath);
        check = strategy.resumeAvailableLocalCheck(task, info);
        assertThat(check.isAvailable()).isFalse();
        check.callbackCause();
        verify(listener, times(2)).downloadFromBeginning(eq(task), eq(info), eq(INFO_DIRTY));

        when(info.getPath()).thenReturn(existPath);
        when(info.getBlockCount()).thenReturn(0);
        check = strategy.resumeAvailableLocalCheck(task, info);
        assertThat(check.isAvailable()).isFalse();
        check.callbackCause();
        verify(listener, times(3)).downloadFromBeginning(eq(task), eq(info), eq(INFO_DIRTY));

        // output stream not support
        when(info.getBlockCount()).thenReturn(3);
        when(outputStreamFactory.supportSeek()).thenReturn(false);
        check = strategy.resumeAvailableLocalCheck(task, info);
        assertThat(check.isAvailable()).isFalse();
        check.callbackCause();
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(OUTPUT_STREAM_NOT_SUPPORT));

        when(info.getBlockCount()).thenReturn(1);
        doReturn(false).when(strategyOnOkDownload).isPreAllocateLength();
        check = strategy.resumeAvailableLocalCheck(task, info);
        assertThat(check.isAvailable()).isTrue();
        check.callbackCause();
        verify(listener, times(2)).downloadFromBreakpoint(eq(task), eq(info));

        when(outputStreamFactory.supportSeek()).thenReturn(false);
        doReturn(true).when(strategyOnOkDownload).isPreAllocateLength();
        check = strategy.resumeAvailableLocalCheck(task, info);
        assertThat(check.isAvailable()).isFalse();
        check.callbackCause();
        verify(listener, times(2))
                .downloadFromBeginning(eq(task), eq(info), eq(OUTPUT_STREAM_NOT_SUPPORT));
    }

}