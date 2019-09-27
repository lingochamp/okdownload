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
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import com.liulishuo.okdownload.kotlin.listener.createListener3
import com.liulishuo.okdownload.kotlin.listener.onCanceled
import com.liulishuo.okdownload.kotlin.listener.onCompleted
import com.liulishuo.okdownload.kotlin.listener.onConnected
import com.liulishuo.okdownload.kotlin.listener.onError
import com.liulishuo.okdownload.kotlin.listener.onProgress
import com.liulishuo.okdownload.kotlin.listener.onRetry
import com.liulishuo.okdownload.kotlin.listener.onStarted
import com.liulishuo.okdownload.kotlin.listener.onWarn
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DownloadListener3ExtensionTest {

    @MockK
    lateinit var onRetry: onRetry
    @MockK
    lateinit var onConnected: onConnected
    @MockK
    lateinit var onProgress: onProgress
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
    lateinit var mockTask: DownloadTask
    @MockK
    lateinit var mockListener1Model: Listener1Assist.Listener1Model

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

    @Test
    fun `create DownloadTaskListener3 with only terminal callback`() {
        val mockTerminal = mockk<() -> Unit>()
        val listener3 = createListener3 {
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
        val listener3 = createListener3(
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
}