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

package cn.dreamtobe.okdownload.sample.single;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import java.util.List;
import java.util.Map;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.SpeedCalculator;
import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.cause.EndCause;
import cn.dreamtobe.okdownload.core.cause.ResumeFailedCause;

import static cn.dreamtobe.okdownload.sample.single.SingleTaskUtil.TAG;

class SingleTaskListener implements DownloadListener {
    private SpeedCalculator taskSpeed = new SpeedCalculator();
    private SparseArray<SpeedCalculator> blockSpeeds = new SparseArray<>(4);

    private SingleTaskViewAdapter viewAdapter;
    private final boolean detail;

    // because of data on breakpoint-store real time enough, so we record offset from listener.
    private final SparseArray<Long> blockInstantOffsetMap;
    private long totalOffset;

    SingleTaskListener(@NonNull SingleTaskViewAdapter viewAdapter, boolean detail) {
        this.detail = detail;
        this.viewAdapter = viewAdapter;
        final int blockCount = viewAdapter.blockViewSize();
        for (int i = 0; i < blockCount; i++) blockSpeeds.put(i, new SpeedCalculator());
        this.blockInstantOffsetMap = new SparseArray<>();
    }

    public SparseArray<Long> getBlockInstantOffsetMap() {
        return blockInstantOffsetMap;
    }

    public void reattach(@NonNull SingleTaskViewAdapter viewAdapter) {
        this.viewAdapter = viewAdapter;
    }

    SingleTaskListener(@NonNull SingleTaskViewAdapter viewAdapter) {
        this(viewAdapter, false);
    }

    @Override public void taskStart(DownloadTask task) {
        updateStatus("taskStart " + task.getId());
    }

    @Override public void breakpointData(DownloadTask task,
                                         @Nullable BreakpointInfo info) {
        updateStatus("breakpointData " + info);

    }

    @Override public void downloadFromBeginning(DownloadTask task,
                                                BreakpointInfo info,
                                                ResumeFailedCause cause) {
        updateStatus("downloadFromBeginning " + info + " " + cause);
    }

    @Override public void downloadFromBreakpoint(DownloadTask task,
                                                 BreakpointInfo info) {
        updateStatus("downloadFromBreakpoint " + info);
        initInfo(info);
    }

    @Override public void connectStart(DownloadTask task, int blockIndex,
                                       @NonNull Map<String, List<String>> requestHeaderFields) {
        updateStatus("connectStart " + blockIndex + " " + requestHeaderFields);
    }

    @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                     @NonNull Map<String, List<String>> responseHeaderFields) {
        updateStatus(
                "connectEnd " + blockIndex + " " + responseCode + " " + responseHeaderFields);

    }

    @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        updateStatus("splitBlockEnd " + info.getBlockCount() + " " + info.getTotalLength());
        initInfo(info);
    }

    @Override public void fetchStart(DownloadTask task, int blockIndex,
                                     long contentLength) {
        updateStatus("fetchStart " + blockIndex + " " + contentLength);

    }

    @Override public void fetchProgress(DownloadTask task, int blockIndex,
                                        long increaseBytes) {
        taskSpeed.downloading(increaseBytes);
        final String speed = taskSpeed.speed();
        updateStatus(
                "fetchProgress " + blockIndex + " " + Util.humanReadableBytes(increaseBytes,
                        false) + " " + speed);


        if (detail) {
            final long offset = blockInstantOffsetMap.get(blockIndex);
            final long currentOffset = offset + increaseBytes;
            blockInstantOffsetMap.put(blockIndex, currentOffset);

            totalOffset += increaseBytes;
            viewAdapter.setTaskProcess(totalOffset, speed);
            if (blockIndex >= viewAdapter.blockViewSize()) return;

            final SpeedCalculator blockSpeed = blockSpeeds.get(blockIndex);
            blockSpeed.downloading(increaseBytes);

            viewAdapter.setBlockProcess(blockIndex, currentOffset, blockSpeed.speed());
        }
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex,
                                   long contentLength) {
        updateStatus("fetchEnd " + blockIndex + " " + contentLength);
    }

    @Override public void taskEnd(DownloadTask task, EndCause cause,
                                  @Nullable Exception realCause) {
        taskSpeed.endTask();
        final String speedFromBegin = taskSpeed.speedFromBegin();
        updateStatus("taskEnd " + cause + " " + realCause + " " + speedFromBegin);

        if (detail) {
            viewAdapter.onTaskEnd(speedFromBegin);
            viewAdapter.onBlocksEnd(blockSpeeds);
        }
    }

    private void updateStatus(String status) {
        Log.d(TAG, status);
        if (detail) viewAdapter.updateStatus(status);
        else viewAdapter.setExtInfo(status);

    }

    private void initInfo(BreakpointInfo info) {
        viewAdapter.refreshData(info, null);

        blockInstantOffsetMap.clear();
        totalOffset = info.getTotalOffset();
        final int blockCount = info.getBlockCount();
        for (int i = 0; i < blockCount; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            blockInstantOffsetMap.put(i, blockInfo.getCurrentOffset());
        }
    }
}