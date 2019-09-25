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