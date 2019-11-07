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

package com.liulishuo.filedownloader;

import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.liulishuo.okdownload.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DownloadTaskAdapterTest {

    @Before
    public void setup() throws IOException {
        TestUtils.mockOkDownload();
    }

    @Test
    public void build() {
        final FileDownloadListener mockListener = mock(FileDownloadListener.class);
        final DownloadTaskAdapter taskAdapter =
                (DownloadTaskAdapter) FileDownloader.getImpl().create("url")
                        .setPath("/path/p")
                        .setListener(mockListener)
                        .setCallbackProgressTimes(200)
                        .setCallbackProgressMinInterval(50)
                        .setTag("tag")
                        .setTag(1, "t")
                        .setForceReDownload(true)
                        .setAutoRetryTimes(4)
                        .setSyncCallback(true)
                        .setWifiRequired(true);
        taskAdapter.insureAssembleDownloadTask();

        assertThat(taskAdapter.getPath()).isEqualTo("/path/p");
        assertThat(taskAdapter.getUrl()).isEqualTo("url");
        assertThat(taskAdapter.getListener()).isEqualTo(mockListener);
        assertThat(taskAdapter.getCallbackProgressTimes()).isEqualTo(200);
        assertThat(taskAdapter.getCallbackProgressMinInterval()).isEqualTo(50);
        assertThat(taskAdapter.getTag()).isEqualTo("tag");
        assertThat(taskAdapter.getTag(1)).isEqualTo("t");
        assertThat(taskAdapter.isForceReDownload()).isTrue();
        assertThat(taskAdapter.getAutoRetryTimes()).isEqualTo(4);
        assertThat(taskAdapter.isSyncCallback()).isTrue();
        assertThat(taskAdapter.isWifiRequired()).isTrue();
        assertThat(taskAdapter.isPathAsDirectory()).isFalse();

        final DownloadTaskAdapter anotherTask =
                (DownloadTaskAdapter) FileDownloader.getImpl().create("url")
                        .setListener(mockListener)
                        .setPath("pp", true);
        anotherTask.insureAssembleDownloadTask();

        assertThat(anotherTask.isPathAsDirectory()).isTrue();
    }

    @Test
    public void build_default() {
        final FileDownloadListener mockListener = mock(FileDownloadListener.class);
        final DownloadTaskAdapter taskAdapter = (DownloadTaskAdapter) FileDownloader.getImpl()
                .create("url").setListener(mockListener);
        FileDownloadUtils.setDefaultSaveRootPath("/sdcard");
        taskAdapter.insureAssembleDownloadTask();

        assertThat(taskAdapter.getPath())
                .isEqualTo(FileDownloadUtils.getDefaultSaveFilePath("url"));
        assertThat(taskAdapter.getCallbackProgressMinInterval())
                .isEqualTo(DownloadTaskAdapter.DEFAULT_CALLBACK_PROGRESS_MIN_INTERVAL_MILLIS);
        assertThat(taskAdapter.isSyncCallback()).isFalse();
        assertThat(taskAdapter.isWifiRequired()).isFalse();
        assertThat(taskAdapter.isForceReDownload()).isFalse();
    }

    @Test
    public void addHeader() {
        final DownloadTaskAdapter taskAdapter = FileDownloader.getImpl().create("url");
        taskAdapter.addHeader("a:b");
        taskAdapter.addHeader("c", "d");

        assertThat(taskAdapter.builder.headerMap.containsKey("a")).isTrue();
        assertThat(taskAdapter.builder.headerMap.containsKey("c")).isTrue();
        assertThat(taskAdapter.builder.headerMap.get("a")).isEqualTo("b");
        assertThat(taskAdapter.builder.headerMap.get("c")).isEqualTo("d");
    }

    @Test
    public void marAdded2List() {
        final DownloadTaskAdapter taskAdapter = FileDownloader.getImpl().create("url");
        taskAdapter.markAdded2List();
        assertThat(taskAdapter.isMarkedAdded2List()).isTrue();
    }

    @Test
    public void setAttachKeyDefault() {
        final FileDownloadListener mockListener = mock(FileDownloadListener.class);
        final DownloadTaskAdapter taskAdapter = FileDownloader.getImpl()
                .create("url");

        taskAdapter.setAttachKeyDefault();
        assertThat(taskAdapter.getAttachKey()).isEqualTo(taskAdapter.hashCode());

        taskAdapter.setListener(mockListener);
        taskAdapter.setAttachKeyDefault();

        assertThat(taskAdapter.getAttachKey()).isEqualTo(mockListener.hashCode());
    }

    @Test
    public void assembleDownloadTask() {
        final FileDownloadListener mockListener = mock(FileDownloadListener.class);
        final DownloadTaskAdapter taskAdapter = (DownloadTaskAdapter) FileDownloader.getImpl()
                .create("url").setPath("path");
        taskAdapter.setListener(mockListener);

        taskAdapter.insureAssembleDownloadTask();

        assertThat(taskAdapter.getDownloadTask()).isNotNull();
        assertThat(taskAdapter.getCompatListener()).isNotNull();
        assertThat(taskAdapter.getProgressAssist()).isNotNull();
        assertThat(taskAdapter.getRetryAssist()).isNull();
        assertThat(taskAdapter.statusAssist.getDownloadTask())
                .isEqualTo(taskAdapter.getDownloadTask());
        assertThat(FileDownloadUtils.findDownloadTaskAdapter(taskAdapter.getDownloadTask()))
                .isEqualTo(taskAdapter);

        taskAdapter.downloadTask = null;
        taskAdapter.setAutoRetryTimes(3);

        taskAdapter.insureAssembleDownloadTask();

        assertThat(taskAdapter.getDownloadTask()).isNotNull();
        assertThat(taskAdapter.getCompatListener()).isNotNull();
        assertThat(taskAdapter.getProgressAssist()).isNotNull();
        assertThat(taskAdapter.getRetryAssist()).isNotNull();
        assertThat(taskAdapter.statusAssist.getDownloadTask())
                .isEqualTo(taskAdapter.getDownloadTask());
        assertThat(FileDownloadUtils.findDownloadTaskAdapter(taskAdapter.getDownloadTask()))
                .isEqualTo(taskAdapter);
    }
}
