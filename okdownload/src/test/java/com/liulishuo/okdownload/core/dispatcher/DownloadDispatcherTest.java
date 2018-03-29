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

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.download.DownloadCall;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.cause.EndCause.CANCELED;
import static com.liulishuo.okdownload.core.cause.EndCause.FILE_BUSY;
import static com.liulishuo.okdownload.core.cause.EndCause.SAME_TASK_BUSY;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DownloadDispatcherTest {

    private DownloadDispatcher dispatcher;

    private List<DownloadCall> readyAsyncCalls;

    private List<DownloadCall> runningAsyncCalls;
    private List<DownloadCall> runningSyncCalls;

    @Mock private DownloadStore store;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
        Util.setLogger(mock(Util.Logger.class));
    }

    @Before
    public void setup() {
        initMocks(this);

        readyAsyncCalls = new ArrayList<>();
        runningAsyncCalls = spy(new ArrayList<DownloadCall>());
        runningSyncCalls = spy(new ArrayList<DownloadCall>());

        dispatcher = spy(
                new DownloadDispatcher(readyAsyncCalls, runningAsyncCalls, runningSyncCalls));
        dispatcher.setDownloadStore(store);

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
        final DownloadCall readyCall = DownloadCall.create(mockReadyTask, true, store);
        readyAsyncCalls.add(readyCall);

        final DownloadTask mockRunningAsyncTask = mockTask();
        final DownloadCall runningAsyncCall = spy(
                DownloadCall.create(mockRunningAsyncTask, true, store));
        runningAsyncCalls.add(runningAsyncCall);

        final DownloadTask mockRunningSyncTask = mockTask();
        final DownloadCall runningSyncCall = DownloadCall.create(mockRunningSyncTask, false, store);
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

        // ignore canceled
        assertThat(runningAsyncCalls.size()).isEqualTo(1);
        when(runningAsyncCall.isCanceled()).thenReturn(true);
        dispatcher.enqueue(mockRunningAsyncTask);
        assertThat(runningAsyncCalls.size()).isEqualTo(2);
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
        assertThat(runningAsyncCalls).hasSize(dispatcher.maxParallelRunningCount);
    }

    @Test
    public void enqueue_countIgnoreCanceled() {
        maxRunningTask();

        assertThat(runningAsyncCalls).hasSize(dispatcher.maxParallelRunningCount);

        final DownloadTask task = mockTask();
        final DownloadCall canceledCall = runningAsyncCalls.get(0);
        dispatcher.cancel(canceledCall.task);
        // maybe here is bad design, because of here relate to DownloadCall#cancel we have to invoke
        // flyingCanceled manually which does on DownloadCall#cancel
        dispatcher.flyingCanceled(canceledCall);

        dispatcher.enqueue(task);

        assertThat(readyAsyncCalls).hasSize(0);
        assertThat(runningAsyncCalls).hasSize(dispatcher.maxParallelRunningCount + 1);
        assertThat(runningAsyncCalls.get(dispatcher.maxParallelRunningCount).task).isEqualTo(task);
        assertThat(runningSyncCalls).isEmpty();
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
        for (int i = 0; i < dispatcher.maxParallelRunningCount; i++) {
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
    public void cancel_readyAsyncCall() throws IOException {
        mockOkDownload();
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();

        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(1);
        final DownloadCall call = spy(DownloadCall.create(task, false, store));
        readyAsyncCalls.add(call);
        dispatcher.cancel(task);
        verify(call, never()).cancel();
        verify(store, never()).onTaskEnd(eq(1), eq(CANCELED), nullable(Exception.class));

        verify(listener).taskEnd(eq(task), eq(CANCELED), nullable(Exception.class));
        assertThat(readyAsyncCalls.isEmpty()).isTrue();
    }

    @Test
    public void cancel_runningAsync() throws IOException {
        mockOkDownload();

        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(1);
        final DownloadCall call = spy(DownloadCall.create(task, false, store));

        runningAsyncCalls.add(call);
        dispatcher.cancel(task);
        verify(call).cancel();
        verify(listener).taskEnd(eq(task), eq(CANCELED), nullable(Exception.class));
        verify(store).onTaskEnd(eq(1), eq(CANCELED), nullable(Exception.class));
    }

    @Test
    public void cancel_runningSync() {
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(1);
        final DownloadCall call = spy(DownloadCall.create(task, false, store));

        runningSyncCalls.add(call);
        dispatcher.cancel(task);
        verify(call).cancel();
        verify(listener).taskEnd(eq(task), eq(CANCELED), nullable(Exception.class));
        verify(store).onTaskEnd(eq(1), eq(CANCELED), nullable(Exception.class));
    }

    @Test
    public void cancel_bunch() throws IOException {
        mockOkDownload();

        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();

        final DownloadTask readyASyncCallTask = mock(DownloadTask.class);
        when(readyASyncCallTask.getId()).thenReturn(1);
        final DownloadCall readyAsyncCall = spy(
                DownloadCall.create(readyASyncCallTask, false, store));
        readyAsyncCalls.add(readyAsyncCall);

        final DownloadTask runningAsyncCallTask = mock(DownloadTask.class);
        when(runningAsyncCallTask.getId()).thenReturn(2);
        final DownloadCall runningAsyncCall = spy(
                DownloadCall.create(runningAsyncCallTask, false, store));
        runningSyncCalls.add(runningAsyncCall);

        final DownloadTask runningSyncCallTask = mock(DownloadTask.class);
        when(runningSyncCallTask.getId()).thenReturn(3);
        final DownloadCall runningSyncCall = spy(
                DownloadCall.create(runningSyncCallTask, false, store));
        runningSyncCalls.add(runningSyncCall);

        DownloadTask[] tasks = new DownloadTask[3];
        tasks[0] = readyASyncCallTask;
        tasks[1] = runningAsyncCallTask;
        tasks[2] = runningSyncCallTask;

        dispatcher.cancel(tasks);

        verify(listener).taskEnd(eq(readyASyncCallTask), eq(CANCELED), nullable(Exception.class));
        verify(listener).taskEnd(eq(runningAsyncCallTask), eq(CANCELED), nullable(Exception.class));
        verify(listener).taskEnd(eq(runningSyncCallTask), eq(CANCELED), nullable(Exception.class));

        verify(store, never()).onTaskEnd(eq(1), eq(CANCELED), nullable(Exception.class));

        ArgumentCaptor<int[]> callCaptor = ArgumentCaptor.forClass(int[].class);

        verify(store).bunchTaskCanceled(callCaptor.capture());
        final int[] bunchTaskCanceledIds = callCaptor.getValue();
        assertThat(bunchTaskCanceledIds[0]).isEqualTo(2);
        assertThat(bunchTaskCanceledIds[1]).isEqualTo(3);

        verify(readyAsyncCall, never()).cancel();
        verify(runningAsyncCall).cancel();
        verify(runningSyncCall).cancel();
    }

    @Test
    public void cancelAll() {
        readyAsyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), true, store)));
        readyAsyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), true, store)));
        readyAsyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), true, store)));
        readyAsyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), true, store)));

        runningAsyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), true, store)));
        runningAsyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), true, store)));
        runningAsyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), true, store)));
        runningAsyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), true, store)));

        runningSyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), false, store)));
        runningSyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), false, store)));
        runningSyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), false, store)));
        runningSyncCalls.add(spy(DownloadCall.create(mock(DownloadTask.class), false, store)));

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
        final DownloadCall mockRunningCall = DownloadCall.create(mockTask(), true, store);
        runningAsyncCalls.add(mockRunningCall);
        final DownloadCall mockReadyCall = DownloadCall.create(mockTask(), true, store);
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
        DownloadCall call = spy(DownloadCall.create(mockAsyncTask, true, store));
        runningAsyncCalls.add(call);

        boolean isConflict = dispatcher.isFileConflictAfterRun(samePathTask);
        assertThat(isConflict).isTrue();

        // ignore canceled
        when(call.isCanceled()).thenReturn(true);
        isConflict = dispatcher.isFileConflictAfterRun(samePathTask);
        assertThat(isConflict).isFalse();
        // not canceled and another path task
        when(call.isCanceled()).thenReturn(false);

        final DownloadTask mockSyncTask = mockTask();
        doReturn(mockSyncTask.getPath()).when(samePathTask).getPath();
        runningSyncCalls.add(DownloadCall.create(mockSyncTask, false, store));

        isConflict = dispatcher.isFileConflictAfterRun(samePathTask);
        assertThat(isConflict).isTrue();

        final DownloadTask noSamePathTask = mockTask();
        isConflict = dispatcher.isFileConflictAfterRun(noSamePathTask);
        assertThat(isConflict).isFalse();
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void setMaxParallelRunningCount() {
        doReturn(mock(MockDownloadDispatcher.class)).when(OkDownload.with()).downloadDispatcher();
        thrown.expect(IllegalStateException.class);
        DownloadDispatcher.setMaxParallelRunningCount(1);

        doReturn(dispatcher).when(OkDownload.with()).breakpointStore();

        DownloadDispatcher.setMaxParallelRunningCount(0);
        assertThat(dispatcher.maxParallelRunningCount).isEqualTo(1);

        DownloadDispatcher.setMaxParallelRunningCount(2);
        assertThat(dispatcher.maxParallelRunningCount).isEqualTo(2);
    }

    private static class MockDownloadDispatcher extends DownloadDispatcher {
    }

}