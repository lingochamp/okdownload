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

package com.liulishuo.okdownload.sample.comprehensive.single;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend;

import java.util.List;
import java.util.Map;

class SingleTaskListener extends DownloadListener4WithSpeed {

    @Nullable private SingleTaskViewAdapter viewAdapter;
    private final boolean detail;
    private FinishListener finishListener;
    private SparseArray<Long> blockCurrentOffsetMap;

    SingleTaskListener(@NonNull SingleTaskViewAdapter viewAdapter, boolean detail) {
        this.detail = detail;
        this.viewAdapter = viewAdapter;
    }

    SparseArray<Long> getBlockInstantOffsetMap() {
        return this.blockCurrentOffsetMap;
    }

    void detach() {
        this.viewAdapter = null;
        this.finishListener = null;
    }

    void reattach(@NonNull SingleTaskViewAdapter viewAdapter,
                  @NonNull FinishListener finishListener) {
        this.viewAdapter = viewAdapter;
        this.finishListener = finishListener;
    }

    SingleTaskListener(@NonNull SingleTaskViewAdapter viewAdapter) {
        this(viewAdapter, false);
    }

    @Override public void taskStart(@NonNull DownloadTask task) {
        updateStatus("taskStart " + task.getId());
    }

    @Override
    public void infoReady(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                          boolean fromBreakpoint,
                          @NonNull Listener4SpeedAssistExtend.Listener4SpeedModel model) {
        this.blockCurrentOffsetMap = model.cloneBlockCurrentOffsetMap();
        if (viewAdapter != null) viewAdapter.refreshData(info, null);
    }

    @Override public void connectStart(@NonNull DownloadTask task, int blockIndex,
                                       @NonNull Map<String, List<String>> requestHeaderFields) {
        updateStatus("connectStart " + blockIndex + " " + requestHeaderFields);
    }

    @Override public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode,
                                     @NonNull Map<String, List<String>> responseHeaderFields) {
        updateStatus(
                "connectEnd " + blockIndex + " " + responseCode + " " + responseHeaderFields);

    }

    @Override
    public void progressBlock(@NonNull DownloadTask task, int blockIndex, long currentBlockOffset,
                              @NonNull SpeedCalculator blockSpeed) {
        if (detail && viewAdapter != null) {
            if (blockIndex >= viewAdapter.blockViewSize()) return;

            viewAdapter.setBlockProcess(blockIndex, currentBlockOffset,
                    blockSpeed.speed());
        }
    }

    @Override public void progress(@NonNull DownloadTask task, long currentOffset,
                                   @NonNull SpeedCalculator taskSpeed) {
        if (detail && viewAdapter != null) {
            viewAdapter.setTaskProcess(currentOffset, taskSpeed.speed());
        }
    }

    @Override public void blockEnd(@NonNull DownloadTask task, int blockIndex, BlockInfo info,
                                   @NonNull SpeedCalculator blockSpeed) {
        if (detail && viewAdapter != null) {
            viewAdapter.onBlocksEnd(blockIndex, blockSpeed);
        }

    }

    @Override public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                                  @Nullable Exception realCause,
                                  @NonNull SpeedCalculator taskSpeed) {
        if (detail && viewAdapter != null) {
            viewAdapter.onTaskEnd(taskSpeed.averageSpeed());
        }

        if (this.finishListener != null) this.finishListener.finish();
    }

    private void updateStatus(String status) {
        Log.d(SingleTaskUtil.TAG, status);
        if (viewAdapter == null) return;

        if (detail) viewAdapter.updateStatus(status);
        else viewAdapter.setExtInfo(status);

    }

    interface FinishListener {
        void finish();
    }
}