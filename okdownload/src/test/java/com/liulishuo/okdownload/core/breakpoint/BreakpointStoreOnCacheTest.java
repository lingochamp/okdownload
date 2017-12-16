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

import android.util.SparseArray;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import com.liulishuo.okdownload.DownloadTask;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class) // for SparseArray
@Config(manifest = NONE)
public class BreakpointStoreOnCacheTest {

    private BreakpointStoreOnCache storeOnCache;
    private final int insertedId = 6;

    @Before
    public void setup() {
        storeOnCache = new BreakpointStoreOnCache();
    }

    @Test
    public void createAndInsert() {
        final DownloadTask task = mock(DownloadTask.class);

        when(task.getId()).thenReturn(insertedId);
        when(task.getParentPath()).thenReturn("/p-path/");
        when(task.getFilename()).thenReturn("filename");
        when(task.getUrl()).thenReturn("url");
        storeOnCache.createAndInsert(task);

        final BreakpointInfo info = storeOnCache.get(insertedId);
        assertThat(info).isNotNull();
        assertThat(info.id).isEqualTo(insertedId);
    }

    @Test
    public void onSyncToFilesystemSuccess() {
        createAndInsert();

        final BreakpointInfo info = storeOnCache.get(insertedId);
        final BlockInfo blockInfo = spy(new BlockInfo(0, 0, 0));
        info.addBlock(blockInfo);

        storeOnCache.onSyncToFilesystemSuccess(info, 0, 1);

        assertThat(blockInfo.getCurrentOffset()).isEqualTo(1);
    }

    @Test
    public void update() {
        createAndInsert();

        BreakpointInfo newOne = new BreakpointInfo(insertedId, "", "", "newOne");

        // replace
        storeOnCache.update(newOne);

        BreakpointInfo onStoreInfo = storeOnCache.get(insertedId);
        assertThat(onStoreInfo.getFilename()).isEqualTo("newOne");

        final BlockInfo blockInfo = mock(BlockInfo.class);
        onStoreInfo.addBlock(blockInfo);

        // Not replace.
        storeOnCache.update(onStoreInfo);
        assertThat(storeOnCache.get(insertedId)).isEqualTo(onStoreInfo);

    }

    @Test
    public void unStoredTasks() {
        final SparseArray<DownloadTask> unStoredTasks = new SparseArray<>();
        final SparseArray<BreakpointInfo> storedInfos = new SparseArray<>();
        storeOnCache = new BreakpointStoreOnCache(storedInfos,
                unStoredTasks,
                new ArrayList<Integer>());

        DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(insertedId);
        unStoredTasks.put(task.getId(), task);
        doReturn(true).when(task).compareIgnoreId(task);

        assertThat(storeOnCache.findOrCreateId(task)).isEqualTo(insertedId);
        storeOnCache.createAndInsert(task);
        assertThat(unStoredTasks.size()).isZero();
        assertThat(storedInfos.valueAt(0).getId()).isEqualTo(insertedId);
    }

    @Test
    public void findAnotherInfoFromCompare() {
        final SparseArray<DownloadTask> unStoredTasks = new SparseArray<>();
        final SparseArray<BreakpointInfo> storedInfos = new SparseArray<>();
        storeOnCache = new BreakpointStoreOnCache(storedInfos,
                unStoredTasks,
                new ArrayList<Integer>());

        final BreakpointInfo info1 = mock(BreakpointInfo.class);
        final BreakpointInfo info2 = mock(BreakpointInfo.class);
        final DownloadTask task = mock(DownloadTask.class);

        storedInfos.put(insertedId, info1);

        doReturn(true).when(info1).isSameFrom(task);
        doReturn(false).when(info2).isSameFrom(task);

        BreakpointInfo result = storeOnCache.findAnotherInfoFromCompare(task, info1);
        assertThat(result).isNull();
        result = storeOnCache.findAnotherInfoFromCompare(task, info2);
        assertThat(result).isEqualToComparingFieldByField(info1);
    }

    @Test
    public void allocateId() {
        final List<Integer> sortedOccupiedIds = new ArrayList<>();
        storeOnCache = new BreakpointStoreOnCache(new SparseArray<BreakpointInfo>(),
                new SparseArray<DownloadTask>(),
                sortedOccupiedIds);

        assertThat(storeOnCache.allocateId()).isEqualTo(1);
        //when
        sortedOccupiedIds.add(3);
        sortedOccupiedIds.add(5);
        sortedOccupiedIds.add(6);
        sortedOccupiedIds.add(7);


        assertThat(storeOnCache.allocateId()).isEqualTo(2);
        assertThat(sortedOccupiedIds).containsExactly(1, 2, 3, 5, 6, 7);
        assertThat(sortedOccupiedIds.get(1)).isEqualTo(2);
        assertThat(storeOnCache.allocateId()).isEqualTo(4);
        assertThat(storeOnCache.allocateId()).isEqualTo(8);

        assertThat(sortedOccupiedIds).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        storeOnCache.discard(6);
        assertThat(sortedOccupiedIds).containsExactly(1, 2, 3, 4, 5, 7, 8);
        assertThat(storeOnCache.allocateId()).isEqualTo(6);

        assertThat(sortedOccupiedIds).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        storeOnCache.completeDownload(1);
        assertThat(sortedOccupiedIds).containsExactly(2, 3, 4, 5, 6, 7, 8);
        assertThat(storeOnCache.allocateId()).isEqualTo(1);
    }
}