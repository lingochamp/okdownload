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

package com.liulishuo.okdownload.kotlin.listener

import com.liulishuo.okdownload.DownloadTask

typealias taskCallback = (task: DownloadTask) -> Unit

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.started].
 */
typealias onStarted = taskCallback

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.completed].
 */
typealias onCompleted = taskCallback

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.canceled].
 */
typealias onCanceled = taskCallback

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.warn].
 */
typealias onWarn = taskCallback

/**
 * Correspond to [com.liulishuo.okdownload.core.listener.DownloadListener3.error].
 */
typealias onError = (task: DownloadTask, e: Exception) -> Unit