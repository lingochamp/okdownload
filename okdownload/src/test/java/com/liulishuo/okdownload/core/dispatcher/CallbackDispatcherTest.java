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

package com.liulishuo.okdownload.core.dispatcher;

import android.os.Handler;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.TestUtils;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class CallbackDispatcherTest {

    private CallbackDispatcher dispatcher;

    @Mock private Handler handler;
    @Mock private DownloadListener transmit;

    @Before
    public void setup() {
        initMocks(this);

        TestUtils.initProvider();
        dispatcher = new CallbackDispatcher(handler, transmit);
    }

    @Test
    public void dispatch() {
        dispatcher = new CallbackDispatcher(handler);

        final DownloadTask task = mock(DownloadTask.class);
        final DownloadListener listener = mock(DownloadListener.class);
        when(task.getListener()).thenReturn(listener);
        final BreakpointInfo info = mock(BreakpointInfo.class);
        final Map<String, List<String>> headerFields = mock(Map.class);
        final ResumeFailedCause resumeFailedCause = mock(ResumeFailedCause.class);
        final EndCause endCause = mock(EndCause.class);
        final Exception exception = mock(Exception.class);

        dispatcher.dispatch().taskStart(task);
        verify(listener).taskStart(eq(task));

        dispatcher.dispatch().connectTrialStart(task, headerFields);
        verify(listener).connectTrialStart(eq(task), eq(headerFields));

        dispatcher.dispatch().connectTrialEnd(task, 200, headerFields);
        verify(listener).connectTrialEnd(eq(task), eq(200), eq(headerFields));

        dispatcher.dispatch().downloadFromBeginning(task, info, resumeFailedCause);
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));

        dispatcher.dispatch().downloadFromBreakpoint(task, info);
        verify(listener).downloadFromBreakpoint(eq(task), eq(info));

        dispatcher.dispatch().connectStart(task, 1, headerFields);
        verify(listener).connectStart(eq(task), eq(1), eq(headerFields));

        dispatcher.dispatch().connectEnd(task, 2, 200, headerFields);
        verify(listener).connectEnd(eq(task), eq(2), eq(200), eq(headerFields));

        dispatcher.dispatch().fetchStart(task, 1, 2L);
        verify(listener).fetchStart(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().fetchProgress(task, 1, 2L);
        verify(listener).fetchProgress(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().fetchEnd(task, 1, 2L);
        verify(listener).fetchEnd(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().taskEnd(task, endCause, exception);
        verify(listener).taskEnd(eq(task), eq(endCause), eq(exception));
    }

    @Test
    public void endTasks() {
        final Collection<DownloadTask> completedTaskCollection = new ArrayList<>();
        final Collection<DownloadTask> sameTaskConflictCollection = new ArrayList<>();
        final Collection<DownloadTask> fileBusyCollection = new ArrayList<>();

        dispatcher
                .endTasks(completedTaskCollection, sameTaskConflictCollection, fileBusyCollection);
        verify(handler, never()).post(any(Runnable.class));

        final DownloadTask autoUiTask = mock(DownloadTask.class);
        final DownloadTask nonUiTask = mock(DownloadTask.class);

        final DownloadListener nonUiListener = mock(DownloadListener.class);
        final DownloadListener autoUiListener = mock(DownloadListener.class);

        when(autoUiTask.getListener()).thenReturn(autoUiListener);
        when(autoUiTask.isAutoCallbackToUIThread()).thenReturn(true);

        when(nonUiTask.getListener()).thenReturn(nonUiListener);
        when(nonUiTask.isAutoCallbackToUIThread()).thenReturn(false);

        completedTaskCollection.add(autoUiTask);
        completedTaskCollection.add(nonUiTask);

        sameTaskConflictCollection.add(autoUiTask);
        sameTaskConflictCollection.add(nonUiTask);

        fileBusyCollection.add(autoUiTask);
        fileBusyCollection.add(nonUiTask);

        dispatcher
                .endTasks(completedTaskCollection, sameTaskConflictCollection, fileBusyCollection);

        verify(nonUiListener)
                .taskEnd(eq(nonUiTask), eq(EndCause.COMPLETED), nullable(Exception.class));
        verify(nonUiListener)
                .taskEnd(eq(nonUiTask), eq(EndCause.SAME_TASK_BUSY), nullable(Exception.class));
        verify(nonUiListener)
                .taskEnd(eq(nonUiTask), eq(EndCause.FILE_BUSY), nullable(Exception.class));

        verify(handler).post(any(Runnable.class));
    }

    @Test
    public void endTasksWithError() {
        final Collection<DownloadTask> errorCollection = new ArrayList<>();
        final Exception realCause = mock(Exception.class);

        final DownloadTask autoUiTask = mock(DownloadTask.class);
        final DownloadTask nonUiTask = mock(DownloadTask.class);

        final DownloadListener nonUiListener = mock(DownloadListener.class);
        final DownloadListener autoUiListener = mock(DownloadListener.class);

        when(autoUiTask.getListener()).thenReturn(autoUiListener);
        when(autoUiTask.isAutoCallbackToUIThread()).thenReturn(true);

        when(nonUiTask.getListener()).thenReturn(nonUiListener);
        when(nonUiTask.isAutoCallbackToUIThread()).thenReturn(false);

        errorCollection.add(autoUiTask);
        errorCollection.add(nonUiTask);

        dispatcher.endTasksWithError(errorCollection, realCause);

        verify(nonUiListener).taskEnd(eq(nonUiTask), eq(EndCause.ERROR), eq(realCause));

        verify(handler).post(any(Runnable.class));
    }
}