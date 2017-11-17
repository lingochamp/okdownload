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

package com.liulishuo.okdownload;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class UnifiedListenerManagerTest {
    private UnifiedListenerManager listenerManager;
    @Mock private DownloadListener listener;

    @Before
    public void setup() {
        initMocks(this);
        listenerManager = new UnifiedListenerManager();
    }

    @Test
    public void detachListener() {
        final ArrayList<DownloadListener> list = new ArrayList<>();
        list.add(listener);
        listenerManager.realListenerMap.put(1, list);

        final DownloadTask task = mockTask(1);

        listenerManager.detachListener(task, listener);

        assertThat(listenerManager.realListenerMap.size()).isZero();
    }

    @Test
    public void attachListener() {
        final DownloadTask task = mockTask(2);
        listenerManager.attachListener(task, listener);

        assertThat(listenerManager.realListenerMap.size()).isEqualTo(1);
        assertThat(listenerManager.realListenerMap.get(2)).containsExactly(listener);
    }

    private DownloadTask mockTask(int id) {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getId()).thenReturn(id);
        return task;
    }
}