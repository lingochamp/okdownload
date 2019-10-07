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

package com.liulishuo.okdownload.kotlin.listener

import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.DownloadListener2
import java.lang.Exception

/**
 * A concise way to create a [DownloadListener2], only the [DownloadListener2.taskEnd] is necessary.
 */
fun createListener2(
    onTaskStart: onTaskStart = {},
    onTaskEnd: onTaskEnd
): DownloadListener2 = object : DownloadListener2() {
    override fun taskStart(task: DownloadTask) = onTaskStart(task)

    override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?) =
        onTaskEnd(task, cause, realCause)
}