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

import com.liulishuo.okdownload.core.cause.EndCause
import org.junit.Test

class DownloadResultTest {

    @Test
    fun becauseOfCompleted() {
        val result = DownloadResult(EndCause.COMPLETED)
        assert(result.becauseOfCompleted())
    }

    @Test
    fun becauseOfRepeatedTask() {
        var result = DownloadResult(EndCause.SAME_TASK_BUSY)
        assert(result.becauseOfRepeatedTask())

        result = DownloadResult(EndCause.FILE_BUSY)
        assert(result.becauseOfRepeatedTask())
    }
}