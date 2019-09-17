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

import com.liulishuo.okdownload.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FileDownloadQueueSetTest {

    @Mock
    private
    FileDownloadListener listener;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        TestUtils.mockOkDownload();
    }

//    @Test
//    public void start() {
//        final FileDownloadQueueSet queueSet = spy(new FileDownloadQueueSet(listener));
//        final BaseDownloadTask.FinishListener mockFinishListener =
//                mock(BaseDownloadTask.FinishListener.class);
//        final BaseDownloadTask task1 = FileDownloader.getImpl().create("url1");
//        final BaseDownloadTask task2 = FileDownloader.getImpl().create("url2");
//        doNothing().when(queueSet).realStart();
//
//        String directory = "directory";
//        int autoRetryTimes = 5;
//        boolean syncCallback = true;
//        boolean foreDownload = true;
//        int callbackProgressTimes = 300;
//        int minCallbackInterval = 20;
//        String tag = "tag";
//        boolean wifiRequired = true;
//        queueSet.setDirectory(directory)
//                .setAutoRetryTimes(autoRetryTimes)
//                .setSyncCallback(syncCallback)
//                .setForceReDownload(foreDownload)
//                .setCallbackProgressTimes(callbackProgressTimes)
//                .setCallbackProgressMinInterval(minCallbackInterval)
//                .setTag(tag)
//                .addTaskFinishListener(mockFinishListener)
//                .setWifiRequired(wifiRequired)
//                .downloadTogether(task1, task2)
//                .start();
//
//        for (BaseDownloadTask task : queueSet.tasks) {
//            assertThat(task instanceof DownloadTaskAdapter).isEqualTo(true);
//            assertThat(task.getPath()).isEqualTo(directory);
//            assertThat(task.isPathAsDirectory()).isEqualTo(true);
//            assertThat(task.getAutoRetryTimes()).isEqualTo(autoRetryTimes);
//            assertThat(task.isForceReDownload()).isEqualTo(foreDownload);
//            assertThat(task.getCallbackProgressTimes()).isEqualTo(callbackProgressTimes);
//            assertThat(task.getCallbackProgressMinInterval()).isEqualTo(minCallbackInterval);
//            assertThat(task.getTag()).isEqualTo(tag);
//            assertThat(((DownloadTaskAdapter) task).getFinishListeners()).hasSize(1);
//            assertThat(((DownloadTaskAdapter) task).getFinishListeners().get(0))
//                    .isEqualTo(mockFinishListener);
//            assertThat(task.isWifiRequired()).isEqualTo(wifiRequired);
//            assertThat(((DownloadTaskAdapter) task).getDownloadTask() != null).isEqualTo(true);
//            assertThat(((DownloadTaskAdapter) task).getCompatListener() != null).isEqualTo(true);
//        }
//
//        final BaseDownloadTask task3 = FileDownloader.getImpl().create("url3");
//        queueSet.ignoreEachTaskInternalProgress()
//                .downloadTogether(task3)
//                .start();
//
//        assertThat(task3.getCallbackProgressTimes()).isEqualTo(-1);
//
//        final BaseDownloadTask task4 = FileDownloader.getImpl().create("url4");
//        queueSet.disableCallbackProgressTimes()
//                .downloadTogether(task4)
//                .start();
//
//        assertThat(task4.getCallbackProgressTimes()).isEqualTo(0);
//    }

    @Test
    public void downloadTogether() {
        final BaseDownloadTask task1 = FileDownloader.getImpl().create("url1");
        final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(listener);

        queueSet.downloadTogether(task1);

        assertThat(queueSet.isSerial).isEqualTo(false);
        assertThat(queueSet.tasks).hasSize(1);
        assertThat(queueSet.tasks[0]).isEqualTo(task1);

        queueSet.tasks[0] = null;
        assertThat(queueSet.tasks[0]).isEqualTo(null);

        final List<BaseDownloadTask> tasks = new ArrayList<>();
        tasks.add(task1);
        queueSet.downloadTogether(tasks);

        assertThat(queueSet.isSerial).isEqualTo(false);
        assertThat(queueSet.tasks).hasSize(1);
        assertThat(queueSet.tasks[0]).isEqualTo(task1);
    }

    @Test
    public void downloadSequentially() {
        final BaseDownloadTask task1 = FileDownloader.getImpl().create("url1");
        final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(listener);

        queueSet.downloadSequentially(task1);

        assertThat(queueSet.isSerial).isEqualTo(true);
        assertThat(queueSet.tasks).hasSize(1);
        assertThat(queueSet.tasks[0]).isEqualTo(task1);

        queueSet.tasks[0] = null;
        assertThat(queueSet.tasks[0]).isEqualTo(null);

        final List<BaseDownloadTask> tasks = new ArrayList<>();
        tasks.add(task1);
        queueSet.downloadSequentially(tasks);

        assertThat(queueSet.isSerial).isEqualTo(true);
        assertThat(queueSet.tasks).hasSize(1);
        assertThat(queueSet.tasks[0]).isEqualTo(task1);
    }
}
