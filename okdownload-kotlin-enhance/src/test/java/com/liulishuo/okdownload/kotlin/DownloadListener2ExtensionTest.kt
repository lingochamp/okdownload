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
import com.liulishuo.okdownload.kotlin.listener.createListener2
import com.liulishuo.okdownload.kotlin.listener.onTaskEnd
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class DownloadListener2ExtensionTest {

    @MockK
    lateinit var onTaskStart: onTaskStart
    @MockK
    lateinit var onTaskEnd: onTaskEnd
    @MockK
    lateinit var mockTask: DownloadTask
    @MockK
    lateinit var mockCause: EndCause
    @MockK
    lateinit var mockException: Exception

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

    @Test
    fun `create DownloadListener2`() {
        val listener2 = createListener2(onTaskStart) { task, cause, realCause ->
            onTaskEnd(task, cause, realCause)
        }

        listener2.taskStart(mockTask)
        verify { onTaskStart(mockTask) }
        confirmVerified(onTaskStart)

        listener2.taskEnd(mockTask, mockCause, mockException)
        verify { onTaskEnd(mockTask, mockCause, mockException) }
        confirmVerified(onTaskEnd)
    }
}