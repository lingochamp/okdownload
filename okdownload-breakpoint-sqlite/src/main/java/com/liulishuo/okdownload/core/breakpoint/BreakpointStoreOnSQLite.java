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

package com.liulishuo.okdownload.core.breakpoint;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;

import java.io.IOException;

public class BreakpointStoreOnSQLite implements DownloadStore {

    private static final String TAG = "BreakpointStoreOnSQLite";
    protected final BreakpointSQLiteHelper helper;
    protected final BreakpointStoreOnCache onCache;

    BreakpointStoreOnSQLite(BreakpointSQLiteHelper helper, BreakpointStoreOnCache onCache) {
        this.helper = helper;
        this.onCache = onCache;
    }

    public BreakpointStoreOnSQLite(Context context) {
        this.helper = new BreakpointSQLiteHelper(context.getApplicationContext());
        this.onCache = new BreakpointStoreOnCache(helper.loadToCache(),
                helper.loadResponseFilenameToMap());
    }

    @Nullable @Override public BreakpointInfo get(int id) {
        return onCache.get(id);
    }

    @NonNull @Override public BreakpointInfo createAndInsert(@NonNull DownloadTask task)
            throws IOException {
        final BreakpointInfo info = onCache.createAndInsert(task);
        helper.insert(info);
        return info;
    }

    @Override public void onTaskStart(int id) {
        onCache.onTaskStart(id);
    }

    @Override public void onSyncToFilesystemSuccess(@NonNull BreakpointInfo info, int blockIndex,
                                                    long increaseLength) {
        onCache.onSyncToFilesystemSuccess(info, blockIndex, increaseLength);
        final long newCurrentOffset = info.getBlock(blockIndex).getCurrentOffset();
        helper.updateBlockIncrease(info, blockIndex, newCurrentOffset);
    }

    @Override public boolean update(@NonNull BreakpointInfo breakpointInfo) throws IOException {
        final boolean result = onCache.update(breakpointInfo);
        helper.updateInfo(breakpointInfo);
        final String filename = breakpointInfo.getFilename();
        Util.d(TAG, "update " + breakpointInfo);
        if (breakpointInfo.isTaskOnlyProvidedParentPath() && filename != null) {
            helper.updateFilename(breakpointInfo.getUrl(), filename);
        }
        return result;
    }

    @Override
    public void onTaskEnd(int id, @NonNull EndCause cause, @Nullable Exception exception) {
        onCache.onTaskEnd(id, cause, exception);
        if (cause == EndCause.COMPLETED) {
            helper.removeInfo(id);
        }
    }

    @Override public void bunchTaskCanceled(int[] ids) {
    }

    @Override public void remove(int id) {
        onCache.remove(id);
        helper.removeInfo(id);
    }

    @Override public int findOrCreateId(@NonNull DownloadTask task) {
        return onCache.findOrCreateId(task);
    }

    @Nullable @Override
    public BreakpointInfo findAnotherInfoFromCompare(@NonNull DownloadTask task,
                                                     @NonNull BreakpointInfo ignored) {
        return onCache.findAnotherInfoFromCompare(task, ignored);
    }

    @Nullable @Override public String getResponseFilename(String url) {
        return onCache.getResponseFilename(url);
    }

    void close() {
        helper.close();
    }

    @NonNull public DownloadStore createRemitSelf() {
        return new RemitStoreOnSQLite(this);
    }
}
