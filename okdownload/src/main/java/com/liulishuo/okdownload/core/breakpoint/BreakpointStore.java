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

package com.liulishuo.okdownload.core.breakpoint;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;

import java.io.IOException;

public interface BreakpointStore {

    @Nullable BreakpointInfo get(int id);

    @NonNull BreakpointInfo createAndInsert(@NonNull DownloadTask task) throws IOException;

    void onTaskStart(int id);

    void onSyncToFilesystemSuccess(@NonNull BreakpointInfo info, int blockIndex,
                                   long increaseLength);

    boolean update(@NonNull BreakpointInfo breakpointInfo) throws IOException;

    void onTaskEnd(int id, @NonNull EndCause cause, @Nullable Exception exception);

    void bunchTaskCanceled(int[] ids);

    void discard(int id);

    int findOrCreateId(@NonNull DownloadTask task);

    @Nullable BreakpointInfo findAnotherInfoFromCompare(DownloadTask task, BreakpointInfo ignored);

    @Nullable String getResponseFilename(String url);
}
