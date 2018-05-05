/*
 * Copyright (c) 2018 LingoChamp Inc.
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
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class ListenerModelHandlerTest {

    private ListenerModelHandler<ListenerModelHandler.ListenerModel> handler;
    @Mock private ListenerModelHandler.ListenerModel model;
    @Mock private ListenerModelHandler.ModelCreator<ListenerModelHandler.ListenerModel> creator;
    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;

    @Before
    public void setup() {
        initMocks(this);
        handler = spy(new ListenerModelHandler<>(creator));

        when(creator.create(anyInt())).thenReturn(model);
    }

    @Test
    public void isAlwaysRecover() {
        // default value is false
        assertThat(handler.isAlwaysRecoverAssistModel()).isFalse();

        handler.setAlwaysRecoverAssistModel(true);
        assertThat(handler.isAlwaysRecoverAssistModel()).isTrue();

        handler.setAlwaysRecoverAssistModel(false);
        assertThat(handler.isAlwaysRecoverAssistModel()).isFalse();

        // already set
        handler.setAlwaysRecoverAssistModelIfNotSet(true);
        assertThat(handler.isAlwaysRecoverAssistModel()).isFalse();
    }

    @Test
    public void addAndGetModel_provideInfo() {
        final ListenerModelHandler.ListenerModel model = handler.addAndGetModel(task, info);
        assertThat(model).isEqualTo(this.model);

        verify(creator).create(anyInt());
        assertThat(handler.singleTaskModel).isEqualTo(model);
        assertThat(handler.modelList.size()).isZero();

        verify(model).onInfoValid(eq(info));

        handler.addAndGetModel(task, info);
        assertThat(handler.modelList.size()).isOne();
    }

    @Test
    public void addAndGetModel_noInfo() {
        final ListenerModelHandler.ListenerModel model = handler.addAndGetModel(task, null);
        assertThat(model).isEqualTo(this.model);

        verify(creator).create(anyInt());
        assertThat(handler.singleTaskModel).isEqualTo(model);
        assertThat(handler.modelList.size()).isZero();

        verify(model, never()).onInfoValid(eq(info));
    }

    @Test
    public void getOrRecoverModel_notAlwaysRecoverAssistModel() {
        doReturn(false).when(handler).isAlwaysRecoverAssistModel();

        handler.singleTaskModel = model;
        assertThat(handler.getOrRecoverModel(task, info)).isEqualTo(model);
        verify(handler, never()).addAndGetModel(eq(task), eq(info));

        handler.singleTaskModel = null;
        handler.modelList.put(task.getId(), model);
        assertThat(handler.getOrRecoverModel(task, info)).isEqualTo(model);
        verify(handler, never()).addAndGetModel(eq(task), eq(info));

        handler.modelList.clear();
        doReturn(model).when(handler).addAndGetModel(task, info);
        assertThat(handler.getOrRecoverModel(task, info)).isNull();
        verify(handler, never()).addAndGetModel(eq(task), eq(info));
    }

    @Test
    public void getOrRecoverModel_alwaysRecoverAssistModel() {
        doReturn(true).when(handler).isAlwaysRecoverAssistModel();
        doReturn(model).when(handler).addAndGetModel(task, info);

        handler.singleTaskModel = model;
        assertThat(handler.getOrRecoverModel(task, info)).isEqualTo(model);
        verify(handler, never()).addAndGetModel(eq(task), eq(info));

        handler.singleTaskModel = null;
        handler.modelList.put(task.getId(), model);
        assertThat(handler.getOrRecoverModel(task, info)).isEqualTo(model);
        verify(handler, never()).addAndGetModel(eq(task), eq(info));

        // create one
        handler.modelList.clear();
        doReturn(model).when(handler).addAndGetModel(task, info);
        assertThat(handler.getOrRecoverModel(task, info)).isEqualTo(model);
        verify(handler).addAndGetModel(eq(task), eq(info));
    }

    @Test
    public void removeOrCreate_withInfo() {
        handler.singleTaskModel = model;
        assertThat(handler.removeOrCreate(task, info)).isEqualTo(model);
        verify(creator, never()).create(anyInt());
        verify(model, never()).onInfoValid(eq(info));

        handler.singleTaskModel = null;
        handler.modelList.put(task.getId(), model);
        assertThat(handler.removeOrCreate(task, info)).isEqualTo(model);
        verify(creator, never()).create(anyInt());
        verify(model, never()).onInfoValid(eq(info));

        handler.modelList.clear();
        assertThat(handler.removeOrCreate(task, info)).isEqualTo(model);
        verify(creator).create(anyInt());
        verify(model).onInfoValid(eq(info));
    }

    @Test
    public void removeOrCreate_noInfo() {
        handler.modelList.clear();
        assertThat(handler.removeOrCreate(task, null)).isEqualTo(model);
        verify(creator).create(anyInt());
        verify(model, never()).onInfoValid(any(BreakpointInfo.class));
    }
}