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
import com.liulishuo.okdownload.core.assist.DownloadProgressAssist;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
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
public abstract class DownloadListener4 implements DownloadListener,
        DownloadProgressAssist.DownloadProgress {

    private final DownloadProgressAssist assist = new DownloadProgressAssist();

    @Nullable
    protected SparseArray<Long> blockCurrentOffsetMap(int id) {
        final DownloadProgressAssist.ProgressModel model = assist.findModel(id);
        return model == null ? null : model.getBlockCurrentOffsetMap();
    }

    /**
     * If you only have one task attach to this listener instance, you can use this method without
     * provide task id, otherwise please use {@link #blockCurrentOffsetMap(int)} instead.
     */
    @Nullable protected SparseArray<Long> blockCurrentOffsetMap() {
        final DownloadProgressAssist.ProgressModel model = assist.getOneModel();
        return model == null ? null : model.getBlockCurrentOffsetMap();
    }

    protected long getCurrentOffset(int id) {
        final DownloadProgressAssist.ProgressModel model = assist.findModel(id);
        return model == null ? 0 : model.getCurrentOffset();
    }

    /**
     * If you only have one task attach to this listener instance, you can use this method without
     * provide task id, otherwise please use {@link #getCurrentOffset(int)} instead.
     */
    protected long getCurrentOffset() {
        final DownloadProgressAssist.ProgressModel model = assist.getOneModel();
        return model == null ? 0 : model.getCurrentOffset();
    }

    protected abstract void infoReady(DownloadTask task, @NonNull BreakpointInfo info,
                                      boolean fromBreakpoint);

    protected abstract void blockEnd(DownloadTask task, int blockIndex, BlockInfo info);

    @Override public void breakpointData(DownloadTask task, @Nullable BreakpointInfo info) { }

    @Override public void downloadFromBeginning(DownloadTask task, BreakpointInfo info,
                                                ResumeFailedCause cause) { }

    @Override public final void downloadFromBreakpoint(DownloadTask task, BreakpointInfo info) {
        initData(info);

        infoReady(task, info, true);
    }

    @Override public final void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        initData(info);

        infoReady(task, info, false);
    }

    @Override public void fetchStart(DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        assist.fetchProgress(task, blockIndex, increaseBytes, this);
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex, long contentLength) {
        blockEnd(task, blockIndex, assist.getBlockInfo(task.getId(), blockIndex));
    }

    @Override
    public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
        assist.remove(task.getId());
    }

    protected void initData(@NonNull BreakpointInfo info) {
        assist.add(info);
    }
}
