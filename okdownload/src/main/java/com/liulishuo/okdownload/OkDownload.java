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

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.dispatcher.CallbackDispatcher;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;
import com.liulishuo.okdownload.core.download.DownloadStrategy;
import com.liulishuo.okdownload.core.file.DownloadOutputStream;
import com.liulishuo.okdownload.core.file.DownloadUriOutputStream;
import com.liulishuo.okdownload.core.file.ProcessFileStrategy;

public class OkDownload {

    @SuppressLint("StaticFieldLeak") static volatile OkDownload singleton;

    private final DownloadDispatcher downloadDispatcher;
    private final CallbackDispatcher callbackDispatcher;
    private final BreakpointStore breakpointStore;
    private final DownloadConnection.Factory connectionFactory;
    private final DownloadOutputStream.Factory outputStreamFactory;
    private final ProcessFileStrategy processFileStrategy;
    private final DownloadStrategy downloadStrategy;

    private final Context context;

    @Nullable DownloadMonitor monitor;

    OkDownload(Context context, DownloadDispatcher downloadDispatcher,
               CallbackDispatcher callbackDispatcher, DownloadStore store,
               DownloadConnection.Factory connectionFactory,
               DownloadOutputStream.Factory outputStreamFactory,
               ProcessFileStrategy processFileStrategy, DownloadStrategy downloadStrategy) {
        this.context = context;
        this.downloadDispatcher = downloadDispatcher;
        this.callbackDispatcher = callbackDispatcher;
        this.breakpointStore = store;
        this.connectionFactory = connectionFactory;
        this.outputStreamFactory = outputStreamFactory;
        this.processFileStrategy = processFileStrategy;
        this.downloadStrategy = downloadStrategy;

        this.downloadDispatcher.setDownloadStore(Util.createRemitDatabase(store));
    }

    public DownloadDispatcher downloadDispatcher() { return downloadDispatcher; }

    public CallbackDispatcher callbackDispatcher() { return callbackDispatcher; }

    public BreakpointStore breakpointStore() { return breakpointStore; }

    public DownloadConnection.Factory connectionFactory() { return connectionFactory; }

    public DownloadOutputStream.Factory outputStreamFactory() { return outputStreamFactory; }

    public ProcessFileStrategy processFileStrategy() { return processFileStrategy; }

    public DownloadStrategy downloadStrategy() { return downloadStrategy; }

    public Context context() { return this.context; }

    public void setMonitor(@Nullable DownloadMonitor monitor) {
        this.monitor = monitor;
    }

    @Nullable public DownloadMonitor getMonitor() {
        return monitor;
    }

    public static OkDownload with() {
        if (singleton == null) {
            synchronized (OkDownload.class) {
                if (singleton == null) {
                    if (OkDownloadProvider.context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    singleton = new Builder(OkDownloadProvider.context).build();
                }
            }
        }
        return singleton;
    }

    public static void setSingletonInstance(@NonNull OkDownload okDownload) {
        if (singleton != null) {
            throw new IllegalArgumentException(("OkDownload must be null."));
        }

        synchronized (OkDownload.class) {
            if (singleton != null) {
                throw new IllegalArgumentException(("OkDownload must be null."));
            }
            singleton = okDownload;
        }
    }

    public static class Builder {
        private DownloadDispatcher downloadDispatcher;
        private CallbackDispatcher callbackDispatcher;
        private DownloadStore downloadStore;
        private DownloadConnection.Factory connectionFactory;
        private ProcessFileStrategy processFileStrategy;
        private DownloadStrategy downloadStrategy;
        private DownloadOutputStream.Factory outputStreamFactory;
        private DownloadMonitor monitor;
        private final Context context;

        public Builder(@NonNull Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder downloadDispatcher(DownloadDispatcher downloadDispatcher) {
            this.downloadDispatcher = downloadDispatcher;
            return this;
        }

        public Builder callbackDispatcher(CallbackDispatcher callbackDispatcher) {
            this.callbackDispatcher = callbackDispatcher;
            return this;
        }

        public Builder downloadStore(DownloadStore downloadStore) {
            this.downloadStore = downloadStore;
            return this;
        }

        public Builder connectionFactory(DownloadConnection.Factory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public Builder outputStreamFactory(DownloadOutputStream.Factory outputStreamFactory) {
            this.outputStreamFactory = outputStreamFactory;
            return this;
        }

        public Builder processFileStrategy(ProcessFileStrategy processFileStrategy) {
            this.processFileStrategy = processFileStrategy;
            return this;
        }

        public Builder downloadStrategy(DownloadStrategy downloadStrategy) {
            this.downloadStrategy = downloadStrategy;
            return this;
        }

        public Builder monitor(DownloadMonitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public OkDownload build() {
            if (downloadDispatcher == null) {
                downloadDispatcher = new DownloadDispatcher();
            }

            if (callbackDispatcher == null) {
                callbackDispatcher = new CallbackDispatcher();
            }

            if (downloadStore == null) {
                downloadStore = Util.createDefaultDatabase(context);
            }

            if (connectionFactory == null) {
                connectionFactory = Util.createDefaultConnectionFactory();
            }

            if (outputStreamFactory == null) {
                outputStreamFactory = new DownloadUriOutputStream.Factory();
            }

            if (processFileStrategy == null) {
                processFileStrategy = new ProcessFileStrategy();
            }

            if (downloadStrategy == null) {
                downloadStrategy = new DownloadStrategy();
            }

            OkDownload okDownload = new OkDownload(context, downloadDispatcher, callbackDispatcher,
                    downloadStore, connectionFactory, outputStreamFactory, processFileStrategy,
                    downloadStrategy);

            okDownload.setMonitor(monitor);

            Util.d("OkDownload", "downloadStore[" + downloadStore + "] connectionFactory["
                    + connectionFactory);
            return okDownload;
        }
    }
}
