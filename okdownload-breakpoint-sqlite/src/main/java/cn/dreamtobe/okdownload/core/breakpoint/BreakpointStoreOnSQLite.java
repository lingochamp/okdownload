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

package cn.dreamtobe.okdownload.core.breakpoint;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import cn.dreamtobe.okdownload.DownloadTask;

public class BreakpointStoreOnSQLite implements BreakpointStore {

    private final BreakpointSQLiteHelper helper;
    private final BreakpointStoreOnCache onCache;

    BreakpointStoreOnSQLite(BreakpointSQLiteHelper helper, BreakpointStoreOnCache onCache) {
        this.helper = helper;
        this.onCache = onCache;
    }

    public BreakpointStoreOnSQLite(Context context) {
        this.helper = new BreakpointSQLiteHelper(context.getApplicationContext());
        this.onCache = new BreakpointStoreOnCache(helper.loadToCache());
    }

    @Nullable @Override public BreakpointInfo get(int id) {
        return onCache.get(id);
    }

    @Override public BreakpointInfo createAndInsert(@NonNull DownloadTask task) throws IOException {
        final BreakpointInfo info = onCache.createAndInsert(task);
        helper.insert(info);
        return info;
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
        return result;
    }

    @Override public void completeDownload(int id) {
        onCache.completeDownload(id);
        helper.removeInfo(id);
    }

    @Override public void discard(int id) {
        onCache.discard(id);
        helper.removeInfo(id);
    }

    @Override public int createId(@NonNull DownloadTask task) {
        return onCache.createId(task);
    }

    void close() {
        helper.close();
    }
}
