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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.StatusUtil
import com.liulishuo.okdownload.core.Util
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.kotlin.enqueue4WithSpeed
import com.liulishuo.okdownload.sample.base.BaseSampleActivity
import com.liulishuo.okdownload.sample.util.DemoUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * On this demo you can see the simplest way to download a task.
 */
class SingleActivity : BaseSampleActivity() {

    private var task: DownloadTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single)
        initSingleDownload(
            findViewById<View>(R.id.statusTv) as TextView,
            findViewById<View>(R.id.progressBar) as ProgressBar,
            findViewById(R.id.actionView),
            findViewById<View>(R.id.actionTv) as TextView)
    }

    override fun titleRes(): Int {
        return R.string.single_download_title
    }

    override fun onDestroy() {
        super.onDestroy()
        task?.cancel()
    }

    private fun initSingleDownload(
        statusTv: TextView,
        progressBar: ProgressBar,
        actionView: View,
        actionTv: TextView
    ) {
        initTask()
        initStatus(statusTv, progressBar)
        initAction(actionView, actionTv, statusTv, progressBar)
    }

    private fun initTask() {
        val filename = "single-test"
        val url = "https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk"
        val parentFile = DemoUtil.getParentFile(this)
        task = DownloadTask.Builder(url, parentFile)
            .setFilename(filename)
            // the minimal interval millisecond for callback progress
            .setMinIntervalMillisCallbackProcess(16)
            // ignore the same task has already completed in the past.
            .setPassIfAlreadyCompleted(false)
            .build()
    }

    private fun initStatus(statusTv: TextView, progressBar: ProgressBar) = task?.let {
        val status = StatusUtil.getStatus(it)
        if (status == StatusUtil.Status.COMPLETED) {
            progressBar.progress = progressBar.max
        }
        statusTv.text = status.toString()
        StatusUtil.getCurrentInfo(it)?.let { info ->
            Log.d(TAG, "init status with: $info")
            DemoUtil.calcProgressToView(progressBar, info.totalOffset, info.totalLength)
        }
    }

    private fun initAction(
        actionView: View,
        actionTv: TextView,
        statusTv: TextView,
        progressBar: ProgressBar
    ) {
        actionTv.setText(R.string.start)
        actionView.setOnClickListener {
            task?.let {
                if (it.tag != null) {
                    // to cancel
                    it.cancel()
                } else {
                    // to start
                    actionTv.setText(R.string.cancel)
                    startTask(statusTv, progressBar, actionTv)
                    it.tag = "mark-task-started"
                }
            }
        }
    }

    private fun startTask(
        statusTv: TextView,
        progressBar: ProgressBar,
        actionTv: TextView
    ) {
        var totalLength: Long = 0
        var readableTotalLength: String? = null
        task?.enqueue4WithSpeed(
            onTaskStart = { statusTv.setText(R.string.task_start) },
            onInfoReadyWithSpeed = { _, info, _, _ ->
                statusTv.setText(R.string.info_ready)
                totalLength = info.totalLength
                readableTotalLength = Util.humanReadableBytes(totalLength, true)
                DemoUtil.calcProgressToView(progressBar, info.totalOffset, totalLength)
            },
            onConnectStart = { _, blockIndex, _ ->
                val status = "Connect End $blockIndex"
                statusTv.text = status
            },
            onProgressWithSpeed = { _, currentOffset, taskSpeed ->
                val readableOffset = Util.humanReadableBytes(currentOffset, true)
                val progressStatus = "$readableOffset/$readableTotalLength"
                val speed = taskSpeed.speed()
                val progressStatusWithSpeed = "$progressStatus($speed)"
                statusTv.text = progressStatusWithSpeed
                DemoUtil.calcProgressToView(progressBar, currentOffset, totalLength)
            }
        ) { task, cause, realCause, taskSpeed ->
            val statusWithSpeed = cause.toString() + " " + taskSpeed.averageSpeed()
            statusTv.text = statusWithSpeed
            actionTv.setText(R.string.start)
            // remove mark
            task.tag = null
            if (cause == EndCause.COMPLETED) {
                val realMd5 = fileToMD5(task.file!!.absolutePath)
                if (!realMd5!!.equals("f836a37a5eee5dec0611ce15a76e8fd5", ignoreCase = true)) {
                    Log.e(TAG, "file is wrong because of md5 is wrong $realMd5")
                }
            }
            realCause?.let {
                Log.e(TAG, "download error", it)
            }
        }
    }

    companion object {

        private const val TAG = "SingleActivity"

        @SuppressFBWarnings(value = ["REC"])
        fun fileToMD5(filePath: String): String? {
            var inputStream: InputStream? = null
            try {
                inputStream = FileInputStream(filePath)
                val buffer = ByteArray(1024)
                val digest = MessageDigest.getInstance("MD5")
                var numRead = 0
                while (numRead != -1) {
                    numRead = inputStream.read(buffer)
                    if (numRead > 0) {
                        digest.update(buffer, 0, numRead)
                    }
                }
                val md5Bytes = digest.digest()
                return convertHashToString(md5Bytes)
            } catch (ignored: Exception) {
                return null
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "file to md5 failed", e)
                    }
                }
            }
        }

        @SuppressLint("DefaultLocale")
        private fun convertHashToString(md5Bytes: ByteArray): String = StringBuffer().apply {
            md5Bytes.forEach { byte ->
                append(((byte.toInt() and 0xff) + 0x100).toString(16).substring(1))
            }
        }.toString().toUpperCase()
    }
}