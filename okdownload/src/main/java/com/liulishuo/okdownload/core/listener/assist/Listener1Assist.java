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
        final Listener1Model model;

        synchronized (this) {
            if (singleTaskModel != null && singleTaskModel.id == id) {
                model = singleTaskModel;
                singleTaskModel = null;
            } else {
                model = modelList.get(id);
                modelList.remove(id);
            }
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
                                      ResumeFailedCause cause) {
        final Listener1Model model = findModel(task.getId());
        if (model == null) return;

        if (model.isStarted && callback != null) {
            callback.retry(task, cause);
        }

        model.blockCount = 0;
        model.totalLength = 0;
        model.currentOffset.set(0);

        model.isStarted = true;
        model.isFromResumed = false;
        model.isFirstConnect = true;
    }

    public void downloadFromBreakpoint(int id, BreakpointInfo info) {
        final Listener1Model model = findModel(id);
        if (model == null) return;

        model.blockCount = info.getBlockCount();
        model.totalLength = info.getTotalLength();
        model.currentOffset.set(info.getTotalOffset());

        model.isStarted = true;
        model.isFromResumed = true;
        model.isFirstConnect = true;
    }

    public void connectEnd(DownloadTask task) {
        final Listener1Model model = findModel(task.getId());
        if (model == null) return;

        if (model.isFromResumed && model.isFirstConnect) {
            model.isFirstConnect = false;
            // from break point.
            if (callback != null) {
                callback.connected(task, model.blockCount, model.currentOffset.get(),
                        model.totalLength);
            }
        }
    }

    public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        final Listener1Model model = findModel(task.getId());
        if (model == null) return;

        model.blockCount = info.getBlockCount();
        model.totalLength = info.getTotalLength();
        model.currentOffset.set(info.getTotalOffset());

        // if not from resume we get info after block end, so callback on here.
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
        void taskStart(DownloadTask task, @NonNull Listener1Model model);

        void retry(DownloadTask task, @NonNull ResumeFailedCause cause);

        void connected(DownloadTask task, int blockCount, long currentOffset, long totalLength);

        void progress(DownloadTask task, long currentOffset, long totalLength);

        void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause,
                     @NonNull Listener1Model model);
    }
}
