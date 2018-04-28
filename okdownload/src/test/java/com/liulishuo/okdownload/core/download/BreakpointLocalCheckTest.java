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

package com.liulishuo.okdownload.core.download;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.file.DownloadOutputStream;
import com.liulishuo.okdownload.core.file.ProcessFileStrategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.FILE_NOT_EXIST;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.INFO_DIRTY;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.OUTPUT_STREAM_NOT_SUPPORT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class BreakpointLocalCheckTest {

    private BreakpointLocalCheck check;

    @Mock private DownloadTask task;
    @Mock private BreakpointInfo info;
    @Mock private File fileOnInfo;
    @Mock private File fileOnTask;
    @Mock private BlockInfo blockInfo;
    @Mock private Uri contentUri;
    @Mock private Uri fileUri;

    @Before
    public void setup() {
        initMocks(this);

        check = spy(new BreakpointLocalCheck(task, info, -1));

        when(info.getBlockCount()).thenReturn(1);
        when(info.isChunked()).thenReturn(false);
        when(info.getFile()).thenReturn(fileOnInfo);
        when(task.getFile()).thenReturn(fileOnInfo);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getContentLength()).thenReturn(1L);

        when(contentUri.getScheme()).thenReturn(ContentResolver.SCHEME_CONTENT);
        when(fileUri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);
    }

    @Test
    public void getCauseOrThrow_infoDirty() {
        check.infoRight = false;
        check.fileExist = true;
        check.outputStreamSupport = true;
        assertThat(check.getCauseOrThrow()).isEqualTo(INFO_DIRTY);
    }

    @Test
    public void getCauseOrThrow_fileNotExist() {
        check.infoRight = true;
        check.fileExist = false;
        check.outputStreamSupport = true;
        assertThat(check.getCauseOrThrow()).isEqualTo(FILE_NOT_EXIST);
    }

    @Test
    public void getCauseOrThrow_outputStreamNotSupport() {
        check.infoRight = true;
        check.fileExist = true;
        check.outputStreamSupport = false;
        assertThat(check.getCauseOrThrow()).isEqualTo(OUTPUT_STREAM_NOT_SUPPORT);
    }

    @Test(expected = IllegalStateException.class)
    public void getCauseOrThrow_notDirty() {
        check.infoRight = true;
        check.fileExist = true;
        check.outputStreamSupport = true;
        check.getCauseOrThrow();
    }

    @Test
    public void isInfoRightToResume_noBlock() {
        when(info.getBlockCount()).thenReturn(0);
        assertThat(check.isInfoRightToResume()).isFalse();
    }

    @Test
    public void isInfoRightToResume_chunked() {
        when(info.isChunked()).thenReturn(true);
        assertThat(check.isInfoRightToResume()).isFalse();
    }

    @Test
    public void isInfoRightToResume_noFile() {
        when(info.getFile()).thenReturn(null);
        assertThat(check.isInfoRightToResume()).isFalse();
    }

    @Test
    public void isInfoRightToResume_fileNotEqual() {
        when(task.getFile()).thenReturn(fileOnTask);
        assertThat(check.isInfoRightToResume()).isFalse();
    }

    @Test
    public void isInfoRightToResume_fileLengthLargerThanTotalLength() {
        when(fileOnInfo.length()).thenReturn(2L);
        when(info.getTotalLength()).thenReturn(1L);
        assertThat(check.isInfoRightToResume()).isFalse();
    }

    @Test
    public void isInfoRightToResume_instanceLengthEqual() {
        check = spy(new BreakpointLocalCheck(task, info, 2));

        when(info.getBlockCount()).thenReturn(1);
        when(info.isChunked()).thenReturn(false);
        when(info.getFile()).thenReturn(fileOnInfo);
        when(task.getFile()).thenReturn(fileOnInfo);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getContentLength()).thenReturn(1L);
        when(contentUri.getScheme()).thenReturn(ContentResolver.SCHEME_CONTENT);
        when(fileUri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);

        when(info.getTotalLength()).thenReturn(1L);
        assertThat(check.isInfoRightToResume()).isFalse();
    }

    @Test
    public void isInfoRightToResume_blockRight() {
        when(blockInfo.getContentLength()).thenReturn(0L);
        assertThat(check.isInfoRightToResume()).isFalse();
    }

    @Test
    public void isInfoRightToResume() {
        when(info.getBlockCount()).thenReturn(1);
        when(info.isChunked()).thenReturn(false);
        when(info.getFile()).thenReturn(fileOnInfo);
        when(task.getFile()).thenReturn(fileOnInfo);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getContentLength()).thenReturn(1L);

        assertThat(check.isInfoRightToResume()).isTrue();
    }

    @Test
    public void isOutputStreamSupportResume_support() throws IOException {
        mockOkDownload();

        // support seek
        final DownloadOutputStream.Factory factory = OkDownload.with().outputStreamFactory();
        when(factory.supportSeek()).thenReturn(true);
        assertThat(check.isOutputStreamSupportResume()).isTrue();

        // not support seek
        when(factory.supportSeek()).thenReturn(false);
        // just one block
        when(info.getBlockCount()).thenReturn(1);
        // not pre allocate length
        final ProcessFileStrategy strategy = OkDownload.with().processFileStrategy();
        when(strategy.isPreAllocateLength(task)).thenReturn(false);
        assertThat(check.isOutputStreamSupportResume()).isTrue();
    }

    @Test
    public void isOutputStreamSupportResume_notSupport() throws IOException {
        mockOkDownload();

        final DownloadOutputStream.Factory factory = OkDownload.with().outputStreamFactory();
        when(factory.supportSeek()).thenReturn(false);
        when(info.getBlockCount()).thenReturn(2);

        assertThat(check.isOutputStreamSupportResume()).isFalse();

        when(info.getBlockCount()).thenReturn(1);
        // pre allocate length but not support seek
        final ProcessFileStrategy strategy = OkDownload.with().processFileStrategy();
        doReturn(true).when(strategy).isPreAllocateLength(task);
        assertThat(check.isOutputStreamSupportResume()).isFalse();
    }

    @Test
    public void isFileExistToResume_contentUri() throws IOException {
        mockOkDownload();

        final OkDownload okDownload = OkDownload.with();
        final Context context = mock(Context.class);
        when(okDownload.context()).thenReturn(context);
        final ContentResolver resolver = mock(ContentResolver.class);
        when(context.getContentResolver()).thenReturn(resolver);

        when(task.getUri()).thenReturn(contentUri);
        assertThat(check.isFileExistToResume()).isFalse();

        // size > 0
        final Cursor cursor = mock(Cursor.class);
        when(resolver.query(contentUri, null, null, null, null)).thenReturn(cursor);
        doReturn(1L).when(cursor).getLong(anyInt());
        assertThat(check.isFileExistToResume()).isTrue();
    }

    @Test
    public void isFileExistToResume_fileUri() {
        when(task.getUri()).thenReturn(fileUri);
        when(task.getFile()).thenReturn(null);
        assertThat(check.isFileExistToResume()).isFalse();

        final File file = mock(File.class);
        when(task.getFile()).thenReturn(file);
        when(file.exists()).thenReturn(false);
        assertThat(check.isFileExistToResume()).isFalse();

        when(file.exists()).thenReturn(true);
        assertThat(check.isFileExistToResume()).isTrue();
    }

    @Test
    public void check_notDirty() {
        doReturn(true).when(check).isFileExistToResume();
        doReturn(true).when(check).isInfoRightToResume();
        doReturn(true).when(check).isOutputStreamSupportResume();

        check.check();

        assertThat(check.isDirty()).isFalse();
    }

    @Test
    public void check_fileNotExist() {
        doReturn(false).when(check).isFileExistToResume();
        doReturn(true).when(check).isInfoRightToResume();
        doReturn(true).when(check).isOutputStreamSupportResume();

        check.check();

        assertThat(check.isDirty()).isTrue();
    }

    @Test
    public void check_infoNotRight() {
        doReturn(true).when(check).isFileExistToResume();
        doReturn(false).when(check).isInfoRightToResume();
        doReturn(true).when(check).isOutputStreamSupportResume();

        check.check();

        assertThat(check.isDirty()).isTrue();
    }

    @Test
    public void check_outputStreamNotSupport() {
        doReturn(true).when(check).isFileExistToResume();
        doReturn(true).when(check).isInfoRightToResume();
        doReturn(false).when(check).isOutputStreamSupportResume();

        check.check();

        assertThat(check.isDirty()).isTrue();
    }
}
