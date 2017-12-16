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

import android.util.SparseArray;
import android.widget.TextView;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.sample.R;

import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.liulishuo.okdownload.sample.DemoUtil.setProgress;

public class SingleTaskUtil {
    static final String TAG = "SingleTask";

    static final String FILENAME = "tkzpx40x-lls-LLS-5.7-785-20171108-111118.apk";
    public static final String URL =
            "https://cdn.llscdn.com/yy/files/tkzpx40x-lls-LLS-5.7-785-20171108-111118.apk";

    static DownloadTask createTask(String url, File parentFile) {
        return new DownloadTask.Builder(url, parentFile)
                .setFilename(FILENAME)
                .setMinIntervalMillisCallbackProcess(150)
                .build();
    }

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
            for (int i = 0; i < count; i++) totalOffset += blockInstantOffsetMap.get(i);
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

            Long offsetLong = null;
            if (blockInstantOffsetMap != null) {
                offsetLong = blockInstantOffsetMap.get(i);
            }

            if (offsetLong == null) offsetLong = blockInfo.getCurrentOffset();

            setProgress(viewHolder.pb, blockInfo.getContentLength(), offsetLong);
        }
    }
}
