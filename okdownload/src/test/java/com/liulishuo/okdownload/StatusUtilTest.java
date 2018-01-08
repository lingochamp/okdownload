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

package com.liulishuo.okdownload;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

import static com.liulishuo.okdownload.StatusUtil.Status.COMPLETED;
import static com.liulishuo.okdownload.StatusUtil.Status.PENDING;
import static com.liulishuo.okdownload.StatusUtil.Status.RUNNING;
import static com.liulishuo.okdownload.StatusUtil.Status.UNKNOWN;
import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class StatusUtilTest {
    private File file;
    private String url = "url";

    @Before
    public void setup() throws IOException {
        file = new File("p-path/filename");
        // for new instance of store
        mockOkDownload();
    }

    @After
    public void tearDown() {
        if (file.exists()) file.delete();
        if (file.getParentFile().exists()) file.getParentFile().delete();
    }

    @Test
    public void getStatus() throws IOException {
        file.getParentFile().mkdirs();
        file.createNewFile();
        assertThat(file.exists()).isTrue();

        StatusUtil.Status status = StatusUtil
                .getStatus(url, file.getParent(), file.getName());
        assertThat(status).isEqualTo(COMPLETED);

        // no filename
        status = StatusUtil.getStatus(url, file.getParentFile().getPath(), null);
        assertThat(status).isEqualTo(UNKNOWN);

        final DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();
        doReturn(true).when(dispatcher).isRunning(any(DownloadTask.class));
        status = StatusUtil.getStatus(url, file.getParentFile().getPath(), null);

        assertThat(status).isEqualTo(RUNNING);

        doReturn(true).when(dispatcher).isPending(any(DownloadTask.class));
        status = StatusUtil.getStatus(url, file.getParentFile().getPath(), null);
        assertThat(status).isEqualTo(PENDING);
    }

    @Test
    public void isCompleted() throws IOException {
        assertThat(file.exists()).isFalse();
        boolean isCompleted = StatusUtil
                .isCompleted(url, file.getParentFile().getPath(), file.getName());
        assertThat(isCompleted).isFalse();

        file.getParentFile().mkdirs();
        file.createNewFile();
        isCompleted = StatusUtil
                .isCompleted(url, file.getParentFile().getPath(), file.getName());
        assertThat(isCompleted).isTrue();

        final BreakpointStore store = OkDownload.with().breakpointStore();
        doReturn(mock(BreakpointInfo.class)).when(store).get(anyInt());
        isCompleted = StatusUtil
                .isCompleted(url, file.getParentFile().getPath(), file.getName());
        assertThat(isCompleted).isFalse();
    }

    @Test
    public void isCompletedOrUnknown_infoNotExist() throws IOException {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getUrl()).thenReturn("url");
        when(task.getParentPath()).thenReturn(file.getParent());
        file.getParentFile().mkdirs();
        file.createNewFile();

        // filename is null and can't find ---> unknown
        final BreakpointStore store = OkDownload.with().breakpointStore();
        doReturn(null).when(store).getResponseFilename(anyString());
        assertThat(StatusUtil.isCompletedOrUnknown(task)).isEqualTo(StatusUtil.Status.UNKNOWN);

        // filename is null but found on store but not exist ---> unknown
        doReturn("no-exist-filename").when(store).getResponseFilename(anyString());
        assertThat(StatusUtil.isCompletedOrUnknown(task)).isEqualTo(StatusUtil.Status.UNKNOWN);

        // filename is null but found on store and exist ---> completed
        doReturn(file.getName()).when(store).getResponseFilename(anyString());
        assertThat(StatusUtil.isCompletedOrUnknown(task)).isEqualTo(StatusUtil.Status.COMPLETED);

        // file name not null and exist
        when(task.getFilename()).thenReturn(file.getName());
        assertThat(StatusUtil.isCompletedOrUnknown(task)).isEqualTo(StatusUtil.Status.COMPLETED);
    }

    @Test
    public void isCompletedOrUnknown_infoExist() throws IOException {
        final DownloadTask task = mock(DownloadTask.class);
        file.getParentFile().mkdirs();
        file.createNewFile();

        // case of info exist
        final BreakpointStore store = OkDownload.with().breakpointStore();
        final BreakpointInfo info = mock(BreakpointInfo.class);
        doReturn(info).when(store).get(anyInt());

        // info exist but no filename ---> idle
        assertThat(StatusUtil.isCompletedOrUnknown(task)).isEqualTo(StatusUtil.Status.IDLE);

        // info exist but filename not the same ---> idle
        when(task.getFilename()).thenReturn("filename");
        assertThat(StatusUtil.isCompletedOrUnknown(task)).isEqualTo(StatusUtil.Status.IDLE);

        // info exist and filename is the same but offset not the same to total ---> idle
        when(task.getFilename()).thenReturn(file.getName());
        when(task.getParentPath()).thenReturn(file.getParent());
        when(info.getFilename()).thenReturn(file.getName());
        when(info.getTotalLength()).thenReturn(1L);
        assertThat(StatusUtil.isCompletedOrUnknown(task)).isEqualTo(StatusUtil.Status.IDLE);

        // info exist and filename is the same and offset the same to total ---> completed
        when(info.getTotalOffset()).thenReturn(1L);
        assertThat(StatusUtil.isCompletedOrUnknown(task)).isEqualTo(StatusUtil.Status.COMPLETED);
    }

    @Test
    public void getCurrentInfo() {
        final BreakpointStore store = OkDownload.with().breakpointStore();
        final BreakpointInfo origin = mock(BreakpointInfo.class);

        doReturn(origin).when(store).get(anyInt());

        StatusUtil.getCurrentInfo(mock(DownloadTask.class));
        verify(origin).copy();
    }

    @Test
    public void createFinder() throws IOException {
        DownloadTask task = StatusUtil.createFinder(url, file.getParent(), null);
        assertThat(task.getPath()).isNull();

        assertThat(task.getParentPath()).isEqualTo(file.getParentFile().getAbsolutePath());

        task = StatusUtil.createFinder(url, file.getParent(), file.getName());
        assertThat(task.getPath()).isEqualTo(file.getAbsolutePath());
    }
}