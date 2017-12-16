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

package com.liulishuo.okdownload.sample.comprehensive.multiple;

import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.sample.comprehensive.single.SingleTaskUtil;


public class MultipleTaskUtil {
    static final String URL1 = SingleTaskUtil.URL;
    static final String URL2 = "http://dldir1.qq.com/weixin/android/weixin6516android1120.apk";
    static final String URL3 =
            "http://download.chinaunix.net/down.php?id=10608&ResourceID=5267&site=1";
    static final String URL4 = "http://7xjww9.com1.z0.glb.clouddn.com/Hopetoun_falls.jpg";
    static final String URL5 = "http://dg.101.hk/1.rar";
    static final String URL6 =
            "http://180.153.105.144/dd.myapp.com/16891/E2F3DEBB12A049ED921C6257C5E9FB11.apk";
    static final String URL7 = "http://mirror.internode.on.net/pub/test/10meg.test4";
    static final String URL8 = "http://www.pc6.com/down.asp?id=72873";

    private static final int KEY_STATUS = 0;
    private static final int KEY_OFFSET = 1;
    private static final int KEY_TOTAL = 2;

    static void saveStatus(DownloadTask task, String status) {
        task.addTag(KEY_STATUS, status);
    }

    @Nullable static String getStatus(DownloadTask task) {
        return task.getTag(KEY_STATUS) != null ? (String) task.getTag(KEY_STATUS) : null;
    }

    static void saveOffset(DownloadTask task, long offset) {
        task.addTag(KEY_OFFSET, offset);
    }

    static long getOffset(DownloadTask task) {
        return task.getTag(KEY_OFFSET) != null ? (long) task.getTag(KEY_OFFSET) : 0;
    }

    static void saveTotal(DownloadTask task, long total) {
        task.addTag(KEY_TOTAL, total);
    }

    static long getTotal(DownloadTask task) {
        return task.getTag(KEY_TOTAL) != null ? (long) task.getTag(KEY_TOTAL) : 0;
    }

}
