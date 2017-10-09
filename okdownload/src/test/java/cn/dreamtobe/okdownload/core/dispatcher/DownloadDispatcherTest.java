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

package cn.dreamtobe.okdownload.core.dispatcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.core.download.DownloadCall;
import cn.dreamtobe.okdownload.DownloadTask;

import static cn.dreamtobe.okdownload.DownloadListener.EndCause.sameTaskBusy;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownloadDispatcherTest {

    private DownloadDispatcher dispatcher;

    private List<DownloadCall> readyAsyncCalls;

    private List<DownloadCall> runningAsyncCalls;
    private List<DownloadCall> runningSyncCalls;

    @Before
    public void setup() {
        readyAsyncCalls = new ArrayList<>();
        runningAsyncCalls = new ArrayList<>();
        runningSyncCalls = spy(new ArrayList<DownloadCall>());

        dispatcher = spy(new DownloadDispatcher(readyAsyncCalls, runningAsyncCalls, runningSyncCalls));

        doReturn(mock(ExecutorService.class)).when(dispatcher).executorService();
        doNothing().when(dispatcher).syncRunCall(any(DownloadCall.class));
    }

    private DownloadTask mockTask() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        when(mockTask.getListener()).thenReturn(mock(DownloadListener.class));
        when(mockTask.getPath()).thenReturn("/sdcard/abc" + mockTask.hashCode());
        return mockTask;
    }

    @Test
    public void enqueue_conflict_notEnqueue() {
        final DownloadTask mockReadyTask = mockTask();
        final DownloadCall readyCall = DownloadCall.create(mockReadyTask, true);
        readyAsyncCalls.add(readyCall);

        final DownloadTask mockRunningAsyncTask = mockTask();
        final DownloadCall runningAsyncCall = DownloadCall.create(mockRunningAsyncTask, true);
        runningAsyncCalls.add(runningAsyncCall);

        final DownloadTask mockRunningSyncTask = mockTask();
        final DownloadCall runningSyncCall = DownloadCall.create(mockRunningSyncTask, false);
        runningSyncCalls.add(runningSyncCall);

        dispatcher.enqueue(mockReadyTask);
        dispatcher.enqueue(mockRunningAsyncTask);
        dispatcher.execute(mockRunningSyncTask);

        assertThat(readyAsyncCalls).containsOnlyOnce(readyCall);
        assertThat(runningAsyncCalls).containsOnlyOnce(runningAsyncCall);
        assertThat(runningSyncCalls).containsOnlyOnce(runningSyncCall);

        verifyTaskEnd(mockReadyTask, sameTaskBusy, null);
        verifyTaskEnd(mockRunningAsyncTask, sameTaskBusy, null);
        verifyTaskEnd(mockRunningSyncTask, sameTaskBusy, null);
    }

    private void verifyTaskEnd(DownloadTask task, DownloadListener.EndCause cause, Exception realCause) {
        final DownloadListener listener = task.getListener();
        verify(listener).taskEnd(task, cause, realCause);
    }

    @Test
    public void enqueue_maxTaskCountControl() {
        maxRunningTask();

        final DownloadTask mockTask = mockTask();
        dispatcher.enqueue(mockTask);

        assertThat(readyAsyncCalls).hasSize(1);
        assertThat(readyAsyncCalls.get(0).task).isEqualTo(mockTask);

        assertThat(runningSyncCalls).isEmpty();
        assertThat(runningAsyncCalls).hasSize(dispatcher.maxTaskCount);
    }

    @Test
    public void enqueue_priority() {
        final DownloadTask mockTask1 = mockTask();
        when(mockTask1.getPriority()).thenReturn(1);

        final DownloadTask mockTask2 = mockTask();
        when(mockTask2.getPriority()).thenReturn(2);

        final DownloadTask mockTask3 = mockTask();
        when(mockTask3.getPriority()).thenReturn(3);

        maxRunningTask();

        dispatcher.enqueue(mockTask2);
        dispatcher.enqueue(mockTask1);
        dispatcher.enqueue(mockTask3);

        assertThat(readyAsyncCalls.get(0).task).isEqualTo(mockTask3);
        assertThat(readyAsyncCalls.get(1).task).isEqualTo(mockTask2);
        assertThat(readyAsyncCalls.get(2).task).isEqualTo(mockTask1);
    }

    private void maxRunningTask() {
        for (int i = 0; i < dispatcher.maxTaskCount; i++) {
            dispatcher.enqueue(mockTask());
        }
    }

    @Test
    public void execute() {
        final DownloadTask mockTask = mockTask();

        dispatcher.execute(mockTask);

        ArgumentCaptor<DownloadCall> callCaptor = ArgumentCaptor.forClass(DownloadCall.class);

        verify(runningSyncCalls).add(callCaptor.capture());
        final DownloadCall call = callCaptor.getValue();

        assertThat(call.task).isEqualTo(mockTask);
        verify(dispatcher).syncRunCall(call);
    }

    @Test(expected = AssertionError.class)
    public void finish_removeFailed_exception() {
        dispatcher.finish(mock(DownloadCall.class));
    }

    @Test
    public void finish_asyncExecuted() {
        final DownloadCall mockRunningCall = DownloadCall.create(mockTask(), true);
        runningAsyncCalls.add(mockRunningCall);
        final DownloadCall mockReadyCall = DownloadCall.create(mockTask(), true);
        readyAsyncCalls.add(mockReadyCall);

        dispatcher.finish(mockRunningCall);

        assertThat(runningAsyncCalls).containsExactly(mockReadyCall);
        assertThat(readyAsyncCalls).isEmpty();

        final ExecutorService executorService = dispatcher.executorService();
        verify(executorService).execute(mockReadyCall);
    }
}