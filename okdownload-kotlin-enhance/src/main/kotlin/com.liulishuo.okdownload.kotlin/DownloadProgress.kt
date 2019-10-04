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

package com.liulishuo.okdownload.kotlin

import com.liulishuo.okdownload.DownloadTask

data class DownloadProgress(val task: DownloadTask, val currentOffset: Long, val totalOffset: Long) {
    companion object {
        private const val UNKNOWN_TOTAL_OFFSET = -1L
        const val UNKNOWN_PROGRESS = 0f
    }

    fun totalUnknown(): Boolean = totalOffset == UNKNOWN_TOTAL_OFFSET

    fun progress(): Float = when (totalOffset) {
        UNKNOWN_TOTAL_OFFSET -> UNKNOWN_PROGRESS
        0L -> if (currentOffset == 0L) 1f else UNKNOWN_PROGRESS
        else -> currentOffset * 1.0f / totalOffset
    }
}