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

package com.liulishuo.filedownloader.retry;

import android.support.annotation.NonNull;
import com.liulishuo.okdownload.DownloadTask;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryAssist {

    @NonNull
    final AtomicInteger retriedTimes;
    final int retryTimes;

    public RetryAssist(int retryTimes) {
        this.retryTimes = retryTimes;
        this.retriedTimes = new AtomicInteger(0);
    }

    public void doRetry(@NonNull DownloadTask task) {
        final int retryingTime = retriedTimes.incrementAndGet();
        if (retryingTime > retryTimes) {
            throw new RuntimeException("retry has exceeded limit");
        }
        task.enqueue(task.getListener());
    }

    public boolean canRetry() {
        return  retriedTimes.get() < retryTimes;
    }

    public int getRetriedTimes() {
        return retriedTimes.get();
    }
}
