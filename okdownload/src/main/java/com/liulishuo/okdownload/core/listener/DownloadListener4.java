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
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.assist.DownloadListener4Assist;

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
        DownloadListener4Assist.Listener4Callback {

    final DownloadListener4Assist assist;

    DownloadListener4(DownloadListener4Assist assist) {
        this.assist = assist;
        assist.setCallback(this);
    }

    public DownloadListener4() {
        this(new DownloadListener4Assist());
    }

    @Nullable
    protected SparseArray<Long> blockCurrentOffsetMap(int id) {
        final DownloadListener4Assist.Listener4Model model = assist.findModel(id);
        return model == null ? null : model.getBlockCurrentOffsetMap();
    }

    /**
     * If you only have one task attach to this listener instance, you can use this method without
     * provide task id, otherwise please use {@link #blockCurrentOffsetMap(int)} instead.
     */
    @Nullable protected SparseArray<Long> blockCurrentOffsetMap() {
        final DownloadListener4Assist.Listener4Model model = assist.getSingleTaskModel();
        return model == null ? null : model.getBlockCurrentOffsetMap();
    }

    protected long getCurrentOffset(int id) {
        final DownloadListener4Assist.Listener4Model model = assist.findModel(id);
        return model == null ? 0 : model.getCurrentOffset();
    }

    /**
     * If you only have one task attach to this listener instance, you can use this method without
     * provide task id, otherwise please use {@link #getCurrentOffset(int)} instead.
     */
    protected long getCurrentOffset() {
        final DownloadListener4Assist.Listener4Model model = assist.getSingleTaskModel();
        return model == null ? 0 : model.getCurrentOffset();
    }

    @Override public void downloadFromBeginning(DownloadTask task, BreakpointInfo info,
                                                ResumeFailedCause cause) { }

    @Override public final void downloadFromBreakpoint(DownloadTask task, BreakpointInfo info) {
        initData(task, info, true);
    }

    @Override public final void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        initData(task, info, false);
    }

    @Override public void fetchStart(DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        assist.fetchProgress(task, blockIndex, increaseBytes);
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex, long contentLength) {
        assist.fetchEnd(task, blockIndex);
    }

    @Override
    public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
        assist.taskEnd(task.getId());
    }

    protected void initData(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                            boolean fromBreakpoint) {
        assist.initData(task, info, fromBreakpoint);
    }
}
