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
import com.liulishuo.okdownload.kotlin.listener.createListener1
import com.liulishuo.okdownload.kotlin.listener.onConnected
import com.liulishuo.okdownload.kotlin.listener.onProgress
import com.liulishuo.okdownload.kotlin.listener.onRetry
import com.liulishuo.okdownload.kotlin.listener.onTaskEndWithModel
import com.liulishuo.okdownload.kotlin.listener.onTaskStartWithModel
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DownloadListener1ExtensionTest {

    @MockK
    lateinit var mockTask: DownloadTask
    @MockK
    lateinit var mockListener1Model: Listener1Assist.Listener1Model
    @MockK
    lateinit var mockCause: EndCause
    @MockK
    lateinit var mockException: Exception
    @MockK
    lateinit var resumeFailedCause: ResumeFailedCause
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

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

    @Test
    fun `create DownloadListener1 with only end call back`() {
        val listener1 = createListener1 { task, cause, realCause, model ->
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
        val listener1 = createListener1(
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
}