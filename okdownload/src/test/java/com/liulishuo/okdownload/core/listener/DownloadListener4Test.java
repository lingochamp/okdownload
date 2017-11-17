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

package com.liulishuo.okdownload.core.listener;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import static org.assertj.core.api.Java6Assertions.assertThat;
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
        listener4 = spy(new DownloadListener4() {
            @Override protected void infoReady(DownloadTask task, @NonNull BreakpointInfo info) {
            }

            @Override
            protected void progressBlock(DownloadTask task, int blockIndex,
                                         long currentBlockOffset) {
            }

            @Override protected void progress(DownloadTask task, long currentOffset) {
            }

            @Override protected void blockEnd(DownloadTask task, int blockIndex, BlockInfo info) {
            }

            @Override public void taskStart(DownloadTask task) {
            }

            @Override public void connectStart(DownloadTask task, int blockIndex,
                                               @NonNull Map<String, List<String>>
                                                       requestHeaderFields) {
            }

            @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                             @NonNull Map<String, List<String>>
                                                     responseHeaderFields) {
            }

            @Override
            public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
            }
        });
    }

    @Test
    public void callback() {
        listener4.taskStart(task);
        listener4.breakpointData(task, null);
        listener4.downloadFromBeginning(task, info, resumeFailedCause);
        listener4.connectStart(task, 0, tmpFields);
        listener4.connectEnd(task, 0, 206, tmpFields);
        when(info.getBlockCount()).thenReturn(3);
        when(info.getTotalOffset()).thenReturn(15L);
        for (int i = 0; i < 3; i++) {
            final BlockInfo blockInfo = mock(BlockInfo.class);
            when(blockInfo.getCurrentOffset()).thenReturn(i + 5L);
            doReturn(blockInfo).when(info).getBlock(i);
        }
        listener4.splitBlockEnd(task, info);
        verify(listener4).infoReady(eq(task), eq(info));
        assertThat(listener4.blockCurrentOffsetMap().size()).isEqualTo(3);
        assertThat(listener4.blockCurrentOffsetMap().get(1)).isEqualTo(6L);
        assertThat(listener4.currentOffset).isEqualTo(15L);

        listener4.fetchStart(task, 0, 30L);
        listener4.connectStart(task, 1, tmpFields);
        listener4.connectEnd(task, 1, 206, tmpFields);

        listener4.fetchProgress(task, 0, 10);
        assertThat(listener4.blockCurrentOffsetMap().get(0)).isEqualTo(15L);
        assertThat(listener4.currentOffset).isEqualTo(25L);
        verify(listener4).progressBlock(eq(task), eq(0), eq(15L));
        verify(listener4).progress(eq(task), eq(25L));

        listener4.fetchEnd(task, 0, 30L);
        final BlockInfo firstBlock = info.getBlock(0);
        verify(listener4).blockEnd(eq(task), eq(0), eq(firstBlock));

        listener4.taskEnd(task, EndCause.COMPLETE, null);
    }
}