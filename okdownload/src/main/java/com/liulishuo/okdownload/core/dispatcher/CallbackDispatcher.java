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
import com.liulishuo.okdownload.DownloadMonitor;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
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
            public void taskStart(@NonNull final DownloadTask task) {
                Util.d(TAG, "taskStart: " + task.getId());
                inspectTaskStart(task);
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
            public void connectTrialStart(@NonNull final DownloadTask task,
                                          @NonNull final Map<String, List<String>> headerFields) {
                Util.d(TAG, "-----> start trial task(" + task.getId() + ") " + headerFields);
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener().connectTrialStart(task, headerFields);
                        }
                    });
                } else {
                    task.getListener().connectTrialStart(task, headerFields);
                }
            }

            @Override
            public void connectTrialEnd(@NonNull final DownloadTask task, final int responseCode,
                                        @NonNull final Map<String, List<String>> headerFields) {
                Util.d(TAG, "<----- finish trial task(" + task.getId()
                        + ") code[" + responseCode + "]" + headerFields);
                if (task.isAutoCallbackToUIThread()) {
                    uiHandler.post(new Runnable() {
                        @Override public void run() {
                            task.getListener()
                                    .connectTrialEnd(task, responseCode, headerFields);
                        }
                    });
                } else {
                    task.getListener()
                            .connectTrialEnd(task, responseCode, headerFields);
                }
            }

            @Override
            public void downloadFromBeginning(@NonNull final DownloadTask task,
                                              @NonNull final BreakpointInfo info,
                                              @NonNull final ResumeFailedCause cause) {
                Util.d(TAG, "downloadFromBeginning: " + task.getId());
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
            public void downloadFromBreakpoint(@NonNull final DownloadTask task,
                                               @NonNull final BreakpointInfo info) {
                Util.d(TAG, "downloadFromBreakpoint: " + task.getId());
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
            public void connectStart(@NonNull final DownloadTask task, final int blockIndex,
                                     @NonNull final Map<String, List<String>> requestHeaderFields) {
                Util.d(TAG, "-----> start connection task(" + task.getId()
                        + ") block(" + blockIndex + ") " + requestHeaderFields);
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
            public void connectEnd(@NonNull final DownloadTask task, final int blockIndex,
                                   final int responseCode,
                                   @NonNull final Map<String, List<String>> requestHeaderFields) {
                Util.d(TAG, "<----- finish connection task(" + task.getId()
                        + ") block(" + blockIndex + ") code[" + responseCode + "]" + requestHeaderFields);
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

            @Override
            public void fetchStart(@NonNull final DownloadTask task, final int blockIndex,
                                   final long contentLength) {
                Util.d(TAG, "fetchStart: " + task.getId());
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
            public void fetchProgress(@NonNull final DownloadTask task, final int blockIndex,
                                      final long increaseBytes) {
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
            public void fetchEnd(@NonNull final DownloadTask task, final int blockIndex,
                                 final long contentLength) {
                Util.d(TAG, "fetchEnd: " + task.getId());
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
            public void taskEnd(@NonNull final DownloadTask task, @NonNull final EndCause cause,
                                @Nullable final Exception realCause) {
                if (cause == EndCause.ERROR) {
                    // only care about error.
                    Util.d(TAG, "taskEnd: " + task.getId() + " " + cause + " " + realCause);
                }
                inspectTaskEnd(task, cause, realCause);
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

    private void inspectTaskStart(DownloadTask task) {
        final DownloadMonitor monitor = OkDownload.with().getMonitor();
        if (monitor != null) monitor.taskStart(task);
    }

    private void inspectTaskEnd(final DownloadTask task, final EndCause cause,
                                @Nullable final Exception realCause) {
        final DownloadMonitor monitor = OkDownload.with().getMonitor();
        if (monitor != null) monitor.taskEnd(task, cause, realCause);
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
