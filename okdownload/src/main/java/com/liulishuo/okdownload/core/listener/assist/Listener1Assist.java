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

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.concurrent.atomic.AtomicLong;

public class Listener1Assist implements ListenerAssist,
        ListenerModelHandler.ModelCreator<Listener1Assist.Listener1Model> {
    private final ListenerModelHandler<Listener1Model> modelHandler;
    private Listener1Callback callback;

    public Listener1Assist() {
        this.modelHandler = new ListenerModelHandler<>(this);
    }

    Listener1Assist(ListenerModelHandler<Listener1Model> handler) {
        this.modelHandler = handler;
    }

    public void setCallback(@NonNull Listener1Callback callback) {
        this.callback = callback;
    }

    public void taskStart(DownloadTask task) {
        final Listener1Model model = modelHandler.addAndGetModel(task, null);
        if (callback != null) callback.taskStart(task, model);
    }

    public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
        Listener1Model model = modelHandler.removeOrCreate(task, task.getInfo());
        if (callback != null) callback.taskEnd(task, cause, realCause, model);
    }

    public void downloadFromBeginning(DownloadTask task,
                                      @NonNull BreakpointInfo info,
                                      ResumeFailedCause cause) {
        final Listener1Model model = modelHandler.getOrRecoverModel(task, info);
        if (model == null) return;
        model.onInfoValid(info);

        if (model.isStarted && callback != null) {
            callback.retry(task, cause);
        }

        model.isStarted = true;
        model.isFromResumed = false;
        model.isFirstConnect = true;
    }

    public void downloadFromBreakpoint(DownloadTask task, @NonNull BreakpointInfo info) {
        final Listener1Model model = modelHandler.getOrRecoverModel(task, info);
        if (model == null) return;
        model.onInfoValid(info);

        model.isStarted = true;
        model.isFromResumed = true;
        model.isFirstConnect = true;
    }


    public void connectEnd(DownloadTask task) {
        final Listener1Model model = modelHandler.getOrRecoverModel(task, task.getInfo());
        if (model == null) return;

        if (model.isFromResumed != null && model.isFromResumed && model.isFirstConnect) {
            model.isFirstConnect = false;
        }

        if (callback != null) {
            callback.connected(task, model.blockCount, model.currentOffset.get(),
                    model.totalLength);
        }
    }

    public void fetchProgress(DownloadTask task, long increaseBytes) {
        final Listener1Model model = modelHandler.getOrRecoverModel(task, task.getInfo());
        if (model == null) return;

        model.currentOffset.addAndGet(increaseBytes);
        if (callback != null) callback.progress(task, model.currentOffset.get(), model.totalLength);
    }

    @Override public boolean isAlwaysRecoverAssistModel() {
        return modelHandler.isAlwaysRecoverAssistModel();
    }

    @Override public void setAlwaysRecoverAssistModel(boolean isAlwaysRecoverAssistModel) {
        modelHandler.setAlwaysRecoverAssistModel(isAlwaysRecoverAssistModel);
    }

    @Override public void setAlwaysRecoverAssistModelIfNotSet(boolean isAlwaysRecoverAssistModel) {
        modelHandler.setAlwaysRecoverAssistModelIfNotSet(isAlwaysRecoverAssistModel);
    }

    @Override public Listener1Model create(int id) {
        return new Listener1Model(id);
    }

    public static class Listener1Model implements ListenerModelHandler.ListenerModel {
        final int id;
        Boolean isStarted;
        Boolean isFromResumed;

        volatile Boolean isFirstConnect;

        int blockCount;
        long totalLength;
        final AtomicLong currentOffset = new AtomicLong();

        Listener1Model(int id) {
            this.id = id;
        }

        public long getTotalLength() {
            return totalLength;
        }

        @Override public int getId() {
            return id;
        }

        @Override public void onInfoValid(@NonNull BreakpointInfo info) {
            blockCount = info.getBlockCount();
            totalLength = info.getTotalLength();
            currentOffset.set(info.getTotalOffset());

            if (isStarted == null) isStarted = false;
            if (isFromResumed == null) isFromResumed = currentOffset.get() > 0;
            if (isFirstConnect == null) isFirstConnect = true;
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
