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
import com.liulishuo.okdownload.SpeedCalculator;
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
public class Listener4SpeedAssistExtendTest {

    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;
    @Mock private Listener4SpeedAssistExtend.Listener4SpeedCallback callback;

    private Listener4SpeedAssistExtend.Listener4SpeedModel model;
    private Listener4SpeedAssistExtend assistExtend;

    @Before
    public void setup() {
        initMocks(this);

        when(task.getId()).thenReturn(1);

        when(info.getId()).thenReturn(1);


        final SparseArray<Long> blockOffsetMap = new SparseArray<>();
        final SparseArray<SpeedCalculator> blockSpeeds = new SparseArray<>();


        for (int i = 0; i < 3; i++) {
            when(info.getBlock(i)).thenReturn(mock(BlockInfo.class));
            blockOffsetMap.put(i, (long) i);
            blockSpeeds.put(i, mock(SpeedCalculator.class));
        }

        final Listener4Assist.Listener4Model originModel = new Listener4Assist.Listener4Model(info,
                0, blockOffsetMap);
        model = new Listener4SpeedAssistExtend.Listener4SpeedModel(originModel,
                mock(SpeedCalculator.class), blockSpeeds);

        assistExtend = new Listener4SpeedAssistExtend();
        assistExtend.setCallback(callback);
    }

    @Test
    public void dispatchFetchProgress() {
        final boolean result = assistExtend.dispatchFetchProgress(task, 0, 1L, model);
        assertThat(result).isTrue();

        verify(model.blockSpeeds.get(0)).downloading(eq(1L));
        verify(model.blockSpeeds.get(1), never()).downloading(eq(1L));
        verify(model.blockSpeeds.get(2), never()).downloading(eq(1L));
        verify(model.taskSpeed).downloading(eq(1L));
        verify(callback).progressBlock(eq(task), eq(0), eq(model.blockCurrentOffsetMap.get(0)),
                eq(model.getBlockSpeed(0)));
    }

    @Test
    public void dispatchBlockEnd() {
        final boolean result = assistExtend.dispatchBlockEnd(task, 0, model);
        assertThat(result).isTrue();

        verify(model.blockSpeeds.get(0)).endTask();
        verify(callback)
                .blockEnd(task, eq(0), info.getBlock(0), model.getBlockSpeed(0));
    }

    @Test
    public void dispatchTaskEnd() {
        final boolean result = assistExtend.dispatchTaskEnd(task, EndCause.COMPLETE, null, model);
        assertThat(result).isTrue();

        verify(model.taskSpeed).endTask();
        verify(callback).taskEnd(eq(task), eq(EndCause.COMPLETE), nullable(Exception.class),
                eq(model.taskSpeed));
    }
}