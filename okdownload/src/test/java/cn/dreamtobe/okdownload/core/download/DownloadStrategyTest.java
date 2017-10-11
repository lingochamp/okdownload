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

package cn.dreamtobe.okdownload.core.download;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DownloadStrategyTest {

    private DownloadStrategy strategy;
    @Mock private DownloadTask task;
    @Mock private DownloadConnection.Connected connected;

    @Before
    public void setup() {
        initMocks(this);

        strategy = new DownloadStrategy();
    }

    @Test
    public void determineFilename_tmpFilenameValid() throws IOException {
        final String validResponseFilename = "file name";
        String result = strategy.determineFilename(validResponseFilename, task, connected);
        assertThat(result).isEqualTo(validResponseFilename);

        when(task.getUrl()).thenReturn("https://jacksgong.com/okdownload.3_1.apk?abc&ddd");
        result = strategy.determineFilename(null, task, connected);
        assertThat(result).isEqualTo("okdownload.3_1.apk");


        when(task.getUrl()).thenReturn("https://jacksgong.com/dreamtobe.cn");
        result = strategy.determineFilename(null, task, connected);
        assertThat(result).isEqualTo("dreamtobe.cn");

        when(task.getUrl()).thenReturn("https://jacksgong.com/?abc");
        result = strategy.determineFilename(null, task, connected);
        assertThat(result).isNotEmpty();
    }
}