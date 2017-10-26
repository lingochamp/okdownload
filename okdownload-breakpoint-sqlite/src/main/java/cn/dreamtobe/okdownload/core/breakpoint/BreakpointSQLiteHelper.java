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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.dreamtobe.okdownload.core.exception.SQLiteException;

import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.BLOCK_INDEX;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.CHUNKED;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.CONTENT_LENGTH;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.CURRENT_OFFSET;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.ETAG;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.FILENAME;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.HOST_ID;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.ID;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.PARENT_PATH;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.START_OFFSET;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.TASK_ONLY_PARENT_PATH;
import static cn.dreamtobe.okdownload.core.breakpoint.BreakpointSQLiteKey.URL;

public class BreakpointSQLiteHelper extends SQLiteOpenHelper {

    private static final String NAME = "okdownload-breakpoint.db";
    private static final int VERSION = 1;

    private static final String BREAKPOINT_TABLE_NAME = "breakpoint";
    private static final String BLOCK_TABLE_NAME = "block";

    public BreakpointSQLiteHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setWriteAheadLoggingEnabled(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            db.enableWriteAheadLogging();
        }
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + BREAKPOINT_TABLE_NAME + "( "
                + ID + " INTEGER PRIMARY KEY, "
                + URL + " VARCHAR NOT NULL, "
                + ETAG + " VARCHAR, "
                + PARENT_PATH + " VARCHAR NOT NULL, "
                + FILENAME + " VARCHAR, "
                + TASK_ONLY_PARENT_PATH + " TINYINT(1) DEFAULT 0, "
                + CHUNKED + " TINYINT(1) DEFAULT 0)"
        );

        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + BLOCK_TABLE_NAME + "( "
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HOST_ID + " INTEGER, "
                + BLOCK_INDEX + " INTEGER, "
                + START_OFFSET + " INTEGER, "
                + CONTENT_LENGTH + " INTEGER, "
                + CURRENT_OFFSET + " INTEGER)"
        );
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public SparseArray<BreakpointInfo> loadToCache() {
        Cursor breakpointCursor = null;
        Cursor blockCursor = null;
        final SQLiteDatabase db = getWritableDatabase();

        final List<BreakpointInfoRow> breakpointInfoRows = new ArrayList<>();
        final List<BlockInfoRow> blockInfoRows = new ArrayList<>();

        try {
            breakpointCursor = db.rawQuery("SELECT * FROM " + BREAKPOINT_TABLE_NAME, null);
            while (breakpointCursor.moveToNext()) {
                breakpointInfoRows.add(new BreakpointInfoRow(breakpointCursor));
            }
            blockCursor = db.rawQuery("SELECT * FROM " + BLOCK_TABLE_NAME, null);
            while (blockCursor.moveToNext()) {
                blockInfoRows.add(new BlockInfoRow(blockCursor));
            }
        } finally {
            if (breakpointCursor != null) breakpointCursor.close();
            if (blockCursor != null) blockCursor.close();
        }

        final SparseArray<BreakpointInfo> breakpointInfoMap = new SparseArray<>();

        for (BreakpointInfoRow infoRow : breakpointInfoRows) {
            final BreakpointInfo info = infoRow.toInfo();
            final Iterator<BlockInfoRow> blockIt = blockInfoRows.iterator();
            while (blockIt.hasNext()) {
                final BlockInfoRow blockInfoRow = blockIt.next();
                if (blockInfoRow.getBreakpointId() == info.id) {
                    info.addBlock(blockInfoRow.toInfo());
                    blockIt.remove();
                }
            }
            breakpointInfoMap.put(info.id, info);
        }

        return breakpointInfoMap;
    }

    public void insert(@NonNull BreakpointInfo info) throws IOException {
        final int blockCount = info.getBlockCount();
        final SQLiteDatabase db = getWritableDatabase();
        for (int i = 0; i < blockCount; i++) {
            final BlockInfo blockInfo = info.getBlock(i);
            if (db.insert(BLOCK_TABLE_NAME, null, toValues(info.id, i, blockInfo)) == -1) {
                throw new SQLiteException("insert block " + blockInfo + " failed!");
            }
        }

        final long result = db.insert(BREAKPOINT_TABLE_NAME, null,
                toValues(info));
        if (result == -1) throw new SQLiteException("insert info " + info + " failed!");

    }

    public void updateBlockIncrease(@NonNull BreakpointInfo info, int blockIndex,
                                    long newCurrentOffset) {
        final ContentValues values = new ContentValues();
        values.put(CURRENT_OFFSET, newCurrentOffset);
        getWritableDatabase().update(BLOCK_TABLE_NAME, values,
                HOST_ID + " = ? AND " + BLOCK_INDEX + " = ?",
                new String[]{Integer.toString(info.id), Integer.toString(blockIndex)});
    }

    public void updateInfo(@NonNull BreakpointInfo info) throws IOException {
        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            final Cursor cursor = getWritableDatabase().rawQuery(
                    "SELECT " + ID + " FROM " + BREAKPOINT_TABLE_NAME + " WHERE " + ID + " ="
                            + info.id + " LIMIT 1",
                    null);
            if (!cursor.moveToNext()) return; // not exist

            // update
            removeInfo(info.id);
            insert(info);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void removeInfo(int id) {
        getWritableDatabase().delete(BREAKPOINT_TABLE_NAME, ID + " = ?",
                new String[]{String.valueOf(id)});
        removeBlock(id);
    }

    public void removeBlock(int breakpointId) {
        getWritableDatabase().delete(BLOCK_TABLE_NAME, HOST_ID + " = ?",
                new String[]{String.valueOf(breakpointId)});
    }

    private static ContentValues toValues(@NonNull BreakpointInfo info) {
        final ContentValues values = new ContentValues();
        values.put(ID, info.id);
        values.put(URL, info.getUrl());
        values.put(ETAG, info.getEtag());
        values.put(PARENT_PATH, info.parentPath);
        values.put(FILENAME, info.getFilename());
        values.put(TASK_ONLY_PARENT_PATH, info.isTaskOnlyProvidedParentPath ? 1 : 0);
        values.put(CHUNKED, info.isChunked() ? 1 : 0);

        return values;
    }

    private static ContentValues toValues(int breakpointId, int index, @NonNull BlockInfo info) {
        final ContentValues values = new ContentValues();
        values.put(HOST_ID, breakpointId);
        values.put(BLOCK_INDEX, index);
        values.put(START_OFFSET, info.getStartOffset());
        values.put(CONTENT_LENGTH, info.getContentLength());
        values.put(CURRENT_OFFSET, info.getCurrentOffset());
        return values;
    }
}
