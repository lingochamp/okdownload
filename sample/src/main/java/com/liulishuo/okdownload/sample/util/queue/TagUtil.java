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

package com.liulishuo.okdownload.sample.util.queue;

import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;

class TagUtil {
    private static final int KEY_STATUS = 0;
    private static final int KEY_OFFSET = 1;
    private static final int KEY_TOTAL = 2;
    private static final int KEY_TASK_NAME = 3;
    private static final int KEY_PRIORITY = 4;

    static void saveStatus(DownloadTask task, String status) {
        task.addTag(KEY_STATUS, status);
    }

    @Nullable static String getStatus(DownloadTask task) {
        final Object status = task.getTag(KEY_STATUS);
        return status != null ? (String) status : null;
    }

    static void saveOffset(DownloadTask task, long offset) {
        task.addTag(KEY_OFFSET, offset);
    }

    static long getOffset(DownloadTask task) {
        final Object offset = task.getTag(KEY_OFFSET);
        return offset != null ? (long) offset : 0;
    }

    static void saveTotal(DownloadTask task, long total) {
        task.addTag(KEY_TOTAL, total);
    }

    static long getTotal(DownloadTask task) {
        final Object total = task.getTag(KEY_TOTAL);
        return total != null ? (long) total : 0;
    }

    static void saveTaskName(DownloadTask task, String name) {
        task.addTag(KEY_TASK_NAME, name);
    }

    static String getTaskName(DownloadTask task) {
        final Object taskName = task.getTag(KEY_TASK_NAME);
        return taskName != null ? (String) taskName : null;
    }

    static void savePriority(DownloadTask task, int priority) {
        task.addTag(KEY_PRIORITY, priority);
    }

    static int getPriority(DownloadTask task) {
        final Object priority = task.getTag(KEY_PRIORITY);
        return priority != null ? (int) priority : 0;
    }

    static void clearProceedTask(DownloadTask task) {
        task.removeTag(KEY_STATUS);
        task.removeTag(KEY_OFFSET);
        task.removeTag(KEY_TOTAL);
        task.removeTag(KEY_OFFSET);
    }
}