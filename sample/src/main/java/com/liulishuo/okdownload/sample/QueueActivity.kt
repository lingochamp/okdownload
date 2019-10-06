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

import android.os.Bundle
import android.support.v7.widget.AppCompatRadioButton
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView

import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.sample.base.BaseSampleActivity
import com.liulishuo.okdownload.sample.util.queue.QueueController
import com.liulishuo.okdownload.sample.util.queue.QueueRecyclerAdapter

/**
 * On this demo you will be known how to download batch tasks as a queue and download with different
 * priority.
 */
class QueueActivity : BaseSampleActivity() {

    private var controller: QueueController? = null
    private var adapter: QueueRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)
        initQueueActivity(
            findViewById(R.id.actionView),
            findViewById<View>(R.id.actionTv) as TextView,
            findViewById<View>(R.id.serialRb) as AppCompatRadioButton,
            findViewById<View>(R.id.parallelRb) as AppCompatRadioButton,
            findViewById<View>(R.id.recyclerView) as RecyclerView,
            findViewById<View>(R.id.deleteActionView) as CardView,
            findViewById(R.id.deleteActionTv)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        this.controller?.stop()
    }

    override fun titleRes(): Int = R.string.queue_download_title

    private fun initQueueActivity(
        actionView: View,
        actionTv: TextView,
        serialRb: AppCompatRadioButton,
        parallelRb: AppCompatRadioButton,
        recyclerView: RecyclerView,
        deleteActionView: CardView,
        deleteActionTv: View
    ) {
        initController(actionView, actionTv, serialRb, parallelRb, deleteActionView, deleteActionTv)
        initRecyclerView(recyclerView)
        initAction(actionView, actionTv, serialRb, parallelRb, deleteActionView, deleteActionTv)
    }

    private fun initController(
        actionView: View,
        actionTv: TextView,
        serialRb: AppCompatRadioButton,
        parallelRb: AppCompatRadioButton,
        deleteActionView: CardView,
        deleteActionTv: View
    ) {
        val controller = QueueController()
        this.controller = controller
        controller.initTasks(
            this,
            object : DownloadContextListener {
                override fun taskEnd(
                    context: DownloadContext,
                    task: DownloadTask,
                    cause: EndCause,
                    realCause: Exception?,
                    remainCount: Int) {
                }

                override fun queueEnd(context: DownloadContext) {
                    actionView.tag = null
                    actionTv.setText(R.string.start)
                    // to cancel
                    controller.stop()

                    serialRb.isEnabled = true
                    parallelRb.isEnabled = true

                    deleteActionView.isEnabled = true
                    deleteActionView.cardElevation = deleteActionView.tag as Float
                    deleteActionTv.isEnabled = true

                    adapter?.notifyDataSetChanged()
                }
            }
        )
    }

    private fun initRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        controller?.let {
            val adapter = QueueRecyclerAdapter(it)
            this.adapter = adapter
            recyclerView.adapter = adapter
        }
    }

    private fun initAction(
        actionView: View,
        actionTv: TextView,
        serialRb: AppCompatRadioButton,
        parallelRb: AppCompatRadioButton,
        deleteActionView: CardView, deleteActionTv: View) {
        deleteActionView.setOnClickListener {
            controller?.deleteFiles()
            adapter?.notifyDataSetChanged()
        }

        actionTv.setText(R.string.start)
        actionView.setOnClickListener { v ->
            val started = v.tag != null

            if (started) {
                controller?.stop()
            } else {
                v.tag = Any()
                actionTv.setText(R.string.cancel)

                // to start
                controller?.start(serialRb.isChecked)
                adapter?.notifyDataSetChanged()

                serialRb.isEnabled = false
                parallelRb.isEnabled = false
                deleteActionView.isEnabled = false
                deleteActionView.tag = deleteActionView.cardElevation
                deleteActionView.cardElevation = 0f
                deleteActionTv.isEnabled = false
            }
        }
    }
}