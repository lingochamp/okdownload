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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.HashMap;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class BreakpointSQLiteHelperTest {
    private BreakpointSQLiteHelper helper;

    private BreakpointInfo insertedInfo1;
    private BreakpointInfo insertedInfo2;

    @Before
    public void setup() throws IOException {
        helper = new BreakpointSQLiteHelper(application);
        final BreakpointInfo info1 = new BreakpointInfo(1, "url1", "p-path1", null);
        info1.addBlock(new BlockInfo(0, 10));
        helper.insert(info1);
        insertedInfo1 = info1;

        final BreakpointInfo info2 = new BreakpointInfo(2, "url2", "p-path2", "filename2");
        info2.addBlock(new BlockInfo(0, 20));
        info2.addBlock(new BlockInfo(20, 20, 10));
        helper.insert(info2);
        insertedInfo2 = info2;
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void loadToCacheAndInsert() {
        final SparseArray<BreakpointInfo> infoSparseArray = helper.loadToCache();
        assertThat(infoSparseArray.size()).isEqualTo(2);

        final BreakpointInfo info1 = infoSparseArray.get(insertedInfo1.id);
        assertThat(info1.getBlockCount()).isEqualTo(1);
        assertThat(info1.getUrl()).isEqualTo("url1");
        assertThat(info1.parentPath).isEqualTo("p-path1");
        assertThat(info1.getFilename()).isNull();

        final BreakpointInfo info2 = infoSparseArray.get(insertedInfo2.id);
        assertThat(info2.getBlockCount()).isEqualTo(2);
        assertThat(info2.getUrl()).isEqualTo("url2");
        assertThat(info2.parentPath).isEqualTo("p-path2");
        assertThat(info2.getFilename()).isEqualTo("filename2");
    }

    @Test
    public void loadResponseFilenameToMap_updateFilename() {
        final String url1 = "url1";
        final String filename1 = "filename1";

        final String url2 = "url2";
        final String filename2 = "filename2";

        helper.updateFilename(url1, filename1);
        helper.updateFilename(url2, filename2);

        final HashMap<String, String> urlFilenameMap = helper.loadResponseFilenameToMap();
        assertThat(urlFilenameMap).containsEntry(url1, filename1);
        assertThat(urlFilenameMap).containsEntry(url2, filename2);
    }

    @Test
    public void updateBlockIncrease() {
        assertThat(insertedInfo2.getBlock(1).getCurrentOffset()).isEqualTo(10);
        helper.updateBlockIncrease(insertedInfo2, 1, 15);

        BreakpointInfo info2 = helper.loadToCache().get(insertedInfo2.id);
        assertThat(info2.getBlock(1).getCurrentOffset()).isEqualTo(15);
    }

    @Test
    public void updateInfo() throws IOException {
        BreakpointInfo info1 = helper.loadToCache().get(insertedInfo1.id);
        assertThat(info1.getEtag()).isNull();
        info1.setEtag("new-etag");
        helper.updateInfo(info1);

        info1 = helper.loadToCache().get(info1.id);
        assertThat(info1.getEtag()).isEqualTo("new-etag");

    }

    @Test
    public void removeInfo() {
        helper = spy(helper);
        helper.removeInfo(insertedInfo2.id);
        final SparseArray<BreakpointInfo> infoSparseArray = helper.loadToCache();
        assertThat(infoSparseArray.size()).isEqualTo(1);
        assertThat(infoSparseArray.get(infoSparseArray.keyAt(0)).id).isEqualTo(insertedInfo1.id);

        verify(helper).removeBlock(insertedInfo2.id);
    }

    @Test
    public void removeBlock() {
        assertThat(insertedInfo2.getBlockCount()).isEqualTo(2);
        helper.removeBlock(insertedInfo2.id);

        assertThat(helper.loadToCache().get(insertedInfo2.id).getBlockCount()).isZero();
    }

}