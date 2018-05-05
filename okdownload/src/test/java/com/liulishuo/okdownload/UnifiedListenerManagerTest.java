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

package com.liulishuo.okdownload;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener1;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class UnifiedListenerManagerTest {
    private UnifiedListenerManager listenerManager;
    @Mock private DownloadListener listener;

    @Before
    public void setup() {
        initMocks(this);
        listenerManager = spy(new UnifiedListenerManager());
    }

    @Test
    public void detachListener() {
        final ArrayList<DownloadListener> list = new ArrayList<>();
        list.add(listener);
        listenerManager.realListenerMap.put(1, list);

        final DownloadTask task = mockTask(1);
        listenerManager.detachListener(task, listener);

        assertThat(listenerManager.realListenerMap.size()).isZero();

        // detach by listener ignore host task.
        list.add(listener);
        listenerManager.realListenerMap.put(2, list);
        assertThat(listenerManager.realListenerMap.size()).isEqualTo(1);

        listenerManager.detachListener(listener);
        assertThat(listenerManager.realListenerMap.size()).isZero();
    }

    @Test
    public void attachListener() {
        final DownloadTask task = mockTask(2);
        listenerManager.attachListener(task, listener);

        assertThat(listenerManager.realListenerMap.size()).isEqualTo(1);
        assertThat(listenerManager.realListenerMap.get(2)).containsExactly(listener);

        final DownloadListener1 listener1 = mock(DownloadListener1.class);
        listenerManager.attachListener(task, listener1);
        verify(listener1).setAlwaysRecoverAssistModelIfNotSet(eq(true));
    }

    @Test
    public void attachAndEnqueueIfNotRun() {
        final DownloadTask task = mockTask(1);
        doNothing().when(listenerManager).attachListener(eq(task), eq(listener));
        doReturn(true).when(listenerManager).isTaskPendingOrRunning(eq(task));

        listenerManager.attachAndEnqueueIfNotRun(task, listener);
        assertThat(listenerManager.realListenerMap.size()).isZero();
        verify(task, never()).enqueue(eq(listenerManager.hostListener));
        verify(listenerManager).attachListener(eq(task), eq(listener));

        doReturn(false).when(listenerManager).isTaskPendingOrRunning(eq(task));
        listenerManager.attachAndEnqueueIfNotRun(task, listener);
        verify(task).enqueue(eq(listenerManager.hostListener));
        verify(listenerManager, times(2)).attachListener(eq(task), eq(listener));
    }

    @Test
    public void executeTaskWithUnifiedListener() {
        final DownloadTask task = mockTask(1);
        doNothing().when(listenerManager).attachListener(eq(task), eq(listener));
        doNothing().when(task).execute(eq(listenerManager.hostListener));

        listenerManager.executeTaskWithUnifiedListener(task, listener);

        verify(listenerManager).attachListener(eq(task), eq(listener));
        verify(task).execute(eq(listenerManager.hostListener));
    }

    @Test
    public void hostListener() {
        final DownloadListener listener1 = mock(DownloadListener.class);
        final DownloadListener listener2 = mock(DownloadListener.class);
        final ArrayList<DownloadListener> list = new ArrayList<>();
        list.add(listener1);
        list.add(listener2);
        listenerManager.realListenerMap.put(1, list);

        final Map<String, List<String>> headerFields = mock(Map.class);
        final BreakpointInfo info = mock(BreakpointInfo.class);
        final ResumeFailedCause resumeFailedCause = mock(ResumeFailedCause.class);
        final EndCause endCause = mock(EndCause.class);
        final Exception exception = mock(Exception.class);

        final DownloadTask task = mockTask(1);
        final DownloadTask noAttachTask = mockTask(2);
        final DownloadListener listener = listenerManager.hostListener;

        listener.taskStart(task);
        listener.taskStart(noAttachTask);
        verify(listener1).taskStart(eq(task));
        verify(listener2).taskStart(eq(task));

        listener.connectTrialStart(task, headerFields);
        listener.connectTrialStart(noAttachTask, headerFields);
        verify(listener1).connectTrialStart(eq(task), eq(headerFields));
        verify(listener2).connectTrialStart(eq(task), eq(headerFields));


        listener.connectTrialEnd(task, 200, headerFields);
        listener.connectTrialEnd(noAttachTask, 200, headerFields);
        verify(listener1).connectTrialEnd(eq(task), eq(200), eq(headerFields));
        verify(listener2).connectTrialEnd(eq(task), eq(200), eq(headerFields));

        listener.downloadFromBeginning(task, info, resumeFailedCause);
        listener.downloadFromBeginning(noAttachTask, info, resumeFailedCause);
        verify(listener1).downloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));
        verify(listener2).downloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));

        listener.downloadFromBreakpoint(task, info);
        listener.downloadFromBreakpoint(noAttachTask, info);
        verify(listener1).downloadFromBreakpoint(eq(task), eq(info));
        verify(listener2).downloadFromBreakpoint(eq(task), eq(info));

        listener.connectStart(task, 1, headerFields);
        listener.connectStart(noAttachTask, 1, headerFields);
        verify(listener1).connectStart(eq(task), eq(1), eq(headerFields));
        verify(listener2).connectStart(eq(task), eq(1), eq(headerFields));

        listener.connectEnd(task, 1, 200, headerFields);
        listener.connectEnd(noAttachTask, 1, 200, headerFields);
        verify(listener1).connectEnd(eq(task), eq(1), eq(200), eq(headerFields));
        verify(listener2).connectEnd(eq(task), eq(1), eq(200), eq(headerFields));

        listener.fetchStart(task, 1, 2L);
        listener.fetchStart(noAttachTask, 1, 2L);
        verify(listener1).fetchStart(eq(task), eq(1), eq(2L));
        verify(listener2).fetchStart(eq(task), eq(1), eq(2L));

        listener.fetchProgress(task, 1, 2L);
        listener.fetchProgress(noAttachTask, 1, 2L);
        verify(listener1).fetchProgress(eq(task), eq(1), eq(2L));
        verify(listener2).fetchProgress(eq(task), eq(1), eq(2L));

        listener.fetchEnd(task, 1, 2L);
        listener.fetchEnd(noAttachTask, 1, 2L);
        verify(listener1).fetchEnd(eq(task), eq(1), eq(2L));
        verify(listener2).fetchEnd(eq(task), eq(1), eq(2L));

        listener.taskEnd(task, endCause, exception);
        listener.taskEnd(noAttachTask, endCause, exception);
        verify(listener1).taskEnd(eq(task), eq(endCause), eq(exception));
        verify(listener2).taskEnd(eq(task), eq(endCause), eq(exception));
    }

    @Test
    public void taskEnd_detachListener() {
        final DownloadListener listener1 = mock(DownloadListener.class);
        final ArrayList<DownloadListener> list = new ArrayList<>();
        list.add(listener1);
        listenerManager.realListenerMap.put(1, list);

        final DownloadTask task = mockTask(1);
        final DownloadListener listener = listenerManager.hostListener;

        listenerManager.autoRemoveListenerIdList.add(1);
        listener.taskEnd(task, EndCause.CANCELED, null);
        assertThat(listenerManager.realListenerMap.size()).isZero();
    }

    @Test
    public void detachListener_taskId() {
        final ArrayList<DownloadListener> listenerList = new ArrayList<>();
        listenerList.add(mock(DownloadListener.class));
        listenerManager.realListenerMap.put(1, listenerList);

        listenerManager.detachListener(1);
        assertThat(listenerManager.realListenerMap.size()).isZero();
    }

    @Test
    public void addAutoRemoveListenersWhenTaskEnd() {
        listenerManager.addAutoRemoveListenersWhenTaskEnd(1);
        listenerManager.addAutoRemoveListenersWhenTaskEnd(1);
        assertThat(listenerManager.autoRemoveListenerIdList).containsExactly(1);
    }

    @Test
    public void removeAutoRemoveListenersWhenTaskEnd() {
        listenerManager.autoRemoveListenerIdList.add(1);
        listenerManager.removeAutoRemoveListenersWhenTaskEnd(1);
        assertThat(listenerManager.autoRemoveListenerIdList).isEmpty();
    }

    private DownloadTask mockTask(int id) {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(id);
        return task;
    }
}