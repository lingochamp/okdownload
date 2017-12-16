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

package com.liulishuo.okdownload.core.listener;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

/**
 * When download from resume:
 * taskStart->infoReady->connectStart->connectEnd->
 * (progressBlock(blockIndex,currentOffset)->progress(currentOffset))
 * <-->
 * (progressBlock(blockIndex,currentOffset)->progress(currentOffset))
 * ->blockEnd->taskEnd
 * </p>
 * When download from beginning:
 * taskStart->connectStart->connectEnd->infoReady->
 * (progress(currentOffset)->progressBlock(blockIndex,currentOffset))
 * <-->
 * (progress(currentOffset)->progressBlock(blockIndex,currentOffset))
 * ->blockEnd->taskEnd
 */
public abstract class DownloadListener4 implements DownloadListener {
    private BreakpointInfo info;
    protected long currentOffset;
    private SparseArray<Long> blockCurrentOffsetMap;

    @Nullable protected SparseArray<Long> blockCurrentOffsetMap() { return blockCurrentOffsetMap; }

    protected abstract void infoReady(DownloadTask task, @NonNull BreakpointInfo info);

    protected abstract void progressBlock(DownloadTask task, int blockIndex,
                                          long currentBlockOffset);

    protected abstract void progress(DownloadTask task, long currentOffset);

    protected abstract void blockEnd(DownloadTask task, int blockIndex, BlockInfo info);

    @Override public void breakpointData(DownloadTask task, @Nullable BreakpointInfo info) { }

    @Override public void downloadFromBeginning(DownloadTask task, BreakpointInfo info,
                                                ResumeFailedCause cause) { }

    @Override public void downloadFromBreakpoint(DownloadTask task, BreakpointInfo info) {
        initData(info);

        infoReady(task, info);
    }

    @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        initData(info);

        infoReady(task, info);
    }

    @Override public void fetchStart(DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        synchronized (this) {
            final long blockCurrentOffset = blockCurrentOffsetMap.get(blockIndex) + increaseBytes;
            blockCurrentOffsetMap.put(blockIndex, blockCurrentOffset);
            currentOffset += increaseBytes;

            progressBlock(task, blockIndex, blockCurrentOffset);
            progress(task, currentOffset);
        }
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex, long contentLength) {
        blockEnd(task, blockIndex, info.getBlock(blockIndex));
    }

    private void initData(@NonNull BreakpointInfo info) {
        this.info = info;
        currentOffset = info.getTotalOffset();
        blockCurrentOffsetMap = new SparseArray<>();

        final int blockCount = info.getBlockCount();
        for (int i = 0; i < blockCount; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            blockCurrentOffsetMap.put(i, blockInfo.getCurrentOffset());
        }
    }
}
