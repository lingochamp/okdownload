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
import com.liulishuo.okdownload.core.breakpoint.BlockInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.assist.Listener4Assist
import com.liulishuo.okdownload.kotlin.listener.createListener4
import com.liulishuo.okdownload.kotlin.listener.onBlockEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectStart
import com.liulishuo.okdownload.kotlin.listener.onInfoReady
import com.liulishuo.okdownload.kotlin.listener.onProgressBlock
import com.liulishuo.okdownload.kotlin.listener.onProgressWithoutTotalLength
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithListener4Model
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class DownloadListener4ExtensionTest {

    @MockK
    lateinit var onTaskStart: onTaskStart
    @MockK
    lateinit var onConnectStart: onConnectStart
    @MockK
    lateinit var onConnectEnd: onConnectEnd
    @MockK
    lateinit var onInfoReady: onInfoReady
    @MockK
    lateinit var onProgressBlock: onProgressBlock
    @MockK
    lateinit var onProgressWithoutTotalLength: onProgressWithoutTotalLength
    @MockK
    lateinit var onBlockEnd: onBlockEnd
    @MockK
    lateinit var onTaskEndWithListener4Model: onTaskEndWithListener4Model
    @MockK
    lateinit var mockTask: DownloadTask
    @MockK
    lateinit var mockHeaderFields: Map<String, List<String>>
    @MockK
    lateinit var mockBreakInfo: BreakpointInfo
    @MockK
    lateinit var mockListener4Model: Listener4Assist.Listener4Model
    @MockK
    lateinit var mockBlockInfo: BlockInfo
    @MockK
    lateinit var mockCause: EndCause
    @MockK
    lateinit var mockRealCause: Exception

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

    @Test
    fun `create DownloadListener4 with only task end callback`() {
        val listener4 = createListener4 { task, cause, realCause, model ->
            onTaskEndWithListener4Model(task, cause, realCause, model)
        }

        listener4.taskStart(mockTask)
        listener4.connectStart(mockTask, 1, mockHeaderFields)
        listener4.connectEnd(mockTask, 1, 200, mockHeaderFields)
        listener4.infoReady(mockTask, mockBreakInfo, false, mockListener4Model)
        listener4.progressBlock(mockTask, 1, 100)
        listener4.progress(mockTask, 100)
        listener4.blockEnd(mockTask, 1, mockBlockInfo)
        listener4.taskEnd(mockTask, mockCause, mockRealCause, mockListener4Model)

        verify {
            onTaskEndWithListener4Model.invoke(
                mockTask,
                mockCause,
                mockRealCause,
                mockListener4Model
            )
        }
        confirmVerified(onTaskEndWithListener4Model)
    }

    @Test
    fun `create DownloadListener4 with callbacks`() {
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
        val blockIndex = 1
        val currentOffset = 100L
        val responseCode = 200
        val fromBreakpoint = false

        every { onTaskStart.invoke(mockTask) } returns Unit
        every { onConnectStart.invoke(mockTask, blockIndex, mockHeaderFields) } returns Unit
        every {
            onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockHeaderFields)
        } returns Unit
        every {
            onInfoReady.invoke(mockTask, mockBreakInfo, fromBreakpoint, mockListener4Model)
        } returns Unit
        every { onProgressBlock.invoke(mockTask, blockIndex, currentOffset) } returns Unit
        every { onProgressWithoutTotalLength.invoke(mockTask, currentOffset) } returns Unit
        every { onBlockEnd.invoke(mockTask, blockIndex, mockBlockInfo) } returns Unit

        listener4.taskStart(mockTask)
        listener4.connectStart(mockTask, blockIndex, mockHeaderFields)
        listener4.connectEnd(mockTask, blockIndex, responseCode, mockHeaderFields)
        listener4.infoReady(mockTask, mockBreakInfo, false, mockListener4Model)
        listener4.progressBlock(mockTask, blockIndex, currentOffset)
        listener4.progress(mockTask, currentOffset)
        listener4.blockEnd(mockTask, blockIndex, mockBlockInfo)
        listener4.taskEnd(mockTask, mockCause, mockRealCause, mockListener4Model)

        verify { onTaskStart.invoke(mockTask) }
        verify { onConnectStart.invoke(mockTask, blockIndex, mockHeaderFields) }
        verify { onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockHeaderFields) }
        verify { onInfoReady.invoke(mockTask, mockBreakInfo, fromBreakpoint, mockListener4Model) }
        verify { onProgressBlock.invoke(mockTask, blockIndex, currentOffset) }
        verify { onProgressWithoutTotalLength.invoke(mockTask, currentOffset) }
        verify { onBlockEnd.invoke(mockTask, blockIndex, mockBlockInfo) }
        verify {
            onTaskEndWithListener4Model(mockTask, mockCause, mockRealCause, mockListener4Model)
        }
        confirmVerified(onTaskStart)
        confirmVerified(onConnectStart)
        confirmVerified(onConnectEnd)
        confirmVerified(onInfoReady)
        confirmVerified(onProgressBlock)
        confirmVerified(onProgressWithoutTotalLength)
        confirmVerified(onBlockEnd)
        confirmVerified(onTaskEndWithListener4Model)
    }
}