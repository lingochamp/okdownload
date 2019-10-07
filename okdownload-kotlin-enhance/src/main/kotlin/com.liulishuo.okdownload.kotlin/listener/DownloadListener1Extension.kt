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
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import java.lang.Exception

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener1.taskStart].
 */
typealias onTaskStartWithModel = (task: DownloadTask, model: Listener1Assist.Listener1Model) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener1.retry].
 */
typealias onRetry = (task: DownloadTask, cause: ResumeFailedCause) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener1.connected].
 */
typealias onConnected = (
    task: DownloadTask,
    blockCount: Int,
    currentOffset: Long,
    totalLength: Long
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener1.progress].
 */
typealias onProgress = (task: DownloadTask, currentOffset: Long, totalLength: Long) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener1.taskEnd].
 */
typealias onTaskEndWithModel = (
    task: DownloadTask,
    cause: EndCause,
    realCause: Exception?,
    model: Listener1Assist.Listener1Model
) -> Unit

/**
 * A concise way to create a [DownloadListener1], only the
 * [DownloadListener1.taskEnd] is necessary.
 */
fun createListener1(
    taskStart: onTaskStartWithModel? = null,
    retry: onRetry? = null,
    connected: onConnected? = null,
    progress: onProgress? = null,
    taskEnd: onTaskEndWithModel
): DownloadListener1 = object : DownloadListener1() {
    override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
        taskStart?.invoke(task, model)
    }

    override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
        retry?.invoke(task, cause)
    }

    override fun connected(
        task: DownloadTask,
        blockCount: Int,
        currentOffset: Long,
        totalLength: Long
    ) {
        connected?.invoke(task, blockCount, currentOffset, totalLength)
    }

    override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
        progress?.invoke(task, currentOffset, totalLength)
    }

    override fun taskEnd(
        task: DownloadTask,
        cause: EndCause,
        realCause: Exception?,
        model: Listener1Assist.Listener1Model
    ) = taskEnd(task, cause, realCause, model)
}