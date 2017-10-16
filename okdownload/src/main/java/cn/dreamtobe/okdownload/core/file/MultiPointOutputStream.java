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

package cn.dreamtobe.okdownload.core.file;

import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;

public class MultiPointOutputStream {
    private static final String TAG = "MultiPointOutputStream";
    private static final ExecutorService FILE_IO_EXECUTOR = new ThreadPoolExecutor(0,
            Integer.MAX_VALUE,
            60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload file io", false));

    private SparseArray<DownloadOutputStream> outputStreamMap = new SparseArray<>();

    private SparseArray<AtomicLong> noSyncLengthMap = new SparseArray<>();
    private AtomicLong allNoSyncLength = new AtomicLong();
    private AtomicLong lastSyncTimestamp = new AtomicLong();

    private final int flushBufferSize;
    private final int syncBufferSize;
    private final int syncBufferIntervalMills;
    private final BreakpointInfo info;
    private final DownloadTask task;
    private final BreakpointStore store;
    private final boolean supportSeek;
    private final boolean isPreAllocateLength;

    private boolean syncRunning;

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

    private List<Thread> parkThreadList = new ArrayList<>();
    private static final long WAIT_SYNC_NANO = TimeUnit.MILLISECONDS.toNanos(100);

    public void interceptComplete(int blockIndex) throws IOException {
        final BlockInfo blockInfo = info.getBlock(blockIndex);

        final AtomicLong noSyncLength = noSyncLengthMap.get(blockIndex);
        if (noSyncLength.get() > 0) {
            // sync to store
            if (syncRunning) {
                // wait for sync
                parkThreadList.add(Thread.currentThread());
                while (true) {
                    LockSupport.parkNanos(WAIT_SYNC_NANO);
                    if (!syncRunning || noSyncLength.get() == 0) break;
                }

            } else {
                // sync once
                syncRunning = true;
                syncRunnable.run();
            }
        }

        if (blockInfo.isNotFull()) {
            throw new IOException("The current offset on block-info isn't update correct, "
                    + blockInfo.getCurrentOffset() + " != " + blockInfo.getContentLength());
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
            boolean success;
            final int size = outputStreamMap.size();
            SparseArray<Long> increaseLengthMap = new SparseArray<>(size);
            try {
                for (int i = 0; i < size; i++) {
                    final int blockIndex = outputStreamMap.keyAt(i);
                    // because we get no sync length value before flush and sync,
                    // so the length only possible less than or equal to the real persist length.
                    increaseLengthMap.put(blockIndex, noSyncLengthMap.get(blockIndex).get());
                    final DownloadOutputStream outputStream = outputStreamMap.valueAt(i);
                    outputStream.flushAndSync();
                }
                success = true;
            } catch (IOException ignored) {
                success = false;
            }

            if (success) {
                long allIncreaseLength = 0;
                for (int i = 0; i < size; i++) {
                    final int blockIndex = increaseLengthMap.keyAt(i);
                    final long noSyncLength = increaseLengthMap.valueAt(i);
                    store.onSyncToFilesystemSuccess(info, blockIndex, noSyncLength);
                    allIncreaseLength += noSyncLength;
                    noSyncLengthMap.get(blockIndex).addAndGet(-noSyncLength);
                }
                allNoSyncLength.addAndGet(-allIncreaseLength);
                lastSyncTimestamp.set(SystemClock.uptimeMillis());
            }

            syncRunning = false;
            for (Thread thread : parkThreadList) {
                LockSupport.unpark(thread);
            }
            parkThreadList.clear();
        }
    };

    private boolean isNeedPersist() {
        return allNoSyncLength.get() >= syncBufferSize
                && SystemClock.uptimeMillis() - lastSyncTimestamp.get() >= syncBufferIntervalMills;
    }

    public void close(int blockIndex) throws IOException {
        outputStream(blockIndex).close();
    }

    private boolean firstOutputStream;
    private synchronized DownloadOutputStream outputStream(int blockIndex) throws
            IOException {
        final String path = task.getPath();
        if (path == null) throw new FileNotFoundException("Filename is not ready!");
        final File file = new File(path);

        final File parentFile = file.getParentFile();
        if (!parentFile.exists() && file.getParentFile().mkdirs()) {
            throw new IOException("Create parent folder failed!");
        }

        if (file.createNewFile()) {
            Util.d(TAG, "Create new file: " + file.getName());
        }

        final Uri uri;
        if (task.isUriIsDirectory()) {
            uri = Uri.parse(file.toString());
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
                outputStream.seek(info.getBlock(blockIndex).getRangeLeft());
            }

            if (firstOutputStream && isPreAllocateLength) {
                outputStream.setLength(info.getTotalLength());
            }

            outputStreamMap.put(blockIndex, outputStream);
            noSyncLengthMap.put(blockIndex, new AtomicLong());
        }

        return outputStream;
    }
}
