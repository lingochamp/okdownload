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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessFileStrategyTest {
    private ProcessFileStrategy strategy;

    @Mock private DownloadTask task;
    private final File existFile = new File("./exist-path");

    @Before
    public void setup() throws IOException {
        initMocks(this);
        strategy = new ProcessFileStrategy();

        existFile.createNewFile();
    }

    @After
    public void tearDown() {
        existFile.delete();
    }

    @Test
    public void discardProcess() throws IOException {
        when(task.getFile()).thenReturn(new File("mock path"));

        strategy.discardProcess(task);
        // nothing need to test.
    }
}