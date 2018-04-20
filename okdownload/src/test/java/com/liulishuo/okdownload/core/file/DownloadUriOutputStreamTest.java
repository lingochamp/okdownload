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

package com.liulishuo.okdownload.core.file;

import android.os.ParcelFileDescriptor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.SyncFailedException;
import java.nio.channels.FileChannel;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE, sdk = LOLLIPOP)
public class DownloadUriOutputStreamTest {

    @Mock private FileChannel channel;
    @Mock private ParcelFileDescriptor pdf;
    @Mock private BufferedOutputStream out;
    @Mock private FileOutputStream fos;
    @Mock private FileDescriptor fd;

    private DownloadUriOutputStream outputStream;

    @Before
    public void setup() {
        initMocks(this);

        outputStream = new DownloadUriOutputStream(channel, pdf, fos, out);
    }

    @Test
    public void write() throws Exception {
        byte[] bytes = new byte[2];
        outputStream.write(bytes, 0, 1);
        verify(out).write(eq(bytes), eq(0), eq(1));
    }

    @Test
    public void close() throws Exception {
        outputStream.close();
        verify(out).close();
        verify(fos).close();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    // because of we invoke the native method, so this expected is means to invoked fd.sync
    @Test
    public void flushAndSync() throws Exception {
        when(pdf.getFileDescriptor()).thenReturn(fd);
        thrown.expect(SyncFailedException.class);
        thrown.expectMessage("sync failed");
        outputStream.flushAndSync();
        verify(out).flush();
    }

    @Test
    public void seek() throws Exception {
        outputStream.seek(1);
        verify(channel).position(eq(1L));
    }

    @Test
    public void setLength() throws Exception {
        outputStream.setLength(1);
        verify(pdf).getFileDescriptor();
    }

}