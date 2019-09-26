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