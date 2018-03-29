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
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;

import java.io.IOException;

public class RemitStoreOnSQLite extends BreakpointStoreOnSQLite
        implements RemitSyncToDBHelper.RemitAgent {

    private static final String TAG = "RemitStoreOnSQLite";

    @NonNull final RemitSyncToDBHelper remitHelper;

    RemitStoreOnSQLite(BreakpointSQLiteHelper helper, BreakpointStoreOnCache onCache,
                       @NonNull RemitSyncToDBHelper remitHelper) {
        super(helper, onCache);
        this.remitHelper = remitHelper;
    }

    RemitStoreOnSQLite(BreakpointSQLiteHelper helper, BreakpointStoreOnCache onCache) {
        super(helper, onCache);
        remitHelper = new RemitSyncToDBHelper(this);
    }

    public RemitStoreOnSQLite(Context context) {
        super(context);
        remitHelper = new RemitSyncToDBHelper(this);
    }

    @NonNull @Override public BreakpointInfo createAndInsert(@NonNull DownloadTask task)
            throws IOException {
        if (remitHelper.isNotFreeToDatabase(task.getId())) return onCache.createAndInsert(task);

        return super.createAndInsert(task);
    }

    @Override public void onTaskStart(int id) {
        super.onTaskStart(id);
        remitHelper.onTaskStart(id);
    }

    @Override public void onSyncToFilesystemSuccess(@NonNull BreakpointInfo info, int blockIndex,
                                                    long increaseLength) {
        if (remitHelper.isNotFreeToDatabase(info.getId())) {
            onCache.onSyncToFilesystemSuccess(info, blockIndex, increaseLength);
            return;
        }

        super.onSyncToFilesystemSuccess(info, blockIndex, increaseLength);
    }

    @Override public boolean update(@NonNull BreakpointInfo info) throws IOException {
        if (remitHelper.isNotFreeToDatabase(info.getId())) return onCache.update(info);

        return super.update(info);
    }

    @Override
    public void onTaskEnd(int id, @NonNull EndCause cause, @Nullable Exception exception) {
        onCache.onTaskEnd(id, cause, exception);

        if (cause == EndCause.COMPLETED) {
            remitHelper.discard(id);
            helper.removeInfo(id);
        } else {
            remitHelper.endAndEnsureToDB(id);
        }
    }

    @Override public void bunchTaskCanceled(int[] ids) {
        onCache.bunchTaskCanceled(ids);
        if (ids.length > 0) {
            final SQLiteDatabase database = helper.getWritableDatabase();
            database.beginTransaction();
            try {
                for (int id : ids) {
                    remitHelper.endAndEnsureToDB(id);
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
    }

    @Override public void remove(int id) {
        onCache.remove(id);

        remitHelper.discard(id);
        helper.removeInfo(id);
    }

    @Override public void syncCacheToDB(int id) throws IOException {
        Util.d(TAG, "syncCacheToDB " + id);
        helper.removeInfo(id);

        final BreakpointInfo info = onCache.get(id);
        if (info == null || info.getFilename() == null || info.getTotalOffset() <= 0) return;

        helper.insert(info);
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
