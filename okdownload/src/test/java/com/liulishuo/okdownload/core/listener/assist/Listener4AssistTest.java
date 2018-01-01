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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class Listener4AssistTest {

    @Mock private BreakpointInfo info1;
    @Mock private BreakpointInfo info2;
    @Mock private DownloadTask task1;
    @Mock private DownloadTask task2;
    @Mock private Listener4Assist.Listener4Callback callback;

    private Listener4Assist assist;
    private Listener4Assist.AssistExtend assistExtend;

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

        assist = new Listener4Assist();
        assist.setCallback(callback);

        assistExtend = spy(new Listener4Assist.AssistExtend() {
            @Override
            public Listener4Assist.Listener4Model inspectAddModel(
                    Listener4Assist.Listener4Model origin) {
                return origin;
            }

            @Override
            public boolean dispatchInfoReady(DownloadTask task, @NonNull BreakpointInfo info,
                                             boolean fromBreakpoint,
                                             @NonNull Listener4Assist.Listener4Model model) {
                return false;
            }

            @Override
            public boolean dispatchFetchProgress(@NonNull DownloadTask task, int blockIndex,
                                                 long increaseBytes,
                                                 @NonNull Listener4Assist.Listener4Model model) {
                return false;
            }

            @Override public boolean dispatchBlockEnd(DownloadTask task, int blockIndex,
                                                      Listener4Assist.Listener4Model model) {
                return false;
            }

            @Override
            public boolean dispatchTaskEnd(DownloadTask task, EndCause cause,
                                           @Nullable Exception realCause,
                                           @NonNull Listener4Assist.Listener4Model model) {
                return false;
            }
        });
        assist.setAssistExtend(assistExtend);
    }

    @Test
    public void infoReady_dispatch() {
        // dispatch
        when(assistExtend.dispatchInfoReady(eq(task1), eq(info1), eq(true),
                any(Listener4Assist.Listener4Model.class)))
                .thenReturn(true);
        assist.infoReady(task1, info1, true);
        verify(assist.callback, never()).infoReady(eq(task1), eq(info1), eq(true),
                any(Listener4Assist.Listener4Model.class));
    }

    @Test
    public void infoReady_Inspect() {
        // newModel
        final Listener4Assist.Listener4Model newModel = new Listener4Assist.Listener4Model(info1, 0,
                new SparseArray<Long>());
        when(assistExtend.inspectAddModel(any(Listener4Assist.Listener4Model.class)))
                .thenReturn(newModel);
        assist.infoReady(task1, info1, true);
        assertThat(assist.findModel(info1.getId())).isEqualTo(newModel);
    }

    @Test
    public void initData_findModel_oneModel_remove() {
        assist.infoReady(task1, info1, true);
        assertThat(assist.getSingleTaskModel().info).isEqualTo(info1);
        assertThat(assist.findModel(info1.getId()).info).isEqualTo(info1);
        assertThat(assist.getSingleTaskModel().getBlockCurrentOffsetMap().size()).isEqualTo(3);
        verify(callback).infoReady(eq(task1), eq(info1), eq(true),
                any(Listener4Assist.Listener4Model.class));

        assist.infoReady(task2, info2, false);
        assertThat(assist.getSingleTaskModel().info).isEqualTo(info1);
        assertThat(assist.findModel(info2.getId()).info).isEqualTo(info2);
        assertThat(assist.findModel(info2.getId()).getBlockCurrentOffsetMap().size()).isEqualTo(2);
        verify(callback).infoReady(eq(task2), eq(info2), eq(false),
                any(Listener4Assist.Listener4Model.class));

        assist.taskEnd(task2, EndCause.COMPLETE, null);
        assertThat(assist.findModel(info2.getId())).isNull();
        verify(callback).taskEnd(eq(task2), eq(EndCause.COMPLETE), nullable(Exception.class),
                any(Listener4Assist.Listener4Model.class));

        assist.taskEnd(task1, EndCause.COMPLETE, null);
        assertThat(assist.getSingleTaskModel()).isNull();
        assertThat(assist.findModel(info1.getId())).isNull();
        verify(callback).taskEnd(eq(task1), eq(EndCause.COMPLETE), nullable(Exception.class),
                any(Listener4Assist.Listener4Model.class));
    }

    @Test
    public void fetchProgress_dispatch() {
        mockSingleModelWith0Block();

        final int blockIndex = 0;
        final long increaseBytes = 2L;
        when(assistExtend.dispatchFetchProgress(eq(task1), eq(blockIndex), eq(increaseBytes),
                any(Listener4Assist.Listener4Model.class)))
                .thenReturn(true);
        assist.fetchProgress(task1, blockIndex, increaseBytes);
        verify(callback, never()).progress(eq(task1), anyLong());
        verify(callback, never()).progressBlock(eq(task1), eq(0), anyLong());
    }

    @Test
    public void fetchProgress() {
        assist.infoReady(task1, info1, false);
        assist.fetchProgress(task1, 0, 3);

        final Listener4Assist.Listener4Model model = assist.getSingleTaskModel();
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
    public void fetchEnd_dispatch() {
        mockSingleModelWith0Block();

        final int blockIndex = 0;

        when(assistExtend.dispatchBlockEnd(eq(task1), eq(blockIndex), any(
                Listener4Assist.Listener4Model.class))).thenReturn(true);
        assist.fetchEnd(task1, blockIndex);
        verify(callback, never()).blockEnd(eq(task1), eq(blockIndex), any(BlockInfo.class));
    }

    @Test
    public void fetchEnd() {
        assist.infoReady(task1, info1, false);
        assist.fetchEnd(task1, 1);
        final BlockInfo blockInfo1 = info1.getBlock(1);
        verify(callback).blockEnd(eq(task1), eq(1), eq(blockInfo1));
    }

    @Test
    public void taskEnd_dispatch() {
        mockSingleModelWith0Block();

        when(assistExtend.dispatchTaskEnd(eq(task1), any(EndCause.class), nullable(Exception.class),
                any(Listener4Assist.Listener4Model.class)))
                .thenReturn(true);
        assist.taskEnd(task1, EndCause.COMPLETE, null);
        verify(callback, never())
                .taskEnd(eq(task1), eq(EndCause.COMPLETE), nullable(Exception.class),
                        any(Listener4Assist.Listener4Model.class));
    }

    private void mockSingleModelWith0Block() {
        final SparseArray<Long> blockOffsetMap = new SparseArray<>();
        blockOffsetMap.put(0, 1L);
        assist.singleTaskModel = new Listener4Assist.Listener4Model(info1, 0, blockOffsetMap);
    }
}