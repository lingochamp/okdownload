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

package com.liulishuo.okdownload.core.listener;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

/**
 * taskStart->connect->progress<-->progress(currentOffset)->taskEnd
 */
public abstract class DownloadListener1 implements DownloadListener {
    private boolean start;
    private boolean fromResume;

    private volatile boolean firstConnectEnd;

    private int blockCount;
    protected long totalLength;
    private final AtomicLong currentOffset;

    public DownloadListener1() {
        currentOffset = new AtomicLong();
    }

    protected abstract void connected(DownloadTask task, int blockCount, long currentOffset,
                                      long totalLength);

    protected abstract void progress(DownloadTask task, long currentOffset);

    protected abstract void retry(DownloadTask task, @NonNull ResumeFailedCause cause);

    @Override public void breakpointData(DownloadTask task, @Nullable BreakpointInfo info) {
    }

    @Override public void downloadFromBeginning(DownloadTask task, BreakpointInfo info,
                                                ResumeFailedCause cause) {
        if (start) {
            retry(task, cause);
        }

        blockCount = 0;
        totalLength = 0;
        currentOffset.set(0);

        start = true;
        fromResume = false;
        firstConnectEnd = true;

    }

    @Override public void downloadFromBreakpoint(DownloadTask task, BreakpointInfo info) {
        blockCount = info.getBlockCount();
        totalLength = info.getTotalLength();
        currentOffset.set(info.getTotalOffset());

        firstConnectEnd = true;
        start = true;
        fromResume = true;
    }

    @Override public void connectStart(DownloadTask task, int blockIndex,
                                       @NonNull Map<String, List<String>> requestHeaderFields) {
    }

    @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                     @NonNull Map<String, List<String>> responseHeaderFields) {
        if (fromResume && firstConnectEnd) {
            firstConnectEnd = false;
            // from break point.
            connected(task, blockCount, currentOffset.get(), totalLength);
        }
    }

    @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        blockCount = info.getBlockCount();
        totalLength = info.getTotalLength();
        currentOffset.set(info.getTotalOffset());

        // if not from resume we get info after block end, so callback on here.
        connected(task, blockCount, currentOffset.get(), totalLength);
    }

    @Override public void fetchStart(DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        currentOffset.addAndGet(increaseBytes);
        progress(task, currentOffset.get());
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex, long contentLength) {
    }
}

