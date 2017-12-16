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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import com.liulishuo.okdownload.DownloadTask;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class BreakpointStoreOnSqliteTest {
    private BreakpointStoreOnSQLite store;

    private BreakpointSQLiteHelper helper;
    private BreakpointStoreOnCache onCache;

    @Before
    public void setup() {
        helper = spy(new BreakpointSQLiteHelper(application));
        onCache = spy(new BreakpointStoreOnCache());
        store = spy(new BreakpointStoreOnSQLite(helper, onCache));
    }

    @After
    public void tearDown() {
        store.close();
    }

    @Test
    public void get_createAndInsert_onSyncToFilesystemSuccess_update() throws IOException {
        final int id1 = store.findOrCreateId(mock(DownloadTask.class));
        final int id2 = store.findOrCreateId(mock(DownloadTask.class));
        assertThat(id1).isNotEqualTo(id2);
        verify(onCache, times(2)).findOrCreateId(any(DownloadTask.class));

        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(id2);
        when(task.getUrl()).thenReturn("url");
        when(task.getParentPath()).thenReturn("p-path");
        doReturn("filename").when(task).getFilename();

        store.createAndInsert(task);
        final BreakpointInfo info2 = onCache.get(id2);
        assertThat(info2).isNotNull();
        verify(helper).insert(info2);

        info2.addBlock(new BlockInfo(0, 20, 5));
        store.onSyncToFilesystemSuccess(info2, 0, 10);
        verify(onCache).onSyncToFilesystemSuccess(info2, 0, 10);
        verify(helper).updateBlockIncrease(info2, 0, 15);

        info2.setEtag("new-etag");
        store.update(info2);
        verify(onCache).update(info2);
        verify(helper).updateInfo(info2);
    }

    @Test
    public void completeDownload() {
        final int id = store.findOrCreateId(mock(DownloadTask.class));
        store.completeDownload(id);
        verify(onCache).completeDownload(id);
        verify(helper).removeInfo(id);
    }


    @Test
    public void discard() {
        final int id = store.findOrCreateId(mock(DownloadTask.class));

        store.discard(id);
        verify(onCache).discard(id);
        verify(helper).removeInfo(id);
    }
}