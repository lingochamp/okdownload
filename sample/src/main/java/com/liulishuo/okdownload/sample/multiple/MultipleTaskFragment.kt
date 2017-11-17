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

package com.liulishuo.okdownload.sample.multiple

import android.app.Fragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liulishuo.okdownload.sample.MainActivity
import com.liulishuo.okdownload.sample.R
import kotlinx.android.synthetic.main.fragment_multiple_task.*

class MultipleTaskFragment : Fragment() {

    companion object {
        fun newInstance() = MultipleTaskFragment()
    }

    var demo: MultipleTaskDemo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (demo == null) demo = MultipleTaskDemo(activity
                , (activity as MainActivity).listenerManager)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_multiple_task, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMultipleTaskDemo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        demo!!.detachViews()
    }

    private fun initMultipleTaskDemo() {
        recyclerView.adapter = MultipleTaskViewAdapter(demo!!)
        recyclerView.layoutManager = LinearLayoutManager(activity)
    }
}