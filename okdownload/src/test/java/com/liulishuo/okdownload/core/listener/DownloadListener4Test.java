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

package com.liulishuo.okdownload.core.listener;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.assist.Listener4Assist;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadListener4Test {
    private DownloadListener4 listener4;
    @Mock private BreakpointInfo info;
    @Mock private DownloadTask task;
    @Mock private ResumeFailedCause resumeFailedCause;

    private Map<String, List<String>> tmpFields;


    @Before
    public void setup() {
        initMocks(this);

        tmpFields = new HashMap<>();
        listener4 = spy(new DownloadListener4(spy(new Listener4Assist())) {
            @Override
            public void infoReady(DownloadTask task, @NonNull BreakpointInfo info,
                                  boolean fromBreakpoint,
                                  @NonNull Listener4Assist.Listener4Model model) { }

            @Override
            public void progressBlock(DownloadTask task, int blockIndex, long currentBlockOffset) {
            }

            @Override public void progress(DownloadTask task, long currentOffset) { }

            @Override public void blockEnd(DownloadTask task, int blockIndex, BlockInfo info) { }

            @Override
            public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                                @NonNull Listener4Assist.Listener4Model model) { }

            @Override public void taskStart(@NonNull DownloadTask task) { }

            @Override public void connectStart(@NonNull DownloadTask task, int blockIndex,
                                               @NonNull Map<String, List<String>> requestHeader) { }

            @Override
            public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode,
                                   @NonNull Map<String, List<String>> responseHeader) { }
        });
    }

    @Test
    public void callback() {
        final DownloadTask anotherTask = mock(DownloadTask.class);
        when(anotherTask.getId()).thenReturn(1);
        final BreakpointInfo anotherInfo = mock(BreakpointInfo.class);
        when(anotherInfo.getId()).thenReturn(1);
        final BlockInfo anotherBlockInfo = mock(BlockInfo.class);
        when(anotherInfo.getBlockCount()).thenReturn(1);
        when(anotherInfo.getTotalOffset()).thenReturn(1L);
        when(anotherBlockInfo.getCurrentOffset()).thenReturn(1L);
        when(anotherBlockInfo.getContentLength()).thenReturn(5L);
        when(anotherInfo.getBlock(0)).thenReturn(anotherBlockInfo);


        listener4.taskStart(task);
        listener4.downloadFromBeginning(task, info, resumeFailedCause);
        verify(listener4.assist).infoReady(eq(task), eq(info), eq(false));

        listener4.connectStart(task, 0, tmpFields);
        listener4.connectEnd(task, 0, 206, tmpFields);

        // another task coming.
        when(info.getBlockCount()).thenReturn(3);
        for (int i = 0; i < 3; i++) {
            final BlockInfo blockInfo = mock(BlockInfo.class);
            doReturn(blockInfo).when(info).getBlock(i);
        }
        listener4.taskStart(anotherTask);
        listener4.downloadFromBreakpoint(anotherTask, anotherInfo);

        listener4.fetchStart(task, 0, 30L);
        listener4.downloadFromBreakpoint(task, info);
        listener4.connectStart(task, 1, tmpFields);
        listener4.connectEnd(task, 1, 206, tmpFields);

        // another task running.
        listener4.connectStart(anotherTask, 0, tmpFields);
        listener4.connectEnd(anotherTask, 0, 206, tmpFields);
        listener4.fetchProgress(anotherTask, 0, 2);
        listener4.fetchProgress(task, 1, 10);
        verify(listener4.assist).fetchProgress(eq(task), eq(1), eq(10L));

        listener4.fetchProgress(task, 0, 15);
        verify(listener4.assist).fetchProgress(eq(task), eq(0), eq(15L));

        listener4.fetchEnd(task, 0, 30L);
        verify(listener4.assist).fetchEnd(eq(task), eq(0));
    }
}