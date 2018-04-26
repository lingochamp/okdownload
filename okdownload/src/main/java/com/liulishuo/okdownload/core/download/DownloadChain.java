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

package com.liulishuo.okdownload.core.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.dispatcher.CallbackDispatcher;
import com.liulishuo.okdownload.core.exception.InterruptException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;
import com.liulishuo.okdownload.core.interceptor.BreakpointInterceptor;
import com.liulishuo.okdownload.core.interceptor.FetchDataInterceptor;
import com.liulishuo.okdownload.core.interceptor.Interceptor;
import com.liulishuo.okdownload.core.interceptor.RetryInterceptor;
import com.liulishuo.okdownload.core.interceptor.connect.CallServerInterceptor;
import com.liulishuo.okdownload.core.interceptor.connect.HeaderInterceptor;
import com.liulishuo.okdownload.core.interceptor.connect.RedirectInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadChain implements Runnable {

    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload Cancel Block", false));

    private final int blockIndex;

    @NonNull private final DownloadTask task;
    @NonNull private final BreakpointInfo info;
    @NonNull private final DownloadCache cache;

    final List<Interceptor.Connect> connectInterceptorList = new ArrayList<>();
    final List<Interceptor.Fetch> fetchInterceptorList = new ArrayList<>();
    int connectIndex = 0;
    int fetchIndex = 0;

    private long responseContentLength;
    private DownloadConnection connection;

    long noCallbackIncreaseBytes;
    private volatile Thread currentThread;

    private final CallbackDispatcher callbackDispatcher;

    @NonNull private final DownloadStore store;

    static DownloadChain createChain(int blockIndex, DownloadTask task,
                                     @NonNull BreakpointInfo info,
                                     @NonNull DownloadCache cache,
                                     @NonNull DownloadStore store) {
        return new DownloadChain(blockIndex, task, info, cache, store);
    }

    private DownloadChain(int blockIndex, @NonNull DownloadTask task, @NonNull BreakpointInfo info,
                          @NonNull DownloadCache cache, @NonNull DownloadStore store) {
        this.blockIndex = blockIndex;
        this.task = task;
        this.cache = cache;
        this.info = info;
        this.store = store;
        this.callbackDispatcher = OkDownload.with().callbackDispatcher();
    }

    public long getResponseContentLength() {
        return responseContentLength;
    }

    public void setResponseContentLength(long responseContentLength) {
        this.responseContentLength = responseContentLength;
    }

    public void cancel() {
        if (finished.get() || this.currentThread == null) return;

        currentThread.interrupt();
    }

    @NonNull public DownloadTask getTask() {
        return task;
    }

    @NonNull public BreakpointInfo getInfo() {
        return this.info;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public void setConnection(@NonNull DownloadConnection connection) {
        this.connection = connection;
    }

    @NonNull public DownloadCache getCache() {
        return cache;
    }

    public void setRedirectLocation(String location) {
        this.cache.setRedirectLocation(location);
    }

    public MultiPointOutputStream getOutputStream() {
        return this.cache.getOutputStream();
    }

    @Nullable public DownloadConnection getConnection() {
        return this.connection;
    }

    @NonNull public DownloadConnection getConnectionOrCreate() throws IOException {
        if (cache.isInterrupt()) throw InterruptException.SIGNAL;

        if (connection == null) {
            final String url;
            final String redirectLocation = cache.getRedirectLocation();
            if (redirectLocation != null) {
                url = redirectLocation;
            } else {
                url = info.getUrl();
            }

            connection = OkDownload.with().connectionFactory().create(url);
        }
        return connection;
    }

    public void increaseCallbackBytes(long increaseBytes) {
        this.noCallbackIncreaseBytes += increaseBytes;
    }

    public void flushNoCallbackIncreaseBytes() {
        if (noCallbackIncreaseBytes == 0) return;

        callbackDispatcher.dispatch().fetchProgress(task, blockIndex, noCallbackIncreaseBytes);
        noCallbackIncreaseBytes = 0;
    }

    void start() throws IOException {
        final CallbackDispatcher dispatcher = OkDownload.with().callbackDispatcher();
        // connect chain
        final RetryInterceptor retryInterceptor = new RetryInterceptor();
        final BreakpointInterceptor breakpointInterceptor = new BreakpointInterceptor();
        connectInterceptorList.add(retryInterceptor);
        connectInterceptorList.add(breakpointInterceptor);
        connectInterceptorList.add(new RedirectInterceptor());
        connectInterceptorList.add(new HeaderInterceptor());
        connectInterceptorList.add(new CallServerInterceptor());

        connectIndex = 0;
        final DownloadConnection.Connected connected = processConnect();
        if (cache.isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        dispatcher.dispatch().fetchStart(task, blockIndex, getResponseContentLength());
        // fetch chain
        final FetchDataInterceptor fetchDataInterceptor =
                new FetchDataInterceptor(blockIndex, connected.getInputStream(),
                        getOutputStream(), task);
        fetchInterceptorList.add(retryInterceptor);
        fetchInterceptorList.add(breakpointInterceptor);
        fetchInterceptorList.add(fetchDataInterceptor);

        fetchIndex = 0;
        final long totalFetchedBytes = processFetch();
        dispatcher.dispatch().fetchEnd(task, blockIndex, totalFetchedBytes);
    }

    public void resetConnectForRetry() {
        connectIndex = 1;
        if (connection != null) connection.release();
        connection = null;
    }

    public DownloadConnection.Connected processConnect() throws IOException {
        if (cache.isInterrupt()) throw InterruptException.SIGNAL;
        return connectInterceptorList.get(connectIndex++).interceptConnect(this);
    }

    public long processFetch() throws IOException {
        if (cache.isInterrupt()) throw InterruptException.SIGNAL;
        return fetchInterceptorList.get(fetchIndex++).interceptFetch(this);
    }

    public long loopFetch() throws IOException {
        if (fetchIndex == fetchInterceptorList.size()) {
            // last one is fetch data interceptor
            fetchIndex--;
        }
        return processFetch();
    }

    private AtomicBoolean finished = new AtomicBoolean(false);

    boolean isFinished() { return finished.get(); }

    @NonNull public DownloadStore getDownloadStore() {
        return store;
    }

    @Override
    public void run() {
        if (isFinished()) {
            throw new IllegalAccessError("The chain has been finished!");
        }
        this.currentThread = Thread.currentThread();

        try {
            start();
        } catch (IOException ignored) {
            // interrupt.
        } finally {
            finished.set(true);
            if (cache.isUserCanceled()) EXECUTOR.execute(cancelRunnable);
        }
    }

    private final Runnable cancelRunnable = new Runnable() {
        @Override public void run() {
            if (finished.get() || currentThread == null) return;

            final DownloadConnection connection = getConnection();
            if (connection != null) {
                connection.release();
            }
        }
    };
}