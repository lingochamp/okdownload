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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import cn.dreamtobe.okdownload.DownloadTask;

import static org.assertj.core.api.Java6Assertions.assertThat;
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

        BreakpointInfo onStoreInfo =  storeOnCache.get(insertedId);
        assertThat(onStoreInfo.getFilename()).isEqualTo("newOne");

        final BlockInfo blockInfo = mock(BlockInfo.class);
        onStoreInfo.addBlock(blockInfo);

        // Not replace.
        storeOnCache.update(onStoreInfo);
        assertThat(storeOnCache.get(insertedId)).isEqualTo(onStoreInfo);

    }
}