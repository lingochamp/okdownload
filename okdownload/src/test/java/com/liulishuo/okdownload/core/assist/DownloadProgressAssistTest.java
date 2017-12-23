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

package com.liulishuo.okdownload.core.assist;

import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadProgressAssistTest {

    @Mock private BreakpointInfo info1;
    @Mock private BreakpointInfo info2;

    private DownloadProgressAssist assist;

    @Before
    public void setup() {
        initMocks(this);

        when(info1.getId()).thenReturn(1);
        when(info1.getBlockCount()).thenReturn(3);
        for (int i = 0; i < 3; i++) {
            when(info1.getBlock(i)).thenReturn(mock(BlockInfo.class));
        }

        when(info2.getId()).thenReturn(2);
        when(info2.getBlockCount()).thenReturn(2);
        for (int i = 0; i < 2; i++) {
            when(info2.getBlock(i)).thenReturn(mock(BlockInfo.class));
        }

        assist = new DownloadProgressAssist();
    }

    @Test
    public void add_findModel_oneModel_remove() {
        assist.add(info1);
        assertThat(assist.getOneModel().info).isEqualTo(info1);
        assertThat(assist.findModel(info1.getId()).info).isEqualTo(info1);
        assertThat(assist.getOneModel().getBlockCurrentOffsetMap().size()).isEqualTo(3);

        assist.add(info2);
        assertThat(assist.getOneModel().info).isEqualTo(info1);
        assertThat(assist.findModel(info2.getId()).info).isEqualTo(info2);
        assertThat(assist.findModel(info2.getId()).getBlockCurrentOffsetMap().size()).isEqualTo(2);

        assist.remove(info2.getId());
        assertThat(assist.findModel(info2.getId())).isNull();

        assist.remove(info1.getId());
        assertThat(assist.getOneModel()).isNull();
        assertThat(assist.findModel(info1.getId())).isNull();
    }

    @Test
    public void fetchProgress() {
        assist.add(info1);
        assist.fetchProgress(info1.getId(), 0, 3);

        final DownloadProgressAssist.ProgressModel model = assist.getOneModel();
        assertThat(model.getCurrentOffset()).isEqualTo(3);
        assertThat(model.getBlockCurrentOffsetMap().get(0)).isEqualTo(3);
        assertThat(model.getBlockCurrentOffsetMap().get(1)).isEqualTo(0);
        assertThat(model.getBlockCurrentOffsetMap().get(2)).isEqualTo(0);

        assist.fetchProgress(info1.getId(), 1, 2);
        assertThat(model.getCurrentOffset()).isEqualTo(5);
        assertThat(model.getBlockCurrentOffsetMap().get(1)).isEqualTo(2);
    }
}