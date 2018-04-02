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

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.cause.EndCause;

import java.io.IOException;
import java.util.List;

public class RemitStoreOnSQLite implements RemitSyncExecutor.RemitAgent, DownloadStore {

    private static final String TAG = "RemitStoreOnSQLite";

    @NonNull private final RemitSyncToDBHelper remitHelper;

    @NonNull private final BreakpointStoreOnSQLite onSQLiteWrapper;
    @NonNull private final BreakpointSQLiteHelper sqLiteHelper;
    @NonNull private final DownloadStore sqliteCache;

    RemitStoreOnSQLite(@NonNull BreakpointStoreOnSQLite sqlite) {
        this.remitHelper = new RemitSyncToDBHelper(this);

        this.onSQLiteWrapper = sqlite;
        this.sqliteCache = onSQLiteWrapper.onCache;
        this.sqLiteHelper = onSQLiteWrapper.helper;
    }

    RemitStoreOnSQLite(@NonNull RemitSyncToDBHelper helper, @NonNull BreakpointStoreOnSQLite sqlite,
                       @NonNull DownloadStore sqliteCache,
                       @NonNull BreakpointSQLiteHelper sqLiteHelper) {
        this.remitHelper = helper;

        this.onSQLiteWrapper = sqlite;
        this.sqliteCache = sqliteCache;
        this.sqLiteHelper = sqLiteHelper;
    }

    @Nullable @Override public BreakpointInfo get(int id) {
        return onSQLiteWrapper.get(id);
    }

    @NonNull @Override public BreakpointInfo createAndInsert(@NonNull DownloadTask task)
            throws IOException {
        if (remitHelper.isNotFreeToDatabase(task.getId())) return sqliteCache.createAndInsert(task);

        return onSQLiteWrapper.createAndInsert(task);
    }

    @Override public void onTaskStart(int id) {
        onSQLiteWrapper.onTaskStart(id);
        remitHelper.onTaskStart(id);
    }

    @Override public void onSyncToFilesystemSuccess(@NonNull BreakpointInfo info, int blockIndex,
                                                    long increaseLength) {
        if (remitHelper.isNotFreeToDatabase(info.getId())) {
            sqliteCache.onSyncToFilesystemSuccess(info, blockIndex, increaseLength);
            return;
        }

        onSQLiteWrapper.onSyncToFilesystemSuccess(info, blockIndex, increaseLength);
    }

    @Override public boolean update(@NonNull BreakpointInfo info) throws IOException {
        if (remitHelper.isNotFreeToDatabase(info.getId())) return sqliteCache.update(info);

        return onSQLiteWrapper.update(info);
    }

    @Override
    public void onTaskEnd(int id, @NonNull EndCause cause, @Nullable Exception exception) {
        sqliteCache.onTaskEnd(id, cause, exception);

        if (cause == EndCause.COMPLETED) {
            remitHelper.discard(id);
        } else {
            remitHelper.endAndEnsureToDB(id);
        }
    }

    @Override public void bunchTaskCanceled(int[] ids) {
        sqliteCache.bunchTaskCanceled(ids);
        if (ids.length > 0) {
            remitHelper.endAndEnsureToDB(ids);
        }
    }

    @Override public void remove(int id) {
        sqliteCache.remove(id);

        remitHelper.discard(id);
    }

    @Override public int findOrCreateId(@NonNull DownloadTask task) {
        return onSQLiteWrapper.findOrCreateId(task);
    }

    @Nullable @Override
    public BreakpointInfo findAnotherInfoFromCompare(@NonNull DownloadTask task,
                                                     @NonNull BreakpointInfo ignored) {
        return onSQLiteWrapper.findAnotherInfoFromCompare(task, ignored);
    }

    @Nullable @Override public String getResponseFilename(String url) {
        return onSQLiteWrapper.getResponseFilename(url);
    }

    // following accept database operation what is controlled by helper.
    @Override public void syncCacheToDB(List<Integer> idList) throws IOException {
        final SQLiteDatabase database = sqLiteHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            for (Integer id : idList) {
                syncCacheToDB(id);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    @Override public void syncCacheToDB(int id) throws IOException {
        sqLiteHelper.removeInfo(id);

        final BreakpointInfo info = sqliteCache.get(id);
        if (info == null || info.getFilename() == null || info.getTotalOffset() <= 0) return;

        sqLiteHelper.insert(info);
    }

    @Override public void removeInfo(int id) {
        sqLiteHelper.removeInfo(id);
    }

    public static void setRemitToDBDelayMillis(int delayMillis) {
        final BreakpointStore store = OkDownload.with().breakpointStore();
        if (!(store instanceof RemitStoreOnSQLite)) {
            throw new IllegalStateException(
                    "The current store is " + store + " not RemitStoreOnSQLite!");
        }

        delayMillis = Math.max(0, delayMillis);
        ((RemitStoreOnSQLite) store).remitHelper.delayMillis = delayMillis;
    }
}
