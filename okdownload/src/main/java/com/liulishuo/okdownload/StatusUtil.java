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

package com.liulishuo.okdownload;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;

public class StatusUtil {

    public static boolean isSameTaskPendingOrRunning(@NonNull DownloadTask task) {
        return OkDownload.with().downloadDispatcher().findSameTask(task) != null;
    }

    public static Status getStatus(@NonNull DownloadTask task) {

        if (isCompleted(task)) return Status.COMPLETED;

        final DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();

        if (dispatcher.isPending(task)) return Status.PENDING;
        if (dispatcher.isRunning(task)) return Status.RUNNING;

        if (task.getFilename() == null) return Status.UNKNOWN;

        return Status.IDLE;
    }

    public static Status getStatus(@NonNull String url, @NonNull String parentPath,
                                   @Nullable String filename) {
        return getStatus(createFinder(url, parentPath, filename));
    }

    public static boolean isCompleted(@NonNull DownloadTask task) {
        if (task.getFilename() == null) return false; //unknown filename can't recognize

        return isCompleted(task.getUrl(), task.getParentPath(), task.getFilename());
    }

    public static boolean isCompleted(@NonNull String url, @NonNull String parentPath,
                                      @NonNull String filename) {
        final BreakpointStore store = OkDownload.with().breakpointStore();
        final int id = store.findOrCreateId(createFinder(url, parentPath, filename));

        // because we remove info if task is completed, so if it exist it must be not completed.
        return store.get(id) == null && new File(parentPath, filename).exists();
    }

    @Nullable public static BreakpointInfo getCurrentInfo(@NonNull String url,
                                                          @NonNull String parentPath,
                                                          @Nullable String filename) {
        return getCurrentInfo(createFinder(url, parentPath, filename));
    }

    @Nullable public static BreakpointInfo getCurrentInfo(@NonNull DownloadTask task) {
        final BreakpointStore store = OkDownload.with().breakpointStore();
        final int id = store.findOrCreateId(task);

        final BreakpointInfo info = store.get(id);

        return info == null ? null : info.copy();
    }

    @NonNull static DownloadTask createFinder(@NonNull String url,
                                              @NonNull String parentPath,
                                              @Nullable String filename) {
        final Uri uri = Uri.fromFile(new File(parentPath));

        return new DownloadTask.Builder(url, uri)
                .setFilename(filename)
                .build();
    }

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        IDLE,
        // may completed, but no filename can't ensure.
        UNKNOWN
    }
}
