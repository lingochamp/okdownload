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
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.download.DownloadCall;
import com.liulishuo.okdownload.core.download.DownloadStrategy;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DownloadDispatcherTest {

    private DownloadDispatcher dispatcher;

    private List<DownloadCall> readyAsyncCalls;

    private List<DownloadCall> runningAsyncCalls;
    private List<DownloadCall> runningSyncCalls;

    @Mock private DownloadStore store;
    private File existFile = new File("./p-path/filename");

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
        Util.setLogger(mock(Util.Logger.class));
    }

    @Before
    public void setup() throws IOException {
        initMocks(this);

        readyAsyncCalls = new ArrayList<>();
        runningAsyncCalls = spy(new ArrayList<DownloadCall>());
        runningSyncCalls = spy(new ArrayList<DownloadCall>());

        dispatcher = spy(
                new DownloadDispatcher(readyAsyncCalls, runningAsyncCalls, runningSyncCalls));
        dispatcher.setDownloadStore(store);

        doReturn(mock(ExecutorService.class)).when(dispatcher).executorService();
        doNothing().when(dispatcher).syncRunCall(any(DownloadCall.class));

        existFile.getParentFile().mkdirs();
        existFile.createNewFile();
    }

    @After
    public void tearDown() {
        existFile.delete();
        existFile.getParentFile().delete();
    }

    private DownloadTask mockTask() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        when(mockTask.getListener()).thenReturn(mock(DownloadListener.class));
        when(mockTask.getFile()).thenReturn(new File("/sdcard/abc" + mockTask.hashCode()));
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
        doReturn(mockReadyTask.getFile()).when(mockFileBusyTask1).getFile();
        dispatcher.enqueue(mockFileBusyTask1);
        verifyTaskEnd(mockFileBusyTask1, FILE_BUSY, null);

        final DownloadTask mockFileBusyTask2 = mockTask();
        doReturn(mockRunningAsyncTask.getFile()).when(mockFileBusyTask2).getFile();
        dispatcher.execute(mockFileBusyTask2);
        verifyTaskEnd(mockFileBusyTask2, FILE_BUSY, null);

        final DownloadTask mockFileBusyTask3 = mockTask();
        doReturn(mockRunningSyncTask.getFile()).when(mockFileBusyTask3).getFile();
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
    public void enqueue_tasks() throws IOException {
        mockOkDownload();
        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();

        DownloadTask[] tasks = new DownloadTask[]{mock(DownloadTask.class), mock(
                DownloadTask.class), mock(DownloadTask.class)};

        doReturn(true).when(dispatcher)
                .inspectCompleted(any(DownloadTask.class), any(Collection.class));
        doReturn(true).when(dispatcher)
                .inspectForConflict(any(DownloadTask.class), any(Collection.class),
                        any(Collection.class), any(Collection.class));

        dispatcher.enqueue(tasks);

        verify(callbackDispatcher)
                .endTasks(any(Collection.class), any(Collection.class), any(Collection.class));
    }

    @Test
    public void enqueue_tasksWithNetworkNotAvailable() throws IOException {
        mockOkDownload();
        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();
        final DownloadStrategy downloadStrategy = OkDownload.with().downloadStrategy();

        DownloadTask[] tasks = new DownloadTask[]{mock(DownloadTask.class), mock(
                DownloadTask.class), mock(DownloadTask.class)};

        doThrow(UnknownHostException.class).when(downloadStrategy).inspectNetworkAvailable();
        dispatcher.enqueue(tasks);

        final ArgumentCaptor<Collection<DownloadTask>> listCaptor = ArgumentCaptor
                .forClass(Collection.class);

        verify(callbackDispatcher, never())
                .endTasks(any(Collection.class), any(Collection.class), any(Collection.class));
        assertThat(readyAsyncCalls).isEmpty();
        verify(dispatcher, never()).executorService();

        final ArgumentCaptor<Exception> causeCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(callbackDispatcher).endTasksWithError(listCaptor.capture(), causeCaptor.capture());

        assertThat(listCaptor.getValue()).containsExactly(tasks);
        assertThat(causeCaptor.getValue()).isExactlyInstanceOf(UnknownHostException.class);
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
    public void cancel_notSameReferenceButSameId_runningSync() {
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        final DownloadTask runningSyncTask = mock(DownloadTask.class);
        final DownloadTask runningAsyncTask = mock(DownloadTask.class);
        final DownloadTask readyAsyncTask = mock(DownloadTask.class);
        when(runningSyncTask.getId()).thenReturn(1);
        when(runningAsyncTask.getId()).thenReturn(1);
        when(readyAsyncTask.getId()).thenReturn(1);


        final DownloadTask sameIdTask = mock(DownloadTask.class);
        when(sameIdTask.getId()).thenReturn(1);

        final DownloadCall runningSyncCall = spy(
                DownloadCall.create(runningSyncTask, false, store));
        runningSyncCalls.add(runningSyncCall);

        dispatcher.cancel(sameIdTask);

        verify(runningSyncCall).cancel();
        verify(listener).taskEnd(eq(runningSyncTask), eq(CANCELED), nullable(Exception.class));
        verify(store).onTaskEnd(eq(1), eq(CANCELED), nullable(Exception.class));
    }

    @Test
    public void cancel_notSameReferenceButSameId_runningAsync() {
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        final DownloadTask runningSyncTask = mock(DownloadTask.class);
        final DownloadTask runningAsyncTask = mock(DownloadTask.class);
        final DownloadTask readyAsyncTask = mock(DownloadTask.class);
        when(runningSyncTask.getId()).thenReturn(1);
        when(runningAsyncTask.getId()).thenReturn(1);
        when(readyAsyncTask.getId()).thenReturn(1);


        final DownloadTask sameIdTask = mock(DownloadTask.class);
        when(sameIdTask.getId()).thenReturn(1);

        final DownloadCall runningAsyncCall = spy(
                DownloadCall.create(runningAsyncTask, false, store));
        runningAsyncCalls.add(runningAsyncCall);

        dispatcher.cancel(sameIdTask);

        verify(runningAsyncCall).cancel();
        verify(listener).taskEnd(eq(runningAsyncTask), eq(CANCELED), nullable(Exception.class));
        verify(store).onTaskEnd(eq(1), eq(CANCELED), nullable(Exception.class));
    }

    @Test
    public void cancel_notSameReferenceButSameId_readyAsync() {
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        final DownloadTask runningSyncTask = mock(DownloadTask.class);
        final DownloadTask runningAsyncTask = mock(DownloadTask.class);
        final DownloadTask readyAsyncTask = mock(DownloadTask.class);
        when(runningSyncTask.getId()).thenReturn(1);
        when(runningAsyncTask.getId()).thenReturn(1);
        when(readyAsyncTask.getId()).thenReturn(1);

        final DownloadCall readyAsyncCall = spy(DownloadCall.create(readyAsyncTask, false, store));
        readyAsyncCalls.add(readyAsyncCall);

        final DownloadTask sameIdTask = mock(DownloadTask.class);
        when(sameIdTask.getId()).thenReturn(1);

        dispatcher.cancel(sameIdTask);

        verify(listener).taskEnd(eq(readyAsyncTask), eq(CANCELED), nullable(Exception.class));
    }

    @Test
    public void cancel_bunch() throws IOException {
        mockOkDownload();

        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();

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

        ArgumentCaptor<Collection<DownloadTask>> callbackCanceledList = ArgumentCaptor
                .forClass(Collection.class);
        verify(callbackDispatcher).endTasksWithCanceled(callbackCanceledList.capture());
        assertThat(callbackCanceledList.getValue())
                .containsExactly(readyASyncCallTask, runningAsyncCallTask, runningSyncCallTask);

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

    private DownloadTask mockTask(int id) {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(id);
        return task;
    }

    @Test
    public void cancelAll() {
        readyAsyncCalls.add(spy(DownloadCall.create(mockTask(1), true, store)));
        readyAsyncCalls.add(spy(DownloadCall.create(mockTask(2), true, store)));
        readyAsyncCalls.add(spy(DownloadCall.create(mockTask(3), true, store)));
        readyAsyncCalls.add(spy(DownloadCall.create(mockTask(4), true, store)));

        runningAsyncCalls.add(spy(DownloadCall.create(mockTask(5), true, store)));
        runningAsyncCalls.add(spy(DownloadCall.create(mockTask(6), true, store)));
        runningAsyncCalls.add(spy(DownloadCall.create(mockTask(7), true, store)));
        runningAsyncCalls.add(spy(DownloadCall.create(mockTask(8), true, store)));

        runningSyncCalls.add(spy(DownloadCall.create(mockTask(9), false, store)));
        runningSyncCalls.add(spy(DownloadCall.create(mockTask(10), false, store)));
        runningSyncCalls.add(spy(DownloadCall.create(mockTask(11), false, store)));
        runningSyncCalls.add(spy(DownloadCall.create(mockTask(12), false, store)));

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
        doReturn(mockAsyncTask.getFile()).when(samePathTask).getFile();
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
        doReturn(mockSyncTask.getFile()).when(samePathTask).getFile();
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

    @Test
    public void inspectCompleted() throws IOException {
        mockOkDownload();
        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();

        final DownloadTask task = mock(DownloadTask.class);
        when(task.isPassIfAlreadyCompleted()).thenReturn(true);
        when(task.getId()).thenReturn(0);
        when(task.getUrl()).thenReturn("url");
        when(task.getParentFile()).thenReturn(existFile.getParentFile());

        final BreakpointStore store = OkDownload.with().breakpointStore();
        doReturn(existFile.getName()).when(store).getResponseFilename("url");

        // valid filename failed.
        final DownloadStrategy downloadStrategy = OkDownload.with().downloadStrategy();
        doReturn(false).when(downloadStrategy).validFilenameFromStore(task);
        assertThat(dispatcher.inspectCompleted(task)).isFalse();
        verify(callbackDispatcher, never()).dispatch();

        //  valid filename success.
        doReturn(true).when(downloadStrategy).validFilenameFromStore(task);
        assertThat(dispatcher.inspectCompleted(task)).isTrue();
        final DownloadListener listener = callbackDispatcher.dispatch();
        verify(listener).taskEnd(eq(task), eq(EndCause.COMPLETED), nullable(Exception.class));
        verify(downloadStrategy).validInfoOnCompleted(eq(task), eq(this.store));
    }

    @Test
    public void inspectCompleted_collection() throws IOException {
        mockOkDownload();
        final DownloadStrategy downloadStrategy = OkDownload.with().downloadStrategy();
        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();
        final DownloadListener listener = callbackDispatcher.dispatch();

        final DownloadTask task = mock(DownloadTask.class);
        when(task.isPassIfAlreadyCompleted()).thenReturn(true);
        when(task.getId()).thenReturn(0);
        when(task.getUrl()).thenReturn("url");
        when(task.getParentFile()).thenReturn(existFile.getParentFile());

        final BreakpointStore store = OkDownload.with().breakpointStore();
        doReturn(existFile.getName()).when(store).getResponseFilename("url");
        doReturn(true).when(downloadStrategy).validFilenameFromStore(task);
        final Collection<DownloadTask> completedCollection = new ArrayList<>();

        assertThat(dispatcher.inspectCompleted(task, completedCollection)).isTrue();
        verify(listener, never()).taskEnd(eq(task), any(EndCause.class), nullable(Exception.class));
        assertThat(completedCollection).containsExactly(task);
    }

    @Test
    public void inspectForConflict_sameTask() throws IOException {
        mockOkDownload();
        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();
        final DownloadListener listener = callbackDispatcher.dispatch();

        DownloadTask task = mock(DownloadTask.class);
        final Collection<DownloadCall> calls = new ArrayList<>();
        final Collection<DownloadTask> sameTaskList = new ArrayList<>();
        final Collection<DownloadTask> fileBusyList = new ArrayList<>();

        final DownloadCall call = mock(DownloadCall.class);
        when(call.equalsTask(task)).thenReturn(true);
        calls.add(call);

        assertThat(dispatcher.inspectForConflict(task, calls, sameTaskList, fileBusyList)).isTrue();
        assertThat(sameTaskList).containsExactly(task);
        assertThat(fileBusyList).isEmpty();
        verify(listener, never()).taskEnd(eq(task), any(EndCause.class), nullable(Exception.class));
    }

    @Test
    public void inspectForConflict_fileBusy() throws IOException {
        mockOkDownload();
        final CallbackDispatcher callbackDispatcher = OkDownload.with().callbackDispatcher();
        final DownloadListener listener = callbackDispatcher.dispatch();

        DownloadTask task = mock(DownloadTask.class);
        final Collection<DownloadCall> calls = new ArrayList<>();
        final Collection<DownloadTask> sameTaskList = new ArrayList<>();
        final Collection<DownloadTask> fileBusyList = new ArrayList<>();

        final DownloadCall call = mock(DownloadCall.class);
        final File file = mock(File.class);
        when(task.getFile()).thenReturn(file);
        when(call.getFile()).thenReturn(file);

        calls.add(call);

        assertThat(dispatcher.inspectForConflict(task, calls, sameTaskList, fileBusyList)).isTrue();
        assertThat(sameTaskList).isEmpty();
        assertThat(fileBusyList).containsExactly(task);
        verify(listener, never()).taskEnd(eq(task), any(EndCause.class), nullable(Exception.class));
    }

    @Test
    public void findSameTask_readyAsyncCall() {
        final DownloadTask task = mock(DownloadTask.class);
        final DownloadStore store = mock(DownloadStore.class);
        final DownloadCall canceledCall = spy(DownloadCall
                .create(mock(DownloadTask.class), true, store));
        final DownloadCall nonCanceledCall = spy(DownloadCall
                .create(mock(DownloadTask.class), true, store));
        when(canceledCall.isCanceled()).thenReturn(true);
        when(canceledCall.equalsTask(task)).thenReturn(true);
        when(nonCanceledCall.equalsTask(task)).thenReturn(true);

        readyAsyncCalls.add(canceledCall);
        readyAsyncCalls.add(nonCanceledCall);

        assertThat(dispatcher.findSameTask(task)).isEqualTo(nonCanceledCall.task);
    }

    @Test
    public void findSameTask_runningAsyncCall() {
        final DownloadTask task = mock(DownloadTask.class);
        final DownloadStore store = mock(DownloadStore.class);
        final DownloadCall canceledCall = spy(DownloadCall
                .create(mock(DownloadTask.class), true, store));
        final DownloadCall nonCanceledCall = spy(DownloadCall
                .create(mock(DownloadTask.class), true, store));

        when(canceledCall.equalsTask(task)).thenReturn(true);
        when(canceledCall.isCanceled()).thenReturn(true);
        when(nonCanceledCall.equalsTask(task)).thenReturn(true);

        runningAsyncCalls.add(canceledCall);
        runningAsyncCalls.add(nonCanceledCall);

        assertThat(dispatcher.findSameTask(task)).isEqualTo(nonCanceledCall.task);
    }

    @Test
    public void findSameTask_runningSyncCall() {
        final DownloadTask task = mock(DownloadTask.class);
        final DownloadStore store = mock(DownloadStore.class);
        final DownloadCall canceledCall = spy(DownloadCall
                .create(mock(DownloadTask.class), false, store));
        final DownloadCall nonCanceledCall = spy(DownloadCall
                .create(mock(DownloadTask.class), false, store));
        when(canceledCall.isCanceled()).thenReturn(true);
        when(canceledCall.equalsTask(task)).thenReturn(true);
        when(nonCanceledCall.equalsTask(task)).thenReturn(true);

        runningSyncCalls.add(canceledCall);
        runningSyncCalls.add(nonCanceledCall);

        assertThat(dispatcher.findSameTask(task)).isEqualTo(nonCanceledCall.task);
    }

    @Test
    public void findSameTask_nonMatch() {
        final DownloadTask task = mock(DownloadTask.class);
        assertThat(dispatcher.findSameTask(task)).isNull();
    }

    private static class MockDownloadDispatcher extends DownloadDispatcher {
    }
}