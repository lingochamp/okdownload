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
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class DownloadProgressTest {

    @MockK
    lateinit var mockTask: DownloadTask

    @Before
    fun setup() = MockKAnnotations.init(this)

    @Test
    fun `totalUnknown returns true`() {
        val dp = DownloadProgress(mockTask, 100, -1)
        assert(dp.totalUnknown())
    }

    @Test
    fun `totalUnknown returns false`() {
        val dp = DownloadProgress(mockTask, 100, 200)
        assert(!dp.totalUnknown())
    }

    @Test
    fun `progress returns unknown progress`() {
        val dp1 = DownloadProgress(mockTask, 100, -1)
        assert(dp1.progress() == DownloadProgress.UNKNOWN_PROGRESS)

        val dp2 = DownloadProgress(mockTask, 1, 0)
        assert(dp2.progress() == DownloadProgress.UNKNOWN_PROGRESS)
    }

    @Test
    fun `progress returns full progress`() {
        val dp1 = DownloadProgress(mockTask, 100, 100)
        assert(dp1.progress() == 1f)

        val dp2 = DownloadProgress(mockTask, 0, 0)
        assert(dp2.progress() == 1f)
    }

    @Test
    fun `progress returns half progress`() {
        val dp = DownloadProgress(mockTask, 50, 100)
        assert(dp.progress() == 0.5f)
    }
}