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

package com.liulishuo.okdownload.core;

import android.content.Context;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.ThreadFactory;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class UtilTest {

    @Mock private Util.Logger logger;
    @Mock private Exception e;

    private String tag = "tag";
    private String msg = "msg";

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void setLogger() throws Exception {
        Util.setLogger(logger);
        assertThat(Util.getLogger()).isEqualTo(logger);
    }

    @Test
    public void e() throws Exception {
        Util.setLogger(logger);
        Util.e(tag, msg, e);
        verify(logger).e(eq(tag), eq(msg), eq(e));
    }

    @Test
    public void w() throws Exception {
        Util.setLogger(logger);
        Util.w(tag, msg);
        verify(logger).w(eq(tag), eq(msg));
    }

    @Test
    public void d() throws Exception {
        Util.setLogger(logger);
        Util.d(tag, msg);
        verify(logger).d(eq(tag), eq(msg));
    }

    @Test
    public void i() throws Exception {
        Util.setLogger(logger);
        Util.i(tag, msg);
        verify(logger).i(eq(tag), eq(msg));
    }

    @Test
    public void isEmpty() throws Exception {
        assertThat(Util.isEmpty(null)).isTrue();
        assertThat(Util.isEmpty("")).isTrue();
        assertThat(Util.isEmpty("1")).isFalse();
    }

    @Test
    public void threadFactory() throws Exception {
        final String name = "name";
        final boolean daemon = true;
        final ThreadFactory factory = Util.threadFactory(name, daemon);

        assertThat(factory.newThread(mock(Runnable.class)).getName()).isEqualTo(name);
        assertThat(factory.newThread(mock(Runnable.class)).isDaemon()).isEqualTo(daemon);
    }

    @Test
    public void md5() throws Exception {
        assertThat(Util.md5("abc")).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
        assertThat(Util.md5("")).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
    }

    @Test
    public void isCorrectFull() throws Exception {
        assertThat(Util.isCorrectFull(1, 2)).isFalse();
        assertThat(Util.isCorrectFull(2, 2)).isTrue();
    }

    @Test
    public void resetBlockIfDirty_offsetLessThan0_reset() throws Exception {
        Util.setLogger(logger);
        final BlockInfo info = mock(BlockInfo.class);
        when(info.getCurrentOffset()).thenReturn(-1L);

        Util.resetBlockIfDirty(info);

        verify(info).resetBlock();
    }

    @Test
    public void resetBlockIfDirty_offsetLargerThanContent_reset() throws Exception {
        Util.setLogger(logger);
        final BlockInfo info = spy(new BlockInfo(0, 1));
        info.increaseCurrentOffset(2);

        Util.resetBlockIfDirty(info);

        verify(info).resetBlock();
    }

    public void resetBlockIfDirty() throws Exception {
        Util.setLogger(logger);
        final BlockInfo info = spy(new BlockInfo(0, 2));
        info.increaseCurrentOffset(1);

        Util.resetBlockIfDirty(info);

        verify(info, never()).resetBlock();
    }

    @Test
    public void getFreeSpaceBytes() throws Exception {
        Util.getFreeSpaceBytes("~/path");
    }

    @Test
    public void humanReadableBytes() throws Exception {
        assertThat(Util.humanReadableBytes(1054, true)).isEqualTo("1.1 kB");
        assertThat(Util.humanReadableBytes(1054, false)).isEqualTo("1.0 KiB");
    }

    @Test
    public void createDefaultDatabase() throws Exception {
        Util.createDefaultDatabase(mock(Context.class));
    }

    @Test
    public void createDefaultConnectionFactory() throws Exception {
        Util.createDefaultConnectionFactory();
    }

    @Test
    public void assembleBlock_oneBlock() throws Exception {
        mockOkDownload();
        final DownloadTask task = mock(DownloadTask.class);
        final BreakpointInfo info = mock(BreakpointInfo.class);
        final ArgumentCaptor<BlockInfo> capture = ArgumentCaptor.forClass(BlockInfo.class);

        when(OkDownload.with().downloadStrategy().isUseMultiBlock(false))
                .thenReturn(false);
        Util.assembleBlock(task, info, 10, false);

        verify(info).addBlock(capture.capture());
        List<BlockInfo> infoList = capture.getAllValues();
        assertThat(infoList.size()).isOne();
        BlockInfo blockInfo = infoList.get(0);
        assertThat(blockInfo.getStartOffset()).isZero();
        assertThat(blockInfo.getCurrentOffset()).isZero();
        assertThat(blockInfo.getContentLength()).isEqualTo(10L);
    }

    @Test
    public void assembleBlock_multiBlock() throws Exception {
        mockOkDownload();
        final DownloadTask task = mock(DownloadTask.class);
        final BreakpointInfo info = mock(BreakpointInfo.class);
        final ArgumentCaptor<BlockInfo> capture = ArgumentCaptor.forClass(BlockInfo.class);

        when(OkDownload.with().downloadStrategy().isUseMultiBlock(false))
                .thenReturn(true);
        when(OkDownload.with().downloadStrategy().determineBlockCount(task, 10))
                .thenReturn(3);
        Util.assembleBlock(task, info, 10, false);

        verify(info, times(3)).addBlock(capture.capture());
        List<BlockInfo> infoList = capture.getAllValues();
        assertThat(infoList.size()).isEqualTo(3);

        BlockInfo blockInfo1 = infoList.get(0);
        assertThat(blockInfo1.getStartOffset()).isZero();
        assertThat(blockInfo1.getCurrentOffset()).isZero();
        assertThat(blockInfo1.getContentLength()).isEqualTo(4L);

        BlockInfo blockInfo2 = infoList.get(1);
        assertThat(blockInfo2.getStartOffset()).isEqualTo(4L);
        assertThat(blockInfo2.getCurrentOffset()).isZero();
        assertThat(blockInfo2.getContentLength()).isEqualTo(3L);

        BlockInfo blockInfo3 = infoList.get(2);
        assertThat(blockInfo3.getStartOffset()).isEqualTo(7L);
        assertThat(blockInfo3.getCurrentOffset()).isZero();
        assertThat(blockInfo3.getContentLength()).isEqualTo(3L);
    }

    @Test
    public void parseContentLength() throws Exception {
        assertThat(Util.parseContentLength(null)).isEqualTo(CHUNKED_CONTENT_LENGTH);
        assertThat(Util.parseContentLength("123")).isEqualTo(123L);
    }
}