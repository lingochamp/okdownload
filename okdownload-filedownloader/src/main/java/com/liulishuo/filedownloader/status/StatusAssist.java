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

package com.liulishuo.filedownloader.status;

import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;

public class StatusAssist {

    private byte status = FileDownloadStatus.INVALID_STATUS;

    private DownloadTask downloadTask;

    public synchronized void setDownloadTask(DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    public synchronized DownloadTask getDownloadTask() {
        return downloadTask;
    }

    public synchronized byte getStatus() {
        if (downloadTask == null) {
            return status;
        }
        StatusUtil.Status okDownloadStatus = StatusUtil.getStatus(downloadTask);
        status = convert(okDownloadStatus);
        return status;
    }

    synchronized byte convert(StatusUtil.Status status) {
        switch (status) {
            case COMPLETED:
                return FileDownloadStatus.completed;
            case IDLE:
                return FileDownloadStatus.paused;
            case UNKNOWN:
                return FileDownloadStatus.INVALID_STATUS;
            case PENDING:
                return FileDownloadStatus.pending;
            case RUNNING:
                return FileDownloadStatus.progress;
            default:
                return FileDownloadStatus.INVALID_STATUS;
        }
    }

    public synchronized boolean isUsing() {
        return getStatus() != FileDownloadStatus.INVALID_STATUS;
    }

    public synchronized boolean isOver() {
        return FileDownloadStatus.isOver(getStatus());
    }
}
