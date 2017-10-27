/*
 * Copyright (C) 2017 Jacksgong(jacksgong.com)
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

package cn.dreamtobe.okdownload.sample

import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_one -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        initSingleTaskDemo()
    }

    private fun initSingleTaskDemo() {
        val demo = SingleTaskDemo.Builder()
                .setStatusTv(status_title)
                .setTaskViews(task_title, task_speed, task_pb)
                .setBlock0Views(block0_title, block0_speed, block0_pb)
                .setBlock1Views(block1_title, block1_speed, block1_pb)
                .setBlock2Views(block2_title, block2_speed, block2_pb)
                .setBlock3Views(block3_title, block3_speed, block3_pb)
                .setExtInfoTv(extInfo_tv)
                .build()

        val storePath = Uri.fromFile(externalCacheDir)

        startSameTaskBtn.isEnabled = false
        startSameFileBtn.isEnabled = false

        startOrCancelBtn.setOnClickListener { v ->
            run {
                if (v.tag == null) {
                    demo.startAsync({ runOnUiThread { setToStart(v) } })
                    setToCancel(v)
                } else {
                    demo.cancelTask()
                }
            }
        }
        startSameTaskBtn.setOnClickListener { demo.startSameTask_sameTaskBusy(storePath) }
        startSameFileBtn.setOnClickListener { demo.startSamePathTask_fileBusy(storePath) }
        getStatusBtn.setOnClickListener { demo.updateStatus() }
    }

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
