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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class Listener4AssistTest {

    @Mock private BreakpointInfo info;
    @Mock private DownloadTask task;
    @Mock private Listener4Assist.Listener4Callback callback;
    @Mock private Listener4Assist.AssistExtend assistExtend;
    @Mock private ListenerModelHandler<Listener4Assist.Listener4Model> handler;
    private Listener4Assist.Listener4Model model;

    private Listener4Assist assist;

    @Before
    public void setup() {
        initMocks(this);

        when(task.getId()).thenReturn(1);
        when(task.getInfo()).thenReturn(info);
        when(info.getId()).thenReturn(1);
        when(info.getBlockCount()).thenReturn(3);
        for (int i = 0; i < 3; i++) {
            when(info.getBlock(i)).thenReturn(mock(BlockInfo.class));
        }
        model = new Listener4Assist.Listener4Model(1);
        model.onInfoValid(info);

        assist = new Listener4Assist<>(handler);
        assist.setAssistExtend(assistExtend);
        assist.setCallback(callback);
    }

    @Test
    public void setCallback() {
        assist.setCallback(callback);
        assertThat(assist.callback).isEqualTo(callback);
    }

    @Test
    public void setAssistExtend() {
        assist.setAssistExtend(assistExtend);
        assertThat(assist.getAssistExtend()).isEqualTo(assistExtend);
    }

    @Test
    public void infoReady_dispatch() {
        // dispatch
        when(assistExtend.dispatchInfoReady(eq(task), eq(info), eq(true),
                any(Listener4Assist.Listener4Model.class)))
                .thenReturn(true);
        assist.infoReady(task, info, true);
        verify(assist.callback, never()).infoReady(eq(task), eq(info), eq(true),
                any(Listener4Assist.Listener4Model.class));
        verify(handler).addAndGetModel(eq(task), eq(info));
    }

    @Test
    public void infoReady_noDispatch() {
        when(handler.addAndGetModel(task, info)).thenReturn(model);
        when(assistExtend.dispatchInfoReady(eq(task), eq(info), eq(true),
                any(Listener4Assist.Listener4Model.class)))
                .thenReturn(false);
        assist.infoReady(task, info, true);
        verify(assist.callback).infoReady(eq(task), eq(info), eq(true),
                eq(model));
        verify(handler).addAndGetModel(eq(task), eq(info));
    }

    @Test
    public void fetchProgress_noModel() {
        when(handler.getOrRecoverModel(task, info)).thenReturn(null);
        assist.fetchProgress(task, 0, 0);
        verify(callback, never()).progress(eq(task), eq(0L));
        verify(assistExtend, never()).dispatchFetchProgress(eq(task), eq(0), eq(0L),
                any(Listener4Assist.Listener4Model.class));
        verify(handler).getOrRecoverModel(eq(task), eq(info));
    }

    @Test
    public void fetchProgress_dispatch() {
        when(handler.getOrRecoverModel(task, info)).thenReturn(model);

        final int blockIndex = 0;
        final long increaseBytes = 2L;
        when(assistExtend.dispatchFetchProgress(eq(task), eq(blockIndex), eq(increaseBytes),
                any(Listener4Assist.Listener4Model.class)))
                .thenReturn(true);
        assist.fetchProgress(task, blockIndex, increaseBytes);
        verify(callback, never()).progress(eq(task), anyLong());
        verify(callback, never()).progressBlock(eq(task), eq(0), anyLong());
        verify(handler).getOrRecoverModel(eq(task), eq(info));
    }

    @Test
    public void fetchProgress() {
        when(handler.getOrRecoverModel(task, info)).thenReturn(model);

        assist.fetchProgress(task, 0, 3);

        assertThat(model.getCurrentOffset()).isEqualTo(3);
        assertThat(model.getBlockCurrentOffsetMap().get(0)).isEqualTo(3);
        assertThat(model.getBlockCurrentOffsetMap().get(1)).isEqualTo(0);
        assertThat(model.getBlockCurrentOffsetMap().get(2)).isEqualTo(0);
        verify(callback).progressBlock(eq(task), eq(0), eq(3L));
        verify(callback).progress(eq(task), eq(3L));
        verify(handler).getOrRecoverModel(eq(task), eq(info));

        assist.fetchProgress(task, 1, 2);
        // not effect to single-task-model
        assertThat(model.getCurrentOffset()).isEqualTo(5);
        assertThat(model.getBlockCurrentOffsetMap().get(1)).isEqualTo(2);
        verify(callback).progressBlock(eq(task), eq(1), eq(2L));
        verify(callback).progress(eq(task), eq(5L));
    }

    @Test
    public void fetchEnd_dispatch() {
        when(handler.getOrRecoverModel(task, info)).thenReturn(model);

        final int blockIndex = 0;

        when(assistExtend.dispatchBlockEnd(eq(task), eq(blockIndex), any(
                Listener4Assist.Listener4Model.class))).thenReturn(true);
        assist.fetchEnd(task, blockIndex);
        verify(callback, never()).blockEnd(eq(task), eq(blockIndex), any(BlockInfo.class));
        verify(handler).getOrRecoverModel(eq(task), eq(info));
    }

    @Test
    public void fetchEnd() {
        when(handler.getOrRecoverModel(task, info)).thenReturn(model);
        assist.fetchEnd(task, 1);
        final BlockInfo blockInfo1 = info.getBlock(1);
        verify(callback).blockEnd(eq(task), eq(1), eq(blockInfo1));
        verify(handler).getOrRecoverModel(eq(task), eq(info));
    }

    @Test
    public void taskEnd_dispatch() {
        when(handler.removeOrCreate(task, info)).thenReturn(model);

        when(assistExtend.dispatchTaskEnd(eq(task), any(EndCause.class), nullable(Exception.class),
                any(Listener4Assist.Listener4Model.class)))
                .thenReturn(true);
        assist.taskEnd(task, EndCause.COMPLETED, null);
        verify(callback, never())
                .taskEnd(eq(task), eq(EndCause.COMPLETED), nullable(Exception.class),
                        any(Listener4Assist.Listener4Model.class));
    }

    @Test
    public void taskEnd_noModel() {
        when(handler.removeOrCreate(task, info)).thenReturn(model);
        assist.taskEnd(task, EndCause.COMPLETED, null);
        verify(callback).taskEnd(eq(task), eq(EndCause.COMPLETED), nullable(Exception.class),
                any(Listener4Assist.Listener4Model.class));
    }

    @Test
    public void isAlwaysRecoverAssistModel() {
        when(handler.isAlwaysRecoverAssistModel()).thenReturn(true);
        assertThat(assist.isAlwaysRecoverAssistModel()).isTrue();
        when(handler.isAlwaysRecoverAssistModel()).thenReturn(false);
        assertThat(assist.isAlwaysRecoverAssistModel()).isFalse();
    }

    @Test
    public void setAlwaysRecoverAssistModel() {
        assist.setAlwaysRecoverAssistModel(true);
        verify(handler).setAlwaysRecoverAssistModel(eq(true));
        assist.setAlwaysRecoverAssistModel(false);
        verify(handler).setAlwaysRecoverAssistModel(eq(false));
    }

    @Test
    public void setAlwaysRecoverAssistModelIfNotSet() {
        assist.setAlwaysRecoverAssistModelIfNotSet(true);
        verify(handler).setAlwaysRecoverAssistModelIfNotSet(eq(true));
        assist.setAlwaysRecoverAssistModelIfNotSet(false);
        verify(handler).setAlwaysRecoverAssistModelIfNotSet(eq(false));
    }

    @Test
    public void getBlockCurrentOffsetMap() {
        model.blockCurrentOffsetMap = mock(SparseArray.class);
        assertThat(model.getBlockCurrentOffsetMap()).isEqualTo(model.blockCurrentOffsetMap);
    }

    @Test
    public void getBlockCurrentOffset() {
        model.blockCurrentOffsetMap.put(1, 2L);
        assertThat(model.getBlockCurrentOffset(1)).isEqualTo(2L);
    }

    @Test
    public void cloneBlockCurrentOffsetMap() {
        final SparseArray map = mock(SparseArray.class);
        final SparseArray cloned = mock(SparseArray.class);
        model.blockCurrentOffsetMap = map;
        when(map.clone()).thenReturn(cloned);

        assertThat(model.cloneBlockCurrentOffsetMap()).isEqualTo(cloned);
    }
}