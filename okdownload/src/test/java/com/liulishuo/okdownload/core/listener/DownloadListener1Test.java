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
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadListener1Test {
    private DownloadListener1 listener1;

    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;

    private Map<String, List<String>> tmpFields;

    @Before
    public void setup() {
        initMocks(this);

        listener1 = spy(new DownloadListener1(mock(Listener1Assist.class)) {
            @Override
            public void taskStart(@NonNull DownloadTask task,
                                  @NonNull Listener1Assist.Listener1Model model) { }

            @Override
            public void retry(@NonNull DownloadTask task, @NonNull ResumeFailedCause cause) { }

            @Override
            public void connected(@NonNull DownloadTask task, int blockCount, long currentOffset,
                                  long totalLength) { }

            @Override
            public void progress(@NonNull DownloadTask task, long currentOffset,
                                 long totalLength) { }

            @Override
            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                                @Nullable Exception realCause,
                                @NonNull Listener1Assist.Listener1Model model) { }
        });

        tmpFields = new HashMap<>();

        when(task.getId()).thenReturn(1);
    }

    @Test
    public void taskStart() {
        listener1.taskStart(task);
        verify(listener1.assist).taskStart(eq(task));
    }

    @Test
    public void downloadFromBeginning() {
        final ResumeFailedCause cause = mock(ResumeFailedCause.class);
        listener1.downloadFromBeginning(task, info, cause);
        verify(listener1.assist).downloadFromBeginning(eq(task), eq(cause));
    }

    @Test
    public void downloadFromBreakpoint() {
        listener1.downloadFromBreakpoint(task, info);
        verify(listener1.assist).downloadFromBreakpoint(eq(task.getId()), eq(info));
    }

    @Test
    public void connectEnd() {
        listener1.connectEnd(task, 1, 200, tmpFields);
        verify(listener1.assist).connectEnd(eq(task));
    }

    @Test
    public void splitBlockEnd() {
        listener1.splitBlockEnd(task, info);
        verify(listener1.assist).splitBlockEnd(eq(task), eq(info));
    }

    @Test
    public void fetchProgress() {
        listener1.fetchProgress(task, 1, 2);
        verify(listener1.assist).fetchProgress(eq(task), eq(2L));
    }
}
