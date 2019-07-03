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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.assist.Listener4Assist;
import com.liulishuo.okdownload.core.listener.assist.ListenerAssist;
import com.liulishuo.okdownload.core.listener.assist.ListenerModelHandler;

import java.util.List;
import java.util.Map;

/**
 * When download from resume:
 * taskStart->infoReady->connectStart->connectEnd->
 * (progressBlock(blockIndex,currentOffset)->progress(currentOffset))
 * <-->
 * (progressBlock(blockIndex,currentOffset)->progress(currentOffset))
 * ->blockEnd->taskEnd
 * </p>
 * When download from beginning:
 * taskStart->connectStart->connectEnd->infoReady->
 * (progress(currentOffset)->progressBlock(blockIndex,currentOffset))
 * <-->
 * (progress(currentOffset)->progressBlock(blockIndex,currentOffset))
 * ->blockEnd->taskEnd
 */
public abstract class DownloadListener4 implements DownloadListener,
        Listener4Assist.Listener4Callback, ListenerAssist {

    final Listener4Assist assist;

    DownloadListener4(Listener4Assist assist) {
        this.assist = assist;
        assist.setCallback(this);
    }

    public DownloadListener4() {
        this(new Listener4Assist<>(new Listener4ModelCreator()));
    }

    public void setAssistExtend(@NonNull Listener4Assist.AssistExtend assistExtend) {
        this.assist.setAssistExtend(assistExtend);
    }

    @Override public boolean isAlwaysRecoverAssistModel() {
        return assist.isAlwaysRecoverAssistModel();
    }

    @Override public void setAlwaysRecoverAssistModel(boolean isAlwaysRecoverAssistModel) {
        assist.setAlwaysRecoverAssistModel(isAlwaysRecoverAssistModel);
    }

    @Override public void setAlwaysRecoverAssistModelIfNotSet(boolean isAlwaysRecoverAssistModel) {
        assist.setAlwaysRecoverAssistModelIfNotSet(isAlwaysRecoverAssistModel);
    }

    @Override
    public void connectTrialStart(@NonNull DownloadTask task,
                                  @NonNull Map<String, List<String>> requestHeaderFields) {
    }

    @Override public void connectTrialEnd(@NonNull DownloadTask task, int responseCode,
                                          @NonNull Map<String, List<String>> responseHeaderFields) {
    }

    @Override public final void downloadFromBeginning(@NonNull DownloadTask task,
                                                      @NonNull BreakpointInfo info,
                                                      @NonNull ResumeFailedCause cause) {
        assist.infoReady(task, info, false);
    }

    @Override public final void downloadFromBreakpoint(@NonNull DownloadTask task,
                                                       @NonNull BreakpointInfo info) {
        assist.infoReady(task, info, true);
    }

    @Override
    public void fetchStart(@NonNull DownloadTask task, int blockIndex, long contentLength) {
    }

    @Override
    public final void fetchProgress(@NonNull DownloadTask task, int blockIndex,
                                    long increaseBytes) {
        assist.fetchProgress(task, blockIndex, increaseBytes);
    }

    @Override public void fetchEnd(@NonNull DownloadTask task, int blockIndex, long contentLength) {
        assist.fetchEnd(task, blockIndex);
    }

    @Override
    public final void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                              @Nullable Exception realCause) {
        assist.taskEnd(task, cause, realCause);
    }

    static class Listener4ModelCreator implements
            ListenerModelHandler.ModelCreator<Listener4Assist.Listener4Model> {
        @Override public Listener4Assist.Listener4Model create(int id) {
            return new Listener4Assist.Listener4Model(id);
        }
    }
}
