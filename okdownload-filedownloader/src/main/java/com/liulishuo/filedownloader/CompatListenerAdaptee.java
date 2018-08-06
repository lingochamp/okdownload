/*
 * Copyright (c) 2018 LingoChamp Inc.
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

package com.liulishuo.filedownloader;

import android.support.annotation.NonNull;

public class CompatListenerAdaptee implements CompatListenerAssist.CompatListenerAssistCallback {

    @NonNull
    private final FileDownloadListener fileDownloadListener;

    public CompatListenerAdaptee(@NonNull FileDownloadListener fileDownloadListener) {
        this.fileDownloadListener = fileDownloadListener;
    }

    @Override
    public void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        if (fileDownloadListener instanceof FileDownloadLargeFileListener) {
            final FileDownloadLargeFileListener largeFileListener =
                    (FileDownloadLargeFileListener) fileDownloadListener;
            largeFileListener.pending(task, soFarBytes, totalBytes);
        } else {
            fileDownloadListener.pending(task, (int) soFarBytes, (int) totalBytes);
        }
    }

    @Override
    public void started(BaseDownloadTask task) {
        fileDownloadListener.started(task);
    }

    @Override
    public void connected(BaseDownloadTask task, String etag, boolean isContinue, long soFarBytes,
                          long totalBytes) {
        if (fileDownloadListener instanceof FileDownloadLargeFileListener) {
            final FileDownloadLargeFileListener largeFileListener =
                    (FileDownloadLargeFileListener) fileDownloadListener;
            largeFileListener.connected(task, etag, isContinue, soFarBytes, totalBytes);
        } else {
            fileDownloadListener.connected(task, etag, isContinue, (int) soFarBytes,
                    (int) totalBytes);
        }
    }

    @Override
    public void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        if (fileDownloadListener instanceof FileDownloadLargeFileListener) {
            ((FileDownloadLargeFileListener) fileDownloadListener)
                    .progress(task, soFarBytes, totalBytes);
        } else {
            fileDownloadListener.progress(task, (int) soFarBytes, (int) totalBytes);
        }
    }

    @Override
    public void blockComplete(BaseDownloadTask task) throws Throwable {
        fileDownloadListener.blockComplete(task);
    }

    @Override
    public void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, long soFarBytes) {
        if (fileDownloadListener instanceof FileDownloadLargeFileListener) {
            ((FileDownloadLargeFileListener) fileDownloadListener)
                    .retry(task, ex, retryingTimes, soFarBytes);
        } else {
            fileDownloadListener.retry(task, ex, retryingTimes, (int) soFarBytes);
        }
    }

    @Override
    public void completed(BaseDownloadTask task) {
        fileDownloadListener.completed(task);
    }

    @Override
    public void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        if (fileDownloadListener instanceof FileDownloadLargeFileListener) {
            ((FileDownloadLargeFileListener) fileDownloadListener)
                    .paused(task, soFarBytes, totalBytes);
        } else {
            fileDownloadListener.paused(task, (int) soFarBytes, (int) totalBytes);
        }
    }

    @Override
    public void error(BaseDownloadTask task, Throwable e) {
        fileDownloadListener.error(task, e);
    }

    @Override
    public void warn(BaseDownloadTask task) {
        fileDownloadListener.warn(task);
    }

}
