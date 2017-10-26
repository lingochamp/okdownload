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

package cn.dreamtobe.okdownload.sample;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.SpeedCalculator;
import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.cause.EndCause;
import cn.dreamtobe.okdownload.core.cause.ResumeFailedCause;

public class SingleTaskDemo {

    private static final String TAG = "SingleTaskDemo";

    private DownloadTask task;
    private final String demoUrl = "https://t.alipayobjects.com/L1/71/100/and/alipay_wap_main.apk";

    @NonNull private final ViewHolder taskViewHolder;
    @NonNull private final SparseArray<ViewHolder> blockViewHolderMap;
    @NonNull private final TextView statusTv;
    @NonNull private final TextView sameTaskTv;
    @NonNull private final TextView sameFileTv;

    private SingleTaskDemo(@NonNull ViewHolder taskViewHolder,
                           @NonNull SparseArray<ViewHolder> blockViewHolderMap,
                           @NonNull TextView statusTv, @NonNull TextView sameTaskTv,
                           @NonNull TextView sameFileTv) {
        this.taskViewHolder = taskViewHolder;
        this.blockViewHolderMap = blockViewHolderMap;
        this.statusTv = statusTv;
        this.sameTaskTv = sameTaskTv;
        this.sameFileTv = sameFileTv;
    }

    public void startAsync(Context context, @NonNull final FinishListener listener) {
        if (task != null) return;

        DownloadTask.Builder builder = new DownloadTask.Builder(demoUrl,
                Uri.fromFile(context.getExternalCacheDir()));
        task = builder
                .setMinIntervalMillisCallbackProcess(150)
                .build();

        task.enqueue(new SingleTaskListener(taskViewHolder, blockViewHolderMap, statusTv) {
            @Override public void taskEnd(DownloadTask task, EndCause cause,
                                          @Nullable Exception realCause) {
                super.taskEnd(task, cause, realCause);
                SingleTaskDemo.this.task = null;
                listener.finish();
            }
        });
    }

    public void startSamePathTask_fileBusy(Uri path) {
        final String otherUrl = "http://dldir1.qq.com/weixin/android/seixin6516android1120.apk";
        DownloadTask.Builder builder = new DownloadTask.Builder(otherUrl, path);
        final DownloadTask task = builder
                .setAutoCallbackToUIThread(false)
                // same filename to #startAsync
                .setFilename("alipay_wap_main.apk")
                .build();

        task.enqueue(new SingleTaskListener(sameFileTv));

    }

    public void startSameTask_sameTaskBusy(Uri path) {
        DownloadTask.Builder builder = new DownloadTask.Builder(demoUrl, path);
        DownloadTask task = builder
                .setAutoCallbackToUIThread(false)
                .build();

        task.enqueue(new SingleTaskListener(sameTaskTv));
    }

    public void cancelTask() {
        final DownloadTask task = this.task;
        if (task == null) return;

        task.cancel();
    }

    private class SingleTaskListener implements DownloadListener {
        private SpeedCalculator taskSpeed = new SpeedCalculator();
        private SparseArray<SpeedCalculator> blockSpeeds = new SparseArray<>(4);

        private ViewHolder taskViewHolder;
        private SparseArray<ViewHolder> blockViewHolders;
        private TextView statusTv;

        SingleTaskListener(@NonNull TextView statusTv) {
            this.statusTv = statusTv;
        }

        SingleTaskListener(@NonNull ViewHolder taskViewHolder,
                           @NonNull SparseArray<ViewHolder> blockViewHolders,
                           @NonNull TextView statusTv) {
            this.taskViewHolder = taskViewHolder;
            this.blockViewHolders = blockViewHolders;
            this.statusTv = statusTv;

            final int blockCount = blockViewHolders.size();
            for (int i = 0; i < blockCount; i++) blockSpeeds.put(i, new SpeedCalculator());
        }

        @Override public void taskStart(DownloadTask task) {
            updateStatus("taskStart " + task.getId());
        }

        @Override public void breakpointData(DownloadTask task,
                                             @Nullable BreakpointInfo info) {
            updateStatus("breakpointData " + info);

        }

        @Override public void downloadFromBeginning(DownloadTask task,
                                                    BreakpointInfo info,
                                                    ResumeFailedCause cause) {
            updateStatus("downloadFromBeginning " + info + " " + cause);
        }

        @Override public void downloadFromBreakpoint(DownloadTask task,
                                                     BreakpointInfo info) {
            updateStatus("downloadFromBreakpoint " + info);
            initInfo(info);
        }

        @Override public void connectStart(DownloadTask task, int blockIndex,
                                           @NonNull Map<String, List<String>> requestHeaderFields) {
            updateStatus("connectStart " + blockIndex + " " + requestHeaderFields);
        }

        @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                         @NonNull Map<String, List<String>> responseHeaderFields) {
            updateStatus(
                    "connectEnd " + blockIndex + " " + responseCode + " " + responseHeaderFields);

        }

        @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
            updateStatus("splitBlockEnd " + info.getBlockCount() + " " + info.getTotalLength());
            initInfo(info);
        }

        @Override public void fetchStart(DownloadTask task, int blockIndex,
                                         long contentLength) {
            updateStatus("fetchStart " + blockIndex + " " + contentLength);

        }

        int updateSpeedRateUtil = 0;

        @Override public void fetchProgress(DownloadTask task, int blockIndex,
                                            long increaseBytes) {
            taskSpeed.downloading(increaseBytes);
            final String speed = taskSpeed.speed();
            updateStatus(
                    "fetchProgress " + blockIndex + " " + Util.humanReadableBytes(increaseBytes,
                            false) + " " + speed);

            final boolean isNeedUpdateSpeed = updateSpeedRateUtil++ % 4 == 0;
            if (taskViewHolder != null) {
                setProgress(taskViewHolder.pb, increaseBytes);

                final SpeedCalculator blockSpeed = blockSpeeds.get(blockIndex);
                blockSpeed.downloading(increaseBytes);
                final ViewHolder blockViewHolder = blockViewHolders.get(blockIndex);
                setProgress(blockViewHolder.pb, increaseBytes);
                if (isNeedUpdateSpeed) {
                    taskViewHolder.speedTv.setText(speed);
                    blockViewHolder.speedTv.setText(blockSpeed.speed());
                }
            }
        }

        @Override public void fetchEnd(DownloadTask task, int blockIndex,
                                       long contentLength) {
            updateStatus("fetchEnd " + blockIndex + " " + contentLength);
        }

        @Override public void taskEnd(DownloadTask task, EndCause cause,
                                      @Nullable Exception realCause) {
            taskSpeed.endTask();
            final String speedFromBegin = taskSpeed.speedFromBegin();
            updateStatus("taskEnd " + cause + " " + realCause + " " + speedFromBegin);

            if (taskViewHolder != null) {
                taskViewHolder.speedTv.setText(speedFromBegin);

                final int blockCount = blockViewHolders.size();
                for (int i = 0; i < blockCount; i++) {
                    final int blockIndex = blockViewHolders.keyAt(i);
                    final ViewHolder viewHolder = blockViewHolders.valueAt(i);
                    final SpeedCalculator blockSpeed = blockSpeeds.get(blockIndex);
                    viewHolder.speedTv.setText(blockSpeed.speedFromBegin());
                }
            }
        }

        private void updateStatus(String status) {
            Log.d(TAG, status);
            if (statusTv != null) statusTv.setText(status);
        }

        private void initInfo(BreakpointInfo info) {
            if (taskViewHolder == null) return;

            final TextView titleTv = taskViewHolder.titleTv;
            titleTv.setText(
                    titleTv.getContext().getString(R.string.task_title,
                            Util.humanReadableBytes(info.getTotalLength(), false)));
            setProgress(taskViewHolder.pb, info.getTotalLength(), info.getTotalOffset());
            resetBlocksInfo(info);

        }

        private void resetBlocksInfo(BreakpointInfo info) {
            if (taskViewHolder == null) return;


            // block
            final int blockCount = blockViewHolders.size();
            for (int i = 0; i < blockCount; i++) {
                final BlockInfo blockInfo = info.getBlock(i);
                final ViewHolder viewHolder = blockViewHolders.get(i);
                resetBlockInfo(viewHolder, i, blockInfo);
            }
        }

        private void resetBlockInfo(ViewHolder viewHolder, int blockIndex, BlockInfo blockInfo) {
            final TextView titleTv = viewHolder.titleTv;
            titleTv.setText(titleTv.getContext().getString(R.string.block_title, blockIndex,
                    Util.humanReadableBytes(blockInfo.getStartOffset(), false),
                    Util.humanReadableBytes(blockInfo.getRangeRight(), false)));

            setProgress(viewHolder.pb, blockInfo.getContentLength(),
                    blockInfo.getCurrentOffset());
        }
    }


    private static void setProgress(ProgressBar bar, long increaseLength) {
        final int shrinkRate = (int) bar.getTag();
        final int progress = (int) ((bar.getProgress() + increaseLength) / shrinkRate);

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

    interface FinishListener {
        void finish();
    }

    static class ViewHolder {
        final TextView titleTv;
        final TextView speedTv;
        final ProgressBar pb;

        ViewHolder(TextView titleTv, TextView speedTv, ProgressBar pb) {
            this.titleTv = titleTv;
            this.speedTv = speedTv;
            this.pb = pb;
        }
    }


    public static class Builder {
        private ViewHolder taskViewHolder;
        private SparseArray<ViewHolder> blockViewHolderMap = new SparseArray<>(4);
        private TextView statusTv;
        private TextView sameTaskTv;
        private TextView sameFileTv;

        public Builder setTaskViews(TextView titleTv, TextView speedTv, ProgressBar pb) {
            taskViewHolder = new ViewHolder(titleTv, speedTv, pb);
            return this;
        }

        public Builder setBlock0Views(TextView titleTv, TextView speedTv, ProgressBar pb) {
            blockViewHolderMap.put(0, new ViewHolder(titleTv, speedTv, pb));
            return this;
        }

        public Builder setBlock1Views(TextView titleTv, TextView speedTv, ProgressBar pb) {
            blockViewHolderMap.put(1, new ViewHolder(titleTv, speedTv, pb));
            return this;
        }

        public Builder setBlock2Views(TextView titleTv, TextView speedTv, ProgressBar pb) {
            blockViewHolderMap.put(2, new ViewHolder(titleTv, speedTv, pb));
            return this;
        }

        public Builder setBlock3Views(TextView titleTv, TextView speedTv, ProgressBar pb) {
            blockViewHolderMap.put(3, new ViewHolder(titleTv, speedTv, pb));
            return this;
        }

        public Builder setStatusTv(TextView statusTv) {
            this.statusTv = statusTv;
            return this;
        }

        public Builder setSameFileTv(TextView sameFileTv) {
            this.sameFileTv = sameFileTv;
            return this;
        }

        public Builder setSameTaskTv(TextView sameTaskTv) {
            this.sameTaskTv = sameTaskTv;
            return this;
        }

        public SingleTaskDemo build() {
            if (taskViewHolder == null || statusTv == null || sameTaskTv == null
                    || sameFileTv == null) {
                throw new IllegalArgumentException();
            }
            return new SingleTaskDemo(taskViewHolder, blockViewHolderMap, statusTv, sameTaskTv,
                    sameFileTv);
        }
    }
}
