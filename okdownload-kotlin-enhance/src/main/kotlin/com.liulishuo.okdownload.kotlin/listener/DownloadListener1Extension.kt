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