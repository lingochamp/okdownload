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

import android.net.Uri
import com.liulishuo.okdownload.DownloadListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.OkDownload
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher
import com.liulishuo.okdownload.kotlin.listener.createListener
import com.liulishuo.okdownload.kotlin.listener.onTaskEnd
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.Exception
import java.lang.IllegalStateException

@RunWith(RobolectricTestRunner::class)
class DownloadTaskExtensionTest {

    @MockK
    lateinit var onTaskStart: onTaskStart
    @MockK
    lateinit var onTaskEnd: onTaskEnd
    @MockK
    lateinit var mockTask: DownloadTask
    @MockK
    lateinit var breakInfo: BreakpointInfo
    @MockK
    lateinit var mockOkDownload: OkDownload

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        try {
            OkDownload.setSingletonInstance(mockOkDownload)
        } catch (ignore: IllegalArgumentException) {
        }
    }

    @Test
    fun `execute DownloadTask with DownloadListener`() {
        mockTask.execute { _, _, _ -> }

        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `execute DownloadTask with DownloadListener1`() {
        mockTask.execute1 { _, _, _, _ -> }

        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `execute DownloadTask with DownloadListener3`() {
        mockTask.execute3 { }

        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `execute DownloadTask with DownloadListener2`() {
        mockTask.execute2(onTaskStart, onTaskEnd)
        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `execute DownloadTask with DownloadListener4`() {
        mockTask.execute4 { _, _, _, _ -> }
        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `execute DownloadTask with DownloadListener4WithSpeed`() {
        mockTask.execute4WithSpeed { _, _, _, _ -> }
        verify { mockTask.execute(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `enqueue DownloadTask with DownloadListener`() {
        mockTask.enqueue { _, _, _ -> }

        verify { mockTask.enqueue(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `enqueue DownloadTask with DownloadListener1`() {
        mockTask.enqueue1 { _, _, _, _ -> }

        verify { mockTask.enqueue(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `enqueue DownloadTask with DownloadListener3`() {
        mockTask.enqueue3 { }

        verify { mockTask.enqueue(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `enqueue DownloadTask with DownloadListener2`() {
        mockTask.enqueue2(onTaskStart, onTaskEnd)
        verify { mockTask.enqueue(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `enqueue DownloadTask with DownloadListener4`() {
        mockTask.enqueue4 { _, _, _, _ -> }
        verify { mockTask.enqueue(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `enqueue DownloadTask with DownloadListener4WithSpeed`() {
        mockTask.enqueue4WithSpeed { _, _, _, _ -> }
        verify { mockTask.enqueue(any()) }
        confirmVerified(mockTask)
    }

    @Test
    fun `get progress channel with previous listener`() {
        val mockBreakpointStore = mockk<BreakpointStore>()
        val mockDownloadDispatcher = mockk<DownloadDispatcher>()
        every { mockBreakpointStore.findOrCreateId(any()) } returns 0
        every { mockOkDownload.breakpointStore() } returns mockBreakpointStore
        every { mockOkDownload.downloadDispatcher() } returns mockDownloadDispatcher
        every { mockDownloadDispatcher.enqueue(any<DownloadTask>()) } returns Unit
        val mockUri = mockk<Uri>()
        every { mockUri.scheme } returns "test"
        every { mockUri.path } returns "path"

        val spiedTask = spyk(DownloadTask.Builder("url", mockUri).build())
        val oldListener = createListener { _, _, _ -> }
        spiedTask.enqueue(oldListener)

        val spChannel = spiedTask.spChannel()
        verify { spiedTask.replaceListener(any()) }

        spiedTask.listener.taskStart(mockTask)
        spiedTask.listener.downloadFromBreakpoint(mockTask, breakInfo)
        spiedTask.listener.fetchProgress(mockTask, 0, 200)
        assert(spChannel.poll()?.currentOffset == 200L)
        spiedTask.listener.taskEnd(mockTask, mockk(), mockk())
        // don't offer any progress after channel is closed
        spiedTask.listener.fetchProgress(mockTask, 0, 300)
        assert(spChannel.poll() == null)
    }

    @Test
    fun `DownloadTask await success`() {
        val spiedBlock: () -> Unit = spyk({})
        every { mockTask.enqueue(any()) } answers {
            val listener = it.invocation.args[0] as DownloadListener
            listener.taskEnd(mockTask, EndCause.COMPLETED, null)
        }
        runBlocking {
            val result = mockTask.await(spiedBlock)
            assert(result.becauseOfCompleted())
            verify { spiedBlock.invoke() }
        }

        every { mockTask.enqueue(any()) } answers {
            val listener = it.invocation.args[0] as DownloadListener
            listener.taskEnd(mockTask, EndCause.FILE_BUSY, null)
        }
        runBlocking {
            val result = mockTask.await()
            assert(result.becauseOfRepeatedTask())
        }

        every { mockTask.enqueue(any()) } answers {
            val listener = it.invocation.args[0] as DownloadListener
            listener.taskEnd(mockTask, EndCause.SAME_TASK_BUSY, null)
        }
        runBlocking {
            val result = mockTask.await()
            assert(result.becauseOfRepeatedTask())
        }
    }

    @Test
    fun `DownloadTask await failed`() {
        val exception = IllegalStateException("test error")
        every { mockTask.enqueue(any()) } answers {
            val listener = it.invocation.args[0] as DownloadListener
            listener.taskEnd(mockTask, EndCause.ERROR, exception)
        }
        runBlocking {
            try {
                mockTask.await()
                error("should failed")
            } catch (e: Exception) {
                assert(e is IllegalStateException)
                assert(e.message == exception.message)
            }
        }
    }

    @Test
    fun `DownloadTask await cancelled`() {
        every { mockTask.enqueue(any()) } answers {
            val listener = it.invocation.args[0] as DownloadListener
            listener.taskStart(mockTask)
        }
        runBlocking {
            try {
                withContext(Dispatchers.Default) {
                    delay(100)
                    cancel("test cancel")
                }
                mockTask.await()
                error("should be cancelled")
            } catch (e: Exception) {
                assert(e is CancellationException)
                require(e.message == "test cancel") {
                    "need message: test cancel, but real is: ${e.message}"
                }
            }
        }
    }
}