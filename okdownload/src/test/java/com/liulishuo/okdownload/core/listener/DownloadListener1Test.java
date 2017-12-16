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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DownloadListener1Test {
    private DownloadListener1 listener1;

    @Mock private DownloadTask task;
    @Mock private ResumeFailedCause resumeFailedCause;

    private BreakpointInfo info;

    private Map<String, List<String>> tmpFields;

    @Before
    public void setup() {
        initMocks(this);

        listener1 = spy(new DownloadListener1() {

            @Override
            protected void connected(DownloadTask task, int blockCount, long currentOffset,
                                     long totalLength) {

            }

            @Override protected void progress(DownloadTask task, long currentOffset) {

            }

            @Override protected void retry(DownloadTask task, @NonNull ResumeFailedCause cause) {

            }

            @Override public void taskStart(DownloadTask task) {
            }

            @Override
            public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
            }
        });

        tmpFields = new HashMap<>();
        info = mockInfo();
    }

    @Test
    public void connected() {
        listener1.downloadFromBeginning(task, info, resumeFailedCause);
        listener1.connectEnd(task, 1, 200, tmpFields);
        verify(listener1, never()).connected(eq(task), eq(2), eq(10L), eq(20L));
        listener1.splitBlockEnd(task, info);
        verify(listener1).connected(eq(task), eq(2), eq(10L), eq(20L));
    }

    @Test
    public void connected_fromResume() {
        listener1.downloadFromBreakpoint(task, info);
        listener1.connectEnd(task, 1, 200, tmpFields);
        verify(listener1).connected(eq(task), eq(2), eq(10L), eq(20L));
    }

    @Test
    public void retry() {
        listener1.downloadFromBeginning(task, info, resumeFailedCause);
        listener1.connectEnd(task, 1, 200, tmpFields);
        listener1.splitBlockEnd(task, info);

        // retry
        listener1.downloadFromBeginning(task, info, resumeFailedCause);
        verify(listener1).retry(task, resumeFailedCause);

        listener1.connectEnd(task, 1, 200, tmpFields);
        listener1.splitBlockEnd(task, info);

        verify(listener1, times(2)).connected(eq(task), eq(2), eq(10L), eq(20L));
    }

    private BreakpointInfo mockInfo() {
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getBlockCount()).thenReturn(2);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.getTotalOffset()).thenReturn(10L);

        return info;
    }
}
