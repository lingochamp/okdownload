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

package com.liulishuo.okdownload.sample.util.queue

import android.content.Context
import android.util.Log
import android.widget.SeekBar

import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.sample.R
import com.liulishuo.okdownload.sample.util.DemoUtil
import com.liulishuo.okdownload.sample.util.queue.QueueRecyclerAdapter.QueueViewHolder
import java.io.File

class QueueController {
    private val taskList = arrayListOf<DownloadTask>()
    private var context: DownloadContext? = null
    private val listener = QueueListener()
    private var queueDir: File? = null

    fun initTasks(context: Context, listener: DownloadContextListener) {
        val set = DownloadContext.QueueSet()
        val parentFile = File(DemoUtil.getParentFile(context), "queue")
        this.queueDir = parentFile

        set.setParentPathFile(parentFile)
        set.minIntervalMillisCallbackProcess = 200

        val builder = set.commit()

        var url = "http://dldir1.qq.com/weixin/android/weixin6516android1120.apk"
        var boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "1. WeChat")

        url = "https://cdn.llscdn.com/yy/files/tkzpx40x-lls-LLS-5.7-785-20171108-111118.apk"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "2. LiuLiShuo")

        url = "https://t.alipayobjects.com/L1/71/100/and/alipay_wap_main.apk"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "3. Alipay")

        url = "https://dldir1.qq.com/qqfile/QQforMac/QQ_V6.2.0.dmg"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "4. QQ for Mac")

        val zhiHuApkHome = "https://zhstatic.zhihu.com/pkg/store/zhihu"
        url = "$zhiHuApkHome/futureve-mobile-zhihu-release-5.8.2(596).apk"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "5. ZhiHu")

        url = "http://d1.music.126.net/dmusic/CloudMusic_official_4.3.2.468990.apk"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "6. NetEaseMusic")

        url = "http://d1.music.126.net/dmusic/NeteaseMusic_1.5.9_622_officialsite.dmg"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "7. NetEaseMusic for Mac")

        url = "http://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "8. WeChat for Windows")

        url = "https://dldir1.qq.com/foxmail/work_weixin/wxwork_android_2.4.5.5571_100001.apk"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "9. WeChat Work")

        url = "https://dldir1.qq.com/foxmail/work_weixin/WXWork_2.4.5.213.dmg"
        boundTask = builder.bind(url)
        TagUtil.saveTaskName(boundTask, "10. WeChat Work for Mac")

        builder.setListener(listener)

        this.context = builder.build().also { this.taskList.addAll(it.tasks) }
    }

    fun deleteFiles() {
        if (queueDir != null) {
            val children = queueDir!!.list()
            if (children != null) {
                for (child in children) {
                    if (!File(queueDir, child).delete()) {
                        Log.w("QueueController", "delete $child failed!")
                    }
                }
            }

            if (!queueDir!!.delete()) {
                Log.w("QueueController", "delete $queueDir failed!")
            }
        }

        for (task in taskList) {
            TagUtil.clearProceedTask(task)
        }
    }

    fun setPriority(task: DownloadTask, priority: Int) {
        val newTask = task.toBuilder().setPriority(priority).build()
        this.context = context?.toBuilder()
            ?.bindSetTask(newTask)
            ?.build()
            ?.also {
                taskList.clear()
                taskList.addAll(it.tasks)
            }
        newTask.setTags(task)
        TagUtil.savePriority(newTask, priority)
    }

    fun start(isSerial: Boolean) {
        this.context?.start(listener, isSerial)
    }

    fun stop() {
        if (this.context?.isStarted == true) {
            this.context?.stop()
        }
    }

    internal fun bind(holder: QueueViewHolder, position: Int) {
        val task = taskList[position]
        Log.d(TAG, "bind " + position + " for " + task.url)

        listener.bind(task, holder)
        listener.resetInfo(task, holder)

        // priority
        val priority = TagUtil.getPriority(task)
        holder.priorityTv.text = holder.priorityTv.context.getString(R.string.priority, priority)
        holder.prioritySb.progress = priority
        if (this.context?.isStarted == true) {
            holder.prioritySb.isEnabled = false
        } else {
            holder.prioritySb.isEnabled = true
            holder.prioritySb.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    var isFromUser: Boolean = false

                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        isFromUser = fromUser
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        if (isFromUser) {
                            val taskPriority = seekBar.progress
                            setPriority(task, taskPriority)
                            holder.priorityTv.text =
                                seekBar.context.getString(R.string.priority, taskPriority)
                        }
                    }
                })
        }
    }

    fun size(): Int = taskList.size

    companion object {
        private const val TAG = "QueueController"
    }
}