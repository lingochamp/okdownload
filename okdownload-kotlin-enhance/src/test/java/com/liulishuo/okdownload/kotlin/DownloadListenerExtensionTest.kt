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
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener4
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed
import com.liulishuo.okdownload.core.listener.DownloadListenerBunch
import com.liulishuo.okdownload.kotlin.listener.createListener
import com.liulishuo.okdownload.kotlin.listener.createListener1
import com.liulishuo.okdownload.kotlin.listener.createListener2
import com.liulishuo.okdownload.kotlin.listener.createListener3
import com.liulishuo.okdownload.kotlin.listener.createListener4
import com.liulishuo.okdownload.kotlin.listener.createListener4WithSpeed
import com.liulishuo.okdownload.kotlin.listener.onBlockEnd
import com.liulishuo.okdownload.kotlin.listener.onBlockEndWithSpeed
import com.liulishuo.okdownload.kotlin.listener.onCompleted
import com.liulishuo.okdownload.kotlin.listener.onConnectEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectStart
import com.liulishuo.okdownload.kotlin.listener.onConnectTrialEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectTrialStart
import com.liulishuo.okdownload.kotlin.listener.onConnected
import com.liulishuo.okdownload.kotlin.listener.onDownloadFromBeginning
import com.liulishuo.okdownload.kotlin.listener.onDownloadFromBreakpoint
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
import com.liulishuo.okdownload.kotlin.listener.switchToExceptProgressListener
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class DownloadListenerExtensionTest {

    @MockK
    lateinit var onTaskStart: onTaskStart
    @MockK
    lateinit var onConnectTrialStart: onConnectTrialStart
    @MockK
    lateinit var onConnectTrialEnd: onConnectTrialEnd
    @MockK
    lateinit var onDownloadFromBeginning: onDownloadFromBeginning
    @MockK
    lateinit var onDownloadFromBreakpoint: onDownloadFromBreakpoint
    @MockK
    lateinit var onConnectStart: onConnectStart
    @MockK
    lateinit var onConnectEnd: onConnectEnd
    @MockK
    lateinit var onFetchStart: onFetchStart
    @MockK
    lateinit var onFetchProgress: onFetchProgress
    @MockK
    lateinit var onFetchEnd: onFetchEnd
    @MockK
    lateinit var onTaskEnd: onTaskEnd
    @MockK
    lateinit var mockMapHeaderFields: Map<String, List<String>>
    @MockK
    lateinit var breakInfo: BreakpointInfo
    @MockK
    lateinit var mockTask: DownloadTask
    @MockK
    lateinit var resumeFailedCause: ResumeFailedCause
    @MockK
    lateinit var mockCause: EndCause
    @MockK
    lateinit var mockException: Exception
    @MockK
    lateinit var onTaskStartWithModel: onTaskStartWithModel
    @MockK
    lateinit var onTaskEndWithModel: onTaskEndWithModel
    @MockK
    lateinit var onRetry: onRetry
    @MockK
    lateinit var onConnected: onConnected
    @MockK
    lateinit var onProgress: onProgress
    @MockK
    lateinit var onStarted: onStarted
    @MockK
    lateinit var onCompleted: onCompleted
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
    lateinit var onInfoReadyWithSpeed: onInfoReadyWithSpeed
    @MockK
    lateinit var onProgressBlockWithSpeed: onProgressBlockWithSpeed
    @MockK
    lateinit var onProgressWithSpeed: onProgressWithSpeed
    @MockK
    lateinit var onBlockEndWithSpeed: onBlockEndWithSpeed
    @MockK
    lateinit var onTaskEndWithSpeed: onTaskEndWithSpeed

    private val responseCode = 200
    private val blockIndex = 1
    private val contentLength = 100L

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { onTaskStart.invoke(mockTask) } returns Unit
        every { onConnectTrialStart.invoke(mockTask, mockMapHeaderFields) } returns Unit
        every {
            onConnectTrialEnd.invoke(mockTask, responseCode, mockMapHeaderFields)
        } returns Unit
        every {
            onDownloadFromBeginning.invoke(mockTask, breakInfo, resumeFailedCause)
        } returns Unit
        every { onDownloadFromBreakpoint.invoke(mockTask, breakInfo) } returns Unit
        every { onConnectStart.invoke(mockTask, blockIndex, mockMapHeaderFields) } returns Unit
        every {
            onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        } returns Unit
        every { onFetchStart.invoke(mockTask, blockIndex, contentLength) } returns Unit
        every { onFetchProgress.invoke(mockTask, blockIndex, contentLength) } returns Unit
        every { onFetchEnd.invoke(mockTask, blockIndex, contentLength) } returns Unit
        every { onTaskEnd.invoke(mockTask, mockCause, mockException) } returns Unit
        every { onTaskStartWithModel.invoke(any(), any()) } returns Unit
        every { onRetry.invoke(any(), any()) } returns Unit
        every { onConnected.invoke(any(), any(), any(), any()) } returns Unit
        every { onStarted.invoke(mockTask) } returns Unit
        every { onConnected.invoke(any(), any(), any(), any()) } returns Unit
        every { onCompleted.invoke(mockTask) } returns Unit
        every { onInfoReady.invoke(any(), any(), any(), any()) } returns Unit
        every { onProgressBlock.invoke(any(), any(), any()) } returns Unit
        every { onBlockEnd.invoke(any(), any(), any()) } returns Unit
        every { onTaskEndWithListener4Model.invoke(any(), any(), any(), any()) } returns Unit
        every { onInfoReadyWithSpeed.invoke(any(), any(), any(), any()) } returns Unit
        every { onProgressBlockWithSpeed.invoke(any(), any(), any(), any()) } returns Unit
        every { onBlockEndWithSpeed.invoke(any(), any(), any(), any()) } returns Unit
        every { onTaskEndWithSpeed.invoke(any(), any(), any(), any()) } returns Unit
    }

    @Test
    fun `create a download listener only reacting task end callback`() {
        val listener = createListener { task, cause, realCause ->
            onTaskEnd(task, cause, realCause)
        }
        listener.taskStart(mockTask)
        listener.connectTrialStart(mockTask, mockMapHeaderFields)
        listener.connectTrialEnd(mockTask, 200, mockMapHeaderFields)
        listener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        listener.downloadFromBreakpoint(mockTask, breakInfo)
        listener.connectStart(mockTask, 1, mockMapHeaderFields)
        listener.connectEnd(mockTask, 1, 200, mockMapHeaderFields)
        listener.fetchStart(mockTask, 1, 100)
        listener.fetchProgress(mockTask, 1, 200)
        listener.fetchEnd(mockTask, 1, 200)
        listener.taskEnd(mockTask, mockCause, mockException)

        verify { onTaskEnd.invoke(mockTask, mockCause, mockException) }
        confirmVerified(onTaskEnd)
    }

    @Test
    fun `create a download listener with all callbacks`() {
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

        listener.taskStart(mockTask)
        verify { onTaskStart.invoke(mockTask) }
        confirmVerified(onTaskStart)

        listener.connectTrialStart(mockTask, mockMapHeaderFields)
        verify { onConnectTrialStart.invoke(mockTask, mockMapHeaderFields) }
        confirmVerified(onConnectTrialStart)

        listener.connectTrialEnd(mockTask, responseCode, mockMapHeaderFields)
        verify { onConnectTrialEnd.invoke(mockTask, responseCode, mockMapHeaderFields) }
        confirmVerified(onConnectTrialEnd)

        listener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        verify { onDownloadFromBeginning.invoke(mockTask, breakInfo, resumeFailedCause) }
        confirmVerified(onDownloadFromBeginning)

        listener.downloadFromBreakpoint(mockTask, breakInfo)
        verify { onDownloadFromBreakpoint.invoke(mockTask, breakInfo) }
        confirmVerified(onDownloadFromBreakpoint)

        listener.connectStart(mockTask, blockIndex, mockMapHeaderFields)
        verify { onConnectStart.invoke(mockTask, blockIndex, mockMapHeaderFields) }
        confirmVerified(onConnectStart)

        listener.connectEnd(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        verify { onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockMapHeaderFields) }
        confirmVerified(onConnectEnd)

        listener.fetchStart(mockTask, blockIndex, contentLength)
        verify { onFetchStart.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchStart)

        listener.fetchProgress(mockTask, blockIndex, contentLength)
        verify { onFetchProgress.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchProgress)

        listener.fetchEnd(mockTask, blockIndex, contentLength)
        verify { onFetchEnd.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchEnd)

        listener.taskEnd(mockTask, mockCause, mockException)
        verify { onTaskEnd.invoke(mockTask, mockCause, mockException) }
        confirmVerified(onTaskEnd)
    }

    @Test
    fun `DownloadListener dispels progress callback`() {
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
        val noProgressListener = listener.switchToExceptProgressListener()

        noProgressListener.taskStart(mockTask)
        noProgressListener.connectTrialStart(mockTask, mockMapHeaderFields)
        noProgressListener.connectTrialEnd(mockTask, responseCode, mockMapHeaderFields)
        noProgressListener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        noProgressListener.downloadFromBreakpoint(mockTask, breakInfo)
        noProgressListener.connectStart(mockTask, blockIndex, mockMapHeaderFields)
        noProgressListener.connectEnd(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        noProgressListener.fetchStart(mockTask, blockIndex, contentLength)
        noProgressListener.fetchProgress(mockTask, blockIndex, contentLength)
        noProgressListener.fetchEnd(mockTask, blockIndex, contentLength)
        noProgressListener.taskEnd(mockTask, mockCause, mockException)

        verify { onTaskStart.invoke(mockTask) }
        confirmVerified(onTaskStart)
        verify { onConnectTrialStart.invoke(mockTask, mockMapHeaderFields) }
        confirmVerified(onConnectTrialStart)
        verify { onConnectTrialEnd.invoke(mockTask, responseCode, mockMapHeaderFields) }
        confirmVerified(onConnectTrialEnd)
        verify { onDownloadFromBeginning.invoke(mockTask, breakInfo, resumeFailedCause) }
        confirmVerified(onDownloadFromBeginning)
        verify { onDownloadFromBreakpoint.invoke(mockTask, breakInfo) }
        confirmVerified(onDownloadFromBreakpoint)
        verify { onConnectStart.invoke(mockTask, blockIndex, mockMapHeaderFields) }
        confirmVerified(onConnectStart)
        verify { onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockMapHeaderFields) }
        confirmVerified(onConnectEnd)
        verify { onFetchStart.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchStart)
        verify(exactly = 0) { onFetchProgress.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchProgress)
        verify { onFetchEnd.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchEnd)
        verify { onTaskEnd.invoke(mockTask, mockCause, mockException) }
        confirmVerified(onTaskEnd)
    }

    @Test
    fun `DownloadListenerBunch dispels progress callback`() {
        val listener1 = createListener(
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
        val listener2 = createListener(
            onFetchProgress = onFetchProgress,
            onTaskEnd = onTaskEnd
        )
        val listenerBunch = DownloadListenerBunch.Builder()
            .append(listener1)
            .append(listener2)
            .build()
        val noProgressListener = listenerBunch.switchToExceptProgressListener()

        noProgressListener.taskStart(mockTask)
        noProgressListener.connectTrialStart(mockTask, mockMapHeaderFields)
        noProgressListener.connectTrialEnd(mockTask, responseCode, mockMapHeaderFields)
        noProgressListener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        noProgressListener.downloadFromBreakpoint(mockTask, breakInfo)
        noProgressListener.connectStart(mockTask, blockIndex, mockMapHeaderFields)
        noProgressListener.connectEnd(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        noProgressListener.fetchStart(mockTask, blockIndex, contentLength)
        noProgressListener.fetchProgress(mockTask, blockIndex, contentLength)
        noProgressListener.fetchEnd(mockTask, blockIndex, contentLength)
        noProgressListener.taskEnd(mockTask, mockCause, mockException)

        verify { onTaskStart.invoke(mockTask) }
        confirmVerified(onTaskStart)
        verify { onConnectTrialStart.invoke(mockTask, mockMapHeaderFields) }
        confirmVerified(onConnectTrialStart)
        verify { onConnectTrialEnd.invoke(mockTask, responseCode, mockMapHeaderFields) }
        confirmVerified(onConnectTrialEnd)
        verify { onDownloadFromBeginning.invoke(mockTask, breakInfo, resumeFailedCause) }
        confirmVerified(onDownloadFromBeginning)
        verify { onDownloadFromBreakpoint.invoke(mockTask, breakInfo) }
        confirmVerified(onDownloadFromBreakpoint)
        verify { onConnectStart.invoke(mockTask, blockIndex, mockMapHeaderFields) }
        confirmVerified(onConnectStart)
        verify { onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockMapHeaderFields) }
        confirmVerified(onConnectEnd)
        verify { onFetchStart.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchStart)
        verify(exactly = 0) { onFetchProgress.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchProgress)
        verify { onFetchEnd.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchEnd)
        verify(exactly = 2) { onTaskEnd.invoke(mockTask, mockCause, mockException) }
        confirmVerified(onTaskEnd)
    }

    @Test
    fun `DownloadListener1 dispels progress callback`() {
        val listener1 = createListener1(
            taskStart = onTaskStartWithModel,
            progress = onProgress,
            taskEnd = onTaskEndWithModel
        )
        val noProgressListener = listener1.switchToExceptProgressListener()

        noProgressListener.taskStart(mockTask)
        noProgressListener.connectTrialStart(mockTask, mockMapHeaderFields)
        noProgressListener.connectTrialEnd(mockTask, responseCode, mockMapHeaderFields)
        noProgressListener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        noProgressListener.downloadFromBreakpoint(mockTask, breakInfo)
        noProgressListener.connectStart(mockTask, blockIndex, mockMapHeaderFields)
        noProgressListener.connectEnd(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        noProgressListener.fetchStart(mockTask, blockIndex, contentLength)
        noProgressListener.fetchProgress(mockTask, blockIndex, contentLength)
        noProgressListener.fetchEnd(mockTask, blockIndex, contentLength)
        noProgressListener.taskEnd(mockTask, mockCause, mockException)

        verify { onTaskStartWithModel.invoke(mockTask, any()) }
        confirmVerified(onTaskStartWithModel)
        verify(exactly = 0) { onProgress.invoke(any(), any(), any()) }
        confirmVerified(onProgress)
        verify { onTaskEndWithModel.invoke(any(), any(), any(), any()) }
    }

    @Test
    fun `DownloadListener2 dispels progress callback`() {
        val listener = createListener2(onTaskStart, onTaskEnd)
        val noProgressListener = listener.switchToExceptProgressListener()
        assert(listener == noProgressListener)
    }

    @Test
    fun `DownloadListener3 dispels progress callback`() {
        val listener = createListener3(
            onStarted,
            onConnected,
            onProgress,
            onCompleted
        )
        val noProgressListener = listener.switchToExceptProgressListener()

        noProgressListener.taskStart(mockTask)
        noProgressListener.connectTrialStart(mockTask, mockMapHeaderFields)
        noProgressListener.connectTrialEnd(mockTask, responseCode, mockMapHeaderFields)
        noProgressListener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        noProgressListener.downloadFromBreakpoint(mockTask, breakInfo)
        noProgressListener.connectStart(mockTask, blockIndex, mockMapHeaderFields)
        noProgressListener.connectEnd(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        noProgressListener.fetchStart(mockTask, blockIndex, contentLength)
        noProgressListener.fetchProgress(mockTask, blockIndex, contentLength)
        noProgressListener.fetchEnd(mockTask, blockIndex, contentLength)
        noProgressListener.taskEnd(mockTask, EndCause.COMPLETED, mockException)

        verify { onStarted.invoke(mockTask) }
        confirmVerified(onStarted)
        verify { onConnected.invoke(any(), any(), any(), any()) }
        confirmVerified(onConnected)
        verify(exactly = 0) { onProgress.invoke(any(), any(), any()) }
        confirmVerified(onProgress)
        verify { onCompleted.invoke(mockTask) }
        confirmVerified(onCompleted)
    }

    @Test
    fun `DownloadListener4 dispels progress callback`() {
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

        val noProgressListener = spyk(listener4.switchToExceptProgressListener())
        if (noProgressListener is DownloadListener4) {
            every {
                noProgressListener.fetchProgress(mockTask, blockIndex, contentLength)
            } answers {
                noProgressListener.progressBlock(mockTask, blockIndex, contentLength)
                noProgressListener.progress(mockTask, contentLength)
            }
        }

        noProgressListener.taskStart(mockTask)
        noProgressListener.connectTrialStart(mockTask, mockMapHeaderFields)
        noProgressListener.connectTrialEnd(mockTask, responseCode, mockMapHeaderFields)
        noProgressListener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        noProgressListener.connectStart(mockTask, blockIndex, mockMapHeaderFields)
        noProgressListener.connectEnd(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        noProgressListener.fetchStart(mockTask, blockIndex, contentLength)
        noProgressListener.fetchProgress(mockTask, blockIndex, contentLength)
        noProgressListener.fetchEnd(mockTask, blockIndex, contentLength)
        noProgressListener.taskEnd(mockTask, mockCause, mockException)

        verify { onTaskStart.invoke(mockTask) }
        verify { onConnectStart.invoke(mockTask, blockIndex, mockMapHeaderFields) }
        verify { onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockMapHeaderFields) }
        verify { onInfoReady.invoke(any(), any(), any(), any()) }
        verify { onProgressBlock.invoke(any(), any(), any()) }
        verify(exactly = 0) { onProgressWithoutTotalLength.invoke(any(), any()) }
        verify { onBlockEnd.invoke(any(), any(), any()) }
        verify { onTaskEndWithListener4Model.invoke(any(), any(), any(), any()) }
    }

    @Test
    fun `DownloadListener4WithSpeed dispels progress callback`() {
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

        val noProgressListener = spyk(listener4WithSpeed.switchToExceptProgressListener())
        if (noProgressListener is DownloadListener4WithSpeed) {
            every {
                noProgressListener.fetchProgress(mockTask, blockIndex, contentLength)
            } answers {
                noProgressListener.progressBlock(mockTask, blockIndex, contentLength, mockk())
                noProgressListener.progress(mockTask, contentLength)
            }
            every {
                noProgressListener.fetchEnd(mockTask, blockIndex, contentLength)
            } answers {
                noProgressListener.blockEnd(mockTask, blockIndex, mockk(), mockk())
            }
            every {
                noProgressListener.taskEnd(mockTask, mockCause, mockException)
            } answers {
                noProgressListener.taskEnd(
                    mockTask,
                    mockCause,
                    mockException,
                    mockk<SpeedCalculator>()
                )
            }
        }

        noProgressListener.taskStart(mockTask)
        noProgressListener.connectTrialStart(mockTask, mockMapHeaderFields)
        noProgressListener.connectTrialEnd(mockTask, responseCode, mockMapHeaderFields)
        noProgressListener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        noProgressListener.connectStart(mockTask, blockIndex, mockMapHeaderFields)
        noProgressListener.connectEnd(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        noProgressListener.fetchStart(mockTask, blockIndex, contentLength)
        noProgressListener.fetchProgress(mockTask, blockIndex, contentLength)
        noProgressListener.fetchEnd(mockTask, blockIndex, contentLength)
        noProgressListener.taskEnd(mockTask, mockCause, mockException)

        verify { onTaskStart.invoke(mockTask) }
        verify { onConnectStart.invoke(mockTask, blockIndex, mockMapHeaderFields) }
        verify { onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockMapHeaderFields) }
        verify { onInfoReadyWithSpeed.invoke(any(), any(), any(), any()) }
        verify { onProgressBlockWithSpeed.invoke(any(), any(), any(), any()) }
        verify(exactly = 0) { onProgressWithSpeed.invoke(any(), any(), any()) }
        verify { onBlockEndWithSpeed.invoke(any(), any(), any(), any()) }
        verify { onTaskEndWithSpeed.invoke(any(), any(), any(), any()) }
    }
}