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

package com.liulishuo.okdownload.core.download;

import android.net.Uri;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.dispatcher.CallbackDispatcher;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;
import com.liulishuo.okdownload.core.file.FileLock;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;
import com.liulishuo.okdownload.core.file.ProcessFileStrategy;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DownloadCallTest {

    private DownloadCall call;

    @Mock
    private DownloadTask task;
    @Mock
    private BreakpointInfo info;
    @Mock
    private DownloadStore store;
    @Mock
    private FileLock fileLock;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() {
        initMocks(this);
        when(task.getUri()).thenReturn(mock(Uri.class));
        when(task.getFile()).thenReturn(mock(File.class));
        when(task.getListener()).thenReturn(mock(DownloadListener.class));
        call = spy(DownloadCall.create(task, false, store));

        final Future mockFuture = mock(Future.class);
        doReturn(mockFuture).when(call).submitChain(any(DownloadChain.class));
        when(mockFuture.isDone()).thenReturn(true);

        when(info.getBlockCount()).thenReturn(3);
        when(info.getTotalLength()).thenReturn(30L);
        when(info.getBlock(0)).thenReturn(new BlockInfo(0, 10));
        when(info.getBlock(1)).thenReturn(new BlockInfo(10, 10));
        when(info.getBlock(2)).thenReturn(new BlockInfo(20, 10));

        when(store.get(anyInt())).thenReturn(info);
        when(task.getUrl()).thenReturn("https://jacksgong.com");

    }

    private void setupFileStrategy() throws IOException {
        mockOkDownload();

        final ProcessFileStrategy fileStrategy = OkDownload.with().processFileStrategy();
        when(fileStrategy.getFileLock()).thenReturn(fileLock);
    }

    @Test
    public void execute_createIfNon() throws IOException, InterruptedException {
        when(store.get(anyInt())).thenReturn(null);
        when(store.createAndInsert(task)).thenReturn(info);
        doReturn(mock(BreakpointRemoteCheck.class)).when(call).createRemoteCheck(eq(info));
        doReturn(mock(BreakpointLocalCheck.class)).when(call).createLocalCheck(eq(info), anyLong());
        doNothing().when(call).start(any(DownloadCache.class), eq(info));

        call.execute();

        verify(store).createAndInsert(task);
        verify(call).setInfoToTask(eq(info));
    }

    @Test
    public void execute_remoteCheck() throws IOException, InterruptedException {
        final BreakpointRemoteCheck remoteCheck = mock(BreakpointRemoteCheck.class);
        doReturn(remoteCheck).when(call).createRemoteCheck(eq(info));
        doReturn(mock(BreakpointLocalCheck.class)).when(call).createLocalCheck(eq(info), anyLong());
        doNothing().when(call).start(any(DownloadCache.class), eq(info));

        call.execute();

        verify(remoteCheck).check();
    }

    @Test
    public void execute_waitForRelease() throws InterruptedException, IOException {
        setupFileStrategy();

        final BreakpointRemoteCheck remoteCheck = mock(BreakpointRemoteCheck.class);
        doReturn(remoteCheck).when(call).createRemoteCheck(eq(info));
        doReturn(mock(BreakpointLocalCheck.class)).when(call).createLocalCheck(eq(info), anyLong());
        doNothing().when(call).start(any(DownloadCache.class), eq(info));
        when(task.getFile()).thenReturn(new File("certain-path"));

        call.execute();

        verify(fileLock).waitForRelease(eq(new File("certain-path").getAbsolutePath()));
    }

    @Test
    public void execute_reuseAnotherInfo() throws IOException, InterruptedException {
        doReturn(mock(BreakpointRemoteCheck.class)).when(call).createRemoteCheck(eq(info));
        doReturn(mock(BreakpointLocalCheck.class)).when(call).createLocalCheck(eq(info), anyLong());
        doNothing().when(call).start(any(DownloadCache.class), eq(info));

        call.execute();

        final DownloadStrategy strategy = OkDownload.with().downloadStrategy();
        verify(strategy).inspectAnotherSameInfo(eq(task), eq(info), eq(0L));
    }

    @Test
    public void execute_localCheck() throws InterruptedException {
        final BreakpointLocalCheck localCheck = mock(BreakpointLocalCheck.class);
        final BreakpointRemoteCheck remoteCheck = mock(BreakpointRemoteCheck.class);
        doReturn(remoteCheck).when(call).createRemoteCheck(eq(info));
        doReturn(localCheck).when(call).createLocalCheck(eq(info), anyLong());
        doNothing().when(call).start(any(DownloadCache.class), eq(info));

        when(remoteCheck.isResumable()).thenReturn(false);
        call.execute();
        verify(localCheck, never()).check();

        when(remoteCheck.isResumable()).thenReturn(true);
        call.execute();
        verify(localCheck).check();
    }

    @Test
    public void execute_assembleBlockData() throws InterruptedException, IOException {
        setupFileStrategy();

        final ProcessFileStrategy fileStrategy = OkDownload.with().processFileStrategy();
        final BreakpointLocalCheck localCheck = mock(BreakpointLocalCheck.class);
        final BreakpointRemoteCheck remoteCheck = mock(BreakpointRemoteCheck.class);
        final ResumeFailedCause failedCauseByRemote = mock(ResumeFailedCause.class);
        final ResumeFailedCause failedCauseByLocal = mock(ResumeFailedCause.class);
        doReturn(remoteCheck).when(call).createRemoteCheck(eq(info));
        doReturn(localCheck).when(call).createLocalCheck(eq(info), anyLong());
        doReturn(failedCauseByRemote).when(remoteCheck).getCauseOrThrow();
        doReturn(failedCauseByLocal).when(localCheck).getCauseOrThrow();
        doNothing().when(call).assembleBlockAndCallbackFromBeginning(eq(info), eq(remoteCheck),
                any(ResumeFailedCause.class));
        doNothing().when(call).start(any(DownloadCache.class), eq(info));

        when(remoteCheck.isResumable()).thenReturn(false);
        call.execute();
        verify(call).assembleBlockAndCallbackFromBeginning(eq(info), eq(remoteCheck),
                eq(failedCauseByRemote));
        verify(fileStrategy).discardProcess(eq(task));

        when(remoteCheck.isResumable()).thenReturn(true);
        when(localCheck.isDirty()).thenReturn(false);
        call.execute();
        verify(call, never()).assembleBlockAndCallbackFromBeginning(eq(info), eq(remoteCheck),
                eq(failedCauseByLocal));
        // callback download from breakpoint.
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        verify(listener).downloadFromBreakpoint(eq(task), eq(info));

        when(localCheck.isDirty()).thenReturn(true);
        call.execute();
        verify(call).assembleBlockAndCallbackFromBeginning(eq(info), eq(remoteCheck),
                eq(failedCauseByLocal));
        verify(fileStrategy, times(2)).discardProcess(eq(task));
    }

    @Test
    public void execute_start() throws InterruptedException {
        doReturn(mock(BreakpointRemoteCheck.class)).when(call).createRemoteCheck(eq(info));
        doReturn(mock(BreakpointLocalCheck.class)).when(call).createLocalCheck(eq(info), anyLong());
        doNothing().when(call).start(any(DownloadCache.class), eq(info));

        call.execute();
        verify(call).start(any(DownloadCache.class), eq(info));
    }

    @Test
    public void execute_preconditionFailed() throws InterruptedException, IOException {
        setupFileStrategy();

        final DownloadCache cache = mock(DownloadCache.class);
        doReturn(cache).when(call).createCache(eq(info));
        when(cache.isPreconditionFailed()).thenReturn(true, false);
        final ResumeFailedCause resumeFailedCause = mock(ResumeFailedCause.class);
        doReturn(resumeFailedCause).when(cache).getResumeFailedCause();
        doNothing().when(call).start(eq(cache), eq(info));
        doReturn(mock(BreakpointRemoteCheck.class)).when(call).createRemoteCheck(eq(info));
        doReturn(mock(BreakpointLocalCheck.class)).when(call).createLocalCheck(eq(info), anyLong());

        call.execute();

        verify(call, times(2)).start(eq(cache), eq(info));
        final ProcessFileStrategy fileStrategy = OkDownload.with().processFileStrategy();
        final int id = task.getId();
        verify(store).remove(eq(id));
        verify(fileStrategy, times(2)).discardProcess(eq(task));
    }

    @Test
    public void execute_preconditionFailedMaxTimes() throws InterruptedException, IOException {
        final CallbackDispatcher dispatcher = OkDownload.with().callbackDispatcher();
        final DownloadCache cache = mock(DownloadCache.class);
        doReturn(cache).when(call).createCache(eq(info));

        doNothing().when(call).start(eq(cache), eq(info));
        doReturn(mock(BreakpointRemoteCheck.class)).when(call).createRemoteCheck(eq(info));
        doReturn(mock(BreakpointLocalCheck.class)).when(call).createLocalCheck(eq(info), anyLong());
        doReturn(mock(DownloadListener.class)).when(dispatcher).dispatch();

        when(cache.isPreconditionFailed()).thenReturn(true);
        call.execute();

        verify(call, times(DownloadCall.MAX_COUNT_RETRY_FOR_PRECONDITION_FAILED + 1)).start(
                eq(cache), eq(info));

        // only once.
        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        verify(listener).taskStart(eq(task));
    }

    @Test
    public void execute_urlIsEmpty() throws InterruptedException {
        when(task.getUrl()).thenReturn("");

        call.execute();

        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();

        verify(call, never()).start(any(DownloadCache.class), any(BreakpointInfo.class));

        ArgumentCaptor<IOException> captor = ArgumentCaptor.forClass(IOException.class);
        verify(listener).taskEnd(eq(task), eq(EndCause.ERROR), captor.capture());
        IOException exception = captor.getValue();
        assertThat(exception.getMessage()).isEqualTo("unexpected url: ");
    }

    @Test
    public void execute_finish() throws InterruptedException, IOException {
        setupFileStrategy();

        assertThat(call.finishing).isFalse();

        final OkDownload okDownload = OkDownload.with();
        final CallbackDispatcher dispatcher = OkDownload.with().callbackDispatcher();
        final DownloadCache cache = mock(DownloadCache.class);
        final DownloadListener listener = mock(DownloadListener.class);
        final ProcessFileStrategy fileStrategy = okDownload.processFileStrategy();
        final IOException iOException = mock(IOException.class);
        final MultiPointOutputStream multiPointOutputStream = mock(MultiPointOutputStream.class);

        doNothing().when(call).start(eq(cache), eq(info));
        doReturn(mock(BreakpointRemoteCheck.class)).when(call).createRemoteCheck(eq(info));
        doReturn(mock(BreakpointLocalCheck.class)).when(call).createLocalCheck(eq(info), anyLong());

        doReturn(info).when(store).createAndInsert(eq(task));
        doReturn(listener).when(dispatcher).dispatch();
        doReturn(cache).when(call).createCache(eq(info));
        when(cache.getRealCause()).thenReturn(iOException);
        when(cache.getOutputStream()).thenReturn(multiPointOutputStream);


        call.canceled = true;
        call.execute();
        assertThat(call.finishing).isTrue();
        // no callback when cancel status, because it has been handled on the cancel operation.
        verify(listener, never()).taskEnd(any(DownloadTask.class), any(EndCause.class),
                nullable(Exception.class));

        call.canceled = false;
        call.execute();
        verify(listener).taskEnd(eq(task), eq(EndCause.COMPLETED), nullable(IOException.class));
        verify(store)
                .onTaskEnd(eq(task.getId()), eq(EndCause.COMPLETED), nullable(Exception.class));
        final int id = task.getId();
        verify(store).markFileClear(eq(id));
        verify(fileStrategy).completeProcessStream(eq(multiPointOutputStream), eq(task));

        when(cache.isPreAllocateFailed()).thenReturn(true);
        call.execute();
        verify(listener).taskEnd(task, EndCause.PRE_ALLOCATE_FAILED, iOException);

        when(cache.isFileBusyAfterRun()).thenReturn(true);
        call.execute();
        verify(listener).taskEnd(task, EndCause.FILE_BUSY, null);

        when(cache.isServerCanceled()).thenReturn(true);
        call.execute();
        verify(listener).taskEnd(task, EndCause.ERROR, iOException);


        when(cache.isUserCanceled()).thenReturn(false);
        when(cache.isServerCanceled()).thenReturn(false);
        when(cache.isUnknownError()).thenReturn(true);
        call.execute();
        verify(listener, times(2)).taskEnd(task, EndCause.ERROR, iOException);
    }

    @Test
    public void finished_callToDispatch() {
        call.finished();

        verify(OkDownload.with().downloadDispatcher()).finish(call);
    }

    @Test
    public void compareTo() {
        final DownloadCall compareCall = mock(DownloadCall.class);
        when(compareCall.getPriority()).thenReturn(6);
        when(call.getPriority()).thenReturn(3);

        final int result = call.compareTo(compareCall);
        assertThat(result).isEqualTo(3);
    }

    @Test
    public void start() throws InterruptedException {
        final DownloadCache cache = mock(DownloadCache.class);
        final MultiPointOutputStream outputStream = mock(MultiPointOutputStream.class);
        doNothing().when(call).startBlocks(ArgumentMatchers.<DownloadChain>anyList());
        when(cache.getOutputStream()).thenReturn(outputStream);

        call.start(cache, info);
        ArgumentCaptor<List<DownloadChain>> captor = ArgumentCaptor.forClass(List.class);
        verify(call).startBlocks(captor.capture());

        final List<DownloadChain> chainList = captor.getValue();
        assertThat(chainList.size()).isEqualTo(3);
        assertThat(chainList.get(0).getBlockIndex()).isEqualTo(0);
        assertThat(chainList.get(1).getBlockIndex()).isEqualTo(1);
        assertThat(chainList.get(2).getBlockIndex()).isEqualTo(2);

        verify(outputStream).setCurrentNeedFetchBlockCount(chainList.size());
    }

    @Test
    public void startBlocks() throws InterruptedException {
        ArrayList<DownloadChain> runningBlockList = spy(new ArrayList<DownloadChain>());
        call = spy(new DownloadCall(task, false, runningBlockList, store));

        final Future mockFuture = mock(Future.class);
        doReturn(mockFuture).when(call).submitChain(any(DownloadChain.class));

        List<DownloadChain> chains = new ArrayList<>();
        chains.add(mock(DownloadChain.class));
        chains.add(mock(DownloadChain.class));
        chains.add(mock(DownloadChain.class));

        call.startBlocks(chains);

        verify(call, times(3)).submitChain(any(DownloadChain.class));
        verify(runningBlockList).addAll(eq(chains));
        verify(runningBlockList).removeAll(eq(chains));
    }

    @Test
    public void cancel() {
        assertThat(call.cancel()).isTrue();
        // canceled
        assertThat(call.cancel()).isFalse();
    }

    @Test
    public void cancel_cache() {
        final DownloadCache cache = mock(DownloadCache.class);
        final MultiPointOutputStream outputStream = mock(MultiPointOutputStream.class);
        call.cache = cache;
        when(cache.getOutputStream()).thenReturn(outputStream);
        call.cancel();

        verify(call.cache).setUserCanceled();
        verify(outputStream).cancelAsync();
    }

    @Test
    public void cancel_finishing() {
        call.finishing = true;
        assertThat(call.cancel()).isFalse();
        final DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();
        verify(dispatcher, never()).flyingCanceled(eq(call));
    }

    @Test
    public void assembleBlockAndCallbackFromBeginning() {
        final BreakpointRemoteCheck remoteCheck = mock(BreakpointRemoteCheck.class);
        final ResumeFailedCause failedCause = mock(ResumeFailedCause.class);

        call.assembleBlockAndCallbackFromBeginning(info, remoteCheck, failedCause);

        final DownloadListener listener = OkDownload.with().callbackDispatcher().dispatch();
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(failedCause));
    }
}