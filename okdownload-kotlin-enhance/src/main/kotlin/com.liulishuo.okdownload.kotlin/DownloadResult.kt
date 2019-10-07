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

import com.liulishuo.okdownload.core.cause.EndCause

data class DownloadResult(val cause: EndCause) {
    fun becauseOfCompleted(): Boolean = cause == EndCause.COMPLETED
    fun becauseOfRepeatedTask(): Boolean =
        cause == EndCause.SAME_TASK_BUSY || cause == EndCause.FILE_BUSY
}