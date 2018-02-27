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

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.concurrent.atomic.AtomicLong;

public class Listener1Assist {
    private Listener1Model singleTaskModel;
    private final SparseArray<Listener1Model> modelList = new SparseArray<>();

    private Listener1Callback callback;

    public void setCallback(@NonNull Listener1Callback callback) {
        this.callback = callback;
    }

    public void taskStart(DownloadTask task) {
        final int id = task.getId();
        final Listener1Model model = new Listener1Model(id);
        synchronized (this) {
            if (singleTaskModel == null) {
                singleTaskModel = model;
            } else {
                modelList.put(id, model);
            }
        }

        if (callback != null) callback.taskStart(task, model);
    }

    public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
        final int id = task.getId();
        Listener1Model model;

        synchronized (this) {
            if (singleTaskModel != null && singleTaskModel.id == id) {
                model = singleTaskModel;
                singleTaskModel = null;
            } else {
                model = modelList.get(id);
                modelList.remove(id);
            }
        }

        if (model == null) {
            model = new Listener1Model(task.getId());
        }

        if (callback != null) callback.taskEnd(task, cause, realCause, model);
    }

    @Nullable public Listener1Model getSingleTaskModel() {
        return singleTaskModel;
    }

    public Listener1Model findModel(int id) {
        if (singleTaskModel != null && singleTaskModel.id == id) return singleTaskModel;

        return modelList.get(id);
    }

    public void downloadFromBeginning(DownloadTask task,
                                      @NonNull BreakpointInfo info,
                                      ResumeFailedCause cause) {
        final Listener1Model model = assignModelIfNeed(task.getId(), info);
        if (model == null) return;

        if (model.isStarted && callback != null) {
            callback.retry(task, cause);
        }

        model.isStarted = true;
        model.isFromResumed = false;
        model.isFirstConnect = true;
    }

    public void downloadFromBreakpoint(int id, @NonNull BreakpointInfo info) {
        final Listener1Model model = assignModelIfNeed(id, info);
        if (model == null) return;

        model.isStarted = true;
        model.isFromResumed = true;
        model.isFirstConnect = true;
    }

    @Nullable private Listener1Model assignModelIfNeed(int id, @NonNull BreakpointInfo info) {
        final Listener1Model model = findModel(id);
        if (model == null) return null;

        model.blockCount = info.getBlockCount();
        model.totalLength = info.getTotalLength();
        model.currentOffset.set(info.getTotalOffset());

        return model;
    }

    public void connectEnd(DownloadTask task) {
        final Listener1Model model = findModel(task.getId());
        if (model == null) return;

        if (model.isFromResumed && model.isFirstConnect) {
            model.isFirstConnect = false;
        }

        if (callback != null) {
            callback.connected(task, model.blockCount, model.currentOffset.get(),
                    model.totalLength);
        }
    }

    public void fetchProgress(DownloadTask task, long increaseBytes) {
        final Listener1Model model = findModel(task.getId());
        if (model == null) return;

        model.currentOffset.addAndGet(increaseBytes);
        if (callback != null) callback.progress(task, model.currentOffset.get(), model.totalLength);
    }


    public static class Listener1Model {
        final int id;
        boolean isStarted;
        boolean isFromResumed;

        volatile boolean isFirstConnect;

        int blockCount;
        long totalLength;
        final AtomicLong currentOffset = new AtomicLong();

        Listener1Model(int id) {
            this.id = id;
        }

        public long getTotalLength() {
            return totalLength;
        }

        public int getId() {
            return id;
        }
    }

    public interface Listener1Callback {
        void taskStart(@NonNull DownloadTask task, @NonNull Listener1Model model);

        void retry(@NonNull DownloadTask task, @NonNull ResumeFailedCause cause);

        void connected(@NonNull DownloadTask task, @IntRange(from = 0) int blockCount,
                       @IntRange(from = 0) long currentOffset,
                       @IntRange(from = 0) long totalLength);

        void progress(@NonNull DownloadTask task, @IntRange(from = 0) long currentOffset,
                      @IntRange(from = 0) long totalLength);

        void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                     @Nullable Exception realCause,
                     @NonNull Listener1Model model);
    }
}
