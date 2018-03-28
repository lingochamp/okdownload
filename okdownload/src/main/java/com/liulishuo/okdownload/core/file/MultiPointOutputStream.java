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
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.exception.PreAllocateException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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
    private final BreakpointStore store;
    private final boolean supportSeek;
    private final boolean isPreAllocateLength;

    volatile boolean syncRunning;

    public MultiPointOutputStream(@NonNull DownloadTask task,
                                  @NonNull BreakpointInfo info) {
        this.task = task;
        this.flushBufferSize = task.getFlushBufferSize();
        this.syncBufferSize = task.getSyncBufferSize();
        this.syncBufferIntervalMills = task.getSyncBufferIntervalMills();
        this.info = info;

        this.store = OkDownload.with().breakpointStore();
        this.supportSeek = OkDownload.with().outputStreamFactory().supportSeek();
        this.isPreAllocateLength = OkDownload.with().processFileStrategy().isPreAllocateLength();
    }

    public void write(int blockIndex, byte[] bytes, int length) throws IOException {

        outputStream(blockIndex).write(bytes, 0, length);

        // because we add the length value after flush and sync,
        // so the length only possible less than or equal to the real persist length.
        allNoSyncLength.addAndGet(length);
        noSyncLengthMap.get(blockIndex).addAndGet(length);

        inspectAndPersist();
    }

    private final ArrayList<Thread> parkThreadList = new ArrayList<>();
    private static final long WAIT_SYNC_NANO = TimeUnit.MILLISECONDS.toNanos(100);

    public void ensureSyncComplete(int blockIndex) {
        final AtomicLong noSyncLength = noSyncLengthMap.get(blockIndex);
        if (noSyncLength != null && noSyncLength.get() > 0) {
            // sync to store
            if (syncRunning) {
                // wait for sync
                synchronized (parkThreadList) {
                    parkThreadList.add(Thread.currentThread());
                }
                while (true) {
                    LockSupport.parkNanos(WAIT_SYNC_NANO);
                    if (!syncRunning) break;
                }
            }

            // sync once, make sure data has been synced.
            syncRunning = true;
            syncRunnable.run();
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

    private void inspectAndPersist() {
        if (!syncRunning && isNeedPersist()) {
            syncRunning = true;
            FILE_IO_EXECUTOR.execute(syncRunnable);
        }
    }

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
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
                        // so the length only possible less than or equal to the real persist length.
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
                final Thread[] parkThreadArray = new Thread[parkThreadList.size()];
                parkThreadList.toArray(parkThreadArray);
                for (Thread thread : parkThreadArray) {
                    if (thread == null) break; // on end.

                    LockSupport.unpark(thread);
                    synchronized (parkThreadList) {
                        parkThreadList.remove(thread);
                    }
                }
            }
        }
    };

    private boolean isNeedPersist() {
        return allNoSyncLength.get() >= syncBufferSize
                && SystemClock.uptimeMillis() - lastSyncTimestamp.get() >= syncBufferIntervalMills;
    }

    public void close(int blockIndex) throws IOException {
        outputStream(blockIndex).close();
    }

    private volatile boolean firstOutputStream = true;

    synchronized DownloadOutputStream outputStream(int blockIndex) throws IOException {

        @NonNull final Uri uri;
        final boolean isFileScheme = task.getUri().getScheme().equals("file");
        if (isFileScheme) {
            final String path = task.getPath();
            if (path == null) throw new FileNotFoundException("Filename is not ready!");
            final File file = new File(path);

            final File parentFile = file.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                throw new IOException("Create parent folder failed!");
            }

            if (file.createNewFile()) {
                Util.d(TAG, "Create new file: " + file.getName());
            }

            if (task.isUriIsDirectory()) {
                uri = Uri.fromFile(file);
            } else {
                uri = task.getUri();
            }
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
                    final String path = task.getPath();
                    final long requireSpace = totalLength - new File(path).length();
                    if (requireSpace > 0) {
                        inspectFreeSpace(path, requireSpace);
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
}
