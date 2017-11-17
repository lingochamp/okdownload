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

package com.liulishuo.okdownload.sample;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.ProgressBar;

import java.io.File;

public class DemoUtil {

    public static File getParentFile(@NonNull Context context) {
        final File externalSaveDir = context.getExternalCacheDir();
        if (externalSaveDir == null) {
            return context.getCacheDir();
        } else {
            return externalSaveDir;
        }
    }

    public static void setProgress(ProgressBar bar, long currentOffset) {
        if (bar.getTag() == null) return;

        final int shrinkRate = (int) bar.getTag();
        final int progress = (int) ((currentOffset) / shrinkRate);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            bar.setProgress(progress, true);
        } else {
            bar.setProgress(progress);
        }
    }

    public static void setProgress(ProgressBar bar, long contentLength, long beginOffset) {
        final int contentLengthOnInt = reducePrecision(contentLength);
        final int shrinkRate = contentLengthOnInt == 0
                ? 1 : (int) (contentLength / contentLengthOnInt);
        bar.setTag(shrinkRate);
        final int progress = (int) (beginOffset / shrinkRate);


        bar.setMax(contentLengthOnInt);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            bar.setProgress(progress, true);
        } else {
            bar.setProgress(progress);
        }
    }

    private static int reducePrecision(long origin) {
        if (origin <= Integer.MAX_VALUE) return (int) origin;

        int shrinkRate = 10;
        long result = origin;
        while (result > Integer.MAX_VALUE) {
            result /= shrinkRate;
            shrinkRate *= 5;
        }

        return (int) result;
    }
}
