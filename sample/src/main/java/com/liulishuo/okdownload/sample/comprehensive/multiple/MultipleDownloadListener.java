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

package com.liulishuo.okdownload.sample.comprehensive.multiple;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener1;
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist;
import com.liulishuo.okdownload.sample.R;
import com.liulishuo.okdownload.sample.util.ProgressUtil;


public class MultipleDownloadListener extends DownloadListener1 {
    private final SparseArray<MultipleTaskViewHolder> viewHolderMap = new SparseArray<>();

    void resetInfo(DownloadTask task, MultipleTaskViewHolder viewHolder) {
        viewHolder.updatePriority(task.getPriority());

        if (StatusUtil.isSameTaskPendingOrRunning(task)) {
            viewHolder.setToCancel(task);
            viewHolder.getStatusTv().setText(MultipleTaskUtil.getStatus(task));
            ProgressUtil.calcProgressToViewAndMark(viewHolder.getProgressBar(),
                    MultipleTaskUtil.getTotal(task),
                    MultipleTaskUtil.getOffset(task));
        } else {
            viewHolder.setToStart(task, this);
            final BreakpointInfo info = StatusUtil.getCurrentInfo(task);
            if (info != null) {
                viewHolder.getStatusTv().setText(R.string.state_idle);
                ProgressUtil.calcProgressToViewAndMark(viewHolder.getProgressBar(),
                        info.getTotalLength(),
                        info.getTotalOffset());
            } else {
                viewHolder.getStatusTv().setText(R.string.state_unknown);
                ProgressUtil.calcProgressToViewAndMark(viewHolder.getProgressBar(), 0, 0);
            }
        }
    }

    void clearBound() {
        viewHolderMap.clear();
    }

    void bind(DownloadTask task, MultipleTaskViewHolder viewHolder) {
        viewHolderMap.put(task.getId(), viewHolder);
    }

    @Override
    public void taskStart(DownloadTask task, @NonNull Listener1Assist.Listener1Model model) {
        final String status = "Start";
        MultipleTaskUtil.saveStatus(task, status);

        final MultipleTaskViewHolder holder = viewHolderMap.get(task.getId());
        if (holder == null) return;

        holder.getStatusTv().setText(status);
    }

    @Override public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                                  @NonNull Listener1Assist.Listener1Model model) {
        final String status = cause.name() + (realCause != null ? realCause : "");
        MultipleTaskUtil.saveStatus(task, status);

        final MultipleTaskViewHolder holder = viewHolderMap.get(task.getId());
        if (holder == null) return;

        holder.getStatusTv().setText(status);
        holder.setToStart(task, this);
    }

    @Override public void connected(DownloadTask task, int blockCount, long currentOffset,
                                       long totalLength) {
        final String status = "Connected";
        MultipleTaskUtil.saveStatus(task, status);
        MultipleTaskUtil.saveTotal(task, totalLength);
        MultipleTaskUtil.saveOffset(task, currentOffset);

        final MultipleTaskViewHolder holder = viewHolderMap.get(task.getId());
        if (holder == null) return;

        holder.getStatusTv().setText(status);
        ProgressUtil.calcProgressToViewAndMark(holder.getProgressBar(), totalLength, currentOffset);
    }

    @Override public void progress(DownloadTask task, long currentOffset, long totalLength) {
        final String status = "Progress(" + Util
                .humanReadableBytes(currentOffset, false) + "/"
                + Util.humanReadableBytes(totalLength, false) + ")";
        MultipleTaskUtil.saveStatus(task, status);
        MultipleTaskUtil.saveOffset(task, currentOffset);

        final MultipleTaskViewHolder holder = viewHolderMap.get(task.getId());
        if (holder == null) return;

        holder.getStatusTv().setText(status);
        ProgressUtil.updateProgressToViewWithMark(holder.getProgressBar(), currentOffset);
    }

    @Override public void retry(DownloadTask task, @NonNull ResumeFailedCause cause) {
        final String status = "Retry: " + cause.name();
        MultipleTaskUtil.saveStatus(task, status);

        final MultipleTaskViewHolder holder = viewHolderMap.get(task.getId());
        if (holder == null) return;

        holder.getStatusTv().setText(status);
    }
}
