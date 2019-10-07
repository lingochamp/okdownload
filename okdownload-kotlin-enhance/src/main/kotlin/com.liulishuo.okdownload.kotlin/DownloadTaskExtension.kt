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
import com.liulishuo.okdownload.kotlin.listener.createListener
import com.liulishuo.okdownload.kotlin.listener.createListener1
import com.liulishuo.okdownload.kotlin.listener.createListener2
import com.liulishuo.okdownload.kotlin.listener.createListener3
import com.liulishuo.okdownload.kotlin.listener.createListener4
import com.liulishuo.okdownload.kotlin.listener.createListener4WithSpeed
import com.liulishuo.okdownload.kotlin.listener.onBlockEnd
import com.liulishuo.okdownload.kotlin.listener.onBlockEndWithSpeed
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
import com.liulishuo.okdownload.kotlin.listener.onInfoReady
import com.liulishuo.okdownload.kotlin.listener.onInfoReadyWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onProgress
import com.liulishuo.okdownload.kotlin.listener.onProgressBlock
import com.liulishuo.okdownload.kotlin.listener.onProgressBlockWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onProgressWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onProgressWithoutTotalLength
import com.liulishuo.okdownload.kotlin.listener.onRetry
import com.liulishuo.okdownload.kotlin.listener.onStarted
import com.liulishuo.okdownload.kotlin.listener.onTaskEnd
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithListener4Model
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithModel
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import com.liulishuo.okdownload.kotlin.listener.onTaskStartWithModel
import com.liulishuo.okdownload.kotlin.listener.onWarn
import com.liulishuo.okdownload.kotlin.listener.switchToExceptProgressListener
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Correspond to [DownloadTask.execute].
 * This method will create a [com.liulishuo.okdownload.DownloadListener] instance internally.
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
) = execute(createListener(
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
))

/**
 * Correspond to [DownloadTask.execute].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener1]
 * instance internally.
 */
fun DownloadTask.execute1(
    taskStart: onTaskStartWithModel? = null,
    retry: onRetry? = null,
    connected: onConnected? = null,
    progress: onProgress? = null,
    taskEnd: onTaskEndWithModel
) = execute(createListener1(taskStart, retry, connected, progress, taskEnd))

/**
 * Correspond to [DownloadTask.execute].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener2]
 * instance internally.
 */
fun DownloadTask.execute2(
    onTaskStart: onTaskStart = {},
    onTaskEnd: onTaskEnd
) = execute(createListener2(onTaskStart, onTaskEnd))

/**
 * Correspond to [DownloadTask.execute].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener3]
 * instance internally.
 */
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
) = execute(createListener3(
    onStarted,
    onConnected,
    onProgress,
    onCompleted,
    onCanceled,
    onWarn,
    onRetry,
    onError,
    onTerminal
))

/**
 * Correspond to [DownloadTask.execute].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener4]
 * instance internally.
 */
fun DownloadTask.execute4(
    onTaskStart: onTaskStart? = null,
    onConnectStart: onConnectStart? = null,
    onConnectEnd: onConnectEnd? = null,
    onInfoReady: onInfoReady? = null,
    onProgressBlock: onProgressBlock? = null,
    onProgressWithoutTotalLength: onProgressWithoutTotalLength? = null,
    onBlockEnd: onBlockEnd? = null,
    onTaskEndWithListener4Model: onTaskEndWithListener4Model
) = execute(createListener4(
    onTaskStart,
    onConnectStart,
    onConnectEnd,
    onInfoReady,
    onProgressBlock,
    onProgressWithoutTotalLength,
    onBlockEnd,
    onTaskEndWithListener4Model
))

/**
 * Correspond to [DownloadTask.execute].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed]
 * instance internally.
 */
fun DownloadTask.execute4WithSpeed(
    onTaskStart: onTaskStart? = null,
    onConnectStart: onConnectStart? = null,
    onConnectEnd: onConnectEnd? = null,
    onInfoReadyWithSpeed: onInfoReadyWithSpeed? = null,
    onProgressBlockWithSpeed: onProgressBlockWithSpeed? = null,
    onProgressWithSpeed: onProgressWithSpeed? = null,
    onBlockEndWithSpeed: onBlockEndWithSpeed? = null,
    onTaskEndWithSpeed: onTaskEndWithSpeed
) = execute(createListener4WithSpeed(
    onTaskStart,
    onConnectStart,
    onConnectEnd,
    onInfoReadyWithSpeed,
    onProgressBlockWithSpeed,
    onProgressWithSpeed,
    onBlockEndWithSpeed,
    onTaskEndWithSpeed
))

/**
 * Correspond to [DownloadTask.enqueue].
 * This method will create a [com.liulishuo.okdownload.DownloadListener] instance internally.
 */
fun DownloadTask.enqueue(
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
) = enqueue(createListener(
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
))

/**
 * Correspond to [DownloadTask.enqueue].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener1]
 * instance internally.
 */
fun DownloadTask.enqueue1(
    taskStart: onTaskStartWithModel? = null,
    retry: onRetry? = null,
    connected: onConnected? = null,
    progress: onProgress? = null,
    taskEnd: onTaskEndWithModel
) = enqueue(createListener1(taskStart, retry, connected, progress, taskEnd))

/**
 * Correspond to [DownloadTask.enqueue].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener2]
 * instance internally.
 */
fun DownloadTask.enqueue2(
    onTaskStart: onTaskStart = {},
    onTaskEnd: onTaskEnd
) = enqueue(createListener2(onTaskStart, onTaskEnd))

/**
 * Correspond to [DownloadTask.enqueue].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener3]
 * instance internally.
 */
fun DownloadTask.enqueue3(
    onStarted: onStarted? = null,
    onConnected: onConnected? = null,
    onProgress: onProgress? = null,
    onCompleted: onCompleted? = null,
    onCanceled: onCanceled? = null,
    onWarn: onWarn? = null,
    onRetry: onRetry? = null,
    onError: onError? = null,
    onTerminal: () -> Unit = {}
) = enqueue(createListener3(
    onStarted,
    onConnected,
    onProgress,
    onCompleted,
    onCanceled,
    onWarn,
    onRetry,
    onError,
    onTerminal
))

/**
 * Correspond to [DownloadTask.enqueue].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener4]
 * instance internally.
 */
fun DownloadTask.enqueue4(
    onTaskStart: onTaskStart? = null,
    onConnectStart: onConnectStart? = null,
    onConnectEnd: onConnectEnd? = null,
    onInfoReady: onInfoReady? = null,
    onProgressBlock: onProgressBlock? = null,
    onProgressWithoutTotalLength: onProgressWithoutTotalLength? = null,
    onBlockEnd: onBlockEnd? = null,
    onTaskEndWithListener4Model: onTaskEndWithListener4Model
) = enqueue(createListener4(
    onTaskStart,
    onConnectStart,
    onConnectEnd,
    onInfoReady,
    onProgressBlock,
    onProgressWithoutTotalLength,
    onBlockEnd,
    onTaskEndWithListener4Model
))

/**
 * Correspond to [DownloadTask.enqueue].
 * This method will create a [com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed]
 * instance internally.
 */
fun DownloadTask.enqueue4WithSpeed(
    onTaskStart: onTaskStart? = null,
    onConnectStart: onConnectStart? = null,
    onConnectEnd: onConnectEnd? = null,
    onInfoReadyWithSpeed: onInfoReadyWithSpeed? = null,
    onProgressBlockWithSpeed: onProgressBlockWithSpeed? = null,
    onProgressWithSpeed: onProgressWithSpeed? = null,
    onBlockEndWithSpeed: onBlockEndWithSpeed? = null,
    onTaskEndWithSpeed: onTaskEndWithSpeed
) = enqueue(createListener4WithSpeed(
    onTaskStart,
    onConnectStart,
    onConnectEnd,
    onInfoReadyWithSpeed,
    onProgressBlockWithSpeed,
    onProgressWithSpeed,
    onBlockEndWithSpeed,
    onTaskEndWithSpeed
))

/**
 * This method will create a [Channel] to represents a single download task's progress.
 * In this way, the DownloadTask is a producer and the returned [Channel] can be consumed
 * by specific consumer.
 *
 * Note: This method must be invoked after the DownloadTask is started, otherwise, there is no any
 * effect. For example:
 * ```
 * val task = DownloadTask.Builder().build()
 * task.enqueue() { task, cause, realCause ->   }
 *
 * val progressChannel = task.spChannel()
 * runBlocking {
 *     for (dp in progressChannel) {
 *         // show progress
 *     }
 * }
 * ```
 */
fun DownloadTask.spChannel(): Channel<DownloadProgress> {
    val channel = Channel<DownloadProgress>(Channel.CONFLATED)
    val oldListener = listener
    val progressListener = createListener1(
        progress = { task, currentOffset, totalLength ->
            channel.offer(DownloadProgress(task, currentOffset, totalLength))
        }
    ) { _, _, _, _ -> channel.close() }.also { it.setAlwaysRecoverAssistModelIfNotSet(true) }
    val replaceListener = createReplaceListener(oldListener, progressListener)
    replaceListener(replaceListener)
    return channel
}

/**
 * Returns a [DownloadListener] to replace old listener in [DownloadTask].
 * @param oldListener responses all callbacks except progress.
 * @param progressListener only responses progress callback.
 */
internal fun createReplaceListener(
    oldListener: DownloadListener?,
    progressListener: DownloadListener
): DownloadListener {
    if (oldListener == null) {
        return progressListener
    }
    val exceptProgressListener = oldListener.switchToExceptProgressListener()
    return createListener(
        onTaskStart = {
            exceptProgressListener.taskStart(it)
            progressListener.taskStart(it)
        },
        onConnectTrialStart = { task, requestFields ->
            exceptProgressListener.connectTrialStart(task, requestFields)
        },
        onConnectTrialEnd = { task, responseCode, responseHeaderFields ->
            exceptProgressListener.connectTrialEnd(task, responseCode, responseHeaderFields)
        },
        onDownloadFromBeginning = { task, info, cause ->
            exceptProgressListener.downloadFromBeginning(task, info, cause)
            progressListener.downloadFromBeginning(task, info, cause)
        },
        onDownloadFromBreakpoint = { task, info ->
            exceptProgressListener.downloadFromBreakpoint(task, info)
            progressListener.downloadFromBreakpoint(task, info)
        },
        onConnectStart = { task, blockIndex, requestHeaderFields ->
            exceptProgressListener.connectStart(task, blockIndex, requestHeaderFields)
        },
        onConnectEnd = { task, blockIndex, responseCode, responseHeaderFields ->
            exceptProgressListener.connectEnd(task, blockIndex, responseCode, responseHeaderFields)
        },
        onFetchStart = { task, blockIndex, contentLength ->
            exceptProgressListener.fetchStart(task, blockIndex, contentLength)
        },
        onFetchEnd = { task, blockIndex, contentLength ->
            exceptProgressListener.fetchEnd(task, blockIndex, contentLength)
        },
        onFetchProgress = { task, blockIndex, increaseBytes ->
            progressListener.fetchProgress(task, blockIndex, increaseBytes)
        }
    ) { task, cause, realCause ->
        exceptProgressListener.taskEnd(task, cause, realCause)
        progressListener.taskEnd(task, cause, realCause)
    }
}

internal fun CancellableContinuation<*>.cancelDownloadOnCancellation(task: DownloadTask) =
    invokeOnCancellation { task.cancel() }

/**
 * Awaits for completion of the [DownloadTask] without blocking current thread.
 * Returns [DownloadResult] or throws corresponding exception if there is any error occurred.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function
 * is waiting, this function immediately resumes with [CancellationException]
 */
suspend fun DownloadTask.await(): DownloadResult = suspendCancellableCoroutine { cont ->
    val listener2 = createListener2({
        cont.cancelDownloadOnCancellation(this)
    }) { _, cause, realCause ->
        if (realCause != null) {
            cont.resumeWithException(realCause)
        } else {
            cont.resume(DownloadResult(cause))
        }
    }
    enqueue(listener2)
}