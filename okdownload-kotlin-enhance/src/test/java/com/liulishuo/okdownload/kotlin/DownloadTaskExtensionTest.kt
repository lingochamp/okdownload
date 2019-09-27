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
import com.liulishuo.okdownload.kotlin.listener.onTaskEnd
import com.liulishuo.okdownload.kotlin.listener.onTaskStart
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DownloadTaskExtensionTest {

    @MockK
    lateinit var onTaskStart: onTaskStart
    @MockK
    lateinit var onTaskEnd: onTaskEnd
    @MockK
    lateinit var mockTask: DownloadTask

    @Before
    fun setup() = MockKAnnotations.init(this, relaxed = true)

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
}