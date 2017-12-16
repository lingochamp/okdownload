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
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;

import java.util.List;
import java.util.Map;

class SingleTaskListener extends DownloadListener4WithSpeed {

    @Nullable private SingleTaskViewAdapter viewAdapter;
    private final boolean detail;
    private FinishListener finishListener;

    SingleTaskListener(@NonNull SingleTaskViewAdapter viewAdapter, boolean detail) {
        this.detail = detail;
        this.viewAdapter = viewAdapter;
    }

    SparseArray<Long> getBlockInstantOffsetMap() {
        return blockCurrentOffsetMap();
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

    @Override public void taskStart(DownloadTask task) {
        updateStatus("taskStart " + task.getId());
    }

    @Override protected void infoReady(DownloadTask task, @NonNull BreakpointInfo info) {
        if (viewAdapter != null) viewAdapter.refreshData(info, null);
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

    @Override
    protected void progressBlock(DownloadTask task, int blockIndex, long currentBlockOffset) {
        if (detail && viewAdapter != null) {
            if (blockIndex >= viewAdapter.blockViewSize()) return;

            viewAdapter.setBlockProcess(blockIndex, currentBlockOffset,
                    blockSpeed(blockIndex).speed());
        }
    }

    @Override protected void progress(DownloadTask task, long currentOffset) {
        if (detail && viewAdapter != null) {
            viewAdapter.setTaskProcess(currentOffset, taskSpeed().speed());
        }
    }

    @Override protected void blockEnd(DownloadTask task, int blockIndex, BlockInfo info) {
        if (detail && viewAdapter != null) {
            viewAdapter.onBlocksEnd(blockIndex, blockSpeed(blockIndex));
        }
    }

    @Override
    protected void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                           @NonNull String averageSpeed) {
        if (detail && viewAdapter != null) {
            viewAdapter.onTaskEnd(averageSpeed);
        }
    }

    @Override public void breakpointData(DownloadTask task,
                                         @Nullable BreakpointInfo info) {
        super.breakpointData(task, info);
        updateStatus("breakpointData " + info);

    }

    @Override public void downloadFromBeginning(DownloadTask task,
                                                BreakpointInfo info,
                                                ResumeFailedCause cause) {
        super.downloadFromBeginning(task, info, cause);
        updateStatus("downloadFromBeginning " + info + " " + cause);
    }

    @Override public void downloadFromBreakpoint(DownloadTask task,
                                                 BreakpointInfo info) {
        super.downloadFromBreakpoint(task, info);
        updateStatus("downloadFromBreakpoint " + info);
    }


    @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        super.splitBlockEnd(task, info);
        updateStatus("splitBlockEnd " + info.getBlockCount() + " " + info.getTotalLength());
    }

    @Override public void fetchStart(DownloadTask task, int blockIndex,
                                     long contentLength) {
        super.fetchStart(task, blockIndex, contentLength);
        updateStatus("fetchStart " + blockIndex + " " + contentLength);
    }


    @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        super.fetchProgress(task, blockIndex, increaseBytes);
        updateStatus(
                "fetchProgress " + blockIndex + " " + Util.humanReadableBytes(increaseBytes,
                        false) + " " + taskSpeed().speed());
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex,
                                   long contentLength) {
        super.fetchEnd(task, blockIndex, contentLength);
        updateStatus("fetchEnd " + blockIndex + " " + contentLength);
    }


    @Override public void taskEnd(DownloadTask task, EndCause cause,
                                  @Nullable Exception realCause) {
        super.taskEnd(task, cause, realCause);
        updateStatus("taskEnd " + cause + " " + realCause + " " + taskSpeed().speedFromBegin());

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