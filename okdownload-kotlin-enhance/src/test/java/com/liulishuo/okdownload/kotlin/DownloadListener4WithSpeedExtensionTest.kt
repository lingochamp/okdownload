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
import com.liulishuo.okdownload.SpeedCalculator
import com.liulishuo.okdownload.core.breakpoint.BlockInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend
import com.liulishuo.okdownload.kotlin.listener.createListener4WithSpeed
import com.liulishuo.okdownload.kotlin.listener.onBlockEndWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onConnectEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectStart
import com.liulishuo.okdownload.kotlin.listener.onInfoReadyWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onProgressBlockWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onProgressWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class DownloadListener4WithSpeedExtensionTest {

    @MockK
    lateinit var onTaskStart: onTaskStart
    @MockK
    lateinit var onConnectStart: onConnectStart
    @MockK
    lateinit var onConnectEnd: onConnectEnd
    @MockK
    lateinit var onInfoReadyWithSpeed: onInfoReadyWithSpeed
    @MockK
    lateinit var onProgressBlockWithSpeed: onProgressBlockWithSpeed
    @MockK
    lateinit var onProgressWithSpeed: onProgressWithSpeed
    @MockK
    lateinit var onBlockEndWithSpeed: onBlockEndWithSpeed
    @MockK
    lateinit var onTaskEndWithSpeed: onTaskEndWithSpeed
    @MockK
    lateinit var mockTask: DownloadTask
    @MockK
    lateinit var mockHeaderFields: Map<String, List<String>>
    @MockK
    lateinit var mockBreakInfo: BreakpointInfo
    @MockK
    lateinit var mockListener4Model: Listener4SpeedAssistExtend.Listener4SpeedModel
    @MockK
    lateinit var mockSpeedCalculator: SpeedCalculator
    @MockK
    lateinit var mockBlockInfo: BlockInfo
    @MockK
    lateinit var mockCause: EndCause
    @MockK
    lateinit var mockRealCause: Exception

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

    @Test
    fun `create DownloadListener4WithSpeed with only task end callback`() {
        val listener4WithSpeed = createListener4WithSpeed { task, cause, realCause, taskSpeed ->
            onTaskEndWithSpeed(task, cause, realCause, taskSpeed)
        }
        val blockIndex = 1
        val responseCode = 200
        val fromBreakpoint = true
        val currentOffset = 100L

        listener4WithSpeed.taskStart(mockTask)
        listener4WithSpeed.connectStart(mockTask, blockIndex, mockHeaderFields)
        listener4WithSpeed.connectEnd(mockTask, blockIndex, responseCode, mockHeaderFields)
        listener4WithSpeed.infoReady(mockTask, mockBreakInfo, fromBreakpoint, mockListener4Model)
        listener4WithSpeed.progressBlock(mockTask, blockIndex, currentOffset, mockSpeedCalculator)
        listener4WithSpeed.progress(mockTask, currentOffset, mockSpeedCalculator)
        listener4WithSpeed.blockEnd(mockTask, blockIndex, mockBlockInfo, mockSpeedCalculator)
        listener4WithSpeed.taskEnd(mockTask, mockCause, mockRealCause, mockSpeedCalculator)

        verify { onTaskEndWithSpeed(mockTask, mockCause, mockRealCause, mockSpeedCalculator) }
        confirmVerified(onTaskEndWithSpeed)
    }

    @Test
    fun `create DownloadListener4WithSpeed with all callbacks`() {
        val listener4WithSpeed = createListener4WithSpeed(
            onTaskStart,
            onConnectStart,
            onConnectEnd,
            onInfoReadyWithSpeed,
            onProgressBlockWithSpeed,
            onProgressWithSpeed,
            onBlockEndWithSpeed,
            onTaskEndWithSpeed
        )
        val blockIndex = 1
        val responseCode = 200
        val fromBreakpoint = true
        val currentOffset = 100L

        every { onTaskStart.invoke(mockTask) } returns Unit
        every { onConnectStart.invoke(mockTask, blockIndex, mockHeaderFields) } returns Unit
        every {
            onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockHeaderFields)
        } returns Unit
        every {
            onInfoReadyWithSpeed.invoke(mockTask, mockBreakInfo, fromBreakpoint, mockListener4Model)
        } returns Unit
        every {
            onProgressWithSpeed.invoke(mockTask, currentOffset, mockSpeedCalculator)
        } returns Unit
        every {
            onBlockEndWithSpeed.invoke(mockTask, blockIndex, mockBlockInfo, mockSpeedCalculator)
        } returns Unit

        listener4WithSpeed.taskStart(mockTask)
        listener4WithSpeed.connectStart(mockTask, blockIndex, mockHeaderFields)
        listener4WithSpeed.connectEnd(mockTask, blockIndex, responseCode, mockHeaderFields)
        listener4WithSpeed.infoReady(mockTask, mockBreakInfo, fromBreakpoint, mockListener4Model)
        listener4WithSpeed.progressBlock(mockTask, blockIndex, currentOffset, mockSpeedCalculator)
        listener4WithSpeed.progress(mockTask, currentOffset, mockSpeedCalculator)
        listener4WithSpeed.blockEnd(mockTask, blockIndex, mockBlockInfo, mockSpeedCalculator)
        listener4WithSpeed.taskEnd(mockTask, mockCause, mockRealCause, mockSpeedCalculator)

        verify { onTaskStart.invoke(mockTask) }
        verify { onConnectStart.invoke(mockTask, blockIndex, mockHeaderFields) }
        verify { onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockHeaderFields) }
        verify {
            onInfoReadyWithSpeed.invoke(mockTask, mockBreakInfo, fromBreakpoint, mockListener4Model)
        }
        verify { onProgressWithSpeed.invoke(mockTask, currentOffset, mockSpeedCalculator) }
        verify {
            onBlockEndWithSpeed.invoke(mockTask, blockIndex, mockBlockInfo, mockSpeedCalculator)
        }
        verify {
            onTaskEndWithSpeed.invoke(mockTask, mockCause, mockRealCause, mockSpeedCalculator)
        }

        confirmVerified(onTaskStart)
        confirmVerified(onConnectStart)
        confirmVerified(onConnectEnd)
        confirmVerified(onInfoReadyWithSpeed)
        confirmVerified(onProgressWithSpeed)
        confirmVerified(onBlockEndWithSpeed)
        confirmVerified(onTaskEndWithSpeed)
    }
}