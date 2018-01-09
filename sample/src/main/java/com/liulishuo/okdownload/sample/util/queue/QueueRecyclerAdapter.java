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

package com.liulishuo.okdownload.sample.util.queue;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.liulishuo.okdownload.sample.R;

public class QueueRecyclerAdapter
        extends RecyclerView.Adapter<QueueRecyclerAdapter.QueueViewHolder> {

    private final QueueController controller;

    public QueueRecyclerAdapter(@NonNull QueueController controller) {
        this.controller = controller;
    }

    @Override
    public QueueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new QueueViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue, parent, false));
    }

    @Override public void onBindViewHolder(QueueViewHolder holder, int position) {
        controller.bind(holder, position);
    }

    @Override public int getItemCount() {
        return controller.size();
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {

        TextView nameTv;
        TextView priorityTv;
        SeekBar prioritySb;
        TextView statusTv;
        ProgressBar progressBar;

        QueueViewHolder(View itemView) {
            super(itemView);
            nameTv = itemView.findViewById(R.id.nameTv);
            priorityTv = itemView.findViewById(R.id.priorityTv);
            prioritySb = itemView.findViewById(R.id.prioritySb);
            statusTv = itemView.findViewById(R.id.statusTv);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}