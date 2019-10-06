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

package com.liulishuo.okdownload.sample.util.queue

import com.liulishuo.okdownload.DownloadTask

internal object TagUtil {
    private const val KEY_STATUS = 0
    private const val KEY_OFFSET = 1
    private const val KEY_TOTAL = 2
    private const val KEY_TASK_NAME = 3
    private const val KEY_PRIORITY = 4

    fun saveStatus(task: DownloadTask, status: String) {
        task.addTag(KEY_STATUS, status)
    }

    fun getStatus(task: DownloadTask): String? {
        val status = task.getTag(KEY_STATUS)
        return if (status != null) status as String else null
    }

    fun saveOffset(task: DownloadTask, offset: Long) {
        task.addTag(KEY_OFFSET, offset)
    }

    fun getOffset(task: DownloadTask): Long {
        val offset = task.getTag(KEY_OFFSET)
        return if (offset != null) offset as Long else 0
    }

    fun saveTotal(task: DownloadTask, total: Long) {
        task.addTag(KEY_TOTAL, total)
    }

    fun getTotal(task: DownloadTask): Long {
        val total = task.getTag(KEY_TOTAL)
        return if (total != null) total as Long else 0
    }

    fun saveTaskName(task: DownloadTask, name: String) {
        task.addTag(KEY_TASK_NAME, name)
    }

    fun getTaskName(task: DownloadTask): String? {
        val taskName = task.getTag(KEY_TASK_NAME)
        return if (taskName != null) taskName as String else null
    }

    fun savePriority(task: DownloadTask, priority: Int) {
        task.addTag(KEY_PRIORITY, priority)
    }

    fun getPriority(task: DownloadTask): Int {
        val priority = task.getTag(KEY_PRIORITY)
        return if (priority != null) priority as Int else 0
    }

    fun clearProceedTask(task: DownloadTask) {
        task.removeTag(KEY_STATUS)
        task.removeTag(KEY_OFFSET)
        task.removeTag(KEY_TOTAL)
        task.removeTag(KEY_OFFSET)
    }
}