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

package com.liulishuo.filedownloader.util;

import com.liulishuo.filedownloader.DownloadTaskAdapter;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FileDownloadUtilsTest {

    @Before
    public void setup() throws IOException {
        TestUtils.mockOkDownload();
    }

    @Test
    public void findDownloadTaskAdapter() {
        DownloadTask downloadTask = mock(DownloadTask.class);
        DownloadTaskAdapter downloadTaskAdapter = FileDownloadUtils
                .findDownloadTaskAdapter(downloadTask);
        assertNull(downloadTaskAdapter);

        final String url = "url";
        final String path = "path";
        final DownloadTaskAdapter mockDownloadTaskAdapter =
                (DownloadTaskAdapter) FileDownloader.getImpl().create(url).setPath(path);
        mockDownloadTaskAdapter.assembleDownloadTask();
        downloadTaskAdapter = FileDownloadUtils
                .findDownloadTaskAdapter(mockDownloadTaskAdapter.getDownloadTask());
        assertThat(downloadTaskAdapter).isEqualTo(mockDownloadTaskAdapter);

        final DownloadTaskAdapter sameIdTask =
                (DownloadTaskAdapter) FileDownloader.getImpl().create(url).setPath(path);
        sameIdTask.assembleDownloadTask();
        assertThat(sameIdTask.getId()).isEqualTo(mockDownloadTaskAdapter.getId());
        downloadTaskAdapter = FileDownloadUtils
                .findDownloadTaskAdapter(sameIdTask.getDownloadTask());
        assertThat(downloadTaskAdapter).isEqualTo(sameIdTask);
        assertThat(sameIdTask).isNotEqualTo(mockDownloadTaskAdapter);

    }
}
