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

package cn.dreamtobe.okdownload.core.dispatcher;

import cn.dreamtobe.okdownload.task.DownloadTask;

/**
 * Created by Jacksgong on 24/09/2017.
 */

public class DefaultDownloadDispatcher implements DownloadDispatcher {

    @Override
    public void enqueue(DownloadTask task) {
        //
    }

    @Override
    public void finish(DownloadTask task) {

    }

    @Override
    public DownloadTask next() {
        return null;
    }
}
