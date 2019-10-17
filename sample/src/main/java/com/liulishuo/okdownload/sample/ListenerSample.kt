/*
 * Copyright (c) 2017 LingoChamp Inc.
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

package com.liulishuo.okdownload.sample

import android.util.Log
import com.liulishuo.okdownload.DownloadListener
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.DownloadListener2
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.liulishuo.okdownload.core.listener.DownloadListener4
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed
import com.liulishuo.okdownload.kotlin.listener.createListener
import com.liulishuo.okdownload.kotlin.listener.createListener1
import com.liulishuo.okdownload.kotlin.listener.createListener2
import com.liulishuo.okdownload.kotlin.listener.createListener3
import com.liulishuo.okdownload.kotlin.listener.createListener4
import com.liulishuo.okdownload.kotlin.listener.createListener4WithSpeed

class ListenerSample {

    fun listener(): DownloadListener = createListener(
        onTaskStart = { log("taskStart") },
        onConnectTrialStart = { _, _ -> log("connectTrialStart") },
        onConnectTrialEnd = { _, _, _ -> log("connectTrialEnd") },
        onDownloadFromBeginning = { _, _, _ -> log("downloadFromBeginning") },
        onDownloadFromBreakpoint = { _, _ -> log("downloadFromBreakpoint") },
        onConnectStart = { _, _, _ -> log("connectStart") },
        onConnectEnd = { _, _, _, _ -> log("connectEnd") },
        onFetchStart = { _, _, _ -> log("fetchStart") },
        onFetchProgress = { _, _, _ -> log("fetchProgress") },
        onFetchEnd = { _, _, _ -> log("fetchEnd") }
    ) { _, _, _ -> log("taskEnd") }

    fun lisetner1(): DownloadListener1 = createListener1 { _, _, _, _ -> log("taskEnd") }

    fun lisetner2(): DownloadListener2 = createListener2({ log("taskStart") }) { _, _, _ ->
        log("taskEnd")
    }

    fun listener3(): DownloadListener3 = createListener3 { log("taskEnd") }

    fun listener4(): DownloadListener4 = createListener4 { _, _, _, _ -> log("taskEnd") }

    fun listener4WithSpeed(): DownloadListener4WithSpeed = createListener4WithSpeed { _, _, _, _ ->
        log("taskEnd")
    }

    companion object {

        private const val TAG = "ListenerSample"

        private fun log(msg: String) {
            Log.d(TAG, msg)
        }
    }
}