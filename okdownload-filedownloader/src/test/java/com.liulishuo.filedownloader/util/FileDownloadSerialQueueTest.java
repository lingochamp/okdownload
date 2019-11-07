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

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.CompatListenerAdapter;
import com.liulishuo.filedownloader.DownloadTaskAdapter;
import com.liulishuo.filedownloader.FileDownloadList;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadSerialQueue;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.UnifiedListenerManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FileDownloadSerialQueueTest {

    private DownloadSerialQueue serialQueue;
    private UnifiedListenerManager listenerManager;
    private DownloadListener hostListener;
    private FileDownloadSerialQueue fileDownloadSerialQueue;
    private FileDownloadList fileDownloadList;

    @Before
    public void setup() {
        hostListener = mock(DownloadListener.class);
        fileDownloadList = mock(FileDownloadList.class);
        serialQueue = spy(new DownloadSerialQueue());
        listenerManager = spy(new UnifiedListenerManager());
        FileDownloadList.setSingleton(fileDownloadList);

        doReturn(hostListener).when(listenerManager).getHostListener();

        fileDownloadSerialQueue = new FileDownloadSerialQueue(serialQueue, listenerManager);
    }

    @Test
    public void constructor() {
        assertThat(fileDownloadSerialQueue.listenerManager).isEqualTo(listenerManager);
        assertThat(fileDownloadSerialQueue.serialQueue).isEqualTo(serialQueue);

        verify(serialQueue).setListener(hostListener);
    }

    @Test
    public void enqueue() {
        final DownloadTaskAdapter mockBaseTask = spy(FileDownloader.getImpl().create("url"));
        final DownloadTask mockDownloadTask = mock(DownloadTask.class);
        final CompatListenerAdapter mockCompatListener = mock(CompatListenerAdapter.class);
        final int taskId = 1;
        doReturn(taskId).when(mockBaseTask).getId();
        doReturn(mockDownloadTask).when(mockBaseTask).getDownloadTask();
        doReturn(mockCompatListener).when(mockBaseTask).getCompatListener();
        doNothing().when(mockBaseTask).insureAssembleDownloadTask();

        fileDownloadSerialQueue.enqueue(mockBaseTask);

        verify(mockBaseTask).insureAssembleDownloadTask();
        verify(fileDownloadList).addIndependentTask(mockBaseTask);
        verify(listenerManager).addAutoRemoveListenersWhenTaskEnd(taskId);
        verify(listenerManager).attachListener(mockDownloadTask, mockCompatListener);
        verify(serialQueue).enqueue(mockDownloadTask);
    }

    @Test
    public void pause() {
        fileDownloadSerialQueue.pause();
        verify(serialQueue).pause();
    }

    @Test
    public void resume() {
        fileDownloadSerialQueue.resume();
        verify(serialQueue).resume();
    }

    @Test
    public void getWorkingTaskId() {
        fileDownloadSerialQueue.getWorkingTaskId();
        verify(serialQueue).getWorkingTaskId();
    }

    @Test
    public void getWaitingTaskCount() {
        fileDownloadSerialQueue.getWaitingTaskCount();
        verify(serialQueue).getWaitingTaskCount();
    }

    @Test
    public void shutDown() {
        final DownloadTask downloadTask1 = mock(DownloadTask.class);
        final DownloadTask downloadTask2 = mock(DownloadTask.class);
        final DownloadTask downloadTask3 = mock(DownloadTask.class);
        final DownloadTaskAdapter downloadTaskAdapter1 = mock(DownloadTaskAdapter.class);
        final DownloadTaskAdapter downloadTaskAdapter2 = mock(DownloadTaskAdapter.class);
        final DownloadTaskAdapter downloadTaskAdapter3 = mock(DownloadTaskAdapter.class);
        when(downloadTask1.getTag(DownloadTaskAdapter.KEY_TASK_ADAPTER))
                .thenReturn(downloadTaskAdapter1);
        when(downloadTask2.getTag(DownloadTaskAdapter.KEY_TASK_ADAPTER))
                .thenReturn(downloadTaskAdapter2);
        when(downloadTask3.getTag(DownloadTaskAdapter.KEY_TASK_ADAPTER))
                .thenReturn(downloadTaskAdapter3);
        final DownloadTask[] downloadTasks =
                new DownloadTask[]{downloadTask1, downloadTask2, downloadTask3};
        when(serialQueue.shutdown()).thenReturn(downloadTasks);

        final List<BaseDownloadTask> result = fileDownloadSerialQueue.shutdown();

        assertThat(result).hasSize(downloadTasks.length);
        verify(fileDownloadList).remove(downloadTaskAdapter1);
        verify(fileDownloadList).remove(downloadTaskAdapter2);
        verify(fileDownloadList).remove(downloadTaskAdapter3);
    }
}
