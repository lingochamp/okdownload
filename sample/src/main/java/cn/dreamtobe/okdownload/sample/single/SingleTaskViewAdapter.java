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
import android.util.SparseArray;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.dreamtobe.okdownload.SpeedCalculator;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;

import static cn.dreamtobe.okdownload.sample.single.SingleTaskUtil.setProgress;

public class SingleTaskViewAdapter {

    @NonNull private final SingleTaskViewHolder taskViewHolder;
    @NonNull private final SparseArray<SingleTaskViewHolder> blockViewHolderMap;
    @NonNull private final TextView statusTv;
    @NonNull private final TextView extInfoTv;


    private boolean invalidate;

    private SingleTaskViewAdapter(@NonNull SingleTaskViewHolder taskViewHolder,
                                  @NonNull SparseArray<SingleTaskViewHolder> blockViewHolderMap,
                                  @NonNull TextView statusTv, @NonNull TextView extInfoTv) {
        this.taskViewHolder = taskViewHolder;
        this.blockViewHolderMap = blockViewHolderMap;
        this.statusTv = statusTv;
        this.extInfoTv = extInfoTv;

    }

    public void refreshData(@NonNull BreakpointInfo info,
                            @Nullable SparseArray<Long> blockInstantOffsetMap) {
        if (invalidate) return;

        SingleTaskUtil.initInfo(taskViewHolder, blockViewHolderMap, info, blockInstantOffsetMap);
    }

    public void setExtInfo(CharSequence info) {
        if (invalidate) return;
        extInfoTv.setText(info);
    }

    public int blockViewSize() {
        return blockViewHolderMap.size();
    }

    public void updateStatus(CharSequence status) {
        if (invalidate) return;
        statusTv.setText(status);
    }

    public void setTaskProcess(long currentOffset, String globalSpeed) {
        if (invalidate) return;

        setProgress(taskViewHolder.pb, currentOffset);
        taskViewHolder.speedTv.setText(globalSpeed);
    }

    public void setBlockProcess(int blockIndex, long currentOffset, String blockSpeed) {
        if (invalidate) return;

        if (blockIndex >= blockViewHolderMap.size()) return;

        final SingleTaskViewHolder blockViewHolder = blockViewHolderMap.get(blockIndex);
        setProgress(blockViewHolder.pb, currentOffset);
        blockViewHolder.speedTv.setText(blockSpeed);
    }

    public void onTaskEnd(String speedFromBegin) {
        taskViewHolder.speedTv.setText(speedFromBegin);
    }

    public void onBlocksEnd(SparseArray<SpeedCalculator> blockSpeeds) {
        if (invalidate) return;

        final int blockCount = blockViewHolderMap.size();
        for (int i = 0; i < blockCount; i++) {
            final int blockIndex = blockViewHolderMap.keyAt(i);
            final SingleTaskViewHolder viewHolder = blockViewHolderMap.valueAt(i);
            final SpeedCalculator blockSpeed = blockSpeeds.get(blockIndex);
            viewHolder.speedTv.setText(blockSpeed.speedFromBegin());
        }
    }

    public void invalidate() {
        this.invalidate = true;
    }

    public static class Builder {
        private SingleTaskViewHolder taskViewHolder;
        private SparseArray<SingleTaskViewHolder> blockViewHolderMap = new SparseArray<>(4);
        private TextView statusTv;
        private TextView extInfoTv;

        public Builder setTaskViews(TextView titleTv, TextView speedTv, ProgressBar pb) {
            taskViewHolder = new SingleTaskViewHolder(titleTv, speedTv, pb);
            return this;
        }

        public Builder setBlock0Views(TextView titleTv, TextView speedTv, ProgressBar pb) {
            blockViewHolderMap.put(0, new SingleTaskViewHolder(titleTv, speedTv, pb));
            return this;
        }

        public Builder setBlock1Views(TextView titleTv, TextView speedTv, ProgressBar pb) {
            blockViewHolderMap.put(1, new SingleTaskViewHolder(titleTv, speedTv, pb));
            return this;
        }

        public Builder setBlock2Views(TextView titleTv, TextView speedTv, ProgressBar pb) {
            blockViewHolderMap.put(2, new SingleTaskViewHolder(titleTv, speedTv, pb));
            return this;
        }

        public Builder setBlock3Views(TextView titleTv, TextView speedTv, ProgressBar pb) {
            blockViewHolderMap.put(3, new SingleTaskViewHolder(titleTv, speedTv, pb));
            return this;
        }

        public Builder setStatusTv(TextView statusTv) {
            this.statusTv = statusTv;
            return this;
        }

        public Builder setExtInfoTv(TextView extInfoTv) {
            this.extInfoTv = extInfoTv;
            return this;
        }

        public SingleTaskViewAdapter build() {
            if (taskViewHolder == null || statusTv == null || extInfoTv == null) {
                throw new IllegalArgumentException();
            }
            return new SingleTaskViewAdapter(taskViewHolder, blockViewHolderMap, statusTv,
                    extInfoTv);
        }
    }
}
