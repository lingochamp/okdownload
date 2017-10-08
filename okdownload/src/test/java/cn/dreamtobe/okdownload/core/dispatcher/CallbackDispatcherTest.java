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

package cn.dreamtobe.okdownload.core.dispatcher;

import org.junit.Before;
import org.junit.Test;

import cn.dreamtobe.okdownload.DownloadListener;
import cn.dreamtobe.okdownload.task.DownloadTask;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CallbackDispatcherTest {

    private CallbackDispatcher dispatcher;

    @Before
    public void setup() {
        dispatcher = new CallbackDispatcher();
    }

    @Test
    public void dispatch() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        final DownloadListener mockListener = mock(DownloadListener.class);
        when(mockTask.getListener()).thenReturn(mockListener);

        final DownloadListener result = dispatcher.dispatch(mockTask);

        assertThat(result).isEqualTo(mockListener);
    }
}