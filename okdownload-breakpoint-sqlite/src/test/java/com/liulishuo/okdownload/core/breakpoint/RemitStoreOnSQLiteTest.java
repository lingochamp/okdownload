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

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class RemitStoreOnSQLiteTest {

    private RemitStoreOnSQLite store;

    @Mock private BreakpointSQLiteHelper helper;
    @Mock private RemitSyncToDBHelper remitHelper;

    private BreakpointStoreOnCache onCache;

    @Before
    public void setup() {
        initMocks(this);

        onCache = spy(new BreakpointStoreOnCache());
        store = new RemitStoreOnSQLite(helper, onCache, remitHelper);
    }

    @Test
    public void createAndInsert_notFreeToDatabase() throws IOException {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(1);
        when(remitHelper.isNotFreeToDatabase(1)).thenReturn(true);
        store.createAndInsert(task);

        verify(onCache).createAndInsert(eq(task));
        verify(store.helper, never()).insert(any(BreakpointInfo.class));
    }


    @Test
    public void createAndInsert_freeToDatabase() throws IOException {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(1);
        when(remitHelper.isNotFreeToDatabase(1)).thenReturn(false);
        store.createAndInsert(task);

        verify(onCache).createAndInsert(eq(task));
        verify(store.helper).insert(any(BreakpointInfo.class));
    }

    @Test
    public void onTaskStart() {
        store.onTaskStart(1);
        verify(remitHelper).onTaskStart(eq(1));
    }

    @Test
    public void onSyncToFilesystemSuccess_notFreeToDatabase() {
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getId()).thenReturn(1);
        when(info.getBlock(0)).thenReturn(mock(BlockInfo.class));
        when(remitHelper.isNotFreeToDatabase(1)).thenReturn(true);

        doNothing().when(onCache).onSyncToFilesystemSuccess(info, 0, 10);

        store.onSyncToFilesystemSuccess(info, 0, 10);
        verify(onCache).onSyncToFilesystemSuccess(eq(info), eq(0), eq(10L));
        verify(store.helper, never()).updateBlockIncrease(eq(info), eq(0), anyLong());
    }

    @Test
    public void onSyncToFilesystemSuccess_freeToDatabase() {
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getId()).thenReturn(1);
        when(info.getBlock(0)).thenReturn(mock(BlockInfo.class));
        when(remitHelper.isNotFreeToDatabase(1)).thenReturn(false);

        doNothing().when(onCache).onSyncToFilesystemSuccess(info, 0, 10);

        store.onSyncToFilesystemSuccess(info, 0, 10);
        verify(onCache).onSyncToFilesystemSuccess(eq(info), eq(0), eq(10L));
        verify(store.helper).updateBlockIncrease(eq(info), eq(0), anyLong());
    }

    @Test
    public void update_notFreeToDatabase() throws IOException {
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getId()).thenReturn(1);
        when(remitHelper.isNotFreeToDatabase(1)).thenReturn(true);

        store.update(info);

        verify(onCache).update(eq(info));
        verify(store.helper, never()).updateInfo(eq(info));
    }

    @Test
    public void update_freeToDatabase() throws IOException {
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getId()).thenReturn(1);
        when(remitHelper.isNotFreeToDatabase(1)).thenReturn(false);

        store.update(info);

        verify(onCache).update(eq(info));
        verify(store.helper).updateInfo(eq(info));
    }

    @Test
    public void onTaskEnd() {
        store.onTaskEnd(1, EndCause.COMPLETED, null);

        verify(onCache).onTaskEnd(eq(1), eq(EndCause.COMPLETED), nullable(Exception.class));
        verify(remitHelper).discard(eq(1));
        verify(helper).removeInfo(eq(1));
    }

    @Test
    public void onTaskEnd_notCompleted() {
        store.onTaskEnd(1, EndCause.CANCELED, null);

        verify(onCache).onTaskEnd(eq(1), eq(EndCause.CANCELED), nullable(Exception.class));
        verify(remitHelper).endAndEnsureToDB(eq(1));
    }

    @Test
    public void discard() {
        store.discard(1);

        verify(onCache).discard(eq(1));
        verify(remitHelper).discard(eq(1));
        verify(helper).removeInfo(eq(1));
    }

    @Test
    public void syncCacheToDB() throws IOException {
        Util.setLogger(mock(Util.Logger.class));

        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(onCache.get(1)).thenReturn(info);

        store.syncCacheToDB(1);
        verify(store.helper).removeInfo(eq(1));
        verify(store.helper, never()).insert(eq(info));

        when(info.getFilename()).thenReturn("filename");
        store.syncCacheToDB(1);
        verify(store.helper, never()).insert(eq(info));

        when(info.getTotalOffset()).thenReturn(1L);
        store.syncCacheToDB(1);
        verify(store.helper).insert(eq(info));
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void setRemitToDBDelayMillis() throws IOException {
        OkDownload.setSingletonInstance(mock(OkDownload.class));

        doReturn(mock(BreakpointStoreOnCache.class)).when(OkDownload.with()).breakpointStore();
        thrown.expect(IllegalStateException.class);
        RemitStoreOnSQLite.setRemitToDBDelayMillis(1);

        doReturn(store).when(OkDownload.with()).breakpointStore();

        RemitStoreOnSQLite.setRemitToDBDelayMillis(-1);
        assertThat(remitHelper.delayMillis).isEqualTo(0);

        RemitStoreOnSQLite.setRemitToDBDelayMillis(1);
        assertThat(remitHelper.delayMillis).isEqualTo(1);
    }

    @Test
    public void bunchTaskCanceled() {
        final int[] ids = new int[2];
        ids[0] = 1;
        ids[1] = 2;

        final SQLiteDatabase db = mock(SQLiteDatabase.class);
        when(helper.getWritableDatabase()).thenReturn(db);

        store.bunchTaskCanceled(ids);

        verify(remitHelper).endAndEnsureToDB(eq(1));
        verify(remitHelper).endAndEnsureToDB(eq(2));
        verify(db).beginTransaction();
        verify(db).setTransactionSuccessful();
        verify(db).endTransaction();


    }
}