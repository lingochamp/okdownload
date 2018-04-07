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

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class DownloadListenerBunchTest {

    private DownloadListenerBunch listenerBunch;

    @Mock private DownloadListener listener1;
    @Mock private DownloadListener listener2;

    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;
    @Mock private ResumeFailedCause resumeFailedCause;
    @Mock private Map<String, List<String>> headerFields;
    @Mock private EndCause endCause;
    @Mock private Exception realCause;

    @Before
    public void setup() {
        initMocks(this);

        DownloadListenerBunch.Builder builder = new DownloadListenerBunch.Builder();
        listenerBunch = builder.append(listener1).append(listener2).build();
    }

    @Test
    public void build() {
        DownloadListenerBunch.Builder builder = new DownloadListenerBunch.Builder();
        DownloadListenerBunch listenerBunch = builder.append(listener1).append(listener2).build();
        assertThat(listenerBunch.listenerList).containsExactly(listener1, listener2);
    }

    @Test
    public void taskStart() throws Exception {
        listenerBunch.taskStart(task);

        verify(listener1).taskStart(eq(task));
        verify(listener2).taskStart(eq(task));
    }

    @Test
    public void connectTrialStart() throws Exception {
        listenerBunch.connectTrialStart(task, headerFields);

        verify(listener1).connectTrialStart(eq(task), eq(headerFields));
        verify(listener2).connectTrialStart(eq(task), eq(headerFields));
    }

    @Test
    public void connectTrialEnd() throws Exception {
        listenerBunch.connectTrialEnd(task, 200, headerFields);

        verify(listener1).connectTrialEnd(eq(task), eq(200), eq(headerFields));
        verify(listener2).connectTrialEnd(eq(task), eq(200), eq(headerFields));
    }

    @Test
    public void downloadFromBeginning() throws Exception {
        listenerBunch.downloadFromBeginning(task, info, resumeFailedCause);

        verify(listener1).downloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));
        verify(listener2).downloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));
    }

    @Test
    public void downloadFromBreakpoint() throws Exception {
        listenerBunch.downloadFromBreakpoint(task, info);

        verify(listener1).downloadFromBreakpoint(eq(task), eq(info));
        verify(listener2).downloadFromBreakpoint(eq(task), eq(info));
    }

    @Test
    public void connectStart() throws Exception {
        listenerBunch.connectStart(task, 1, headerFields);

        verify(listener1).connectStart(eq(task), eq(1), eq(headerFields));
        verify(listener2).connectStart(eq(task), eq(1), eq(headerFields));
    }

    @Test
    public void connectEnd() throws Exception {
        listenerBunch.connectEnd(task, 1, 1, headerFields);

        verify(listener1).connectEnd(eq(task), eq(1), eq(1), eq(headerFields));
        verify(listener2).connectEnd(eq(task), eq(1), eq(1), eq(headerFields));
    }

    @Test
    public void fetchStart() throws Exception {
        listenerBunch.fetchStart(task, 1, 1L);

        verify(listener1).fetchStart(eq(task), eq(1), eq(1L));
        verify(listener2).fetchStart(eq(task), eq(1), eq(1L));
    }

    @Test
    public void fetchProgress() throws Exception {
        listenerBunch.fetchProgress(task, 1, 1L);

        verify(listener1).fetchProgress(eq(task), eq(1), eq(1L));
        verify(listener2).fetchProgress(eq(task), eq(1), eq(1L));
    }

    @Test
    public void fetchEnd() throws Exception {
        listenerBunch.fetchEnd(task, 1, 1L);

        verify(listener1).fetchEnd(eq(task), eq(1), eq(1L));
        verify(listener2).fetchEnd(eq(task), eq(1), eq(1L));
    }

    @Test
    public void taskEnd() throws Exception {
        listenerBunch.taskEnd(task, endCause, realCause);

        verify(listener1).taskEnd(eq(task), eq(endCause), eq(realCause));
        verify(listener2).taskEnd(eq(task), eq(endCause), eq(realCause));
    }

    @Test
    public void contain() throws Exception {
        assertThat(listenerBunch.contain(listener1)).isTrue();
        assertThat(listenerBunch.contain(listener2)).isTrue();
        assertThat(listenerBunch.contain(mock(DownloadListener.class))).isFalse();
    }

    @Test
    public void indexOf() {
        assertThat(listenerBunch.indexOf(listener1)).isZero();
        assertThat(listenerBunch.indexOf(listener2)).isOne();
        assertThat(listenerBunch.indexOf(mock(DownloadListener.class))).isEqualTo(-1);
    }
}