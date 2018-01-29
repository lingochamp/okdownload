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

package com.liulishuo.okdownload.sample.util.queue;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener1;
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist;
import com.liulishuo.okdownload.sample.R;
import com.liulishuo.okdownload.sample.util.ProgressUtil;
import com.liulishuo.okdownload.sample.util.queue.QueueRecyclerAdapter.QueueViewHolder;

class QueueListener extends DownloadListener1 {
    private static final String TAG = "QueueListener";

    private SparseArray<QueueViewHolder> holderMap = new SparseArray<>();

    void bind(DownloadTask task, QueueViewHolder holder) {
        Log.i(TAG, "bind " + task.getId() + " with " + holder);
        // replace.
        final int size = holderMap.size();
        for (int i = 0; i < size; i++) {
            if (holderMap.valueAt(i) == holder) {
                holderMap.removeAt(i);
                break;
            }
        }
        holderMap.put(task.getId(), holder);
    }

    void resetInfo(DownloadTask task, QueueViewHolder holder) {
        // task name
        final String taskName = TagUtil.getTaskName(task);
        holder.nameTv.setText(taskName);

        // process references
        final String status = TagUtil.getStatus(task);
        if (status != null) {
            //   started
            holder.statusTv.setText(status);
            if (status.equals(EndCause.COMPLETED.toString())) {
                holder.progressBar.setProgress(holder.progressBar.getMax());
            } else {
                final long total = TagUtil.getTotal(task);
                if (total == 0) {
                    holder.progressBar.setProgress(0);
                } else {
                    ProgressUtil.calcProgressToViewAndMark(holder.progressBar,
                            TagUtil.getOffset(task), total, false);
                }
            }
        } else {
            // non-started
            final StatusUtil.Status statusOnStore = StatusUtil.getStatus(task);
            TagUtil.saveStatus(task, statusOnStore.toString());
            if (statusOnStore == StatusUtil.Status.COMPLETED) {
                holder.statusTv.setText(EndCause.COMPLETED.toString());
                holder.progressBar.setProgress(holder.progressBar.getMax());
            } else {
                switch (statusOnStore) {
                    case IDLE:
                        holder.statusTv.setText(R.string.state_idle);
                        break;
                    case PENDING:
                        holder.statusTv.setText(R.string.state_pending);
                        break;
                    case RUNNING:
                        holder.statusTv.setText(R.string.state_running);
                        break;
                    default:
                        holder.statusTv.setText(R.string.state_unknown);
                }

                if (statusOnStore == StatusUtil.Status.UNKNOWN) {
                    holder.progressBar.setProgress(0);
                } else {
                    final BreakpointInfo info = StatusUtil.getCurrentInfo(task);
                    if (info != null) {
                        TagUtil.saveTotal(task, info.getTotalLength());
                        TagUtil.saveOffset(task, info.getTotalOffset());
                        ProgressUtil.calcProgressToViewAndMark(holder.progressBar,
                                info.getTotalOffset(), info.getTotalLength(), false);
                    } else {
                        holder.progressBar.setProgress(0);
                    }
                }

            }
        }
    }

    public void clearBoundHolder() {
        holderMap.clear();
    }

    @Override
    public void taskStart(@NonNull DownloadTask task,
                          @NonNull Listener1Assist.Listener1Model model) {
        final String status = "taskStart";
        TagUtil.saveStatus(task, status);

        final QueueViewHolder holder = holderMap.get(task.getId());

        if (holder == null) return;

        holder.statusTv.setText(status);
    }

    @Override public void retry(@NonNull DownloadTask task, @NonNull ResumeFailedCause cause) {
        final String status = "retry";
        TagUtil.saveStatus(task, status);

        final QueueViewHolder holder = holderMap.get(task.getId());
        if (holder == null) return;

        holder.statusTv.setText(status);
    }

    @Override
    public void connected(@NonNull DownloadTask task, int blockCount, long currentOffset,
                          long totalLength) {
        final String status = "connected";
        TagUtil.saveStatus(task, status);
        TagUtil.saveOffset(task, currentOffset);
        TagUtil.saveTotal(task, totalLength);

        final QueueViewHolder holder = holderMap.get(task.getId());
        if (holder == null) return;

        holder.statusTv.setText(status);

        ProgressUtil.calcProgressToViewAndMark(holder.progressBar, currentOffset, totalLength,
                false);
    }

    @Override
    public void progress(@NonNull DownloadTask task, long currentOffset, long totalLength) {
        final String status = "progress";
        TagUtil.saveStatus(task, status);
        TagUtil.saveOffset(task, currentOffset);

        final QueueViewHolder holder = holderMap.get(task.getId());
        if (holder == null) return;

        holder.statusTv.setText(status);

        Log.i(TAG, "progress " + task.getId() + " with " + holder);
        ProgressUtil.updateProgressToViewWithMark(holder.progressBar, currentOffset, false);
    }

    @Override
    public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                        @Nullable Exception realCause,
                        @NonNull Listener1Assist.Listener1Model model) {
        final String status = cause.toString();
        TagUtil.saveStatus(task, status);

        final QueueViewHolder holder = holderMap.get(task.getId());
        if (holder == null) return;

        holder.statusTv.setText(status);
        if (cause == EndCause.COMPLETED) {
            holder.progressBar.setProgress(holder.progressBar.getMax());
        }
    }
}