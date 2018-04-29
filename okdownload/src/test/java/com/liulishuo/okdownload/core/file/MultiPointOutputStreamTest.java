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

package com.liulishuo.okdownload.core.file;

import android.content.Context;
import android.net.Uri;
import android.os.StatFs;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.exception.PreAllocateException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class) // for sparseArray.
@Config(manifest = NONE)
public class MultiPointOutputStreamTest {

    private MultiPointOutputStream multiPointOutputStream;

    private final String parentPath = "./p-path/";
    private final File existFile = new File("./p-path/filename");
    @Mock private BreakpointInfo info;
    @Mock private DownloadTask task;
    @Mock private DownloadStore store;
    @Mock private Runnable syncRunnable;

    @Mock private DownloadOutputStream stream0;
    @Mock private DownloadOutputStream stream1;
    @Mock private Future syncFuture;
    @Mock private Thread runSyncThread;

    final byte[] bytes = new byte[6];

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() {
        when(OkDownload.with().context()).thenReturn(application);
        initMocks(this);
        when(task.getFile()).thenReturn(existFile);
        when(task.getParentFile()).thenReturn(new File(parentPath));
        multiPointOutputStream = spy(new MultiPointOutputStream(task, info, store, syncRunnable));
        doReturn(syncFuture).when(multiPointOutputStream).executeSyncRunnableAsync();
        multiPointOutputStream.syncFuture = syncFuture;
        multiPointOutputStream.runSyncThread = runSyncThread;
    }

    @After
    public void tearDown() {
        final File file = existFile;
        if (file.exists()) {
            file.delete();
        }
        if (file.getParentFile().exists()) {
            file.getParentFile().delete();
        }
    }

    @Test
    public void write() throws IOException {
        doReturn(stream0).when(multiPointOutputStream).outputStream(anyInt());
        doNothing().when(multiPointOutputStream).inspectAndPersist();

        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong());
        multiPointOutputStream.noSyncLengthMap.put(2, new AtomicLong());

        multiPointOutputStream.write(2, bytes, 16);

        verify(stream0).write(eq(bytes), eq(0), eq(16));

        assertThat(multiPointOutputStream.allNoSyncLength.get()).isEqualTo(16);
        assertThat(multiPointOutputStream.noSyncLengthMap.get(1).get()).isEqualTo(0);
        assertThat(multiPointOutputStream.noSyncLengthMap.get(2).get()).isEqualTo(16);

        verify(multiPointOutputStream).inspectAndPersist();
    }

    @Test
    public void cancel_syncNotRun() throws IOException {
        multiPointOutputStream.outputStreamMap.put(0, stream0);
        multiPointOutputStream.outputStreamMap.put(1, stream1);
        multiPointOutputStream.allNoSyncLength.set(1);
        doNothing().when(multiPointOutputStream).close(anyInt());
        doNothing().when(multiPointOutputStream).ensureSync(true, -1);
        multiPointOutputStream.syncFuture = null;

        final ProcessFileStrategy strategy = OkDownload.with().processFileStrategy();
        final FileLock fileLock = mock(FileLock.class);
        when(strategy.getFileLock()).thenReturn(fileLock);

        multiPointOutputStream.cancel();

        assertThat(multiPointOutputStream.noMoreStreamList).containsExactly(0, 1);
        verify(multiPointOutputStream, never()).ensureSync(eq(true), eq(-1));
        verify(multiPointOutputStream).close(eq(0));
        verify(multiPointOutputStream).close(eq(1));
        verify(fileLock, never()).increaseLock(eq(existFile.getAbsolutePath()));
        verify(fileLock, never()).decreaseLock(eq(existFile.getAbsolutePath()));
        verify(store).onTaskEnd(eq(task.getId()), eq(EndCause.CANCELED), nullable(Exception.class));
    }

    @Test
    public void cancel() throws IOException {
        multiPointOutputStream.outputStreamMap.put(0, stream0);
        multiPointOutputStream.outputStreamMap.put(1, stream1);
        multiPointOutputStream.noSyncLengthMap.put(0, new AtomicLong());
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong());
        multiPointOutputStream.allNoSyncLength.set(1);

        final ProcessFileStrategy strategy = OkDownload.with().processFileStrategy();
        final FileLock fileLock = mock(FileLock.class);
        when(strategy.getFileLock()).thenReturn(fileLock);

        doNothing().when(multiPointOutputStream).close(anyInt());
        doNothing().when(multiPointOutputStream).ensureSync(true, -1);

        multiPointOutputStream.cancel();

        assertThat(multiPointOutputStream.noMoreStreamList).containsExactly(0, 1);
        verify(multiPointOutputStream).ensureSync(eq(true), eq(-1));
        verify(multiPointOutputStream).close(eq(0));
        verify(multiPointOutputStream).close(eq(1));
        verify(fileLock).increaseLock(eq(existFile.getAbsolutePath()));
        verify(fileLock).decreaseLock(eq(existFile.getAbsolutePath()));
        verify(store).onTaskEnd(eq(task.getId()), eq(EndCause.CANCELED), nullable(Exception.class));
    }

    @Test
    public void ensureSync_syncJobNotRunYet() {
        multiPointOutputStream.syncFuture = null;
        multiPointOutputStream.ensureSync(true, -1);
        verify(multiPointOutputStream, never()).unparkThread(any(Thread.class));
        verify(multiPointOutputStream, never()).parkThread();
        assertThat(multiPointOutputStream.parkedRunBlockThreadMap.size()).isZero();

        multiPointOutputStream.syncFuture = syncFuture;
        when(syncFuture.isDone()).thenReturn(true);
        multiPointOutputStream.ensureSync(true, -1);
        verify(multiPointOutputStream, never()).unparkThread(any(Thread.class));
        verify(multiPointOutputStream, never()).parkThread();
        assertThat(multiPointOutputStream.parkedRunBlockThreadMap.size()).isZero();
    }

    @Test
    public void ensureSync_noMoreStream() throws ExecutionException, InterruptedException {
        doNothing().when(multiPointOutputStream).unparkThread(nullable(Thread.class));
        doNothing().when(multiPointOutputStream).parkThread();
        doNothing().when(multiPointOutputStream).parkThread(25);

        multiPointOutputStream.ensureSync(true, -1);

        verify(multiPointOutputStream, times(2)).unparkThread(eq(runSyncThread));
        verify(syncFuture).get();
        verify(multiPointOutputStream, never()).parkThread();
        assertThat(multiPointOutputStream.parkedRunBlockThreadMap.size()).isZero();
    }

    @Test
    public void ensureSync_notNoMoreStream() {
        doNothing().when(multiPointOutputStream).unparkThread(nullable(Thread.class));
        doNothing().when(multiPointOutputStream).parkThread();
        doNothing().when(multiPointOutputStream).parkThread(25);

        multiPointOutputStream.ensureSync(false, 1);

        verify(multiPointOutputStream).unparkThread(eq(runSyncThread));
        verify(multiPointOutputStream).parkThread();
        assertThat(multiPointOutputStream.parkedRunBlockThreadMap.size()).isOne();
        assertThat(multiPointOutputStream.parkedRunBlockThreadMap.get(1))
                .isEqualTo(Thread.currentThread());
    }

    @Test
    public void ensureSync_loop() {
        doNothing().when(multiPointOutputStream).unparkThread(nullable(Thread.class));
        doNothing().when(multiPointOutputStream).parkThread();
        doNothing().when(multiPointOutputStream).parkThread(25);
        multiPointOutputStream.runSyncThread = null;
        when(multiPointOutputStream.isRunSyncThreadValid()).thenReturn(false, true);

        multiPointOutputStream.ensureSync(false, 1);

        verify(multiPointOutputStream).parkThread(eq(25L));
        verify(multiPointOutputStream).unparkThread(nullable(Thread.class));
    }

    @Test
    public void done_noMoreStream() throws IOException {
        doNothing().when(multiPointOutputStream).close(1);
        doNothing().when(multiPointOutputStream).ensureSync(true, 1);
        doNothing().when(multiPointOutputStream)
                .inspectStreamState(multiPointOutputStream.doneState);
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong(10));
        multiPointOutputStream.doneState.isNoMoreStream = true;

        multiPointOutputStream.done(1);

        assertThat(multiPointOutputStream.noMoreStreamList).containsExactly(1);
        verify(multiPointOutputStream).ensureSync(eq(true), eq(1));
        verify(multiPointOutputStream).close(eq(1));
    }

    @Test
    public void done_notNoMoreStream() throws IOException {
        doNothing().when(multiPointOutputStream).close(1);
        doNothing().when(multiPointOutputStream).ensureSync(false, 1);
        doNothing().when(multiPointOutputStream)
                .inspectStreamState(multiPointOutputStream.doneState);
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong(10));
        multiPointOutputStream.doneState.isNoMoreStream = false;

        multiPointOutputStream.done(1);

        assertThat(multiPointOutputStream.noMoreStreamList).containsExactly(1);
        verify(multiPointOutputStream).ensureSync(eq(false), eq(1));
        verify(multiPointOutputStream).close(eq(1));
    }

    @Test(expected = IOException.class)
    public void done_syncException() throws IOException {
        multiPointOutputStream.syncException = new IOException();
        multiPointOutputStream.done(1);
    }

    @Test
    public void done_syncNotRun() throws IOException {
        doNothing().when(multiPointOutputStream).close(1);
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong(10));
        multiPointOutputStream.syncFuture = null;

        multiPointOutputStream.done(1);

        assertThat(multiPointOutputStream.noMoreStreamList).containsExactly(1);
        verify(multiPointOutputStream, never()).ensureSync(eq(false), eq(1));
        verify(multiPointOutputStream).close(eq(1));
    }

    @Test
    public void runSyncDelayException() throws IOException {
        final IOException exception = mock(IOException.class);
        doThrow(exception).when(multiPointOutputStream).runSync();

        multiPointOutputStream.runSyncDelayException();
        assertThat(multiPointOutputStream.syncException).isEqualTo(exception);
    }

    @Test
    public void runSync() throws IOException {
        final MultiPointOutputStream.StreamsState state =
                spy(new MultiPointOutputStream.StreamsState());
        multiPointOutputStream.state = state;

        when(state.isStreamsEndOrChanged()).thenReturn(false, false, false, true);
        doNothing().when(multiPointOutputStream).parkThread(anyLong());
        doNothing().when(multiPointOutputStream).unparkThread(any(Thread.class));
        doNothing().when(multiPointOutputStream).flushProcess();
        doNothing().when(multiPointOutputStream).inspectStreamState(state);

        final Thread thread0 = mock(Thread.class);
        final Thread thread1 = mock(Thread.class);
        multiPointOutputStream.parkedRunBlockThreadMap.put(0, thread0);
        multiPointOutputStream.parkedRunBlockThreadMap.put(1, thread1);
        multiPointOutputStream.allNoSyncLength.set(1);
        state.newNoMoreStreamBlockList.add(0);
        state.newNoMoreStreamBlockList.add(1);
        state.isNoMoreStream = true;

        when(multiPointOutputStream.isNoNeedFlushForLength()).thenReturn(true, false);
        when(multiPointOutputStream.getNextParkMillisecond()).thenReturn(1L, -1L);

        multiPointOutputStream.runSync();

        // first default + scheduler one + last one.
        verify(multiPointOutputStream, times(3)).flushProcess();
        assertThat(multiPointOutputStream.parkedRunBlockThreadMap.size()).isZero();
        verify(multiPointOutputStream).unparkThread(eq(thread0));
        verify(multiPointOutputStream).unparkThread(eq(thread1));
    }

    @Test
    public void isNoNeedFlushForLength() {
        multiPointOutputStream.allNoSyncLength.set(0);
        // syncBufferSize is 0.
        assertThat(multiPointOutputStream.isNoNeedFlushForLength()).isFalse();
        multiPointOutputStream.allNoSyncLength.set(-1);
        assertThat(multiPointOutputStream.isNoNeedFlushForLength()).isTrue();
    }

    @Test
    public void getNextParkMillisecond() {
        // syncBufferIntervalMills is 0.
        when(multiPointOutputStream.now()).thenReturn(2L);
        multiPointOutputStream.lastSyncTimestamp.set(1L);

        assertThat(multiPointOutputStream.getNextParkMillisecond()).isEqualTo(-1L);
    }

    @Test
    public void flushProcess() throws IOException {
        final DownloadOutputStream outputStream = mock(DownloadOutputStream.class);
        doReturn(outputStream).when(multiPointOutputStream).outputStream(1);
        when(info.getBlock(1)).thenReturn(mock(BlockInfo.class));

        multiPointOutputStream.allNoSyncLength.addAndGet(10);
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong(10));
        multiPointOutputStream.outputStreamMap.put(1, mock(DownloadOutputStream.class));


        multiPointOutputStream.flushProcess();

        verify(store).onSyncToFilesystemSuccess(info, 1, 10);
        assertThat(multiPointOutputStream.allNoSyncLength.get()).isZero();
        assertThat(multiPointOutputStream.noSyncLengthMap.get(1).get()).isZero();
    }

    @Test
    public void inspectAndPersist() throws IOException {
        final Future newFuture = mock(Future.class);
        doReturn(newFuture).when(multiPointOutputStream).executeSyncRunnableAsync();

        multiPointOutputStream.syncFuture = syncFuture;
        multiPointOutputStream.inspectAndPersist();
        // not change
        assertThat(multiPointOutputStream.syncFuture).isEqualTo(syncFuture);

        multiPointOutputStream.syncFuture = null;
        multiPointOutputStream.inspectAndPersist();
        // changed
        assertThat(multiPointOutputStream.syncFuture).isEqualTo(newFuture);
    }

    @Test(expected = IOException.class)
    public void inspectAndPersist_syncException() throws IOException {
        multiPointOutputStream.syncException = new IOException();
        multiPointOutputStream.inspectAndPersist();
    }

    @Test(expected = IOException.class)
    public void inspectComplete_notFull() throws IOException {
        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(1)).thenReturn(blockInfo);

        when(blockInfo.getContentLength()).thenReturn(9L);
        when(blockInfo.getCurrentOffset()).thenReturn(10L);

        multiPointOutputStream.inspectComplete(1);
    }

    @Test(expected = IOException.class)
    public void inspectComplete_syncException() throws IOException {
        multiPointOutputStream.syncException = new IOException();
        multiPointOutputStream.inspectAndPersist();
    }

    @Test
    public void outputStream() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(10L);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.isChunked()).thenReturn(false);

        assertThat(multiPointOutputStream.outputStreamMap.get(0)).isNull();
        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);

        assertThat(outputStream).isNotNull();
        assertThat(multiPointOutputStream.outputStreamMap.get(0)).isEqualTo(outputStream);
        verify(outputStream).seek(eq(10L));
        verify(outputStream).setLength(eq(20L));
    }


    @Test
    public void close_noExist() throws IOException {
        final DownloadOutputStream stream0 = mock(DownloadOutputStream.class);
        final DownloadOutputStream stream1 = mock(DownloadOutputStream.class);
        multiPointOutputStream.outputStreamMap.put(0, stream0);
        multiPointOutputStream.outputStreamMap.put(1, stream1);

        multiPointOutputStream.close(2);

        verify(stream0, never()).close();
        verify(stream1, never()).close();
        assertThat(multiPointOutputStream.outputStreamMap.size()).isEqualTo(2);
    }

    @Test
    public void close() throws IOException {
        final DownloadOutputStream stream0 = mock(DownloadOutputStream.class);
        final DownloadOutputStream stream1 = mock(DownloadOutputStream.class);
        multiPointOutputStream.outputStreamMap.put(0, stream0);
        multiPointOutputStream.outputStreamMap.put(1, stream1);

        multiPointOutputStream.close(1);

        verify(stream0, never()).close();
        verify(stream1).close();
        assertThat(multiPointOutputStream.outputStreamMap.size()).isEqualTo(1);
        assertThat(multiPointOutputStream.outputStreamMap.get(0)).isEqualTo(stream0);
    }

    @Test
    public void inspectStreamState() {
        final MultiPointOutputStream.StreamsState state = new MultiPointOutputStream.StreamsState();
        multiPointOutputStream.outputStreamMap.put(0, stream0);
        multiPointOutputStream.outputStreamMap.put(1, stream0);

        // no noMoreStreamList
        multiPointOutputStream.inspectStreamState(state);
        assertThat(state.isNoMoreStream).isFalse();
        assertThat(state.noMoreStreamBlockList).isEmpty();
        assertThat(state.newNoMoreStreamBlockList).isEmpty();

        // 1 noMoreStreamList
        multiPointOutputStream.noMoreStreamList.add(1);
        multiPointOutputStream.inspectStreamState(state);
        assertThat(state.isNoMoreStream).isFalse();
        assertThat(state.noMoreStreamBlockList).containsExactly(1);
        assertThat(state.newNoMoreStreamBlockList).containsExactly(1);

        // 1,0 noMoreStreamList
        multiPointOutputStream.noMoreStreamList.add(0);
        multiPointOutputStream.inspectStreamState(state);
        assertThat(state.isNoMoreStream).isTrue();
        assertThat(state.noMoreStreamBlockList).containsExactly(1, 0);
        assertThat(state.newNoMoreStreamBlockList).containsExactly(0);

        // 1,0 noMoreStreamList again
        multiPointOutputStream.inspectStreamState(state);
        assertThat(state.isNoMoreStream).isTrue();
        assertThat(state.noMoreStreamBlockList).containsExactly(1, 0);
        assertThat(state.newNoMoreStreamBlockList).isEmpty();
    }

    @Test
    public void outputStream_contain_returnDirectly() throws IOException {
        final DownloadOutputStream outputStream = mock(DownloadOutputStream.class);
        multiPointOutputStream.outputStreamMap.put(1, outputStream);
        assertThat(multiPointOutputStream.outputStream(1)).isEqualTo(outputStream);
    }

    @Test
    public void outputStream_rangeLeft0_noSeek() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(0L);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.isChunked()).thenReturn(false);

        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);
        verify(outputStream, never()).seek(anyLong());
        verify(outputStream).setLength(eq(20L));
    }

    @Test
    public void outputStream_chunked_noPreAllocate() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(0L);
        when(info.isChunked()).thenReturn(true);

        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);
        verify(outputStream, never()).seek(anyLong());
        verify(outputStream, never()).setLength(anyLong());
    }

    @Test
    public void outputStream_nonFileScheme() throws IOException {
        prepareOutputStreamEnv();

        final Uri uri = task.getUri();
        when(uri.getScheme()).thenReturn("content");

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(0L);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.isChunked()).thenReturn(false);

        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);
        verify(outputStream, never()).seek(anyLong());
        verify(outputStream).setLength(eq(20L));
        verify(multiPointOutputStream, never()).inspectFreeSpace(any(StatFs.class), anyLong());
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void inspectFreeSpace_freeSpaceNotEnough() throws PreAllocateException {
        final StatFs statFs = mock(StatFs.class);
        when(statFs.getAvailableBlocks()).thenReturn(1);
        when(statFs.getBlockSize()).thenReturn(2);

        thrown.expectMessage("There is Free space less than Require space: 2 < 3");
        thrown.expect(PreAllocateException.class);
        multiPointOutputStream.inspectFreeSpace(statFs, 3);
    }

    @Test
    public void inspectFreeSpace() throws PreAllocateException {
        final StatFs statFs = mock(StatFs.class);
        when(statFs.getAvailableBlocks()).thenReturn(1);
        when(statFs.getBlockSize()).thenReturn(2);

        multiPointOutputStream.inspectFreeSpace(statFs, 2);
    }

    private void prepareOutputStreamEnv() throws FileNotFoundException, PreAllocateException {
        when(OkDownload.with().outputStreamFactory().supportSeek()).thenReturn(true);
        when(OkDownload.with().processFileStrategy().isPreAllocateLength(task)).thenReturn(true);
        when(OkDownload.with().outputStreamFactory().create(any(Context.class), any(Uri.class),
                anyInt())).thenReturn(mock(DownloadOutputStream.class));
        // recreate for new values of support-seek and pre-allocate-length.
        multiPointOutputStream = spy(new MultiPointOutputStream(task, info, store));
        doNothing().when(multiPointOutputStream).inspectFreeSpace(any(StatFs.class), anyLong());

        final Uri uri = mock(Uri.class);
        when(task.getUri()).thenReturn(uri);
        when(uri.getScheme()).thenReturn("file");
    }
}