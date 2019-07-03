/*
 * Copyright (c) 2018 LingoChamp Inc.
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

package com.liulishuo.okdownload.core.listener.assist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;

public class ListenerModelHandler<T extends ListenerModelHandler.ListenerModel> implements
        ListenerAssist {

    volatile T singleTaskModel;
    final SparseArray<T> modelList = new SparseArray<>();

    private Boolean alwaysRecoverModel;
    private final ModelCreator<T> creator;

    ListenerModelHandler(ModelCreator<T> creator) {
        this.creator = creator;
    }

    public boolean isAlwaysRecoverAssistModel() {
        return alwaysRecoverModel != null && alwaysRecoverModel;
    }

    public void setAlwaysRecoverAssistModel(boolean isAlwaysRecoverModel) {
        this.alwaysRecoverModel = isAlwaysRecoverModel;
    }

    @Override public void setAlwaysRecoverAssistModelIfNotSet(boolean isAlwaysRecoverAssistModel) {
        if (this.alwaysRecoverModel == null) this.alwaysRecoverModel = isAlwaysRecoverAssistModel;
    }

    @NonNull T addAndGetModel(@NonNull DownloadTask task, @Nullable BreakpointInfo info) {
        T model = creator.create(task.getId());
        synchronized (this) {
            if (singleTaskModel == null) {
                singleTaskModel = model;
            } else {
                modelList.put(task.getId(), model);
            }

            if (info != null) {
                model.onInfoValid(info);
            }
        }

        return model;
    }

    @Nullable T getOrRecoverModel(@NonNull DownloadTask task, @Nullable BreakpointInfo info) {
        final int id = task.getId();

        T model = null;
        synchronized (this) {
            if (singleTaskModel != null && singleTaskModel.getId() == id) model = singleTaskModel;
        }
        if (model == null) model = modelList.get(id);

        if (model != null || !isAlwaysRecoverAssistModel()) return model;

        model = addAndGetModel(task, info);
        return model;
    }

    @NonNull T removeOrCreate(@NonNull DownloadTask task, @Nullable BreakpointInfo info) {
        final int id = task.getId();
        T model;
        synchronized (this) {
            if (singleTaskModel != null && singleTaskModel.getId() == id) {
                model = singleTaskModel;
                singleTaskModel = null;
            } else {
                model = modelList.get(id);
                modelList.remove(id);
            }
        }

        if (model == null) {
            model = creator.create(id);

            if (info != null) {
                model.onInfoValid(info);
            }
        }

        return model;
    }

    public interface ModelCreator<T extends ListenerModel> {
        T create(int id);
    }

    interface ListenerModel {
        int getId();

        void onInfoValid(@NonNull BreakpointInfo info);
    }
}
