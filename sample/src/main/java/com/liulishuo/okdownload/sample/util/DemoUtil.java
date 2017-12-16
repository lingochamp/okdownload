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

package com.liulishuo.okdownload.sample.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ProgressBar;

import com.liulishuo.okdownload.DownloadTask;

import java.io.File;

public class DemoUtil {

    public static DownloadTask createTask(Context context, String filename, String url,
                                          int progressIntervalMillis) {
        final File parentFile = DemoUtil.getParentFile(context);
        return new DownloadTask.Builder(url, parentFile)
                .setFilename(filename)
                .setMinIntervalMillisCallbackProcess(progressIntervalMillis)
                .build();
    }

    public static DownloadTask createTask(Context context, String filename,
                                          int progressIntervalMillis) {
        final String url =
                "https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk";
        return createTask(context, filename, url, progressIntervalMillis);
    }

    public static void calcProgressToView(ProgressBar progressBar, long offset, long total) {
        final float percent = (float) offset / total;
        progressBar.setProgress((int) (percent * progressBar.getMax()));
    }


    public static File getParentFile(@NonNull Context context) {
        final File externalSaveDir = context.getExternalCacheDir();
        if (externalSaveDir == null) {
            return context.getCacheDir();
        } else {
            return externalSaveDir;
        }
    }
}
