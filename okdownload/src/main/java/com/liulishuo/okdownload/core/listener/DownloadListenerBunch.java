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

package com.liulishuo.okdownload.core.listener;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DownloadListenerBunch implements DownloadListener {

    @NonNull final DownloadListener[] listenerList;

    DownloadListenerBunch(@NonNull DownloadListener[] listenerList) {
        this.listenerList = listenerList;
    }

    @Override
    public void taskStart(DownloadTask task) {
        for (DownloadListener listener : listenerList) {
            listener.taskStart(task);
        }
    }

    @Override public void downloadFromBeginning(DownloadTask task, BreakpointInfo info,
                                                ResumeFailedCause cause) {
        for (DownloadListener listener : listenerList) {
            listener.downloadFromBeginning(task, info, cause);
        }
    }

    @Override public void downloadFromBreakpoint(DownloadTask task, BreakpointInfo info) {
        for (DownloadListener listener : listenerList) {
            listener.downloadFromBreakpoint(task, info);
        }
    }

    @Override public void connectStart(DownloadTask task, int blockIndex,
                                       @NonNull Map<String, List<String>> requestHeaderFields) {
        for (DownloadListener listener : listenerList) {
            listener.connectStart(task, blockIndex, requestHeaderFields);
        }
    }

    @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                     @NonNull Map<String, List<String>> responseHeaderFields) {
        for (DownloadListener listener : listenerList) {
            listener.connectEnd(task, blockIndex, responseCode, responseHeaderFields);
        }
    }

    @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {
        for (DownloadListener listener : listenerList) {
            listener.splitBlockEnd(task, info);
        }
    }

    @Override public void fetchStart(DownloadTask task, int blockIndex, long contentLength) {
        for (DownloadListener listener : listenerList) {
            listener.fetchStart(task, blockIndex, contentLength);
        }
    }

    @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
        for (DownloadListener listener : listenerList) {
            listener.fetchProgress(task, blockIndex, increaseBytes);
        }
    }

    @Override public void fetchEnd(DownloadTask task, int blockIndex, long contentLength) {
        for (DownloadListener listener : listenerList) {
            listener.fetchEnd(task, blockIndex, contentLength);
        }
    }

    @Override
    public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
        for (DownloadListener listener : listenerList) {
            listener.taskEnd(task, cause, realCause);
        }
    }

    public boolean contain(DownloadListener targetListener) {
        for (DownloadListener listener : listenerList) {
            if (listener == targetListener) return true;
        }

        return false;
    }

    public static class Builder {

        private List<DownloadListener> listenerList = new ArrayList<>();

        public DownloadListenerBunch build() {
            return new DownloadListenerBunch(
                    listenerList.toArray(new DownloadListener[listenerList.size()]));
        }

        public DownloadListenerBunch.Builder append(DownloadListener listener) {
            if (listener != null && !listenerList.contains(listener)) {
                listenerList.add(listener);
            }

            return this;
        }

        public boolean remove(DownloadListener listener) {
            return listenerList.remove(listener);
        }
    }
}
