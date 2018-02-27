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

package com.liulishuo.okdownload.sample.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.sample.R;

import java.util.List;
import java.util.Map;

public class EachBlockProgressUtil {
    private static final String TAG = "EachBlockProgressUtil";

    public static void initTitle(@NonNull BreakpointInfo info,
                                 TextView taskTitleTv,
                                 TextView block0TitleTv, TextView block1TitleTv,
                                 TextView block2TitleTv, TextView block3TitleTv) {
        // task
        assembleTitleToView("Task", 0, info.getTotalLength(), taskTitleTv);

        // blocks
        final int blockCount = info.getBlockCount();
        for (int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
            final BlockInfo blockInfo = info.getBlock(blockIndex);
            if (blockIndex == 0) {
                assembleTitleToView("Block0",
                        blockInfo.getRangeLeft(), blockInfo.getRangeRight(),
                        block0TitleTv);
            } else if (blockIndex == 1) {
                assembleTitleToView("Block1",
                        blockInfo.getRangeLeft(), blockInfo.getRangeRight(),
                        block1TitleTv);
            } else if (blockIndex == 2) {
                assembleTitleToView("Block2",
                        blockInfo.getRangeLeft(), blockInfo.getRangeRight(),
                        block2TitleTv);
            } else if (blockIndex == 3) {
                assembleTitleToView("Block3",
                        blockInfo.getRangeLeft(), blockInfo.getRangeRight(),
                        block3TitleTv);
            } else {
                Log.w(TAG, "no more title view to display block: " + blockInfo);
            }
        }
    }

    public static void initProgress(@NonNull BreakpointInfo info,
                                    ProgressBar taskPb,
                                    ProgressBar block0Pb, ProgressBar block1Pb,
                                    ProgressBar block2Pb, ProgressBar block3Pb) {
        // task
        ProgressUtil.calcProgressToViewAndMark(taskPb,
                info.getTotalOffset(), info.getTotalLength());

        // blocks
        final int blockCount = info.getBlockCount();
        for (int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
            final BlockInfo blockInfo = info.getBlock(blockIndex);
            if (blockIndex == 0) {
                ProgressUtil.calcProgressToViewAndMark(block0Pb, blockInfo.getCurrentOffset(),
                        blockInfo.getContentLength());
            } else if (blockIndex == 1) {
                ProgressUtil.calcProgressToViewAndMark(block1Pb, blockInfo.getCurrentOffset(),
                        blockInfo.getContentLength());
            } else if (blockIndex == 2) {
                ProgressUtil.calcProgressToViewAndMark(block2Pb, blockInfo.getCurrentOffset(),
                        blockInfo.getContentLength());
            } else if (blockIndex == 3) {
                ProgressUtil.calcProgressToViewAndMark(block3Pb, blockInfo.getCurrentOffset(),
                        blockInfo.getContentLength());
            } else {
                Log.w(TAG, "no more progress to display block: " + blockInfo);
            }
        }
    }

    public static void updateProgress(ProgressBar progressBar, long currentOffset) {
        ProgressUtil.updateProgressToViewWithMark(progressBar, currentOffset);
    }

    @Nullable public static ProgressBar getProgressBar(final int blockIndex,
                                                       ProgressBar block0Pb, ProgressBar block1Pb,
                                                       ProgressBar block2Pb, ProgressBar block3Pb) {
        if (blockIndex == 0) {
            return block0Pb;
        } else if (blockIndex == 1) {
            return block1Pb;
        } else if (blockIndex == 2) {
            return block2Pb;
        } else if (blockIndex == 3) {
            return block3Pb;
        } else {
            return null;
        }
    }

    @Nullable public static TextView getSpeedTv(int blockIndex,
                                                final TextView block0SpeedTv,
                                                final TextView block1SpeedTv,
                                                final TextView block2SpeedTv,
                                                final TextView block3SpeedTv) {
        if (blockIndex == 0) {
            return block0SpeedTv;
        } else if (blockIndex == 1) {
            return block1SpeedTv;
        } else if (blockIndex == 2) {
            return block2SpeedTv;
        } else if (blockIndex == 3) {
            return block3SpeedTv;
        } else {
            return null;
        }
    }

    private static void assembleTitleToView(String prefix, long rangeLeft, long rangeRight,
                                            TextView titleTv) {
        final String readableRangeLeft = Util.humanReadableBytes(rangeLeft, true);
        final String readableRangeRight = Util.humanReadableBytes(rangeRight, true);

        final String readableRange = "(" + readableRangeLeft + "~" + readableRangeRight + ")";
        final String title = prefix + readableRange;
        titleTv.setText(title);
    }

    public static DownloadListener createSampleListener(final TextView extInfoTv) {
        return new DownloadListener() {
            @Override public void taskStart(@NonNull DownloadTask task) {
                extInfoTv.setText(R.string.task_start);
            }

            @Override
            public void connectTrialStart(@NonNull DownloadTask task,
                                          @NonNull Map<String, List<String>> requestHeaderFields) {
                extInfoTv.setText(R.string.connect_trial_start);
            }

            @Override
            public void connectTrialEnd(@NonNull DownloadTask task, int responseCode,
                                        @NonNull Map<String, List<String>> responseHeaderFields) {
                extInfoTv.setText(R.string.connect_trial_end);
            }

            @Override public void downloadFromBeginning(@NonNull DownloadTask task,
                                                        @NonNull BreakpointInfo info,
                                                        @NonNull ResumeFailedCause cause) {
                extInfoTv.setText(R.string.download_from_beginning);
            }

            @Override public void downloadFromBreakpoint(@NonNull DownloadTask task,
                                                         @NonNull BreakpointInfo info) {
                extInfoTv.setText(R.string.download_from_breakpoint);
            }

            @Override public void connectStart(@NonNull DownloadTask task, int blockIndex,
                                               @NonNull Map<String, List<String>> requestHeaders) {
                extInfoTv.setText(R.string.connect_start);
            }

            @Override
            public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode,
                                   @NonNull Map<String, List<String>> responseHeaders) {
                extInfoTv.setText(R.string.connect_end);
            }

            @Override
            public void fetchStart(@NonNull DownloadTask task, int blockIndex, long contentLength) {
                extInfoTv.setText(R.string.fetch_start);
            }

            @Override
            public void fetchProgress(@NonNull DownloadTask task, int blockIndex,
                                      long increaseBytes) {
                extInfoTv.setText(R.string.fetch_progress);
            }

            @Override
            public void fetchEnd(@NonNull DownloadTask task, int blockIndex, long contentLength) {
                extInfoTv.setText(R.string.fetch_end);
            }

            @Override
            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                                @Nullable Exception realCause) {
                final String status = "Task" + task.getId() + " End with: " + cause;
                extInfoTv.setText(status);
            }
        };
    }
}
