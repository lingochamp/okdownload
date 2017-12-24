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

package com.liulishuo.okdownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnifiedListenerManager {

    final SparseArray<ArrayList<DownloadListener>> realListenerMap;

    public UnifiedListenerManager() {
        realListenerMap = new SparseArray<>();
    }

    public synchronized void detachListener(DownloadListener listener) {
        final int count = realListenerMap.size();

        final List<Integer> needRemoveKeyList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final List<DownloadListener> listenerList = realListenerMap.valueAt(i);
            if (listenerList == null) continue;
            listenerList.remove(listener);

            if (listenerList.isEmpty()) needRemoveKeyList.add(realListenerMap.keyAt(i));
        }

        for (int key : needRemoveKeyList) {
            realListenerMap.remove(key);
        }
    }

    public synchronized boolean detachListener(DownloadTask task, DownloadListener listener) {
        final int id = task.getId();
        final List<DownloadListener> listenerList = realListenerMap.get(id);

        if (listenerList == null) return false;

        boolean result = listenerList.remove(listener);
        if (listenerList.isEmpty()) realListenerMap.remove(id);

        return result;
    }

    public synchronized void attachListener(DownloadTask task, @NonNull DownloadListener listener) {
        final int id = task.getId();
        ArrayList<DownloadListener> listenerList = realListenerMap.get(id);
        if (listenerList == null) {
            listenerList = new ArrayList<>();
            realListenerMap.put(id, listenerList);
        }

        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public synchronized void attachAndEnqueueIfNotRun(DownloadTask task,
                                                      @NonNull DownloadListener listener) {
        attachListener(task, listener);
        final boolean pendingOrRunning = StatusUtil.isSameTaskPendingOrRunning(task);

        if (!pendingOrRunning) {
            task.enqueue(hostListener);
        }
    }

    public synchronized void enqueueTaskWithUnifiedListener(@NonNull DownloadTask task,
                                                            @NonNull DownloadListener listener) {
        attachListener(task, listener);

        task.enqueue(hostListener);
    }

    public synchronized void executeTaskWithUnifiedListener(@NonNull DownloadTask task,
                                                            @NonNull DownloadListener listener) {
        attachListener(task, listener);

        task.execute(hostListener);
    }

    private final DownloadListener hostListener = new DownloadListener() {
        @Override public void taskStart(DownloadTask task) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.taskStart(task);
            }
        }

        @Override public void downloadFromBeginning(DownloadTask task, BreakpointInfo info,
                                                    ResumeFailedCause cause) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.downloadFromBeginning(task, info, cause);
            }

        }

        @Override public void downloadFromBreakpoint(DownloadTask task, BreakpointInfo info) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.downloadFromBreakpoint(task, info);
            }
        }

        @Override public void connectStart(DownloadTask task, int blockIndex,
                                           @NonNull Map<String, List<String>> requestHeaderFields) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.connectStart(task, blockIndex, requestHeaderFields);
            }
        }

        @Override public void connectEnd(DownloadTask task, int blockIndex, int responseCode,
                                         @NonNull Map<String, List<String>> responseHeaderFields) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.connectEnd(task, blockIndex, responseCode, responseHeaderFields);
            }
        }

        @Override public void splitBlockEnd(DownloadTask task, BreakpointInfo info) {

            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.splitBlockEnd(task, info);
            }
        }

        @Override public void fetchStart(DownloadTask task, int blockIndex, long contentLength) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.fetchStart(task, blockIndex, contentLength);
            }

        }

        @Override public void fetchProgress(DownloadTask task, int blockIndex, long increaseBytes) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.fetchProgress(task, blockIndex, increaseBytes);
            }
        }

        @Override public void fetchEnd(DownloadTask task, int blockIndex, long contentLength) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.fetchEnd(task, blockIndex, contentLength);
            }
        }

        @Override
        public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
            final DownloadListener[] listeners = getThreadSafeArray(task, realListenerMap);
            if (listeners == null) return;

            for (final DownloadListener realOne : listeners) {
                if (realOne == null) continue;
                realOne.taskEnd(task, cause, realCause);
            }
        }
    };

    private static DownloadListener[] getThreadSafeArray(DownloadTask task,
                                                         SparseArray<ArrayList<DownloadListener>>
                                                                 realListenerMap) {
        final ArrayList<DownloadListener> listenerList = realListenerMap.get(task.getId());
        if (listenerList == null || listenerList.size() <= 0) return null;

        final DownloadListener[] copyList = new DownloadListener[listenerList.size()];
        listenerList.toArray(copyList);
        return copyList;
    }
}
