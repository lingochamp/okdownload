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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock private Listener4Assist assist;
    @Mock private Listener4Assist.AssistExtend assistExtend;
    @Mock private EndCause endCause;
    @Mock private Map<String, List<String>> tmpFields;
    @Mock private Exception exception;

    @Before
    public void setup() {
        initMocks(this);

        listener4 = spy(new DownloadListener4(assist) {
            @Override public void taskStart(@NonNull DownloadTask task) {
            }

            @Override public void connectStart(@NonNull DownloadTask task, int blockIndex,
                                               @NonNull Map<String, List<String>> map) {
            }

            @Override
            public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode,
                                   @NonNull Map<String, List<String>> responseHeaderFields) {
            }

            @Override
            public void infoReady(DownloadTask task, @NonNull BreakpointInfo info,
                                  boolean fromBreakpoint,
                                  @NonNull Listener4Assist.Listener4Model model) {
            }

            @Override
            public void progressBlock(DownloadTask task, int blockIndex, long currentBlockOffset) {
            }

            @Override public void progress(DownloadTask task, long currentOffset) {
            }

            @Override public void blockEnd(DownloadTask task, int blockIndex, BlockInfo info) {
            }

            @Override
            public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                                @NonNull Listener4Assist.Listener4Model model) {
            }
        });
    }

    @Test
    public void setAssistExtend() {
        listener4.setAssistExtend(assistExtend);
        verify(assist).setAssistExtend(assistExtend);
    }

    @Test
    public void empty() {
        listener4.connectTrialStart(task, tmpFields);
        listener4.connectTrialEnd(task, 0, tmpFields);
        listener4.fetchStart(task, 0, 0);
    }

    @Test
    public void downloadFromBeginning() {
        listener4.downloadFromBeginning(task, info, resumeFailedCause);
        verify(assist).infoReady(eq(task), eq(info), eq(false));
    }

    @Test
    public void downloadFromBreakpoint() {
        listener4.downloadFromBreakpoint(task, info);
        verify(assist).infoReady(eq(task), eq(info), eq(true));
    }

    @Test
    public void fetchProgress() {
        listener4.fetchProgress(task, 1, 2);
        verify(assist).fetchProgress(eq(task), eq(1), eq(2L));
    }

    @Test
    public void fetchEnd() {
        listener4.fetchEnd(task, 1, 2);
        verify(assist).fetchEnd(eq(task), eq(1));
    }

    @Test
    public void taskEnd() {
        listener4.taskEnd(task, endCause, exception);
        verify(assist).taskEnd(eq(task), eq(endCause), eq(exception));
    }

    @Test
    public void isAlwaysRecoverAssistModel() {
        when(assist.isAlwaysRecoverAssistModel()).thenReturn(true);
        assertThat(listener4.isAlwaysRecoverAssistModel()).isTrue();
        when(assist.isAlwaysRecoverAssistModel()).thenReturn(false);
        assertThat(listener4.isAlwaysRecoverAssistModel()).isFalse();
    }

    @Test
    public void setAlwaysRecoverAssistModel() {
        listener4.setAlwaysRecoverAssistModel(true);
        verify(assist).setAlwaysRecoverAssistModel(eq(true));
        listener4.setAlwaysRecoverAssistModel(false);
        verify(assist).setAlwaysRecoverAssistModel(eq(false));
    }

    @Test
    public void setAlwaysRecoverAssistModelIfNotSet() {
        listener4.setAlwaysRecoverAssistModelIfNotSet(true);
        verify(assist).setAlwaysRecoverAssistModelIfNotSet(eq(true));
        listener4.setAlwaysRecoverAssistModelIfNotSet(false);
        verify(assist).setAlwaysRecoverAssistModelIfNotSet(eq(false));
    }
}