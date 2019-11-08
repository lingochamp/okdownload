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

package com.liulishuo.filedownloader.services;

import android.content.Context;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.connection.DownloadConnectionAdapter;
import com.liulishuo.filedownloader.stream.DownloadOutputStreamAdapter;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.okdownload.OkDownload;

/**
 * Params in this class is used in the downloading manager, and would be used for initialize the
 * download manager in the process the downloader service settled on.
 */
public class DownloadMgrInitialParams {

    public static class InitCustomMaker {
        FileDownloadHelper.OutputStreamCreator mOutputStreamCreator;
        FileDownloadHelper.ConnectionCreator mConnectionCreator;
        final Context mContext;

        public InitCustomMaker(Context context) {
            this.mContext = context;
        }

        public InitCustomMaker idGenerator(FileDownloadHelper.IdGenerator idGenerator) {
            return this;
        }

        public InitCustomMaker connectionCountAdapter(
                FileDownloadHelper.ConnectionCountAdapter adapter) {
            return this;
        }

        public InitCustomMaker database(FileDownloadHelper.DatabaseCustomMaker maker) {
            return this;
        }

        /**
         * The max thread count in OkDownload thread pool is Integer.MAX,
         * this configuration is useless.
         */
        public InitCustomMaker maxNetworkThreadCount(int maxNetworkThreadCount) {
            return this;
        }

        /**
         * Customize the output stream component.
         * <p>
         * If you don't customize the output stream component, we use the result of
         * {@link com.liulishuo.okdownload.core.file.DownloadUriOutputStream} as the default one.
         *
         * @param creator The output stream creator is used for creating
         *                {@link FileDownloadOutputStream} which is used to write the input stream
         *                to the file for downloading.
         */
        public InitCustomMaker outputStreamCreator(FileDownloadHelper.OutputStreamCreator creator) {
            this.mOutputStreamCreator = creator;
            return this;
        }

        /**
         * Customize the connection component.
         * <p>
         * If you don't customize the connection component, we use
         * {@link com.liulishuo.okdownload.core.connection.DownloadUrlConnection}
         *
         * @param creator the connection creator will used for create the connection when start
         *                downloading any task in the FileDownloader.
         */
        public InitCustomMaker connectionCreator(FileDownloadHelper.ConnectionCreator creator) {
            this.mConnectionCreator = creator;
            return this;
        }

        public InitCustomMaker foregroundServiceConfig(ForegroundServiceConfig config) {
            return this;
        }

        public void commit() {
            FileDownloader.setup(mContext);
            OkDownload.Builder builder = FileDownloader.okDownloadBuilder(mContext, null);
            if (mOutputStreamCreator != null) {
                if (builder == null) builder = new OkDownload.Builder(mContext);
                builder.outputStreamFactory(
                        new DownloadOutputStreamAdapter.Factory(mOutputStreamCreator));
            }
            if (mConnectionCreator != null) {
                if (builder == null) builder = new OkDownload.Builder(mContext);
                builder.connectionFactory(
                        new DownloadConnectionAdapter.Factory(mConnectionCreator));
            }
            if (builder != null) OkDownload.setSingletonInstance(builder.build());
        }
    }
}
