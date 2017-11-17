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

package com.liulishuo.okdownload.core.breakpoint;

import android.database.Cursor;

import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.CURRENT_OFFSET;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.HOST_ID;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.START_OFFSET;

public class BlockInfoRow {
    private final int breakpointId;

    private final long startOffset;
    private final long contentLength;
    private final long currentOffset;

    public BlockInfoRow(Cursor cursor) {
        this.breakpointId = cursor.getInt(cursor.getColumnIndex(HOST_ID));
        this.startOffset = cursor.getInt(cursor.getColumnIndex(START_OFFSET));
        this.contentLength = cursor.getInt(cursor.getColumnIndex(CONTENT_LENGTH));
        this.currentOffset = cursor.getInt(cursor.getColumnIndex(CURRENT_OFFSET));
    }

    public int getBreakpointId() {
        return breakpointId;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getCurrentOffset() {
        return currentOffset;
    }

    public BlockInfo toInfo() {
        return new BlockInfo(startOffset, contentLength, currentOffset);
    }
}
