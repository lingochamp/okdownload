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

import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import java.lang.Exception

/**
 * Correspond to [com.liulishuo.okdownload.DownloadContextListener.taskEnd].
 */
typealias onQueueTaskEnd = (
    context: DownloadContext,
    task: DownloadTask,
    cause: EndCause,
    realException: Exception?,
    remainCount: Int
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadContextListener.queueEnd].
 */
typealias onQueueEnd = (context: DownloadContext) -> Unit

/**
 * A concise way to create a [DownloadContextListener], only the
 * [DownloadContextListener.queueEnd] is necessary.
 */
fun createDownloadContextListener(
    onQueueTaskEnd: onQueueTaskEnd? = null,
    onQueueEnd: onQueueEnd
): DownloadContextListener = object : DownloadContextListener {
    override fun taskEnd(
        context: DownloadContext,
        task: DownloadTask,
        cause: EndCause,
        realCause: Exception?,
        remainCount: Int
    ) {
        onQueueTaskEnd?.invoke(context, task, cause, realCause, remainCount)
    }

    override fun queueEnd(context: DownloadContext) = onQueueEnd(context)
}