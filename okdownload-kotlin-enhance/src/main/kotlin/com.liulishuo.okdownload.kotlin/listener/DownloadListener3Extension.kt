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
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3

typealias taskCallback = (task: DownloadTask) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.started].
 */
typealias onStarted = taskCallback

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.completed].
 */
typealias onCompleted = taskCallback

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.canceled].
 */
typealias onCanceled = taskCallback

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.warn].
 */
typealias onWarn = taskCallback

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.error].
 */
typealias onError = (task: DownloadTask, e: Exception) -> Unit

/**
 * @param onCanceled
 * @param onError
 * @param onCompleted
 * @param onWarn
 * Only one of these four callbacks will be active and their default value are all null.
 * @param onTerminal will be invoked after any of those four callbacks every time. If you
 * don't care onCanceled/onError/onCompleted/onWarn but you want to do something after
 * download finished, you can just provide this parameter.
 */
fun createListener3(
    onStarted: onStarted? = null,
    onConnected: onConnected? = null,
    onProgress: onProgress? = null,
    onCompleted: onCompleted? = null,
    onCanceled: onCanceled? = null,
    onWarn: onWarn? = null,
    onRetry: onRetry? = null,
    onError: onError? = null,
    onTerminal: () -> Unit = {}
): DownloadListener3 = object : DownloadListener3() {
    override fun warn(task: DownloadTask) {
        onWarn?.invoke(task)
        onTerminal.invoke()
    }

    override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
        onRetry?.invoke(task, cause)
    }

    override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
        onConnected?.invoke(task, blockCount, currentOffset, totalLength)
    }

    override fun started(task: DownloadTask) {
        onStarted?.invoke(task)
    }

    override fun completed(task: DownloadTask) {
        onCompleted?.invoke(task)
        onTerminal.invoke()
    }

    override fun canceled(task: DownloadTask) {
        onCanceled?.invoke(task)
        onTerminal.invoke()
    }

    override fun error(task: DownloadTask, e: java.lang.Exception) {
        onError?.invoke(task, e)
        onTerminal.invoke()
    }

    override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
        onProgress?.invoke(task, currentOffset, totalLength)
    }
}