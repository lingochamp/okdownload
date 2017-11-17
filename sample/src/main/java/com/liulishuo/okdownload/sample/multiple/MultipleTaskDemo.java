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

package com.liulishuo.okdownload.sample.multiple;

import android.content.Context;
import android.support.annotation.NonNull;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.UnifiedListenerManager;

import java.util.ArrayList;
import java.util.List;

public class MultipleTaskDemo {

    @NonNull private final UnifiedListenerManager listenerManager;
    private final List<DownloadTask> taskList = new ArrayList<>();
    private final MultipleDownloadListener listener = new MultipleDownloadListener();

    public MultipleTaskDemo(@NonNull Context context,
                            @NonNull UnifiedListenerManager listenerManager) {

        this.listenerManager = listenerManager;
    }

    public int size() {
        return taskList.size();
    }

    public void bind(MultipleTaskViewHolder viewHolder, int position) {
        final DownloadTask task = taskList.get(position);
        listenerManager.attachListener(task, listener);
        listener.bind(task, viewHolder);
        listener.resetInfo(task, viewHolder);
    }

    public void detachViews() {
        listener.clearBound();
    }
}
