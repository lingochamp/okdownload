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

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;

import org.junit.Before;
import org.junit.Test;
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
    public void init() {
        onCache.storedInfos.put(1, mock(BreakpointInfo.class));
        onCache.storedInfos.put(3, mock(BreakpointInfo.class));
        onCache.storedInfos.put(5, mock(BreakpointInfo.class));
        onCache.storedInfos.put(7, mock(BreakpointInfo.class));

        store.init();

        assertThat(store.saveOnDBIdList).containsExactly(1, 3, 5, 7);
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
    public void onTaskEnd_completedAndNotOnDatabase() {
        store.onTaskEnd(1, EndCause.COMPLETED, null);

        verify(onCache).onTaskEnd(eq(1), eq(EndCause.COMPLETED), nullable(Exception.class));
        verify(remitHelper).discardFlyingSyncOrEnsureSyncFinish(eq(1));
        verify(helper, never()).removeInfo(eq(1));
        verify(remitHelper).onTaskEnd(eq(1));
    }

    @Test
    public void onTaskEnd_completedAndAlreadyOnDatabase() {
        store.saveOnDBIdList.add(1);

        store.onTaskEnd(1, EndCause.COMPLETED, null);

        verify(onCache).onTaskEnd(eq(1), eq(EndCause.COMPLETED), nullable(Exception.class));
        verify(remitHelper).discardFlyingSyncOrEnsureSyncFinish(eq(1));
        verify(helper).removeInfo(eq(1));
        verify(remitHelper).onTaskEnd(eq(1));

        assertThat(store.saveOnDBIdList).isEmpty();
    }

    @Test
    public void onTaskEnd_notCompleted() {
        store.onTaskEnd(1, EndCause.CANCELED, null);

        verify(remitHelper).ensureCacheToDB(eq(1));
        verify(onCache).onTaskEnd(eq(1), eq(EndCause.CANCELED), nullable(Exception.class));
        verify(remitHelper).onTaskEnd(eq(1));
    }

    @Test
    public void discard_notOnDatabase() {
        store.discard(1);

        verify(onCache).discard(eq(1));
        verify(remitHelper).discardFlyingSyncOrEnsureSyncFinish(eq(1));
        verify(helper, never()).removeInfo(eq(1));
        verify(remitHelper).onTaskEnd(eq(1));
    }

    @Test
    public void discard_alreadyOnDatabase() {
        store.saveOnDBIdList.add(1);
        store.discard(1);

        verify(onCache).discard(eq(1));
        verify(remitHelper).discardFlyingSyncOrEnsureSyncFinish(eq(1));
        verify(helper).removeInfo(eq(1));
        verify(remitHelper).onTaskEnd(eq(1));

        assertThat(store.saveOnDBIdList).isEmpty();
    }

    @Test
    public void syncCacheToDB() throws IOException {
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(onCache.get(1)).thenReturn(info);

        store.syncCacheToDB(1);
        verify(store.helper).insert(eq(info));
        assertThat(store.saveOnDBIdList).containsExactly(1);
    }

    @Test
    public void isInfoNotOnDatabase() {
        assertThat(store.isInfoNotOnDatabase(1)).isTrue();

        store.saveOnDBIdList.add(1);
        assertThat(store.isInfoNotOnDatabase(1)).isFalse();
    }
}