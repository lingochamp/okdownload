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
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
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
import com.liulishuo.okdownload.kotlin.listener.onProgress
import com.liulishuo.okdownload.kotlin.listener.onRetry
import com.liulishuo.okdownload.kotlin.listener.onStarted
import com.liulishuo.okdownload.kotlin.listener.onTaskEnd
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithModel
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import com.liulishuo.okdownload.kotlin.listener.onTaskStartWithModel
import com.liulishuo.okdownload.kotlin.listener.onWarn
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class DownloadTaskExtensionTest {

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
    lateinit var mockCause: EndCause
    @MockK
    lateinit var mockException: Exception
    @MockK
    lateinit var resumeFailedCause: ResumeFailedCause
    @MockK
    lateinit var onStarted: onStarted
    @MockK
    lateinit var onCompleted: onCompleted
    @MockK
    lateinit var onCanceled: onCanceled
    @MockK
    lateinit var onWarn: onWarn
    @MockK
    lateinit var onError: onError
    @MockK
    lateinit var mockMapHeaderFields: Map<String, List<String>>
    @MockK
    lateinit var breakInfo: BreakpointInfo
    @MockK
    lateinit var mockTask: DownloadTask
    @MockK
    lateinit var mockListener1Model: Listener1Assist.Listener1Model

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

    @Test
    fun `create a download listener only reacting task end callback`() {
        val listener = mockTask.createListener { task, cause, realCause ->
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
        val listener = mockTask.createListener(
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

        every { onTaskStart.invoke(mockTask) } returns Unit
        listener.taskStart(mockTask)
        verify { onTaskStart.invoke(mockTask) }
        confirmVerified(onTaskStart)

        every { onConnectTrialStart.invoke(mockTask, mockMapHeaderFields) } returns Unit
        listener.connectTrialStart(mockTask, mockMapHeaderFields)
        verify { onConnectTrialStart.invoke(mockTask, mockMapHeaderFields) }
        confirmVerified(onConnectTrialStart)

        val responseCode = 200
        every {
            onConnectTrialEnd.invoke(mockTask, responseCode, mockMapHeaderFields)
        } returns Unit
        listener.connectTrialEnd(mockTask, responseCode, mockMapHeaderFields)
        verify { onConnectTrialEnd.invoke(mockTask, responseCode, mockMapHeaderFields) }
        confirmVerified(onConnectTrialEnd)

        every {
            onDownloadFromBeginning.invoke(mockTask, breakInfo, resumeFailedCause)
        } returns Unit
        listener.downloadFromBeginning(mockTask, breakInfo, resumeFailedCause)
        verify { onDownloadFromBeginning.invoke(mockTask, breakInfo, resumeFailedCause) }
        confirmVerified(onDownloadFromBeginning)

        every { onDownloadFromBreakpoint.invoke(mockTask, breakInfo) } returns Unit
        listener.downloadFromBreakpoint(mockTask, breakInfo)
        verify { onDownloadFromBreakpoint.invoke(mockTask, breakInfo) }
        confirmVerified(onDownloadFromBreakpoint)

        val blockIndex = 1
        every { onConnectStart.invoke(mockTask, blockIndex, mockMapHeaderFields) } returns Unit
        listener.connectStart(mockTask, blockIndex, mockMapHeaderFields)
        verify { onConnectStart.invoke(mockTask, blockIndex, mockMapHeaderFields) }
        confirmVerified(onConnectStart)

        every {
            onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        } returns Unit
        listener.connectEnd(mockTask, blockIndex, responseCode, mockMapHeaderFields)
        verify { onConnectEnd.invoke(mockTask, blockIndex, responseCode, mockMapHeaderFields) }
        confirmVerified(onConnectEnd)

        val contentLength = 100L
        every { onFetchStart.invoke(mockTask, blockIndex, contentLength) } returns Unit
        listener.fetchStart(mockTask, blockIndex, contentLength)
        verify { onFetchStart.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchStart)

        every { onFetchProgress.invoke(mockTask, blockIndex, contentLength) } returns Unit
        listener.fetchProgress(mockTask, blockIndex, contentLength)
        verify { onFetchProgress.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchProgress)

        every { onFetchEnd.invoke(mockTask, blockIndex, contentLength) } returns Unit
        listener.fetchEnd(mockTask, blockIndex, contentLength)
        verify { onFetchEnd.invoke(mockTask, blockIndex, contentLength) }
        confirmVerified(onFetchEnd)

        every { onTaskEnd.invoke(mockTask, mockCause, mockException) } returns Unit
        listener.taskEnd(mockTask, mockCause, mockException)
        verify { onTaskEnd.invoke(mockTask, mockCause, mockException) }
        confirmVerified(onTaskEnd)
    }

    @Test
    fun `execute DownloadTask with DownloadListener`() {
        mockTask.execute { _, _, _ -> }

        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `create DownloadListener1 with only end call back`() {
        val listener1 = mockTask.createListener1 { task, cause, realCause, model ->
            onTaskEndWithModel(task, cause, realCause, model)
        }

        listener1.taskStart(mockTask, mockListener1Model)
        listener1.connected(mockTask, 3, 100, 300)
        listener1.retry(mockTask, resumeFailedCause)
        listener1.progress(mockTask, 100, 300)
        listener1.taskEnd(mockTask, mockCause, mockException, mockListener1Model)

        verify { onTaskEndWithModel(mockTask, mockCause, mockException, mockListener1Model) }
        confirmVerified(onTaskEndWithModel)
    }

    @Test
    fun `create DownloadListener1 with all callbacks`() {
        val blockCount = 3
        val currentOffset = 100L
        val totalLength = 300L
        val listener1 = mockTask.createListener1(
            onTaskStartWithModel,
            onRetry,
            onConnected,
            onProgress,
            onTaskEndWithModel
        )

        every { onTaskStartWithModel.invoke(mockTask, mockListener1Model) } returns Unit
        every { onRetry.invoke(mockTask, resumeFailedCause) } returns Unit
        every { onConnected.invoke(mockTask, blockCount, currentOffset, totalLength) } returns Unit
        every { onProgress.invoke(mockTask, currentOffset, totalLength) } returns Unit

        listener1.taskStart(mockTask, mockListener1Model)
        verify { onTaskStartWithModel.invoke(mockTask, mockListener1Model) }
        confirmVerified(onTaskStartWithModel)

        listener1.retry(mockTask, resumeFailedCause)
        verify { onRetry.invoke(mockTask, resumeFailedCause) }
        confirmVerified(onRetry)

        listener1.connected(mockTask, blockCount, currentOffset, totalLength)
        verify { onConnected.invoke(mockTask, blockCount, currentOffset, totalLength) }
        confirmVerified(onConnected)

        listener1.progress(mockTask, currentOffset, totalLength)
        verify { onProgress.invoke(mockTask, currentOffset, totalLength) }
        confirmVerified(onProgress)

        listener1.taskEnd(mockTask, mockCause, mockException, mockListener1Model)
        verify { onTaskEndWithModel(mockTask, mockCause, mockException, mockListener1Model) }
        confirmVerified(onTaskEndWithModel)
    }

    @Test
    fun `execute DownloadTask with DownloadListener1`() {
        mockTask.execute1 { _, _, _, _ -> }

        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `create DownloadTaskListener3 with only terminal callback`() {
        val mockTerminal = mockk<() -> Unit>()
        val listener3 = mockTask.createListener3 {
            mockTerminal.invoke()
        }
        val blockCount = 3
        val currentOffset = 100L
        val totalLength = 300L

        every { mockTerminal.invoke() } returns Unit

        listener3.taskEnd(mockTask, EndCause.SAME_TASK_BUSY, mockException, mockListener1Model)
        listener3.retry(mockTask, resumeFailedCause)
        listener3.connected(mockTask, blockCount, currentOffset, totalLength)
        listener3.taskStart(mockTask, mockListener1Model)
        listener3.taskEnd(mockTask, EndCause.COMPLETED, null, mockListener1Model)
        listener3.taskEnd(mockTask, EndCause.CANCELED, null, mockListener1Model)
        listener3.taskEnd(mockTask, EndCause.ERROR, mockException, mockListener1Model)
        listener3.progress(mockTask, currentOffset, totalLength)

        verify(exactly = 4) { mockTerminal.invoke() }
        confirmVerified(mockTerminal)
    }

    @Test
    fun `create DownloadTaskListener3 with all callbacks`() {
        val mockTerminal = mockk<() -> Unit>()
        val listener3 = mockTask.createListener3(
            onStarted,
            onConnected,
            onProgress,
            onCompleted,
            onCanceled,
            onWarn,
            onRetry,
            onError,
            mockTerminal
        )
        val blockCount = 3
        val currentOffset = 100L
        val totalLength = 300L

        every { mockTerminal.invoke() } returns Unit
        every { onWarn.invoke(mockTask) } returns Unit
        every { onRetry.invoke(mockTask, resumeFailedCause) } returns Unit
        every { onConnected.invoke(mockTask, blockCount, currentOffset, totalLength) } returns Unit
        every { onStarted.invoke(mockTask) } returns Unit
        every { onCompleted.invoke(mockTask) } returns Unit
        every { onCanceled.invoke(mockTask) } returns Unit
        every { onError.invoke(mockTask, mockException) } returns Unit
        every { onProgress.invoke(mockTask, currentOffset, totalLength) } returns Unit

        listener3.taskEnd(mockTask, EndCause.SAME_TASK_BUSY, mockException, mockListener1Model)
        verify { onWarn.invoke(mockTask) }
        verify { mockTerminal.invoke() }
        confirmVerified(onWarn)
        confirmVerified(mockTerminal)

        listener3.retry(mockTask, resumeFailedCause)
        verify { onRetry.invoke(mockTask, resumeFailedCause) }
        confirmVerified(onRetry)

        listener3.connected(mockTask, blockCount, currentOffset, totalLength)
        verify { onConnected.invoke(mockTask, blockCount, currentOffset, totalLength) }
        confirmVerified(onConnected)

        listener3.taskStart(mockTask, mockListener1Model)
        verify { onStarted.invoke(mockTask) }
        confirmVerified(onStarted)

        listener3.taskEnd(mockTask, EndCause.COMPLETED, null, mockListener1Model)
        verify { onCompleted.invoke(mockTask) }
        verify(exactly = 2) { mockTerminal.invoke() }
        confirmVerified(onCompleted)
        confirmVerified(mockTerminal)

        listener3.taskEnd(mockTask, EndCause.CANCELED, null, mockListener1Model)
        verify { onCanceled.invoke(mockTask) }
        verify(exactly = 3) { mockTerminal.invoke() }
        confirmVerified(onCanceled)
        confirmVerified(mockTerminal)

        listener3.taskEnd(mockTask, EndCause.ERROR, mockException, mockListener1Model)
        verify { onError.invoke(mockTask, mockException) }
        verify(exactly = 4) { mockTerminal.invoke() }
        confirmVerified(onError)
        confirmVerified(mockTerminal)

        listener3.progress(mockTask, currentOffset, totalLength)
        verify { onProgress.invoke(mockTask, currentOffset, totalLength) }
        confirmVerified(onProgress)
    }

    @Test
    fun `execute DownloadTask with DownloadListener3`() {
        mockTask.execute3 { }

        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }
}