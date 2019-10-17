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

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView

import com.liulishuo.okdownload.sample.R

class QueueRecyclerAdapter(
    private val controller: QueueController
) : RecyclerView.Adapter<QueueRecyclerAdapter.QueueViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        return QueueViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.item_queue,
            parent, false)
        )
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        controller.bind(holder, position)
    }

    override fun getItemCount(): Int = controller.size()

    class QueueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTv: TextView = itemView.findViewById(R.id.nameTv)
        var priorityTv: TextView = itemView.findViewById(R.id.priorityTv)
        var prioritySb: SeekBar = itemView.findViewById(R.id.prioritySb)
        var statusTv: TextView = itemView.findViewById(R.id.statusTv)
        var progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }
}