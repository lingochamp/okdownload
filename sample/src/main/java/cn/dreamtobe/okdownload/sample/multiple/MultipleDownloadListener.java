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

package cn.dreamtobe.okdownload.sample.multiple;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.core.cause.EndCause;
import cn.dreamtobe.okdownload.core.cause.ResumeFailedCause;
import cn.dreamtobe.okdownload.core.listener.DownloadListener1;


public class MultipleDownloadListener extends DownloadListener1 {
    @Override public void taskStart(DownloadTask task) {
    }

    @Override
    public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
    }

    @Override protected void connected(DownloadTask task, int blockCount, long currentOffset,
                                       long totalLength) {
    }

    @Override protected void progress(DownloadTask task, long currentOffset) {
    }

    @Override protected void retry(DownloadTask task, @NonNull ResumeFailedCause cause) {
    }
}
