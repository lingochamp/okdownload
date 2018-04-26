/*
 * Copyright (c) 2018 LingoChamp Inc.
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

import android.support.annotation.NonNull;

import com.liulishuo.okdownload.core.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Because of we cancel call asynchronous like
 * {@link MultiPointOutputStream#cancel()} so we have to know whether the
 * same store file is locked or not before the following call with the same store file.
 */
public class FileLock {

    private static final String TAG = "FileLock";

    @NonNull private final Map<String, AtomicInteger> fileLockCountMap;
    @NonNull private final Map<String, Thread> waitThreadForFileLockMap;

    FileLock(@NonNull Map<String, AtomicInteger> fileLockCountMap,
             @NonNull Map<String, Thread> waitThreadForFileLockMap) {
        this.fileLockCountMap = fileLockCountMap;
        this.waitThreadForFileLockMap = waitThreadForFileLockMap;
    }

    FileLock() {
        this(new HashMap<String, AtomicInteger>(), new HashMap<String, Thread>());
    }

    public void increaseLock(@NonNull String path) {
        AtomicInteger lockCount;
        synchronized (fileLockCountMap) {
            lockCount = fileLockCountMap.get(path);
        }
        if (lockCount == null) {
            lockCount = new AtomicInteger(0);
            synchronized (fileLockCountMap) {
                fileLockCountMap.put(path, lockCount);
            }
        }
        Util.d(TAG, "increaseLock increase lock-count to " + lockCount.incrementAndGet() + path);
    }

    public void decreaseLock(@NonNull String path) {
        AtomicInteger lockCount;
        synchronized (fileLockCountMap) {
            lockCount = fileLockCountMap.get(path);
        }

        if (lockCount != null && lockCount.decrementAndGet() == 0) {
            Util.d(TAG, "decreaseLock decrease lock-count to 0 " + path);
            final Thread lockedThread;
            synchronized (waitThreadForFileLockMap) {
                lockedThread = waitThreadForFileLockMap.get(path);
                if (lockedThread != null) {
                    waitThreadForFileLockMap.remove(path);
                }
            }

            if (lockedThread != null) {
                Util.d(TAG, "decreaseLock " + path + " unpark locked thread " + lockCount);
                unpark(lockedThread);
            }
            synchronized (fileLockCountMap) {
                fileLockCountMap.remove(path);
            }
        }
    }

    private static final long WAIT_RELEASE_LOCK_NANO = TimeUnit.MILLISECONDS.toNanos(100);

    public void waitForRelease(@NonNull String filePath) {
        AtomicInteger lockCount;
        synchronized (fileLockCountMap) {
            lockCount = fileLockCountMap.get(filePath);
        }
        if (lockCount == null || lockCount.get() <= 0) return;

        synchronized (waitThreadForFileLockMap) {
            waitThreadForFileLockMap.put(filePath, Thread.currentThread());
        }
        Util.d(TAG, "waitForRelease start " + filePath);
        while (true) {
            if (isNotLocked(lockCount)) break;
            park();
        }
        Util.d(TAG, "waitForRelease finish " + filePath);
    }

    // convenient for test
    boolean isNotLocked(AtomicInteger lockCount) {
        return lockCount.get() <= 0;
    }

    // convenient for test
    void park() {
        LockSupport.park(WAIT_RELEASE_LOCK_NANO);
    }

    // convenient for test
    void unpark(@NonNull Thread lockedThread) {
        LockSupport.unpark(lockedThread);
    }
}
