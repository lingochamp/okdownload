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

import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.TestUtils;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FileDownloadListTest {

    @Mock
    private FileDownloadListener listener;

    @Before
    public void setupClass() throws IOException {
        initMocks(this);

        TestUtils.mockOkDownload();
    }

    @Before
    public void setup() {
        initMocks(this);

        FileDownloadList.setSingleton(new FileDownloadList());
        DownloadTaskAdapter task1 = (DownloadTaskAdapter) spy(FileDownloader.getImpl()
                .create("url1").setPath("path1").setListener(listener));
        doReturn(1).when(task1).getId();
        DownloadTaskAdapter task2 = (DownloadTaskAdapter) spy(FileDownloader.getImpl()
                .create("url2").setPath("path2").setListener(listener));
        doReturn(2).when(task2).getId();
        DownloadTaskAdapter task3 = (DownloadTaskAdapter) spy(FileDownloader.getImpl()
                .create("url3").setPath("path3").setListener(listener));
        doReturn(3).when(task3).getId();

        FileDownloadList.getImpl().list.add(task1);
        FileDownloadList.getImpl().list.add(task2);
        FileDownloadList.getImpl().list.add(task3);
        task1.markAdded2List();
        task2.markAdded2List();
        task3.markAdded2List();
    }

    @Test
    public void get() {
        BaseDownloadTask.IRunningTask task = FileDownloadList.getImpl().get(0);
        assertNull(task);

        final DownloadTaskAdapter firstAddedTask = FileDownloadList.getImpl().list.get(0);
        firstAddedTask.insureAssembleDownloadTask();
        final DownloadDispatcher downloadDispatcher = OkDownload.with().downloadDispatcher();
        when(downloadDispatcher.isRunning(firstAddedTask.getDownloadTask())).thenReturn(false);
        task = FileDownloadList.getImpl().get(firstAddedTask.getId());
        assertNull(task);

        final DownloadTaskAdapter secondAddedTask = FileDownloadList.getImpl().list.get(1);
        when(downloadDispatcher.isRunning(secondAddedTask.getDownloadTask())).thenReturn(true);
        task = FileDownloadList.getImpl().get(secondAddedTask.getId());
        assertThat(task).isEqualTo(secondAddedTask);
    }

    @Test
    public void addQueueTask() {
        final DownloadTaskAdapter addedTask = FileDownloadList.getImpl().list.get(0);
        final int oldSize = FileDownloadList.getImpl().list.size();

        FileDownloadList.getImpl().addQueueTask(addedTask);

        assertThat(FileDownloadList.getImpl().list).hasSize(oldSize);

        final DownloadTaskAdapter newTask = spy(FileDownloader.getImpl().create("url"));
        doNothing().when(newTask).insureAssembleDownloadTask();

        FileDownloadList.getImpl().addQueueTask(newTask);

        assertThat(FileDownloadList.getImpl().list).hasSize(oldSize + 1);
        verify(newTask).markAdded2List();
        verify(newTask).insureAssembleDownloadTask();
    }

    @Test
    public void addIndependentTask() {
        final int oldSize = FileDownloadList.getImpl().list.size();
        final DownloadTaskAdapter mockIndependentTask = spy(FileDownloader.getImpl().create("url"));

        FileDownloadList.getImpl().addIndependentTask(mockIndependentTask);

        assertThat(FileDownloadList.getImpl().list).hasSize(oldSize + 1);
        verify(mockIndependentTask).setAttachKeyDefault();
        verify(mockIndependentTask).markAdded2List();

        FileDownloadList.getImpl().addIndependentTask(mockIndependentTask);

        assertThat(FileDownloadList.getImpl().list).hasSize(oldSize + 1);
    }

    @Test
    public void assembleTasksToStart() {
        List<DownloadTaskAdapter> tasks = FileDownloadList.getImpl().assembleTasksToStart(listener);
        assertThat(tasks).hasSize(FileDownloadList.getImpl().list.size());
        for (DownloadTaskAdapter task : tasks) {
            verify(task).setAttachKeyByQueue(listener.hashCode());
        }

        tasks = FileDownloadList.getImpl().assembleTasksToStart(mock(FileDownloadListener.class));
        assertThat(tasks).hasSize(0);

        for (DownloadTaskAdapter downloadTaskAdapter : FileDownloadList.getImpl().list) {
            when(downloadTaskAdapter.isAttached()).thenReturn(true);
        }
        tasks = FileDownloadList.getImpl().assembleTasksToStart(listener);
        assertThat(tasks).hasSize(0);
    }

    @Test
    public void getByFileDownloadListener() {
        final FileDownloadListener mockNewListener = mock(FileDownloadListener.class);
        final int oldSize = FileDownloadList.getImpl().list.size();
        List<DownloadTaskAdapter> result = FileDownloadList.getImpl()
                .getByFileDownloadListener(mockNewListener);

        assertThat(result).isEmpty();

        result = FileDownloadList.getImpl().getByFileDownloadListener(listener);
        assertThat(result).hasSize(oldSize);
    }

    @Test
    public void remove_willRunningTask() {
        assertThat(FileDownloadList.getImpl().remove(null, null)).isFalse();
        final DownloadTaskAdapter downloadTaskAdapter = FileDownloadList.getImpl().list.get(0);
        assertThat(FileDownloadList.getImpl().remove(downloadTaskAdapter)).isTrue();
    }

}
