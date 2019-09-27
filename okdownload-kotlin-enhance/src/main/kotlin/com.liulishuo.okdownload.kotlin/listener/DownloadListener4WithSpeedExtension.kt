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
import com.liulishuo.okdownload.SpeedCalculator
import com.liulishuo.okdownload.core.breakpoint.BlockInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed.taskStart]
 */
typealias onInfoReadyWithSpeed = (
    task: DownloadTask,
    info: BreakpointInfo,
    fromBreakpoint: Boolean,
    model: Listener4SpeedAssistExtend.Listener4SpeedModel
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed.progressBlock]
 */
typealias onProgressBlockWithSpeed = (
    task: DownloadTask,
    blockIndex: Int,
    currentBlockOffset: Long,
    blockSpeed: SpeedCalculator
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed.progress]
 */
typealias onProgressWithSpeed = (
    task: DownloadTask,
    currentOffset: Long,
    taskSpeed: SpeedCalculator
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed.blockEnd]
 */
typealias onBlockEndWithSpeed = (
    task: DownloadTask,
    blockIndex: Int,
    info: BlockInfo,
    blockSpeed: SpeedCalculator
) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed.taskEnd]
 */
typealias onTaskEndWithSpeed = (
    task: DownloadTask,
    cause: EndCause,
    realCause: Exception?,
    taskSpeed: SpeedCalculator
) -> Unit

fun createListener4WithSpeed(
    onTaskStart: onTaskStart? = null,
    onConnectStart: onConnectStart? = null,
    onConnectEnd: onConnectEnd? = null,
    onInfoReadyWithSpeed: onInfoReadyWithSpeed? = null,
    onProgressBlockWithSpeed: onProgressBlockWithSpeed? = null,
    onProgressWithSpeed: onProgressWithSpeed? = null,
    onBlockEndWithSpeed: onBlockEndWithSpeed? = null,
    onTaskEndWithSpeed: onTaskEndWithSpeed
): DownloadListener4WithSpeed = object : DownloadListener4WithSpeed() {
    override fun taskStart(task: DownloadTask) {
        onTaskStart?.invoke(task)
    }

    override fun infoReady(
        task: DownloadTask,
        info: BreakpointInfo,
        fromBreakpoint: Boolean,
        model: Listener4SpeedAssistExtend.Listener4SpeedModel
    ) {
        onInfoReadyWithSpeed?.invoke(task, info, fromBreakpoint, model)
    }

    override fun progressBlock(
        task: DownloadTask,
        blockIndex: Int,
        currentBlockOffset: Long,
        blockSpeed: SpeedCalculator
    ) {
        onProgressBlockWithSpeed?.invoke(task, blockIndex, currentBlockOffset, blockSpeed)
    }

    override fun progress(task: DownloadTask, currentOffset: Long, taskSpeed: SpeedCalculator) {
        onProgressWithSpeed?.invoke(task, currentOffset, taskSpeed)
    }

    override fun blockEnd(
        task: DownloadTask,
        blockIndex: Int,
        info: BlockInfo,
        blockSpeed: SpeedCalculator
    ) {
        onBlockEndWithSpeed?.invoke(task, blockIndex, info, blockSpeed)
    }

    override fun taskEnd(
        task: DownloadTask,
        cause: EndCause,
        realCause: java.lang.Exception?,
        taskSpeed: SpeedCalculator
    ) {
        onTaskEndWithSpeed(task, cause, realCause, taskSpeed)
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
}