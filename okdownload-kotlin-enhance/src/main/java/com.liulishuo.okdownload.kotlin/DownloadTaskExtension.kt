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

import com.liulishuo.okdownload.DownloadListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import com.liulishuo.okdownload.kotlin.listener.onCanceled
import com.liulishuo.okdownload.kotlin.listener.onCompleted
import com.liulishuo.okdownload.kotlin.listener.onConnectEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectStart
import com.liulishuo.okdownload.kotlin.listener.onConnectTrialEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectTrialStart
import com.liulishuo.okdownload.kotlin.listener.onConnected
import com.liulishuo.okdownload.kotlin.listener.onDownloadFromBeginning
import com.liulishuo.okdownload.kotlin.listener.onDownloadFromBreakpoint
import com.liulishuo.okdownload.kotlin.listener.onError
import com.liulishuo.okdownload.kotlin.listener.onFetchEnd
import com.liulishuo.okdownload.kotlin.listener.onFetchProgress
import com.liulishuo.okdownload.kotlin.listener.onFetchStart
import com.liulishuo.okdownload.kotlin.listener.onProgress
import com.liulishuo.okdownload.kotlin.listener.onRetry
import com.liulishuo.okdownload.kotlin.listener.onStarted
import com.liulishuo.okdownload.kotlin.listener.onTaskEnd
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithModel
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import com.liulishuo.okdownload.kotlin.listener.onTaskStartWithModel
import com.liulishuo.okdownload.kotlin.listener.onWarn
import java.lang.Exception

/**
 * Correspond to [DownloadTask.execute]
 */
fun DownloadTask.execute(
    onTaskStart: onTaskStart? = null,
    onConnectTrialStart: onConnectTrialStart? = null,
    onConnectTrialEnd: onConnectTrialEnd? = null,
    onDownloadFromBeginning: onDownloadFromBeginning? = null,
    onDownloadFromBreakpoint: onDownloadFromBreakpoint? = null,
    onConnectStart: onConnectStart? = null,
    onConnectEnd: onConnectEnd? = null,
    onFetchStart: onFetchStart? = null,
    onFetchProgress: onFetchProgress? = null,
    onFetchEnd: onFetchEnd? = null,
    onTaskEnd: onTaskEnd
) {
    val listener = createListener(
        onTaskStart,
        onConnectTrialStart,
        onConnectTrialEnd,
        onDownloadFromBeginning,
        onDownloadFromBreakpoint,
        onConnectStart,
        onConnectEnd,
        onFetchStart,
        onFetchProgress,
        onFetchEnd,
        onTaskEnd
    )
    execute(listener)
}

fun DownloadTask.execute1(
    taskStart: onTaskStartWithModel? = null,
    retry: onRetry? = null,
    connected: onConnected? = null,
    progress: onProgress? = null,
    taskEnd: onTaskEndWithModel
) {
    val listener1 = createListener1(taskStart, retry, connected, progress, taskEnd)
    execute(listener1)
}

fun DownloadTask.execute3(
    onStarted: onStarted? = null,
    onConnected: onConnected? = null,
    onProgress: onProgress? = null,
    onCompleted: onCompleted? = null,
    onCanceled: onCanceled? = null,
    onWarn: onWarn? = null,
    onRetry: onRetry? = null,
    onError: onError? = null,
    onTerminal: () -> Unit = {}
) {
    val listener3 = createListener3(
        onStarted,
        onConnected,
        onProgress,
        onCompleted,
        onCanceled,
        onWarn,
        onRetry,
        onError,
        onTerminal
    )
    execute(listener3)
}

fun DownloadTask.createListener(
    onTaskStart: onTaskStart? = null,
    onConnectTrialStart: onConnectTrialStart? = null,
    onConnectTrialEnd: onConnectTrialEnd? = null,
    onDownloadFromBeginning: onDownloadFromBeginning? = null,
    onDownloadFromBreakpoint: onDownloadFromBreakpoint? = null,
    onConnectStart: onConnectStart? = null,
    onConnectEnd: onConnectEnd? = null,
    onFetchStart: onFetchStart? = null,
    onFetchProgress: onFetchProgress? = null,
    onFetchEnd: onFetchEnd? = null,
    onTaskEnd: onTaskEnd
): DownloadListener {
    return object : DownloadListener {
        override fun taskStart(task: DownloadTask) {
            onTaskStart?.invoke(task)
        }

        override fun connectTrialStart(
            task: DownloadTask,
            requestHeaderFields: MutableMap<String, MutableList<String>>
        ) {
            onConnectTrialStart?.invoke(task, requestHeaderFields)
        }

        override fun connectTrialEnd(
            task: DownloadTask,
            responseCode: Int,
            responseHeaderFields: MutableMap<String, MutableList<String>>
        ) {
            onConnectTrialEnd?.invoke(task, responseCode, responseHeaderFields)
        }

        override fun downloadFromBeginning(
            task: DownloadTask,
            info: BreakpointInfo,
            cause: ResumeFailedCause
        ) {
            onDownloadFromBeginning?.invoke(task, info, cause)
        }

        override fun downloadFromBreakpoint(task: DownloadTask, info: BreakpointInfo) {
            onDownloadFromBreakpoint?.invoke(task, info)
        }

        override fun connectStart(
            task: DownloadTask,
            blockIndex: Int,
            requestHeaderFields: MutableMap<String, MutableList<String>>
        ) {
            onConnectStart?.invoke(task, blockIndex, requestHeaderFields)
        }

        override fun connectEnd(
            task: DownloadTask,
            blockIndex: Int,
            responseCode: Int,
            responseHeaderFields: MutableMap<String, MutableList<String>>
        ) {
            onConnectEnd?.invoke(task, blockIndex, responseCode, responseHeaderFields)
        }

        override fun fetchStart(task: DownloadTask, blockIndex: Int, contentLength: Long) {
            onFetchStart?.invoke(task, blockIndex, contentLength)
        }

        override fun fetchProgress(task: DownloadTask, blockIndex: Int, increaseBytes: Long) {
            onFetchProgress?.invoke(task, blockIndex, increaseBytes)
        }

        override fun fetchEnd(task: DownloadTask, blockIndex: Int, contentLength: Long) {
            onFetchEnd?.invoke(task, blockIndex, contentLength)
        }

        override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?) {
            onTaskEnd.invoke(task, cause, realCause)
        }
    }
}

fun DownloadTask.createListener1(
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
fun DownloadTask.createListener3(
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

    override fun error(task: DownloadTask, e: Exception) {
        onError?.invoke(task, e)
        onTerminal.invoke()
    }

    override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
        onProgress?.invoke(task, currentOffset, totalLength)
    }
}