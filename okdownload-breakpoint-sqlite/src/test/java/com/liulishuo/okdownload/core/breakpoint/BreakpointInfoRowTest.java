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

import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.CHUNKED;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.ETAG;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.FILENAME;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.ID;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.PARENT_PATH;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.TASK_ONLY_PARENT_PATH;
import static com.liulishuo.okdownload.core.breakpoint.BreakpointSQLiteKey.URL;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class BreakpointInfoRowTest {

    private BreakpointInfoRow breakpointInfoRow;

    @Before
    public void setup() {
        final Cursor cursor = mock(Cursor.class);
        doReturn(0).when(cursor).getColumnIndex(ID);
        doReturn(10).when(cursor).getInt(0);

        doReturn(1).when(cursor).getColumnIndex(URL);
        doReturn("url").when(cursor).getString(1);

        doReturn(2).when(cursor).getColumnIndex(ETAG);
        doReturn("etag").when(cursor).getString(2);

        doReturn(3).when(cursor).getColumnIndex(PARENT_PATH);
        doReturn("p-path").when(cursor).getString(3);

        doReturn(4).when(cursor).getColumnIndex(FILENAME);
        doReturn(null).when(cursor).getString(4);

        doReturn(5).when(cursor).getColumnIndex(TASK_ONLY_PARENT_PATH);
        doReturn(1).when(cursor).getInt(5);

        doReturn(6).when(cursor).getColumnIndex(CHUNKED);
        doReturn(0).when(cursor).getInt(6);

        breakpointInfoRow = new BreakpointInfoRow(cursor);
    }

    @Test
    public void construction() {
        assertThat(breakpointInfoRow.getId()).isEqualTo(10);
        assertThat(breakpointInfoRow.getUrl()).isEqualTo("url");
        assertThat(breakpointInfoRow.getEtag()).isEqualTo("etag");
        assertThat(breakpointInfoRow.getParentPath()).isEqualTo("p-path");
        assertThat(breakpointInfoRow.getFilename()).isNull();
        assertThat(breakpointInfoRow.isTaskOnlyProvidedParentPath()).isTrue();
        assertThat(breakpointInfoRow.isChunked()).isFalse();
    }

    @Test
    public void toInfo() {
        final BreakpointInfo info = breakpointInfoRow.toInfo();
        assertThat(info.id).isEqualTo(10);
        assertThat(info.getUrl()).isEqualTo("url");
        assertThat(info.getEtag()).isEqualTo("etag");
        assertThat(info.parentPath).isEqualTo("p-path");
        assertThat(info.getFilename()).isNull();
        assertThat(info.isTaskOnlyProvidedParentPath).isTrue();
        assertThat(info.isChunked()).isFalse();
    }
}