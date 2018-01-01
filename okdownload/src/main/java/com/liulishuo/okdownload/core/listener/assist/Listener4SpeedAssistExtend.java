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
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;

public class Listener4SpeedAssistExtend implements Listener4Assist.AssistExtend {

    private Listener4SpeedCallback callback;

    public void setCallback(Listener4SpeedCallback callback) {
        this.callback = callback;
    }

    @Override public Listener4Assist.Listener4Model inspectAddModel(
            Listener4Assist.Listener4Model origin) {
        return new Listener4SpeedModel(origin);
    }

    @Override public boolean dispatchInfoReady(DownloadTask task, @NonNull BreakpointInfo info,
                                               boolean fromBreakpoint,
                                               @NonNull Listener4Assist.Listener4Model model) {
        if (callback != null) {
            callback.infoReady(task, info, fromBreakpoint, (Listener4SpeedModel) model);
        }
        return true;
    }

    @Override public boolean dispatchFetchProgress(@NonNull DownloadTask task, int blockIndex,
                                                   long increaseBytes,
                                                   @NonNull Listener4Assist.Listener4Model model) {
        final Listener4SpeedModel speedModel = (Listener4SpeedModel) model;

        speedModel.blockSpeeds.get(blockIndex).downloading(increaseBytes);
        speedModel.taskSpeed.downloading(increaseBytes);

        if (callback != null) {
            callback.progressBlock(task, blockIndex, model.blockCurrentOffsetMap.get(blockIndex),
                    speedModel.getBlockSpeed(blockIndex));
            callback.progress(task, model.currentOffset, speedModel.taskSpeed);
        }

        return true;
    }

    @Override public boolean dispatchBlockEnd(DownloadTask task, int blockIndex,
                                              Listener4Assist.Listener4Model model) {
        final Listener4SpeedModel speedModel = (Listener4SpeedModel) model;

        speedModel.blockSpeeds.get(blockIndex).endTask();

        if (callback != null) {
            callback.blockEnd(task, blockIndex, model.info.getBlock(blockIndex),
                    speedModel.getBlockSpeed(blockIndex));
        }

        return true;
    }

    @Override
    public boolean dispatchTaskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                                   @NonNull Listener4Assist.Listener4Model model) {
        final Listener4SpeedModel speedModel = (Listener4SpeedModel) model;
        speedModel.taskSpeed.endTask();

        if (callback != null) {
            callback.taskEnd(task, cause, realCause, speedModel.taskSpeed);
        }

        return true;
    }

    public static class Listener4SpeedModel extends Listener4Assist.Listener4Model {
        final SpeedCalculator taskSpeed;
        final SparseArray<SpeedCalculator> blockSpeeds;

        public SpeedCalculator getTaskSpeed() {
            return taskSpeed;
        }

        public SpeedCalculator getBlockSpeed(int blockIndex) {
            return blockSpeeds.get(blockIndex);
        }

        Listener4SpeedModel(Listener4Assist.Listener4Model model, SpeedCalculator taskSpeed,
                            SparseArray<SpeedCalculator> blockSpeeds) {
            super(model.info, model.currentOffset, model.blockCurrentOffsetMap);
            this.taskSpeed = taskSpeed;
            this.blockSpeeds = blockSpeeds;
        }

        public Listener4SpeedModel(Listener4Assist.Listener4Model model) {
            super(model.info, model.currentOffset, model.blockCurrentOffsetMap);

            this.taskSpeed = new SpeedCalculator();
            this.blockSpeeds = new SparseArray<>();

            final int blockCount = info.getBlockCount();
            for (int i = 0; i < blockCount; i++) blockSpeeds.put(i, new SpeedCalculator());
        }
    }

    public interface Listener4SpeedCallback {
        void infoReady(DownloadTask task, @NonNull BreakpointInfo info, boolean fromBreakpoint,
                       @NonNull Listener4SpeedModel model);

        void progressBlock(DownloadTask task, int blockIndex, long currentBlockOffset,
                           @NonNull SpeedCalculator blockSpeed);

        void progress(DownloadTask task, long currentOffset, @NonNull SpeedCalculator taskSpeed);

        void blockEnd(DownloadTask task, int blockIndex, BlockInfo info,
                      @NonNull SpeedCalculator blockSpeed);

        void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                     @NonNull SpeedCalculator taskSpeed);
    }
}
