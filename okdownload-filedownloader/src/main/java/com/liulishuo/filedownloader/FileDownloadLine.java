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

import android.app.Notification;

/**
 * The FileDownload synchronous line.
 *
 * @see FileDownloader#insureServiceBind()
 */

public class FileDownloadLine {

    @Deprecated
    public void startForeground(final int id, final Notification notification) {
        // do nothing
    }

    /**
     * The {@link FileDownloader#getSoFar(int)} request.
     */
    public long getSoFar(final int id) {
        return FileDownloader.getImpl().getSoFar(id);
    }

    /**
     * The {@link FileDownloader#getTotal(int)} request.
     */
    public long getTotal(final int id) {
        return FileDownloader.getImpl().getTotal(id);
    }

    /**
     * The {@link FileDownloader#getStatus(int, String)} request.
     */
    public byte getStatus(final int id, final String path) {
        return FileDownloader.getImpl().getStatus(id, path);
    }
}
