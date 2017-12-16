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

package com.liulishuo.okdownload.sample.comprehensive.single

import android.app.Fragment
import android.os.Bundle
import android.support.v7.widget.AppCompatButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liulishuo.okdownload.sample.R
import com.liulishuo.okdownload.sample.comprehensive.ComprehensiveActivity
import kotlinx.android.synthetic.main.fragment_single_task.*
import org.jetbrains.anko.runOnUiThread


class SingleTaskFragment : Fragment() {

    private var demo: SingleTaskDemo? = null

    companion object {
        fun newInstance(): SingleTaskFragment {
            return SingleTaskFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (demo == null) demo = SingleTaskDemo(
                activity,
                (activity as ComprehensiveActivity).listenerManager)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_single_task, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        demo!!.attachViews(createSingleTaskViewAdapter(), { runOnUiThread { setToStart(startOrCancelBtn) } })
        initSingleTaskDemo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        demo!!.detachViews()
    }

    private fun initSingleTaskDemo() {
        startSameTaskBtn.isEnabled = false
        startSameFileBtn.isEnabled = false

        startOrCancelBtn.setOnClickListener { v ->
            run {
                if (v.tag == null) {
                    demo!!.startAsync()
                    setToCancel(v)
                } else {
                    demo!!.cancelTask()
                }
            }
        }
        startSameTaskBtn.setOnClickListener { demo!!.startSameTask_sameTaskBusy() }
        startSameFileBtn.setOnClickListener { demo!!.startSamePathTask_fileBusy() }
        getStatusBtn.setOnClickListener { demo!!.updateStatus() }

        if (demo!!.isTaskPendingOrRunning) setToCancel(startOrCancelBtn)
        else setToStart(startOrCancelBtn)
    }

    private fun createSingleTaskViewAdapter() = SingleTaskViewAdapter.Builder()
            .setStatusTv(status_title)
            .setTaskViews(task_title, task_speed, task_pb)
            .setBlock0Views(block0_title, block0_speed, block0_pb)
            .setBlock1Views(block1_title, block1_speed, block1_pb)
            .setBlock2Views(block2_title, block2_speed, block2_pb)
            .setBlock3Views(block3_title, block3_speed, block3_pb)
            .setExtInfoTv(extInfo_tv)
            .build()

    private fun setToStart(v: View) {
        v.tag = null
        (v as AppCompatButton).setText(R.string.start_download)
        startSameTaskBtn.isEnabled = false
        startSameFileBtn.isEnabled = false
    }

    private fun setToCancel(v: View) {
        (v as AppCompatButton).setText(R.string.cancel_download)
        v.tag = Object()
        startSameTaskBtn.isEnabled = true
        startSameFileBtn.isEnabled = true
    }
}