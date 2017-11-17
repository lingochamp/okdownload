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

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.sample.R

class MultipleTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val priorityTv: TextView = itemView.findViewById(R.id.priorityTv)
    val priorityBar: SeekBar = itemView.findViewById(R.id.priorityBar)

    val statusTv: TextView = itemView.findViewById(R.id.statusTv)
    private val startOrCancelTv: TextView = itemView.findViewById(R.id.startOrCancelTv)
    val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

    fun setToStart(task: DownloadTask, listener: MultipleDownloadListener) {
        startOrCancelTv.setText(R.string.start)
        startOrCancelTv.setOnClickListener {
            run {
                task.enqueue(listener)
                setToCancel(task)
            }
        }
    }

    fun setToCancel(task: DownloadTask) {
        startOrCancelTv.setText(R.string.cancel)
        startOrCancelTv.setOnClickListener { task.cancel() }
    }

    fun updatePriority(priority: Int) {
        priorityBar.progress = priority
        priorityTv.text = priorityTv.context.getString(R.string.priority, priority)
    }
}