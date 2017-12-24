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

package com.liulishuo.okdownload.core.listener.assist;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadListener1AssistTest {

    private DownloadListener1Assist assist;
    @Mock private DownloadListener1Assist.Listener1Callback callback;
    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;
    @Mock private ResumeFailedCause cause;

    private final int taskId = 1;

    @Before
    public void setup() {
        initMocks(this);

        assist = new DownloadListener1Assist();
        assist.setCallback(callback);

        when(task.getId()).thenReturn(taskId);
    }

    @Test
    public void taskStart() {
        assist.taskStart(1);
        assertThat(assist.findModel(1)).isEqualTo(assist.getSingleTaskModel());
        assist.taskStart(2);
        assertThat(assist.findModel(2).id).isEqualTo(2);
        assertThat(assist.findModel(2)).isNotNull();
        assertThat(assist.findModel(2)).isNotEqualTo(assist.getSingleTaskModel());
    }

    @Test
    public void taskEnd() {
        assist.taskStart(1);
        assist.taskStart(2);

        assist.taskEnd(1);
        assertThat(assist.getSingleTaskModel()).isNull();
        assertThat(assist.findModel(2)).isNotNull();

        assist.taskEnd(2);
        assertThat(assist.findModel(2)).isNull();
    }

    @Test
    public void downloadFromBeginning() {
        assist.taskStart(taskId);
        final DownloadListener1Assist.Listener1Model model = assist.getSingleTaskModel();

        assist.downloadFromBeginning(task, cause);

        assertThat(model.isStarted).isTrue();
        assertThat(model.isFromResumed).isFalse();
        assertThat(model.isFirstConnect).isTrue();

        assertThat(model.blockCount).isZero();
        assertThat(model.totalLength).isZero();
        assertThat(model.currentOffset.get()).isZero();

        verify(callback, never()).retry(eq(task), eq(cause));

        assist.downloadFromBeginning(task, cause);
        verify(callback).retry(eq(task), eq(cause));
    }

    @Test
    public void downloadFromBreakpoint() {
        assist.taskStart(taskId);
        final DownloadListener1Assist.Listener1Model model = assist.getSingleTaskModel();

        when(info.getBlockCount()).thenReturn(2);
        when(info.getTotalLength()).thenReturn(3L);

        assist.downloadFromBreakpoint(taskId, info);

        assertThat(model.isStarted).isTrue();
        assertThat(model.isFromResumed).isTrue();
        assertThat(model.isFirstConnect).isTrue();

        assertThat(model.blockCount).isEqualTo(2);
        assertThat(model.totalLength).isEqualTo(3L);
        assertThat(model.currentOffset.get()).isZero();
    }

    @Test
    public void connectEnd() {
        assist.taskStart(taskId);
        final DownloadListener1Assist.Listener1Model model = assist.getSingleTaskModel();

        when(info.getTotalOffset()).thenReturn(2L);
        when(info.getTotalLength()).thenReturn(3L);
        assist.downloadFromBreakpoint(taskId, info);
        assertThat(model.isFirstConnect).isTrue();

        assist.connectEnd(task);
        assertThat(model.isFirstConnect).isFalse();
        verify(callback).connected(eq(task), eq(0), eq(2L), eq(3L));
    }

    @Test
    public void splitBlockEnd() {
        assist.taskStart(taskId);
        final DownloadListener1Assist.Listener1Model model = assist.getSingleTaskModel();

        when(info.getBlockCount()).thenReturn(1);
        when(info.getTotalOffset()).thenReturn(2L);
        when(info.getTotalLength()).thenReturn(3L);

        assist.splitBlockEnd(task, info);

        assertThat(model.blockCount).isEqualTo(1);
        assertThat(model.currentOffset.get()).isEqualTo(2);
        assertThat(model.totalLength).isEqualTo(3);

        verify(callback).connected(eq(task), eq(1), eq(2L), eq(3L));
    }

    @Test
    public void fetchProgress() {
        assist.taskStart(taskId);
        final DownloadListener1Assist.Listener1Model model = assist.getSingleTaskModel();
        assertThat(model.currentOffset.get()).isZero();

        assist.fetchProgress(task, 1);
        assertThat(model.currentOffset.get()).isEqualTo(1);
        assist.fetchProgress(task, 2);
        assertThat(model.currentOffset.get()).isEqualTo(3);

        verify(callback).progress(eq(task), eq(1L));
        verify(callback).progress(eq(task), eq(3L));

    }
}