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

package com.liulishuo.okdownload.core.listener.assist;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;

public class Listener4Assist {
    @Nullable Listener4Model singleTaskModel;
    private final SparseArray<Listener4Model> modelList = new SparseArray<>();

    Listener4Callback callback;
    private AssistExtend assistExtend;

    public void setCallback(@NonNull Listener4Callback callback) {
        this.callback = callback;
    }

    public void setAssistExtend(@NonNull AssistExtend assistExtend) {
        this.assistExtend = assistExtend;
    }

    private synchronized Listener4Model addAndGetModel(BreakpointInfo info) {
        SparseArray<Long> blockCurrentOffsetMap = new SparseArray<>();
        final int blockCount = info.getBlockCount();
        for (int i = 0; i < blockCount; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            blockCurrentOffsetMap.put(i, blockInfo.getCurrentOffset());
        }

        Listener4Model model = new Listener4Model(info, info.getTotalOffset(),
                blockCurrentOffsetMap);

        if (assistExtend != null) model = assistExtend.inspectAddModel(model);

        if (singleTaskModel == null || singleTaskModel.info.getId() == info.getId()) {
            singleTaskModel = model;
        } else {
            modelList.put(info.getId(), model);
        }

        return model;
    }

    @Nullable public Listener4Model getSingleTaskModel() {
        return singleTaskModel;
    }

    public Listener4Model findModel(int id) {
        if (singleTaskModel != null && singleTaskModel.info.getId() == id) {
            return singleTaskModel;
        } else {
            return modelList.get(id);
        }
    }

    public void infoReady(DownloadTask task, BreakpointInfo info, boolean fromBreakpoint) {
        final Listener4Model model = addAndGetModel(info);

        if (assistExtend != null && assistExtend
                .dispatchInfoReady(task, info, fromBreakpoint, model)) {
            return;
        }

        if (callback != null) callback.infoReady(task, info, fromBreakpoint, model);
    }


    public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        final Listener4Model model = findModel(task.getId());
        if (model == null) return;

        final long blockCurrentOffset = model.blockCurrentOffsetMap
                .get(blockIndex) + increaseBytes;
        model.blockCurrentOffsetMap.put(blockIndex, blockCurrentOffset);
        model.currentOffset += increaseBytes;

        if (assistExtend != null
                && assistExtend.dispatchFetchProgress(task, blockIndex, increaseBytes, model)) {
            return;
        }

        if (callback != null) {
            callback.progressBlock(task, blockIndex, blockCurrentOffset);
            callback.progress(task, model.currentOffset);
        }
    }

    public void fetchEnd(DownloadTask task, int blockIndex) {
        final Listener4Model model = findModel(task.getId());
        if (model == null) return;

        if (assistExtend != null
                && assistExtend.dispatchBlockEnd(task, blockIndex, model)) {
            return;
        }

        if (callback != null) {
            callback.blockEnd(task, blockIndex, model.info.getBlock(blockIndex));
        }
    }

    public synchronized void taskEnd(DownloadTask task, EndCause cause,
                                     @Nullable Exception realCause) {
        final int id = task.getId();
        Listener4Model model;
        if (singleTaskModel != null && singleTaskModel.info.getId() == id) {
            model = singleTaskModel;
            singleTaskModel = null;
        } else {
            model = modelList.get(id);
            modelList.remove(id);
        }

        if (model == null) {
            model = new Listener4Model(
                    new BreakpointInfo(task.getId(), task.getUrl(), task.getParentFile(),
                            task.getFilename()), 0, new SparseArray<Long>());

            if (assistExtend != null) {
                model = assistExtend.inspectAddModel(model);
            }
        }

        if (assistExtend != null
                && assistExtend.dispatchTaskEnd(task, cause, realCause, model)) {
            return;
        }

        if (callback != null) callback.taskEnd(task, cause, realCause, model);
    }

    public static class Listener4Model {
        BreakpointInfo info;
        long currentOffset;
        SparseArray<Long> blockCurrentOffsetMap;

        Listener4Model(@NonNull BreakpointInfo info, long currentOffset,
                       @NonNull SparseArray<Long> blockCurrentOffsetMap) {
            this.info = info;
            this.currentOffset = currentOffset;
            this.blockCurrentOffsetMap = blockCurrentOffsetMap;
        }

        SparseArray<Long> getBlockCurrentOffsetMap() {
            return blockCurrentOffsetMap;
        }

        public long getCurrentOffset() {
            return currentOffset;
        }

        public long getBlockCurrentOffset(int blockIndex) {
            return blockCurrentOffsetMap.get(blockIndex);
        }

        public SparseArray<Long> cloneBlockCurrentOffsetMap() {
            return blockCurrentOffsetMap.clone();
        }

        public BreakpointInfo getInfo() {
            return info;
        }
    }

    public interface AssistExtend {
        Listener4Model inspectAddModel(Listener4Model origin);

        boolean dispatchInfoReady(DownloadTask task, @NonNull BreakpointInfo info,
                                  boolean fromBreakpoint,
                                  @NonNull Listener4Model model);

        boolean dispatchFetchProgress(@NonNull DownloadTask task, int blockIndex,
                                      long increaseBytes,
                                      @NonNull Listener4Model model);

        boolean dispatchBlockEnd(DownloadTask task, int blockIndex, Listener4Model model);

        boolean dispatchTaskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                                @NonNull Listener4Model model);
    }

    public interface Listener4Callback {

        void infoReady(DownloadTask task, @NonNull BreakpointInfo info, boolean fromBreakpoint,
                       @NonNull Listener4Model model);

        void progressBlock(DownloadTask task, int blockIndex, long currentBlockOffset);

        void progress(DownloadTask task, long currentOffset);

        void blockEnd(DownloadTask task, int blockIndex, BlockInfo info);

        void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                     @NonNull Listener4Model model);
    }
}
