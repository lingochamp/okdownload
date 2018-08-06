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

package com.liulishuo.filedownloader;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.liulishuo.okdownload.DownloadMonitor;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

/**
 * The FileDownloader global monitor, monitor the begin、over for all tasks.
 *
 * @see BaseDownloadTask.LifeCycleCallback#onBegin()
 * @see BaseDownloadTask.LifeCycleCallback#onOver() ()
 * @see BaseDownloadTask#start()
 * @see FileDownloader#start(FileDownloadListener, boolean)
 */
public class FileDownloadMonitor {

    private static DownloadMonitorAdapter monitor;

    public static void setGlobalMonitor(@NonNull IMonitor monitor) {
        FileDownloadMonitor.monitor = new DownloadMonitorAdapter(monitor);
    }

    public static void releaseGlobalMonitor() {
        monitor = null;
    }

    public static IMonitor getMonitor() {
        return monitor.callbackMonitor;
    }

    static DownloadMonitor getDownloadMonitor() {
        return monitor;
    }

    public static boolean isValid() {
        return getDownloadMonitor() != null && getMonitor() != null;
    }

    static final class DownloadMonitorAdapter implements DownloadMonitor, IMonitor {

        @NonNull
        final IMonitor callbackMonitor;

        DownloadMonitorAdapter(IMonitor callbackMonitor) {
            this.callbackMonitor = callbackMonitor;
        }

        @Override
        public void taskStart(DownloadTask task) {
            final DownloadTaskAdapter downloadTaskAdapter =
                    FileDownloadUtils.findDownloadTaskAdapter(task);
            if (downloadTaskAdapter != null) {
                onTaskBegin(downloadTaskAdapter);
            }
        }

        @Override
        public void taskDownloadFromBreakpoint(@NonNull DownloadTask task,
                                               @NonNull BreakpointInfo info) {
            taskDownloadFromBeginning(task, info, null);
        }

        @Override
        public void taskDownloadFromBeginning(
                @NonNull DownloadTask task, @NonNull BreakpointInfo info,
                @Nullable ResumeFailedCause cause) {
            final DownloadTaskAdapter downloadTaskAdapter =
                    FileDownloadUtils.findDownloadTaskAdapter(task);
            if (downloadTaskAdapter != null) {
                onRequestStart(downloadTaskAdapter);
                onTaskStarted(downloadTaskAdapter);
            }
        }

        @Override
        public void taskEnd(DownloadTask task, EndCause cause, @Nullable Exception realCause) {
            final DownloadTaskAdapter downloadTaskAdapter =
                    FileDownloadUtils.findDownloadTaskAdapter(task);
            if (downloadTaskAdapter != null) {
                onTaskOver(downloadTaskAdapter);
            }
        }

        // FileDownloadMonitor
        @Override
        public void onRequestStart(int count, boolean serial, FileDownloadListener lis) {
            callbackMonitor.onRequestStart(count, serial, lis);
        }

        @Override
        public void onRequestStart(BaseDownloadTask task) {
            callbackMonitor.onRequestStart(task);
        }

        @Override
        public void onTaskBegin(BaseDownloadTask task) {
            callbackMonitor.onTaskBegin(task);
        }

        @Override
        public void onTaskStarted(BaseDownloadTask task) {
            callbackMonitor.onTaskStarted(task);
        }

        @Override
        public void onTaskOver(BaseDownloadTask task) {
            callbackMonitor.onTaskOver(task);
        }
    }


    /**
     * The interface used to monitor all tasks's status change in the FileDownloader.
     * <p/>
     * All method in this interface will be invoked synchronous, recommend don't to hold the thread
     * of invoking the method.
     *
     * @see FileDownloadMonitor#setGlobalMonitor(IMonitor)
     */
    public interface IMonitor {
        /**
         * Request to start multi-tasks manually.
         *
         * @param count  The count of tasks will start.
         * @param serial Tasks will be started in serial or parallel.
         * @param lis    The listener.
         */
        void onRequestStart(int count, boolean serial, FileDownloadListener lis);

        /**
         * Request to start a task.
         *
         * @param task The task will start.
         */
        void onRequestStart(BaseDownloadTask task);

        /**
         * The method will be invoked when the task in the internal is beginning.
         *
         * @param task The task is received to start internally.
         */
        void onTaskBegin(BaseDownloadTask task);

        /**
         * The method will be invoked when the download runnable of the task has started running.
         *
         * @param task The task finish pending and start download runnable.
         */
        void onTaskStarted(BaseDownloadTask task);

        /**
         * The method will be invoked when the task in the internal is over.
         *
         * @param task The task is over.
         */
        void onTaskOver(BaseDownloadTask task);
    }

}
