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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;

import java.io.File;

public class StatusUtil {

    public static boolean isSameTaskPendingOrRunning(@NonNull DownloadTask task) {
        return OkDownload.with().downloadDispatcher().findSameTask(task) != null;
    }

    public static Status getStatus(@NonNull DownloadTask task) {

        final Status status = isCompletedOrUnknown(task);
        if (status == Status.COMPLETED) return Status.COMPLETED;

        final DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();

        if (dispatcher.isPending(task)) return Status.PENDING;
        if (dispatcher.isRunning(task)) return Status.RUNNING;

        return status;
    }

    public static Status getStatus(@NonNull String url, @NonNull String parentPath,
                                   @Nullable String filename) {
        return getStatus(createFinder(url, parentPath, filename));
    }

    public static boolean isCompleted(@NonNull DownloadTask task) {
        return isCompletedOrUnknown(task) == Status.COMPLETED;
    }

    public static Status isCompletedOrUnknown(@NonNull DownloadTask task) {
        final BreakpointStore store = OkDownload.with().breakpointStore();
        final BreakpointInfo info = store.get(task.getId());

        @Nullable String filename = task.getFilename();
        @NonNull final File parentFile = task.getParentFile();
        @Nullable final File targetFile = task.getFile();

        if (info != null) {
            if (!info.isChunked() && info.getTotalLength() <= 0) {
                return Status.UNKNOWN;
            } else if ((targetFile != null && targetFile.equals(info.getFile()))
                    && targetFile.exists()
                    && info.getTotalOffset() == info.getTotalLength()) {
                return Status.COMPLETED;
            } else if (filename == null && info.getFile() != null
                    && info.getFile().exists()) {
                return Status.IDLE;
            } else if (targetFile != null && targetFile.equals(info.getFile())
                    && targetFile.exists()) {
                return Status.IDLE;
            }
        } else if (store.isOnlyMemoryCache()) {
            return Status.UNKNOWN;
        } else if (targetFile != null && targetFile.exists()) {
            return Status.COMPLETED;
        } else {
            filename = store.getResponseFilename(task.getUrl());
            if (filename != null && new File(parentFile, filename).exists()) {
                return Status.COMPLETED;
            }
        }

        return Status.UNKNOWN;
    }

    public static boolean isCompleted(@NonNull String url, @NonNull String parentPath,
                                      @Nullable String filename) {
        return isCompleted(createFinder(url, parentPath, filename));
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
        return new DownloadTask.Builder(url, parentPath, filename)
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
