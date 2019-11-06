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

package com.liulishuo.filedownloader.util;

import android.annotation.SuppressLint;
import android.content.Context;

import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.database.FileDownloadDatabase;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.OkHttpClient;

/**
 * The helper for cache the {@code appContext} and {@code OK_HTTP_CLIENT} for the main process and
 * the filedownloader process.
 */
public class FileDownloadHelper {

    @SuppressLint("StaticFieldLeak")
    private static Context appContext;

    public static void holdContext(Context context) {
        appContext = context;
    }

    public static Context getAppContext() {
        return appContext;
    }

    @SuppressWarnings("UnusedParameters")
    @Deprecated
    public interface IdGenerator {

        int transOldId(int oldId, String url, String path,
                       boolean pathAsDirectory);

        int generateId(String url, String path, boolean pathAsDirectory);
    }

    /**
     * This interface is replaced by
     * {@link com.liulishuo.okdownload.DownloadTask.Builder#setConnectionCount(int)} in OkDownload.
     */
    @Deprecated
    @SuppressWarnings("UnusedParameters")
    public interface ConnectionCountAdapter {
        int determineConnectionCount(int downloadId, String url, String path, long totalLength);
    }

    /**
     * Database relevant api have a big difference between OkDownload and FileDownloader.
     * On the other hand, reuse FileDownloader database is not recommended.
     */
    @Deprecated
    public interface DatabaseCustomMaker {
        FileDownloadDatabase customMake();
    }

    public interface OutputStreamCreator {
        /**
         * The output stream creator is used for creating {@link FileDownloadOutputStream} which is
         * used to write the input stream to the file for downloading.
         * <p>
         * <strong>Note:</strong> please create a output stream which append the content to the
         * exist file, which means that bytes would be written to the end of the file rather than
         * the beginning.
         *
         * @param file the file will used for storing the downloading content.
         * @return The output stream used to write downloading byte array to the {@code file}.
         * @throws FileNotFoundException if the file exists but is a directory
         *                               rather than a regular file, does not exist but cannot
         *                               be created, or cannot be opened for any other reason
         */
        FileDownloadOutputStream create(File file) throws IOException;

        /**
         * @return {@code true} if the {@link FileDownloadOutputStream} is created through
         * {@link #create(File)} support {@link FileDownloadOutputStream#seek(long)} function.
         * If the {@link FileDownloadOutputStream} is created through {@link #create(File)} doesn't
         * support {@link FileDownloadOutputStream#seek(long)}, please return {@code false}, in
         * order to let the internal mechanism can predict this situation, and handle it smoothly.
         */
        boolean supportSeek();
    }

    public interface ConnectionCreator {
        /**
         * The connection creator is used for creating {@link FileDownloadConnection} component
         * which is used to use some protocol to connect to the remote server.
         *
         * @param url the uniform resource locator, which direct the aim resource we need to connect
         * @return The connection creator.
         * @throws IOException if an I/O exception occurs.
         */
        FileDownloadConnection create(String url) throws IOException;
    }

    public interface OkHttpClientCustomMaker {

        /**
         * @return Nullable, Customize {@link OkHttpClient}, will be used for downloading files.
         * @see OkHttpClient
         */
        OkHttpClient customMake();
    }
}

