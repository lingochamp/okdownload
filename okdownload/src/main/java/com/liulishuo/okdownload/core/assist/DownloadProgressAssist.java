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

package com.liulishuo.okdownload.core.assist;

import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;

public class DownloadProgressAssist {
    private ProgressModel oneModel;
    private final SparseArray<ProgressModel> modelList = new SparseArray<>();

    public void add(BreakpointInfo info) {
        SparseArray<Long> blockCurrentOffsetMap = new SparseArray<>();
        final int blockCount = info.getBlockCount();
        for (int i = 0; i < blockCount; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            blockCurrentOffsetMap.put(i, blockInfo.getCurrentOffset());
        }

        final ProgressModel model = new ProgressModel();
        model.info = info;
        model.currentOffset = info.getTotalOffset();
        model.blockCurrentOffsetMap = blockCurrentOffsetMap;

        if (oneModel == null) {
            oneModel = model;
        } else {
            modelList.put(info.getId(), model);
        }
    }

    public synchronized ProgressModel fetchProgress(int id, int blockIndex, long increaseBytes) {
        final ProgressModel model;
        if (oneModel != null && oneModel.info.getId() == id) {
            model = oneModel;
        } else {
            model = modelList.get(id);
        }

        final long blockCurrentOffset = model.blockCurrentOffsetMap.get(blockIndex) + increaseBytes;
        model.blockCurrentOffsetMap.put(blockIndex, blockCurrentOffset);
        model.currentOffset += increaseBytes;

        return model;
    }

    public BlockInfo getBlockInfo(int id, int blockIndex) {
        return findModel(id).info.getBlock(blockIndex);
    }

    public ProgressModel getOneModel() {
        return oneModel;
    }

    public ProgressModel findModel(int id) {
        if (oneModel != null && oneModel.info.getId() == id) {
            return oneModel;
        } else {
            return modelList.get(id);
        }
    }

    public void remove(int id) {
        if (oneModel.info.getId() == id) {
            oneModel = null;
        } else {
            modelList.remove(id);
        }
    }


    public synchronized void fetchProgress(DownloadTask task, int blockIndex,
                                           long increaseBytes,
                                           @Nullable DownloadProgress progress) {
        final ProgressModel model = findModel(task.getId());

        final long blockCurrentOffset = model.blockCurrentOffsetMap.get(blockIndex) + increaseBytes;
        model.blockCurrentOffsetMap.put(blockIndex, blockCurrentOffset);
        model.currentOffset += increaseBytes;

        if (progress != null) {
            progress.progressBlock(task, blockIndex, blockCurrentOffset);
            progress.progress(task, model.currentOffset);
        }
    }

    public static class ProgressModel {
        BreakpointInfo info;
        long currentOffset;
        SparseArray<Long> blockCurrentOffsetMap;

        public SparseArray<Long> getBlockCurrentOffsetMap() {
            return blockCurrentOffsetMap;
        }

        public long getCurrentOffset() {
            return currentOffset;
        }
    }

    public interface DownloadProgress {
        void progressBlock(DownloadTask task, int blockIndex,
                           long currentBlockOffset);

        void progress(DownloadTask task, long currentOffset);
    }
}


