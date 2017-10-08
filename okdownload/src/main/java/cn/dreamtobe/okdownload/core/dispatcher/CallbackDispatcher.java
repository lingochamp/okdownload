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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.task.DownloadTask;

// Dispatch callback to listeners
public class CallbackDispatcher {

    private boolean isCallbackToUIThread = false;

    public void setCallbackToUIThread(final boolean isCallbackToUIThread) {
        this.isCallbackToUIThread = isCallbackToUIThread;
    }

    public DownloadListener dispatch(DownloadTask task) {
        if (isCallbackToUIThread) {
            return new UIThreadDownloadListener(task.getListener());
        } else {
            return task.getListener();
        }
    }

    // Just transmit to the main looper.
    class UIThreadDownloadListener implements DownloadListener {
        private Handler uiHandler = new Handler(Looper.getMainLooper());
        private final DownloadListener realListener;

        UIThreadDownloadListener(DownloadListener listener) {
            this.realListener = listener;
        }

        @Override
        public void taskStart(final DownloadTask task) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.taskStart(task);
                }
            });
        }

        @Override
        public void breakpointData(final DownloadTask task, @Nullable final BreakpointInfo info) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.breakpointData(task, info);
                }
            });
        }

        @Override
        public void connectStart(final DownloadTask task, final int blockIndex, final DownloadConnection connection) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.connectStart(task, blockIndex, connection);
                }
            });
        }

        @Override
        public void connectEnd(final DownloadTask task, final int blockIndex, final DownloadConnection connection) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.connectEnd(task, blockIndex, connection);
                }
            });
        }

        @Override
        public void downloadFromBeginning(final DownloadTask task, final BreakpointInfo info, final ResumeFailedCause cause) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.downloadFromBeginning(task, info, cause);
                }
            });
        }

        @Override
        public void downloadFromBreakpoint(final DownloadTask task, final BreakpointInfo info) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.downloadFromBreakpoint(task, info);
                }
            });
        }

        @Override
        public void fetchStart(final DownloadTask task, final int blockIndex, final long contentLength) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.fetchStart(task, blockIndex, contentLength);
                }
            });
        }

        @Override
        public void fetchProgress(final DownloadTask task, final int blockIndex, final long downloadedBytes) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.fetchProgress(task, blockIndex, downloadedBytes);
                }
            });
        }

        @Override
        public void fetchEnd(final DownloadTask task, final int blockIndex, final long contentLength) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.fetchEnd(task, blockIndex, contentLength);
                }
            });
        }

        @Override
        public void taskEnd(final DownloadTask task, final EndCause cause, @Nullable final Exception realCause) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    realListener.taskEnd(task, cause, realCause);
                }
            });
        }
    }

}
