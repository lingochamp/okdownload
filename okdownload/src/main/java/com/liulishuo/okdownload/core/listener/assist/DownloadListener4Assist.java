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
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;

public class DownloadListener4Assist {
    private Listener4Model singleTaskModel;
    private final SparseArray<Listener4Model> modelList = new SparseArray<>();

    private Listener4Callback callback;

    public void setCallback(@NonNull Listener4Callback callback) {
        this.callback = callback;
    }

    private synchronized void add(BreakpointInfo info) {
        SparseArray<Long> blockCurrentOffsetMap = new SparseArray<>();
        final int blockCount = info.getBlockCount();
        for (int i = 0; i < blockCount; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            blockCurrentOffsetMap.put(i, blockInfo.getCurrentOffset());
        }

        final Listener4Model model = new Listener4Model();
        model.info = info;
        model.currentOffset = info.getTotalOffset();
        model.blockCurrentOffsetMap = blockCurrentOffsetMap;

        if (singleTaskModel == null) {
            singleTaskModel = model;
        } else {
            modelList.put(info.getId(), model);
        }
    }

    public Listener4Model getSingleTaskModel() {
        return singleTaskModel;
    }

    public Listener4Model findModel(int id) {
        if (singleTaskModel != null && singleTaskModel.info.getId() == id) {
            return singleTaskModel;
        } else {
            return modelList.get(id);
        }
    }

    public void initData(DownloadTask task, BreakpointInfo info, boolean fromBreakpoint) {
        add(info);

        if (callback != null) callback.infoReady(task, info, fromBreakpoint);
    }


    public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        final Listener4Model model = findModel(task.getId());

        final long blockCurrentOffset = model.blockCurrentOffsetMap.get(blockIndex) + increaseBytes;
        model.blockCurrentOffsetMap.put(blockIndex, blockCurrentOffset);
        model.currentOffset += increaseBytes;

        if (callback != null){
            callback.progressBlock(task, blockIndex, blockCurrentOffset);
            callback.progress(task, model.currentOffset);
        }
    }

    public void fetchEnd(DownloadTask task, int blockIndex) {
        if (callback != null){
            callback.blockEnd(task, blockIndex, findModel(task.getId()).info.getBlock(blockIndex));
        }
    }

    public synchronized void taskEnd(int id) {
        if (singleTaskModel.info.getId() == id) {
            singleTaskModel = null;
        } else {
            modelList.remove(id);
        }
    }

    public static class Listener4Model {
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

    public interface Listener4Callback {

        void infoReady(DownloadTask task, @NonNull BreakpointInfo info, boolean fromBreakpoint);

        void progressBlock(DownloadTask task, int blockIndex, long currentBlockOffset);

        void progress(DownloadTask task, long currentOffset);

        void blockEnd(DownloadTask task, int blockIndex, BlockInfo info);
    }
}
