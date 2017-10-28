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

import android.os.Build;
import android.util.SparseArray;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jetbrains.annotations.Nullable;

import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.sample.R;

public class SingleTaskUtil {
    static final String TAG = "SingleTask";

    static void initInfo(SingleTaskViewHolder taskViewHolder,
                         SparseArray<SingleTaskViewHolder> blockViewHolders, BreakpointInfo info,
                         @Nullable SparseArray<Long> blockInstantOffsetMap) {
        if (taskViewHolder == null) return;

        final TextView titleTv = taskViewHolder.titleTv;
        titleTv.setText(
                titleTv.getContext().getString(R.string.task_title,
                        Util.humanReadableBytes(info.getTotalLength(), false)));

        long totalOffset = 0;
        if (blockInstantOffsetMap != null) {
            final int count = blockInstantOffsetMap.size();
            for (int i = 0; i < count; i++) totalOffset += blockInstantOffsetMap.keyAt(i);
        } else {
            totalOffset = info.getTotalOffset();
        }
        setProgress(taskViewHolder.pb, info.getTotalLength(), totalOffset);
        resetBlocksInfo(blockViewHolders, info, blockInstantOffsetMap);

    }

    private static void resetBlocksInfo(SparseArray<SingleTaskViewHolder> blockViewHolders,
                                        BreakpointInfo info,
                                        @Nullable SparseArray<Long> blockInstantOffsetMap) {
        if (blockViewHolders.size() <= 0) return;

        // block
        final int blockCount = Math.min(blockViewHolders.size(), info.getBlockCount());
        for (int i = 0; i < blockCount; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            final SingleTaskViewHolder viewHolder = blockViewHolders.get(i);

            final TextView titleTv = viewHolder.titleTv;
            titleTv.setText(titleTv.getContext().getString(R.string.block_title, i,
                    Util.humanReadableBytes(blockInfo.getStartOffset(), false),
                    Util.humanReadableBytes(blockInfo.getRangeRight(), false)));

            long offset;
            if (blockInstantOffsetMap != null) offset = blockInstantOffsetMap.get(i);
            else offset = blockInfo.getCurrentOffset();
            setProgress(viewHolder.pb, blockInfo.getContentLength(), offset);
        }
    }

    static void setProgress(ProgressBar bar, long currentOffset) {
        final int shrinkRate = (int) bar.getTag();
        final int progress = (int) ((currentOffset) / shrinkRate);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            bar.setProgress(progress, true);
        } else {
            bar.setProgress(progress);
        }
    }

    private static void setProgress(ProgressBar bar, long contentLength, long beginOffset) {
        final int contentLengthOnInt = reducePrecision(contentLength);
        final int shrinkRate = (int) (contentLength / contentLengthOnInt);
        bar.setTag(shrinkRate);
        final int progress = (int) (beginOffset / shrinkRate);


        bar.setMax(contentLengthOnInt);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            bar.setProgress(progress, true);
        } else {
            bar.setProgress(progress);
        }
    }

    private static int reducePrecision(long origin) {
        if (origin <= Integer.MAX_VALUE) return (int) origin;

        int shrinkRate = 10;
        long result = origin;
        while (result > Integer.MAX_VALUE) {
            result /= shrinkRate;
            shrinkRate *= 5;
        }

        return (int) result;
    }
}
