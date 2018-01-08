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

package com.liulishuo.okdownload.core.dispatcher;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.List;
import java.util.Map;

// Dispatch callback to listeners
public class CallbackDispatcher {
    private static final String TAG = "CallbackDispatcher";

    public CallbackDispatcher() {
        transmit = new DownloadListener() {
            private Handler uiHandler = new Handler(Looper.getMainLooper());

            @Override
            public void taskStart(final DownloadTask task) {
                Util.i(TAG, "taskStart: " + task.getId());
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().taskStart(task);
                        }
                    });
                } else {
                    task.getListener().taskStart(task);
                }

            }

            @Override
            public void connectStart(final DownloadTask task, final int blockIndex,
                                     @NonNull final Map<String, List<String>> requestHeaderFields) {
                Util.i(TAG, "connectStart: " + task.getId());
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().connectStart(task, blockIndex, requestHeaderFields);
                        }
                    });
                } else {
                    task.getListener().connectStart(task, blockIndex, requestHeaderFields);
                }
            }

            @Override
            public void connectEnd(final DownloadTask task, final int blockIndex,
                                   final int responseCode,
                                   @NonNull final Map<String, List<String>> requestHeaderFields) {
                Util.i(TAG, "connectEnd: " + task.getId());
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().connectEnd(task, blockIndex, responseCode,
                                    requestHeaderFields);
                        }
                    });
                } else {
                    task.getListener().connectEnd(task, blockIndex, responseCode,
                            requestHeaderFields);
                }
            }

            @Override public void splitBlockEnd(final DownloadTask task,
                                                final BreakpointInfo info) {
                Util.i(TAG, "splitBlockEnd: " + task.getId());
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().splitBlockEnd(task, info);
                        }
                    });
                } else {
                    task.getListener().splitBlockEnd(task, info);
                }
            }

            @Override
            public void downloadFromBeginning(final DownloadTask task, final BreakpointInfo info,
                                              final ResumeFailedCause cause) {
                Util.i(TAG, "downloadFromBeginning: " + task.getId());
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().downloadFromBeginning(task, info, cause);
                        }
                    });
                } else {
                    task.getListener().downloadFromBeginning(task, info, cause);
                }
            }

            @Override
            public void downloadFromBreakpoint(final DownloadTask task, final BreakpointInfo info) {
                Util.i(TAG, "downloadFromBreakpoint: " + task.getId());
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().downloadFromBreakpoint(task, info);
                        }
                    });
                } else {
                    task.getListener().downloadFromBreakpoint(task, info);
                }
            }

            @Override
            public void fetchStart(final DownloadTask task, final int blockIndex,
                                   final long contentLength) {
                Util.i(TAG, "fetchStart: " + task.getId());
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().fetchStart(task, blockIndex, contentLength);
                        }
                    });
                } else {
                    task.getListener().fetchStart(task, blockIndex, contentLength);
                }
            }

            @Override
            public void fetchProgress(final DownloadTask task, final int blockIndex,
                                      final long increaseBytes) {
                Util.i(TAG, "fetchProgress: " + task.getId());
                if (task.getMinIntervalMillisCallbackProcess() > 0) {
                    DownloadTask.TaskCallbackWrapper
                            .setLastCallbackProcessTs(task, SystemClock.uptimeMillis());
                }

                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().fetchProgress(task, blockIndex, increaseBytes);
                        }
                    });
                } else {
                    task.getListener().fetchProgress(task, blockIndex, increaseBytes);
                }
            }

            @Override
            public void fetchEnd(final DownloadTask task, final int blockIndex,
                                 final long contentLength) {
                Util.i(TAG, "fetchEnd: " + task.getId());
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().fetchEnd(task, blockIndex, contentLength);
                        }
                    });
                } else {
                    task.getListener().fetchEnd(task, blockIndex, contentLength);
                }
            }

            @Override
            public void taskEnd(final DownloadTask task, final EndCause cause,
                                @Nullable final Exception realCause) {
                Util.i(TAG, "taskEnd: " + task.getId() + " " + cause);
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().taskEnd(task, cause, realCause);
                        }
                    });
                } else {
                    task.getListener().taskEnd(task, cause, realCause);
                }
            }
        };
    }

    public boolean isFetchProcessMoment(DownloadTask task) {
        final long minInterval = task.getMinIntervalMillisCallbackProcess();
        final long now = SystemClock.uptimeMillis();
        return minInterval <= 0
                || now - DownloadTask.TaskCallbackWrapper
                .getLastCallbackProcessTs(task) >= minInterval;
    }

    public DownloadListener dispatch() {
        return transmit;
    }

    // Just transmit to the main looper.
    private final DownloadListener transmit;
}
