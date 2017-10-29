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

package cn.dreamtobe.okdownload;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.dispatcher.DownloadDispatcher;

public class StatusUtil {

    public static boolean isSameTaskPendingOrRunning(@NonNull DownloadTask task) {
        return OkDownload.with().downloadDispatcher().findSameTask(task) != null;
    }

    public static Status getStatus(@NonNull String url, @NonNull String parentPath,
                                   @Nullable String filename) {
        if (filename != null && isCompleted(url, parentPath, filename)) return Status.COMPLETED;

        final DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();
        final DownloadTask finder = createFinder(url, parentPath, filename);

        if (dispatcher.isPending(finder)) return Status.PENDING;
        if (dispatcher.isRunning(finder)) return Status.RUNNING;

        if (filename == null) return Status.UNKNOWN;

        return Status.IDLE;
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
