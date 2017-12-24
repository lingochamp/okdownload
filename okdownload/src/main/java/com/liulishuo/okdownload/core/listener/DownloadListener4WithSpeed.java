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

package com.liulishuo.okdownload.core.listener;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;

public abstract class DownloadListener4WithSpeed extends DownloadListener4 {
    private final SpeedCalculator taskSpeed = new SpeedCalculator();
    private final SparseArray<SpeedCalculator> blockSpeeds = new SparseArray<>();

    @NonNull protected SpeedCalculator taskSpeed() {
        return taskSpeed;
    }

    @NonNull protected SpeedCalculator blockSpeed(int blockIndex) {
        return blockSpeeds.get(blockIndex);
    }

    @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        taskSpeed.downloading(increaseBytes);
        blockSpeeds.get(blockIndex).downloading(increaseBytes);

        super.fetchProgress(task, blockIndex, increaseBytes);
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex, long contentLength) {
        blockSpeeds.get(blockIndex).endTask();
        super.fetchEnd(task, blockIndex, contentLength);
    }

    @Override public void taskStart(DownloadTask task) {
        taskSpeed.reset();
        blockSpeeds.clear();
    }

    @Override
    public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
        taskSpeed.endTask();
        taskEnd(task, cause, realCause, taskSpeed.speedFromBegin());
    }

    protected abstract void taskEnd(DownloadTask task, EndCause cause,
                                    @Nullable Exception realCause,
                                    @NonNull String averageSpeed);

    @Override protected void initData(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                      boolean fromBreakpoint) {
        super.initData(task, info, fromBreakpoint);
        initSpeed(info);
    }

    private void initSpeed(BreakpointInfo info) {
        taskSpeed.reset();
        blockSpeeds.clear();
        final int blockCount = info.getBlockCount();
        for (int i = 0; i < blockCount; i++) blockSpeeds.put(i, new SpeedCalculator());
    }
}
