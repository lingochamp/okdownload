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

public abstract class FileDownloadListener {

    public FileDownloadListener() {
    }

    public FileDownloadListener(int priority) {
    }

    protected boolean isInvalid() {
        return false;
    }

    protected abstract void pending(BaseDownloadTask task, int soFarBytes,
                                    int totalBytes);

    protected void started(BaseDownloadTask task) {
    }

    protected void connected(BaseDownloadTask task, String etag,
                             boolean isContinue, int soFarBytes, int totalBytes) {
    }

    protected abstract void progress(BaseDownloadTask task, int soFarBytes,
                                     int totalBytes);

    protected void blockComplete(BaseDownloadTask task) throws Throwable {
    }

    protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes,
                         int soFarBytes) {
    }

    protected abstract void completed(BaseDownloadTask task);

    protected abstract void paused(BaseDownloadTask task, int soFarBytes,
                                   int totalBytes);

    protected abstract void error(BaseDownloadTask task, Throwable e);

    protected abstract void warn(BaseDownloadTask task);

}
