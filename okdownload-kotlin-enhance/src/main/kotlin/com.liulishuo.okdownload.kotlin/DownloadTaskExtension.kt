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
import com.liulishuo.okdownload.kotlin.listener.createListener
import com.liulishuo.okdownload.kotlin.listener.createListener1
import com.liulishuo.okdownload.kotlin.listener.createListener2
import com.liulishuo.okdownload.kotlin.listener.createListener3
import com.liulishuo.okdownload.kotlin.listener.createListener4
import com.liulishuo.okdownload.kotlin.listener.onBlockEnd
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
import com.liulishuo.okdownload.kotlin.listener.onProgress
import com.liulishuo.okdownload.kotlin.listener.onProgressBlock
import com.liulishuo.okdownload.kotlin.listener.onProgressWithoutTotalLength
import com.liulishuo.okdownload.kotlin.listener.onRetry
import com.liulishuo.okdownload.kotlin.listener.onStarted
import com.liulishuo.okdownload.kotlin.listener.onTaskEnd
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithListener4Model
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithModel
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import com.liulishuo.okdownload.kotlin.listener.onTaskStartWithModel
import com.liulishuo.okdownload.kotlin.listener.onWarn

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

fun DownloadTask.execute2(
    onTaskStart: onTaskStart = {},
    onTaskEnd: onTaskEnd
) {
    val listener2 = createListener2(onTaskStart, onTaskEnd)
    execute(listener2)
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

fun DownloadTask.execute4(
    onTaskStart: onTaskStart? = null,
    onConnectStart: onConnectStart? = null,
    onConnectEnd: onConnectEnd? = null,
    onInfoReady: onInfoReady? = null,
    onProgressBlock: onProgressBlock? = null,
    onProgressWithoutTotalLength: onProgressWithoutTotalLength? = null,
    onBlockEnd: onBlockEnd? = null,
    onTaskEndWithListener4Model: onTaskEndWithListener4Model
) {
    val listener4 = createListener4(
        onTaskStart,
        onConnectStart,
        onConnectEnd,
        onInfoReady,
        onProgressBlock,
        onProgressWithoutTotalLength,
        onBlockEnd,
        onTaskEndWithListener4Model
    )
    execute(listener4)
}