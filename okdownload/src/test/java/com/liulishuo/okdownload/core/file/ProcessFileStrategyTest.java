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

package com.liulishuo.okdownload.core.file;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessFileStrategyTest {
    private ProcessFileStrategy strategy;

    @Mock private DownloadTask task;

    @Before
    public void setup() throws IOException {
        initMocks(this);
        strategy = new ProcessFileStrategy();
    }

    @Test
    public void discardProcess() throws IOException {
        final File existFile = new File("./exist-path");
        existFile.createNewFile();

        when(task.getFile()).thenReturn(existFile);

        strategy.discardProcess(task);

        assertThat(existFile.exists()).isFalse();
    }

    @Test
    public void isPreAllocateLength() throws IOException {
        mockOkDownload();

        // no pre-allocate set on task.
        when(task.getSetPreAllocateLength()).thenReturn(null);

        final DownloadOutputStream.Factory factory = OkDownload.with().outputStreamFactory();
        when(factory.supportSeek()).thenReturn(false);

        assertThat(strategy.isPreAllocateLength(task)).isFalse();
        when(factory.supportSeek()).thenReturn(true);

        assertThat(strategy.isPreAllocateLength(task)).isTrue();

        // pre-allocate set on task.
        when(task.getSetPreAllocateLength()).thenReturn(false);
        assertThat(strategy.isPreAllocateLength(task)).isFalse();

        when(task.getSetPreAllocateLength()).thenReturn(true);
        assertThat(strategy.isPreAllocateLength(task)).isTrue();

        // pre-allocate set on task is true but can't support seek.
        when(factory.supportSeek()).thenReturn(false);
        assertThat(strategy.isPreAllocateLength(task)).isFalse();
    }
}