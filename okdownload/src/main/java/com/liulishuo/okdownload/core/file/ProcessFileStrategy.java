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

package com.liulishuo.okdownload.core.file;

import android.support.annotation.NonNull;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;

import java.io.File;
import java.io.IOException;

public class ProcessFileStrategy {
    private final FileLock fileLock = new FileLock();

    @NonNull public MultiPointOutputStream createProcessStream(@NonNull DownloadTask task,
                                                               @NonNull BreakpointInfo info,
                                                               @NonNull DownloadStore store) {
        return new MultiPointOutputStream(task, info, store);
    }

    public void completeProcessStream(@NonNull MultiPointOutputStream processOutputStream,
                                      @NonNull DownloadTask task) {
    }

    public void discardProcess(@NonNull DownloadTask task) throws IOException {
        // Remove target file.
        final File file = task.getFile();
        // Do nothing, because the filename hasn't found yet.
        if (file == null) return;

        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Delete file failed!");
            }
        }
    }

    @NonNull public FileLock getFileLock() {
        return fileLock;
    }

    public boolean isPreAllocateLength() {
        // if support seek, enable pre-allocate length.
        return OkDownload.with().outputStreamFactory().supportSeek();
    }
}
