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
import android.util.SparseArray;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.util.ThreadUtil;

public class MultiPointOutputStream {
    private final static ExecutorService fileIOExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            ThreadUtil.threadFactory("OkDownload file io", false));

    private SparseArray<DownloadOutputStream> outputStreamMap = new SparseArray<>();

    private SparseArray<AtomicLong> noSyncLengthMap = new SparseArray<>();
    private AtomicLong allNoSyncLength = new AtomicLong();
    private AtomicLong lastSyncTimestamp = new AtomicLong();

    private final Uri uri;
    private final int flushBufferSize;
    private final int syncBufferSize;
    private final int syncBufferIntervalMills;
    private final BreakpointInfo info;
    private final BreakpointStore store;

    private boolean syncRunning;

    public MultiPointOutputStream(Uri uri, int flushBufferSize,
                                  int syncBufferSize, int syncBufferIntervalMills,
                                  BreakpointInfo info) {
        this.uri = uri;
        this.flushBufferSize = flushBufferSize;
        this.syncBufferSize = syncBufferSize;
        this.syncBufferIntervalMills = syncBufferIntervalMills;
        this.info = info;

        this.store = OkDownload.with().breakpointStore();
    }

    public void write(int blockIndex, byte[] bytes, int length) throws IOException {
        outputStream(blockIndex).write(bytes, 0, length);

        // because we add the length value after flush and sync,
        // so the length only possible less than or equal to the real persist length.
        allNoSyncLength.addAndGet(length);
        noSyncLengthMap.get(blockIndex).addAndGet(length);

        inspectAndPersist();
    }

    private void inspectAndPersist() {
        if (!syncRunning && isNeedPersist()) {
            syncRunning = true;
            fileIOExecutor.execute(syncRunnable);
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
        }
    };

    private boolean isNeedPersist() {
        return allNoSyncLength.get() >= syncBufferSize
                && SystemClock.uptimeMillis() - lastSyncTimestamp.get() >= syncBufferIntervalMills;
    }

    public void close(int blockIndex) throws IOException {
        outputStream(blockIndex).close();
    }

    private synchronized DownloadOutputStream outputStream(int blockIndex) throws FileNotFoundException {
        DownloadOutputStream outputStream = outputStreamMap.get(blockIndex);
        if (outputStream == null) {
            outputStream = OkDownload.with().outputStreamFactory().create(
                    OkDownload.with().context(),
                    uri,
                    flushBufferSize);
            outputStreamMap.put(blockIndex, outputStream);
            noSyncLengthMap.put(blockIndex, new AtomicLong());
        }

        return outputStream;
    }
}
