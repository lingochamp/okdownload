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
import com.liulishuo.okdownload.kotlin.listener.createListener
import com.liulishuo.okdownload.kotlin.listener.onConnectEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectStart
import com.liulishuo.okdownload.kotlin.listener.onConnectTrialEnd
import com.liulishuo.okdownload.kotlin.listener.onConnectTrialStart
import com.liulishuo.okdownload.kotlin.listener.onDownloadFromBeginning
import com.liulishuo.okdownload.kotlin.listener.onDownloadFromBreakpoint
import com.liulishuo.okdownload.kotlin.listener.onFetchEnd
import com.liulishuo.okdownload.kotlin.listener.onFetchProgress
import com.liulishuo.okdownload.kotlin.listener.onFetchStart
import com.liulishuo.okdownload.kotlin.listener.onTaskEnd
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
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

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

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
}