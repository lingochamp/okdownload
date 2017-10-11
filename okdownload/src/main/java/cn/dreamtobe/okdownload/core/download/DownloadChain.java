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

package cn.dreamtobe.okdownload.core.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.OkDownload;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.dispatcher.CallbackDispatcher;
import cn.dreamtobe.okdownload.core.exception.CanceledException;
import cn.dreamtobe.okdownload.core.file.MultiPointOutputStream;
import cn.dreamtobe.okdownload.core.interceptor.BreakpointInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.FetchDataInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.Interceptor;
import cn.dreamtobe.okdownload.core.interceptor.RetryInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.connect.CallServerInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.connect.HeaderInterceptor;
import cn.dreamtobe.okdownload.core.interceptor.connect.RedirectInterceptor;

public class DownloadChain implements Runnable {

    public static final int CHUNKED_CONTENT_LENGTH = -1;

    public final int blockIndex;
    public final DownloadTask task;
    private final BreakpointInfo info;
    private long responseContentLength;

    private DownloadCall.DownloadCache cache;

    private DownloadConnection connection;

    private Thread parkThread;

    static DownloadChain createChain(int blockIndex, DownloadTask task, BreakpointInfo info,
                                     DownloadCall.DownloadCache cache) {
        return new DownloadChain(blockIndex, task, info, cache);
    }

    static DownloadChain createFirstBlockChain(Thread parkThread, DownloadTask task,
                                               BreakpointInfo info,
                                               DownloadCall.DownloadCache cache) {
        final DownloadChain chain = new DownloadChain(0, task, info, cache);
        chain.parkThread = parkThread;

        return chain;
    }

    private DownloadChain(int blockIndex, DownloadTask task, BreakpointInfo info,
                          DownloadCall.DownloadCache cache) {
        this.blockIndex = blockIndex;
        this.task = task;
        this.cache = cache;
        this.info = info;
    }

    private List<Interceptor.Connect> connectInterceptorList = new ArrayList<>();
    private List<Interceptor.Fetch> fetchInterceptorList = new ArrayList<>();
    private int connectIndex = 0;
    private int fetchIndex = 0;

    public boolean isOtherBlockPark() {
        return parkThread != null;
    }

    public void unparkOtherBlock() {
        LockSupport.unpark(parkThread);
        parkThread = null;
    }

    public BreakpointInfo getInfo() {
        return this.info;
    }

    public void setConnection(DownloadConnection connection) {
        this.connection = connection;
    }

    public DownloadCall.DownloadCache getCache() {
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
        if (connection == null) {
            final String url;
            final String redirectLocation = cache.getRedirectLocation();
            if (redirectLocation != null) {
                url = redirectLocation;
            } else if (info == null) {
                throw new IllegalArgumentException(
                        "Invoke getConnection must after breakpoint interceptor!");
            } else {
                url = info.getUrl();
            }

            connection = OkDownload.with().connectionFactory().create(url);
        }
        return connection;
    }

    void start() throws IOException {
        final CallbackDispatcher dispatcher = OkDownload.with().callbackDispatcher();
        // connect chain
        dispatcher.dispatch().connectStart(task, blockIndex);
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
            throw CanceledException.SIGNAL;
        }

        dispatcher.dispatch().connectEnd(task, blockIndex, getConnectionOrCreate(), connected);

        dispatcher.dispatch().fetchStart(task, blockIndex, getResponseContentLength());
        // fetch chain
        final FetchDataInterceptor fetchDataInterceptor =
                new FetchDataInterceptor(blockIndex, connected.getInputStream(),
                        getOutputStream(), task);
        fetchInterceptorList.add(retryInterceptor);
        fetchInterceptorList.add(breakpointInterceptor);
        fetchInterceptorList.add(fetchDataInterceptor);

        final long totalFetchedBytes = processFetch();
        dispatcher.dispatch().fetchEnd(task, blockIndex, totalFetchedBytes);
    }

    public void setResponseContentLength(long responseContentLength) {
        this.responseContentLength = responseContentLength;
    }

    public long getResponseContentLength() {
        return responseContentLength;
    }

    public DownloadConnection.Connected processConnect() throws IOException {
        return connectInterceptorList.get(connectIndex++).interceptConnect(this);
    }

    public long processFetch() throws IOException {
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

    boolean isFinished() {
        return finished.get();
    }

    @Override
    public void run() {
        if (isFinished()) {
            throw new IllegalAccessError("The chain has been finished!");
        }

        try {
            start();
        } catch (IOException e) {
            // cancelled.
            e.printStackTrace();
        } finally {
            finished.set(true);
        }
    }


}