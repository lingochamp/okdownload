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

package com.liulishuo.okdownload.core.listener.assist;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadListener4AssistTest {

    @Mock private BreakpointInfo info1;
    @Mock private BreakpointInfo info2;
    @Mock private DownloadTask task1;
    @Mock private DownloadTask task2;
    @Mock private DownloadListener4Assist.Listener4Callback callback;

    private DownloadListener4Assist assist;

    @Before
    public void setup() {
        initMocks(this);

        when(task1.getId()).thenReturn(1);
        when(info1.getId()).thenReturn(1);
        when(info1.getBlockCount()).thenReturn(3);
        for (int i = 0; i < 3; i++) {
            when(info1.getBlock(i)).thenReturn(mock(BlockInfo.class));
        }

        when(task2.getId()).thenReturn(2);
        when(info2.getId()).thenReturn(2);
        when(info2.getBlockCount()).thenReturn(2);
        for (int i = 0; i < 2; i++) {
            when(info2.getBlock(i)).thenReturn(mock(BlockInfo.class));
        }

        assist = new DownloadListener4Assist();
        assist.setCallback(callback);
    }

    @Test
    public void initData_findModel_oneModel_remove() {
        assist.initData(task1, info1, true);
        assertThat(assist.getSingleTaskModel().info).isEqualTo(info1);
        assertThat(assist.findModel(info1.getId()).info).isEqualTo(info1);
        assertThat(assist.getSingleTaskModel().getBlockCurrentOffsetMap().size()).isEqualTo(3);
        verify(callback).infoReady(eq(task1), eq(info1), eq(true));


        assist.initData(task2, info2, false);
        assertThat(assist.getSingleTaskModel().info).isEqualTo(info1);
        assertThat(assist.findModel(info2.getId()).info).isEqualTo(info2);
        assertThat(assist.findModel(info2.getId()).getBlockCurrentOffsetMap().size()).isEqualTo(2);
        verify(callback).infoReady(eq(task2), eq(info2), eq(false));

        assist.taskEnd(info2.getId());
        assertThat(assist.findModel(info2.getId())).isNull();

        assist.taskEnd(info1.getId());
        assertThat(assist.getSingleTaskModel()).isNull();
        assertThat(assist.findModel(info1.getId())).isNull();
    }

    @Test
    public void fetchProgress() {
        assist.initData(task1, info1, false);
        assist.fetchProgress(task1, 0, 3);

        final DownloadListener4Assist.Listener4Model model = assist.getSingleTaskModel();
        assertThat(model.getCurrentOffset()).isEqualTo(3);
        assertThat(model.getBlockCurrentOffsetMap().get(0)).isEqualTo(3);
        assertThat(model.getBlockCurrentOffsetMap().get(1)).isEqualTo(0);
        assertThat(model.getBlockCurrentOffsetMap().get(2)).isEqualTo(0);
        verify(callback).progressBlock(eq(task1), eq(0), eq(3L));
        verify(callback).progress(eq(task1), eq(3L));

        assist.fetchProgress(task1, 1, 2);
        // not effect to single-task-model
        assertThat(model.getCurrentOffset()).isEqualTo(5);
        assertThat(model.getBlockCurrentOffsetMap().get(1)).isEqualTo(2);
        verify(callback).progressBlock(eq(task1), eq(1), eq(2L));
        verify(callback).progress(eq(task1), eq(5L));
    }

    @Test
    public void fetchEnd() {
        assist.initData(task1, info1, false);
        assist.fetchEnd(task1, 1);
        final BlockInfo blockInfo1 = info1.getBlock(1);
        verify(callback).blockEnd(eq(task1), eq(1), eq(blockInfo1));
    }
}