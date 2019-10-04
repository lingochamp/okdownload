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

import com.liulishuo.okdownload.DownloadListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.DownloadListener2
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.liulishuo.okdownload.core.listener.DownloadListener4
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed
import java.lang.Exception

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.taskStart]
 */
typealias onTaskStart = (task: DownloadTask) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.connectTrialStart]
 */
typealias onConnectTrialStart = (
    task: DownloadTask,
    requestHeaderFields: Map<String, List<String>>
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.connectTrialEnd]
 */
typealias onConnectTrialEnd = (
    task: DownloadTask,
    responseCode: Int,
    responseHeaderFields: Map<String, List<String>>
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.downloadFromBeginning]
 */
typealias onDownloadFromBeginning = (
    task: DownloadTask,
    info: BreakpointInfo,
    cause: ResumeFailedCause
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.downloadFromBreakpoint]
 */
typealias onDownloadFromBreakpoint = (task: DownloadTask, info: BreakpointInfo) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.connectStart]
 */
typealias onConnectStart = (
    task: DownloadTask,
    blockIndex: Int,
    requestHeaderFields: Map<String, List<String>>
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.connectEnd]
 */
typealias onConnectEnd = (
    task: DownloadTask,
    blockIndex: Int,
    responseCode: Int,
    responseHeaderFields: Map<String, List<String>>
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.fetchStart]
 */
typealias onFetchStart = (task: DownloadTask, blockIndex: Int, contentLength: Long) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.fetchProgress]
 */
typealias onFetchProgress = (task: DownloadTask, blockIndex: Int, increaseBytes: Long) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.fetchEnd]
 */
typealias onFetchEnd = onFetchStart

/**
 * Correspond to [com.liulishuo.okdownload.DownloadListener.taskEnd]
 */
typealias onTaskEnd = (task: DownloadTask, cause: EndCause, realCause: Exception?) -> Unit

fun createListener(
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

fun DownloadListener.switchToExceptProgressListener(): DownloadListener = when (this) {
    is DownloadListener4WithSpeed -> createListener4WithSpeed(
        onTaskStart = { this.taskStart(it) },
        onConnectStart = { task, blockIndex, requestHeaderFields ->
            this.connectStart(task, blockIndex, requestHeaderFields)
        },
        onConnectEnd = { task, blockIndex, responseCode, responseHeaderFields ->
            this.connectEnd(task, blockIndex, responseCode, responseHeaderFields)
        },
        onInfoReadyWithSpeed = { task, info, fromBreakpoint, model ->
            this.infoReady(task, info, fromBreakpoint, model)
        },
        onProgressBlockWithSpeed = { task, blockIndex, currentBlockOffset, blockSpeed ->
            this.progressBlock(task, blockIndex, currentBlockOffset, blockSpeed)
        },
        onBlockEndWithSpeed = { task, blockIndex, info, blockSpeed ->
            this.blockEnd(task, blockIndex, info, blockSpeed)
        }
    ) { task, cause, realCause, taskSpeed ->
        this.taskEnd(task, cause, realCause, taskSpeed)
    }
    is DownloadListener4 -> createListener4(
        onTaskStart = { this.taskStart(it) },
        onConnectStart = { task, blockIndex, requestHeaderFields ->
            this.connectStart(task, blockIndex, requestHeaderFields)
        },
        onConnectEnd = { task, blockIndex, responseCode, responseHeaderFields ->
            this.connectEnd(task, blockIndex, responseCode, responseHeaderFields)
        },
        onInfoReady = { task, info, fromBreakpoint, model ->
            this.infoReady(task, info, fromBreakpoint, model)
        },
        onProgressBlock = { task, blockIndex, currentBlockOffset ->
            this.progressBlock(task, blockIndex, currentBlockOffset)
        },
        onBlockEnd = { task, blockIndex, info ->
            this.blockEnd(task, blockIndex, info)
        }
    ) { task, cause, realCause, model ->
        this.taskEnd(task, cause, realCause, model)
    }
    is DownloadListener3 -> createListener3(
        onStarted = { this.taskStart(it) },
        onConnected = { task, blockCount, currentOffset, totalLength ->
            this.connected(task, blockCount, currentOffset, totalLength)
        },
        onCompleted = { this.taskEnd(it, EndCause.COMPLETED, null) },
        onCanceled = { this.taskEnd(it, EndCause.CANCELED, null) },
        onWarn = { this.taskEnd(it, EndCause.SAME_TASK_BUSY, null) },
        onRetry = { task, cause -> this.retry(task, cause) },
        onError = { task, e -> this.taskEnd(task, EndCause.ERROR, e) }
    )
    is DownloadListener1 -> createListener1(
        taskStart = { task, model -> this.taskStart(task, model) },
        retry = { task, cause -> this.retry(task, cause) },
        connected = { task, blockIndex, currentOffset, totalLength ->
            this.connected(task, blockIndex, currentOffset, totalLength)
        }
    ) { task, cause, realCause, model ->
        this.taskEnd(task, cause, realCause, model)
    }
    // DownloadListener2 doesn't concern progress originally
    is DownloadListener2 -> this
    // the origin download listener is DownloadListener or DownloadListenerBunch
    else -> createListener(
        onTaskStart = { this.taskStart(it) },
        onConnectTrialStart = { task, requestFields ->
            this.connectTrialStart(task, requestFields)
        },
        onConnectTrialEnd = { task, responseCode, responseHeaderFields ->
            this.connectTrialEnd(task, responseCode, responseHeaderFields)
        },
        onDownloadFromBeginning = { task, info, cause ->
            this.downloadFromBeginning(task, info, cause)
        },
        onDownloadFromBreakpoint = { task, info ->
            this.downloadFromBreakpoint(task, info)
        },
        onConnectStart = { task, blockIndex, requestHeaderFields ->
            this.connectStart(task, blockIndex, requestHeaderFields)
        },
        onConnectEnd = { task, blockIndex, responseCode, responseHeaderFields ->
            this.connectEnd(task, blockIndex, responseCode, responseHeaderFields)
        },
        onFetchStart = { task, blockIndex, contentLength ->
            this.fetchStart(task, blockIndex, contentLength)
        },
        onFetchEnd = { task, blockIndex, contentLength ->
            this.fetchEnd(task, blockIndex, contentLength)
        }
    ) { task, cause, realCause ->
        this.taskEnd(task, cause, realCause)
    }
}