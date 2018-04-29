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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.connection.DownloadConnection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static com.liulishuo.okdownload.core.Util.IF_MATCH;
import static com.liulishuo.okdownload.core.Util.RANGE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
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
    public void setLogger() {
        Util.setLogger(logger);
        assertThat(Util.getLogger()).isEqualTo(logger);
    }

    @Test
    public void e() {
        Util.setLogger(logger);
        Util.enableConsoleLog();
        Util.e(tag, msg, e);
        verify(logger, never()).e(eq(tag), eq(msg), eq(e));

        Util.setLogger(logger);
        Util.e(tag, msg, e);
        verify(logger).e(eq(tag), eq(msg), eq(e));
    }

    @Test
    public void w() {
        Util.setLogger(logger);
        Util.enableConsoleLog();
        Util.w(tag, msg);
        verify(logger, never()).w(eq(tag), eq(msg));

        Util.setLogger(logger);
        Util.w(tag, msg);
        verify(logger).w(eq(tag), eq(msg));
    }

    @Test
    public void d() {
        Util.setLogger(logger);
        Util.enableConsoleLog();
        Util.d(tag, msg);
        verify(logger, never()).d(eq(tag), eq(msg));

        Util.setLogger(logger);
        Util.d(tag, msg);
        verify(logger).d(eq(tag), eq(msg));
    }

    @Test
    public void i() {
        Util.setLogger(logger);
        Util.enableConsoleLog();
        Util.i(tag, msg);
        verify(logger, never()).i(eq(tag), eq(msg));

        Util.setLogger(logger);
        Util.i(tag, msg);
        verify(logger).i(eq(tag), eq(msg));
    }

    @Test
    public void isEmpty() {
        assertThat(Util.isEmpty(null)).isTrue();
        assertThat(Util.isEmpty("")).isTrue();
        assertThat(Util.isEmpty("1")).isFalse();
    }

    @Test
    public void threadFactory() {
        final String name = "name";
        final boolean daemon = true;
        final ThreadFactory factory = Util.threadFactory(name, daemon);

        assertThat(factory.newThread(mock(Runnable.class)).getName()).isEqualTo(name);
        assertThat(factory.newThread(mock(Runnable.class)).isDaemon()).isEqualTo(daemon);
    }

    @Test
    public void md5() {
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
    public void parseContentLength() {
        assertThat(Util.parseContentLength(null)).isEqualTo(CHUNKED_CONTENT_LENGTH);
        assertThat(Util.parseContentLength("123")).isEqualTo(123L);
    }

    @Test
    public void parseContentLengthFromContentRange() {
        String length801ContentRange = "bytes 200-1000/67589";
        assertThat(Util.parseContentLengthFromContentRange(length801ContentRange)).isEqualTo(801);

        assertThat(Util.parseContentLengthFromContentRange(null)).isEqualTo(CHUNKED_CONTENT_LENGTH);
        assertThat(Util.parseContentLengthFromContentRange("")).isEqualTo(CHUNKED_CONTENT_LENGTH);
        assertThat(Util.parseContentLengthFromContentRange("invalid"))
                .isEqualTo(CHUNKED_CONTENT_LENGTH);
    }

    @Test
    public void isNetworkNotOnWifiType() {
        assertThat(Util.isNetworkNotOnWifiType(null)).isTrue();

        final ConnectivityManager manager = mock(ConnectivityManager.class);
        when(manager.getActiveNetworkInfo()).thenReturn(null);
        assertThat(Util.isNetworkNotOnWifiType(manager)).isTrue();

        final NetworkInfo info = mock(NetworkInfo.class);
        when(manager.getActiveNetworkInfo()).thenReturn(info);

        when(info.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        assertThat(Util.isNetworkNotOnWifiType(manager)).isTrue();

        when(info.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
        assertThat(Util.isNetworkNotOnWifiType(manager)).isFalse();
    }

    @Test
    public void isNetworkAvailable() {
        assertThat(Util.isNetworkAvailable(null)).isTrue();

        final ConnectivityManager manager = mock(ConnectivityManager.class);
        when(manager.getActiveNetworkInfo()).thenReturn(null);
        assertThat(Util.isNetworkAvailable(manager)).isFalse();

        final NetworkInfo info = mock(NetworkInfo.class);
        when(manager.getActiveNetworkInfo()).thenReturn(info);

        when(info.isConnected()).thenReturn(false);
        assertThat(Util.isNetworkAvailable(manager)).isFalse();

        when(info.isConnected()).thenReturn(true);
        assertThat(Util.isNetworkAvailable(manager)).isTrue();
    }

    @Test
    public void getFilenameFromContentUri() throws IOException {
        mockOkDownload();

        final Uri contentUri = mock(Uri.class);
        final ContentResolver resolver = mock(ContentResolver.class);
        final OkDownload okDownload = OkDownload.with();
        final Context context = mock(Context.class);
        when(okDownload.context()).thenReturn(context);
        when(context.getContentResolver()).thenReturn(resolver);

        // null cursor
        when(resolver.query(contentUri, null, null, null, null)).thenReturn(null);
        assertThat(Util.getFilenameFromContentUri(contentUri)).isNull();

        // valid cursor
        final Cursor cursor = mock(Cursor.class);
        when(resolver.query(contentUri, null, null, null, null)).thenReturn(cursor);
        when(cursor.getString(anyInt())).thenReturn("filename");
        assertThat(Util.getFilenameFromContentUri(contentUri)).isEqualTo("filename");
    }

    @Test
    public void getSizeFromContentUri() throws IOException {
        mockOkDownload();

        final Uri contentUri = mock(Uri.class);
        final ContentResolver resolver = mock(ContentResolver.class);
        final OkDownload okDownload = OkDownload.with();
        final Context context = mock(Context.class);
        when(okDownload.context()).thenReturn(context);
        when(context.getContentResolver()).thenReturn(resolver);

        // null cursor
        when(resolver.query(contentUri, null, null, null, null)).thenReturn(null);
        assertThat(Util.getSizeFromContentUri(contentUri)).isZero();

        // valid cursor
        final Cursor cursor = mock(Cursor.class);
        when(resolver.query(contentUri, null, null, null, null)).thenReturn(cursor);
        when(cursor.getLong(anyInt())).thenReturn(1L);
        assertThat(Util.getSizeFromContentUri(contentUri)).isOne();
        verify(cursor).close();
    }

    @Test
    public void addUserRequestHeaderField() throws IOException {
        Map<String, List<String>> userHeaderMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("header1-value1");
        values.add("header1-value2");
        userHeaderMap.put("header1", values);
        values = new ArrayList<>();
        values.add("header2-value");
        userHeaderMap.put("header2", values);

        final DownloadConnection connection = mock(DownloadConnection.class);

        Util.addUserRequestHeaderField(userHeaderMap, connection);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(connection, times(3))
                .addHeader(nameCaptor.capture(), valueCaptor.capture());

        assertThat(nameCaptor.getAllValues())
                .containsExactlyInAnyOrder("header1", "header1", "header2");
        assertThat(valueCaptor.getAllValues())
                .containsExactlyInAnyOrder("header1-value1", "header1-value2", "header2-value");
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void inspectUserHeader() throws IOException {
        Map<String, List<String>> userHeaderMap = new HashMap<>();
        userHeaderMap.put("header1", new ArrayList<String>());
        userHeaderMap.put("header2", new ArrayList<String>());

        Util.inspectUserHeader(userHeaderMap);

        userHeaderMap.put(RANGE, new ArrayList<String>());
        thrown.expect(IOException.class);
        thrown.expectMessage(IF_MATCH + " and " + RANGE + " only can be handle by internal!");
        Util.inspectUserHeader(userHeaderMap);
        userHeaderMap.remove(RANGE);

        userHeaderMap.put(IF_MATCH, new ArrayList<String>());
        thrown.expect(IOException.class);
        thrown.expectMessage(IF_MATCH + " and " + RANGE + " only can be handle by internal!");
        Util.inspectUserHeader(userHeaderMap);
    }
}