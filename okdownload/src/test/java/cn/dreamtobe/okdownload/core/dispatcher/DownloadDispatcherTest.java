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
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.cause.EndCause;
import cn.dreamtobe.okdownload.core.download.DownloadCall;

import static cn.dreamtobe.okdownload.TestUtils.mockOkDownload;
import static cn.dreamtobe.okdownload.core.cause.EndCause.FILE_BUSY;
import static cn.dreamtobe.okdownload.core.cause.EndCause.SAME_TASK_BUSY;
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

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() {
        readyAsyncCalls = new ArrayList<>();
        runningAsyncCalls = spy(new ArrayList<DownloadCall>());
        runningSyncCalls = spy(new ArrayList<DownloadCall>());

        dispatcher = spy(
                new DownloadDispatcher(readyAsyncCalls, runningAsyncCalls, runningSyncCalls));

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

        verifyTaskEnd(mockReadyTask, SAME_TASK_BUSY, null);
        verifyTaskEnd(mockRunningAsyncTask, SAME_TASK_BUSY, null);
        verifyTaskEnd(mockRunningSyncTask, SAME_TASK_BUSY, null);


        final DownloadTask mockFileBusyTask1 = mockTask();
        doReturn(mockReadyTask.getPath()).when(mockFileBusyTask1).getPath();
        dispatcher.enqueue(mockFileBusyTask1);
        verifyTaskEnd(mockFileBusyTask1, FILE_BUSY, null);

        final DownloadTask mockFileBusyTask2 = mockTask();
        doReturn(mockRunningAsyncTask.getPath()).when(mockFileBusyTask2).getPath();
        dispatcher.execute(mockFileBusyTask2);
        verifyTaskEnd(mockFileBusyTask2, FILE_BUSY, null);

        final DownloadTask mockFileBusyTask3 = mockTask();
        doReturn(mockRunningSyncTask.getPath()).when(mockFileBusyTask3).getPath();
        dispatcher.enqueue(mockFileBusyTask3);
        verifyTaskEnd(mockFileBusyTask3, FILE_BUSY, null);
    }

    private void verifyTaskEnd(DownloadTask task, EndCause cause, Exception realCause) {
        verify(OkDownload.with().callbackDispatcher().dispatch()).taskEnd(task, cause, realCause);
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

    @Test
    public void cancelAll() {
        readyAsyncCalls.add(mock(DownloadCall.class));
        readyAsyncCalls.add(mock(DownloadCall.class));
        readyAsyncCalls.add(mock(DownloadCall.class));
        readyAsyncCalls.add(mock(DownloadCall.class));

        runningAsyncCalls.add(mock(DownloadCall.class));
        runningAsyncCalls.add(mock(DownloadCall.class));
        runningAsyncCalls.add(mock(DownloadCall.class));
        runningAsyncCalls.add(mock(DownloadCall.class));

        runningSyncCalls.add(mock(DownloadCall.class));
        runningSyncCalls.add(mock(DownloadCall.class));
        runningSyncCalls.add(mock(DownloadCall.class));
        runningSyncCalls.add(mock(DownloadCall.class));

        dispatcher.cancelAll();

        for (DownloadCall call : readyAsyncCalls) {
            verify(call).cancel();
        }

        for (DownloadCall call : runningAsyncCalls) {
            verify(call).cancel();
        }

        for (DownloadCall call : runningSyncCalls) {
            verify(call).cancel();
        }
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

        verify(runningAsyncCalls).remove(mockRunningCall);

        assertThat(runningAsyncCalls).containsExactly(mockReadyCall);
        assertThat(readyAsyncCalls).isEmpty();

        final ExecutorService executorService = dispatcher.executorService();
        verify(executorService).execute(mockReadyCall);
    }

    @Test
    public void isFileConflictAfterRun() {
        final DownloadTask mockAsyncTask = mockTask();
        final DownloadTask samePathTask = mockTask();
        doReturn(mockAsyncTask.getPath()).when(samePathTask).getPath();
        runningAsyncCalls.add(DownloadCall.create(mockAsyncTask, true));

        boolean isConflict = dispatcher.isFileConflictAfterRun(samePathTask);
        assertThat(isConflict).isTrue();

        final DownloadTask mockSyncTask = mockTask();
        doReturn(mockSyncTask.getPath()).when(samePathTask).getPath();
        runningSyncCalls.add(DownloadCall.create(mockSyncTask, false));

        isConflict = dispatcher.isFileConflictAfterRun(samePathTask);
        assertThat(isConflict).isTrue();

        final DownloadTask noSamePathTask = mockTask();
        isConflict = dispatcher.isFileConflictAfterRun(noSamePathTask);
        assertThat(isConflict).isFalse();
    }
}