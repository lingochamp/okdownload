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

package com.liulishuo.okdownload.sample.comprehensive.multiple

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.liulishuo.okdownload.sample.R

class MultipleTaskViewAdapter(private val demo: MultipleTaskDemo) : RecyclerView.Adapter<MultipleTaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultipleTaskViewHolder {
        return MultipleTaskViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_multiple_task, parent, false))
    }

    override fun onBindViewHolder(holder: MultipleTaskViewHolder, position: Int) {
        demo.bind(holder, position)
    }

    override fun getItemCount(): Int {
        return demo.size()
    }
}