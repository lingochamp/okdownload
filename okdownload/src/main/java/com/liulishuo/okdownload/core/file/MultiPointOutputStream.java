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

import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.exception.PreAllocateException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class MultiPointOutputStream {
    private static final String TAG = "MultiPointOutputStream";
    private static final ExecutorService FILE_IO_EXECUTOR = new ThreadPoolExecutor(0,
            Integer.MAX_VALUE,
            60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload file io", false));

    final SparseArray<DownloadOutputStream> outputStreamMap = new SparseArray<>();

    final SparseArray<AtomicLong> noSyncLengthMap = new SparseArray<>();
    final AtomicLong allNoSyncLength = new AtomicLong();
    private final AtomicLong lastSyncTimestamp = new AtomicLong();

    private final int flushBufferSize;
    private final int syncBufferSize;
    private final int syncBufferIntervalMills;
    private final BreakpointInfo info;
    private final DownloadTask task;
    private final DownloadStore store;
    private final boolean supportSeek;
    private final boolean isPreAllocateLength;

    volatile boolean syncRunning;
    @NonNull private final Runnable syncRunnable;
    private String path;

    MultiPointOutputStream(@NonNull DownloadTask task,
                           @NonNull BreakpointInfo info,
                           @NonNull DownloadStore store,
                           @Nullable Runnable syncRunnable) {
        this.task = task;
        this.flushBufferSize = task.getFlushBufferSize();
        this.syncBufferSize = task.getSyncBufferSize();
        this.syncBufferIntervalMills = task.getSyncBufferIntervalMills();
        this.info = info;

        this.store = store;
        this.supportSeek = OkDownload.with().outputStreamFactory().supportSeek();
        this.isPreAllocateLength = OkDownload.with().processFileStrategy().isPreAllocateLength();
        if (syncRunnable == null) {
            this.syncRunnable = new Runnable() {
                @Override
                public void run() {
                    runSync();
                }
            };
        } else {
            this.syncRunnable = syncRunnable;
        }

        final File file = task.getFile();
        if (file != null) this.path = file.getAbsolutePath();
    }

    public MultiPointOutputStream(@NonNull DownloadTask task,
                                  @NonNull BreakpointInfo info,
                                  @NonNull DownloadStore store) {
        this(task, info, store, null);
    }

    public void write(int blockIndex, byte[] bytes, int length) throws IOException {

        outputStream(blockIndex).write(bytes, 0, length);

        // because we add the length value after flush and sync,
        // so the length only possible less than or equal to the real persist length.
        allNoSyncLength.addAndGet(length);
        noSyncLengthMap.get(blockIndex).addAndGet(length);

        inspectAndPersist();
    }

    @Nullable volatile Thread parkForWaitingSyncThread;

    public void ensureSyncComplete(int blockIndex, boolean isAsync) {
        final AtomicLong noSyncLength = noSyncLengthMap.get(blockIndex);
        if (noSyncLength != null && noSyncLength.get() > 0) {

            if (isAsync) {
                // if async sync-data we just make sure sync one time on the current.
                if (!syncRunning) {
                    syncRunning = true;
                    inspectValidPath();
                    OkDownload.with().processFileStrategy().getFileLock().increaseLock(path);
                    executeSyncRunnableAsync();
                }
            } else {
                // if sync sync-data we need make sure all data sync to disk.
                if (syncRunning) {
                    parkForWaitingSyncThread = Thread.currentThread();
                    while (isSyncRunning()) {
                        parkThread(50);
                    }
                }

                syncRunning = true;
                syncRunnable.run();
            }

            Util.d(TAG, "sync cache to disk certainly task("
                    + task.getId() + ") block(" + blockIndex + ")");
        }
    }

    public void inspectComplete(int blockIndex) throws IOException {
        final BlockInfo blockInfo = info.getBlock(blockIndex);
        if (!Util.isCorrectFull(blockInfo.getCurrentOffset(), blockInfo.getContentLength())) {
            throw new IOException("The current offset on block-info isn't update correct, "
                    + blockInfo.getCurrentOffset() + " != " + blockInfo.getContentLength()
                    + " on " + blockIndex);
        }
    }

    void inspectAndPersist() {
        if (!syncRunning && isNeedPersist()) {
            syncRunning = true;
            executeSyncRunnableAsync();
        }
    }

    // convenient for test
    boolean isSyncRunning() {
        return syncRunning;
    }

    // convenient for test
    void parkThread(long milliseconds) {
        LockSupport.park(TimeUnit.MILLISECONDS.toNanos(milliseconds));
    }

    // convenient for test
    void unparkThread(Thread thread) {
        LockSupport.unpark(thread);
    }

    // convenient for test
    void executeSyncRunnableAsync() {
        FILE_IO_EXECUTOR.execute(syncRunnable);
    }

    // convenient for test
    void runSync() {
        try {
            syncRunning = true;
            boolean success;
            final int size;
            synchronized (noSyncLengthMap) {
                // make sure the length of noSyncLengthMap is equal to outputStreamMap
                size = noSyncLengthMap.size();
            }

            final SparseArray<Long> increaseLengthMap = new SparseArray<>(size);

            try {
                for (int i = 0; i < size; i++) {
                    final int blockIndex = outputStreamMap.keyAt(i);
                    // because we get no sync length value before flush and sync,
                    // so the length only possible less than or equal to the real persist
                    // length.
                    final long noSyncLength = noSyncLengthMap.get(blockIndex).get();
                    if (noSyncLength > 0) {
                        increaseLengthMap.put(blockIndex, noSyncLength);
                        final DownloadOutputStream outputStream = outputStreamMap.valueAt(i);
                        outputStream.flushAndSync();
                    }
                }
                success = true;
            } catch (IOException ignored) {
                success = false;
            }

            if (success) {
                final int increaseLengthSize = increaseLengthMap.size();
                long allIncreaseLength = 0;
                for (int i = 0; i < increaseLengthSize; i++) {
                    final int blockIndex = increaseLengthMap.keyAt(i);
                    final long noSyncLength = increaseLengthMap.valueAt(i);
                    store.onSyncToFilesystemSuccess(info, blockIndex, noSyncLength);
                    allIncreaseLength += noSyncLength;
                    noSyncLengthMap.get(blockIndex).addAndGet(-noSyncLength);
                }
                allNoSyncLength.addAndGet(-allIncreaseLength);
                lastSyncTimestamp.set(SystemClock.uptimeMillis());
            }
        } finally {
            syncRunning = false;
            inspectValidPath();
            OkDownload.with().processFileStrategy().getFileLock().decreaseLock(path);
            if (parkForWaitingSyncThread != null) unparkThread(parkForWaitingSyncThread);
        }
    }

    boolean isNeedPersist() {
        return allNoSyncLength.get() >= syncBufferSize
                && SystemClock.uptimeMillis() - lastSyncTimestamp.get() >= syncBufferIntervalMills;
    }

    public void close(int blockIndex) throws IOException {
        outputStream(blockIndex).close();
    }

    private volatile boolean firstOutputStream = true;

    synchronized DownloadOutputStream outputStream(int blockIndex) throws IOException {

        @NonNull final Uri uri;
        final boolean isFileScheme = Util.isUriFileScheme(task.getUri());
        if (isFileScheme) {
            final File file = task.getFile();
            if (file == null) throw new FileNotFoundException("Filename is not ready!");

            final File parentFile = task.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                throw new IOException("Create parent folder failed!");
            }

            if (file.createNewFile()) {
                Util.d(TAG, "Create new file: " + file.getName());
            }

            uri = Uri.fromFile(file);
        } else {
            uri = task.getUri();
        }

        DownloadOutputStream outputStream = outputStreamMap.get(blockIndex);
        if (outputStream == null) {
            outputStream = OkDownload.with().outputStreamFactory().create(
                    OkDownload.with().context(),
                    uri,
                    flushBufferSize);
            if (supportSeek) {
                final long seekPoint = info.getBlock(blockIndex).getRangeLeft();
                if (seekPoint > 0) {
                    // seek to target point
                    outputStream.seek(seekPoint);
                }
            }

            if (!info.isChunked() && firstOutputStream && isPreAllocateLength) {
                // pre allocate length
                final long totalLength = info.getTotalLength();
                if (isFileScheme) {
                    final File file = task.getFile();
                    final long requireSpace = totalLength - file.length();
                    if (requireSpace > 0) {
                        inspectFreeSpace(file.getAbsolutePath(), requireSpace);
                        outputStream.setLength(totalLength);
                    }
                } else {
                    outputStream.setLength(totalLength);
                }
            }

            synchronized (noSyncLengthMap) {
                // make sure the length of noSyncLengthMap is equal to outputStreamMap
                outputStreamMap.put(blockIndex, outputStream);
                noSyncLengthMap.put(blockIndex, new AtomicLong());
            }

            firstOutputStream = false;
        }

        return outputStream;
    }

    void inspectFreeSpace(String path, long requireSpace) throws PreAllocateException {
        final long freeSpace = Util.getFreeSpaceBytes(path);
        if (freeSpace < requireSpace) {
            throw new PreAllocateException(requireSpace, freeSpace);
        }
    }

    private void inspectValidPath() {
        if (path == null && task.getFile() != null) path = task.getFile().getAbsolutePath();
    }
}
