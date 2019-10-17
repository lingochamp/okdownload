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

import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.kotlin.listener.createDownloadContextListener
import com.liulishuo.okdownload.kotlin.listener.onQueueEnd
import com.liulishuo.okdownload.kotlin.listener.onQueueTaskEnd
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class DownloadContextListenerExtensionTest {

    @MockK
    lateinit var onQueueTaskEnd: onQueueTaskEnd
    @MockK
    lateinit var onQueueEnd: onQueueEnd
    @MockK
    lateinit var downloadContext: DownloadContext
    @MockK
    lateinit var downloadTask: DownloadTask
    @MockK
    lateinit var cause: EndCause
    @MockK
    lateinit var realException: Exception
    private val remainCount = 1

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

    @Test
    fun `create DownloadContextListener only with queue end callback`() {
        val listener = createDownloadContextListener(onQueueEnd = onQueueEnd)
        listener.taskEnd(downloadContext, downloadTask, cause, realException, remainCount)
        listener.queueEnd(downloadContext)
        verify { onQueueEnd.invoke(downloadContext) }
    }

    @Test
    fun `create DownloadContextListener with all callbacks`() {
        every {
            onQueueTaskEnd.invoke(downloadContext, downloadTask, cause, realException, remainCount)
        } returns Unit
        val listener = createDownloadContextListener(onQueueTaskEnd, onQueueEnd)
        listener.taskEnd(downloadContext, downloadTask, cause, realException, remainCount)
        listener.queueEnd(downloadContext)
        verify { onQueueEnd.invoke(downloadContext) }
        verify {
            onQueueTaskEnd.invoke(downloadContext, downloadTask, cause, realException, remainCount)
        }
    }
}