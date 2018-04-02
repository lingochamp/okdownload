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

package com.liulishuo.okdownload.core.download;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.exception.NetworkPolicyException;
import com.liulishuo.okdownload.core.exception.ResumeFailedException;
import com.liulishuo.okdownload.core.exception.ServerCanceledException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.RESPONSE_CREATED_RANGE_NOT_FROM_0;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.RESPONSE_ETAG_CHANGED;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.RESPONSE_PRECONDITION_FAILED;
import static com.liulishuo.okdownload.core.cause.ResumeFailedCause.RESPONSE_RESET_RANGE_NOT_FROM_0;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadStrategyTest {

    private DownloadStrategy strategy;
    @Mock private DownloadTask task;
    @Mock private DownloadConnection.Connected connected;
    @Mock private BreakpointInfo info;

    @Rule public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
        doReturn(spy(DownloadStrategy.class)).when(OkDownload.with()).downloadStrategy();
    }

    @Before
    public void setup() {
        initMocks(this);

        strategy = spy(new DownloadStrategy());
    }

    @Test
    public void resumeAvailableResponseCheck_PreconditionFailed() throws IOException {
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                resumeAvailableResponseCheck();

        when(info.getBlock(0)).thenReturn(mock(BlockInfo.class));
        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_PRECON_FAILED);
        expectResumeFailed(RESPONSE_PRECONDITION_FAILED);

        responseCheck.inspect();
    }

    @Test
    public void resumeAvailableResponseCheck_EtagChangedFromNone() throws IOException {
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                resumeAvailableResponseCheck();

        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connected.getResponseHeaderField("Etag")).thenReturn("new-etag");
        when(info.getBlock(0)).thenReturn(mock(BlockInfo.class));

        responseCheck.inspect();
    }

    @Test
    public void resumeAvailableResponseCheck_EtagChanged() throws IOException {
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                resumeAvailableResponseCheck();

        when(info.getBlock(0)).thenReturn(mock(BlockInfo.class));
        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connected.getResponseHeaderField("Etag")).thenReturn("new-etag");
        when(info.getEtag()).thenReturn("old-etag");

        expectResumeFailed(RESPONSE_ETAG_CHANGED);

        responseCheck.inspect();
    }

    @Test
    public void resumeAvailableResponseCheck_CreatedWithoutFrom0() throws IOException {
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                resumeAvailableResponseCheck();

        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(blockInfo.getCurrentOffset()).thenReturn(100L);
        when(info.getBlock(0)).thenReturn(blockInfo);
        expectResumeFailed(RESPONSE_CREATED_RANGE_NOT_FROM_0);

        responseCheck.inspect();
    }

    @Test
    public void resumeAvailableResponseCheck_ResetWithoutFrom0() throws IOException {
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                resumeAvailableResponseCheck();

        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_RESET);
        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(blockInfo.getCurrentOffset()).thenReturn(100L);
        when(info.getBlock(0)).thenReturn(blockInfo);
        expectResumeFailed(RESPONSE_RESET_RANGE_NOT_FROM_0);

        responseCheck.inspect();
    }


    @Test
    public void resumeAvailableResponseCheck_notPartialAndOk() throws IOException {
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                resumeAvailableResponseCheck();
        when(connected.getResponseCode()).thenReturn(501);
        when(info.getBlock(0)).thenReturn(mock(BlockInfo.class));

        expectServerCancelled(501, 0);
        responseCheck.inspect();
    }


    @Test
    public void resumeAvailableResponseCheck_okNotFrom0() throws IOException {
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                resumeAvailableResponseCheck();

        when(connected.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(blockInfo.getCurrentOffset()).thenReturn(100L);
        when(info.getBlock(0)).thenReturn(blockInfo);
        expectServerCancelled(HttpURLConnection.HTTP_OK, 100L);

        responseCheck.inspect();
    }

    private void expectServerCancelled(int responseCode, long currentOffset) {
        thrown.expect(ServerCanceledException.class);
        thrown.expectMessage(
                "Response code can't handled on internal " + responseCode
                        + " with current offset " + currentOffset);
    }

    private void expectResumeFailed(ResumeFailedCause cause) {
        thrown.expect(ResumeFailedException.class);
        thrown.expectMessage("Resume failed because of " + cause);
    }

    private DownloadStrategy.ResumeAvailableResponseCheck resumeAvailableResponseCheck() {
        return strategy.resumeAvailableResponseCheck(connected, 0, info);
    }

    @Test
    public void determineBlockCount() {
        // less than 1M
        assertThat(strategy.determineBlockCount(task, 500)).isEqualTo(1);
        assertThat(strategy.determineBlockCount(task, 900 * 1024))
                .isEqualTo(1);

        // less than 5M
        assertThat(strategy.determineBlockCount(task, 2 * 1024 * 1024))
                .isEqualTo(2);
        assertThat(strategy.determineBlockCount(task, (long) (4.9 * 1024 * 1024))).isEqualTo(2);

        // less than 50M
        assertThat(strategy.determineBlockCount(task, 18 * 1024 * 1024))
                .isEqualTo(3);
        assertThat(strategy.determineBlockCount(task, 49 * 1024 * 1024))
                .isEqualTo(3);


        // less than 100M
        assertThat(strategy.determineBlockCount(task, 66 * 1024 * 1024))
                .isEqualTo(4);
        assertThat(strategy.determineBlockCount(task, 99 * 1024 * 1024))
                .isEqualTo(4);

        // more than 100M
        assertThat(strategy.determineBlockCount(task, 1000 * 1024 * 1024))
                .isEqualTo(5);
        assertThat(strategy.determineBlockCount(task, 5323L * 1024 * 1024))
                .isEqualTo(5);
    }

    @Test
    public void isUseMultiBlock() throws IOException {
        mockOkDownload();

        when(OkDownload.with().outputStreamFactory().supportSeek()).thenReturn(false);
        assertThat(strategy.isUseMultiBlock(false)).isFalse();

        assertThat(strategy.isUseMultiBlock(true)).isFalse();

        when(OkDownload.with().outputStreamFactory().supportSeek()).thenReturn(true);
        assertThat(strategy.isUseMultiBlock(true)).isTrue();
    }

    @Test
    public void validFilenameResume() {
        final String taskFilename = "task-filename";
        when(task.getFilename()).thenReturn(taskFilename);
        final DownloadStrategy.FilenameHolder filenameHolder = mock(
                DownloadStrategy.FilenameHolder.class);
        when(task.getFilenameHolder()).thenReturn(filenameHolder);

        final String storeFilename = "store-filename";

        strategy.inspectFilenameFromResume(storeFilename, task);
        verify(filenameHolder, never()).set(anyString());

        when(task.getFilename()).thenReturn(null);
        strategy.inspectFilenameFromResume(storeFilename, task);
        verify(filenameHolder).set(storeFilename);
    }

    @Test
    public void validFilenameFromStore() {
        final DownloadStrategy.FilenameHolder holder = new DownloadStrategy.FilenameHolder();
        when(task.getUrl()).thenReturn("url");
        when(task.getFilenameHolder()).thenReturn(holder);
        final BreakpointStore store = OkDownload.with().breakpointStore();
        doReturn(null).when(store).getResponseFilename("url");

        assertThat(strategy.validFilenameFromStore(task)).isFalse();
        assertThat(holder.get()).isNull();

        doReturn("filename").when(store).getResponseFilename("url");
        assertThat(strategy.validFilenameFromStore(task)).isTrue();
        assertThat(holder.get()).isEqualTo("filename");
    }

    @Test
    public void validFilenameFromResponse() throws IOException {
        final String taskFilename = "task-filename";
        when(task.getFilename()).thenReturn(taskFilename);
        final DownloadStrategy.FilenameHolder filenameHolder = mock(
                DownloadStrategy.FilenameHolder.class);
        when(task.getFilenameHolder()).thenReturn(filenameHolder);

        final String responseFilename = "response-filename";
        final BreakpointInfo info = mock(BreakpointInfo.class);
        final DownloadStrategy.FilenameHolder infoFilenameHolder = mock(
                DownloadStrategy.FilenameHolder.class);
        when(info.getFilenameHolder()).thenReturn(infoFilenameHolder);

        final String determineFilename = "determine-filename";
        doReturn(determineFilename).when(strategy).determineFilename(responseFilename, task);

        strategy.validFilenameFromResponse(responseFilename, task, info);
        verify(filenameHolder, never()).set(anyString());

        when(task.getFilename()).thenReturn(null);
        strategy.validFilenameFromResponse(responseFilename, task, info);
        verify(filenameHolder).set(determineFilename);
        verify(infoFilenameHolder).set(determineFilename);
    }

    @Test
    public void determineFilename_tmpFilenameValid() throws IOException {
        final String validResponseFilename = "file name";
        String result = strategy.determineFilename(validResponseFilename, task);
        assertThat(result).isEqualTo(validResponseFilename);

        when(task.getUrl()).thenReturn("https://jacksgong.com/okdownload.3_1.apk?abc&ddd");
        result = strategy.determineFilename(null, task);
        assertThat(result).isEqualTo("okdownload.3_1.apk");


        when(task.getUrl()).thenReturn("https://jacksgong.com/dreamtobe.cn");
        result = strategy.determineFilename(null, task);
        assertThat(result).isEqualTo("dreamtobe.cn");

        when(task.getUrl()).thenReturn("https://jacksgong.com/?abc");
        result = strategy.determineFilename(null, task);
        assertThat(result).isNotEmpty();

        when(task.getUrl())
                .thenReturn("https://jacksgong.com/android-studio-ide-171.4408382-mac.dmg");
        result = strategy.determineFilename(null, task);
        assertThat(result).isEqualTo("android-studio-ide-171.4408382-mac.dmg");
    }

    @Test
    public void inspectNetwork() throws IOException {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.isWifiRequired()).thenReturn(true);

        final Context context = mock(Context.class);
        final OkDownload okDownload = OkDownload.with();
        doReturn(context).when(okDownload).context();
        doReturn(PackageManager.PERMISSION_DENIED).when(context)
                .checkCallingOrSelfPermission(anyString());
        thrown.expect(IOException.class);
        thrown.expectMessage("required for access network state but don't have the "
                + "permission of Manifest.permission.ACCESS_NETWORK_STATE, please declare this "
                + "permission first on your AndroidManifest, so we can handle the case of "
                + "downloading required wifi state.");
        strategy.inspectNetwork(task);

        doReturn(PackageManager.PERMISSION_GRANTED).when(context)
                .checkCallingOrSelfPermission(anyString());
        final ConnectivityManager manager = mock(ConnectivityManager.class);
        doReturn(manager).when(context).getSystemService(eq(Context.CONNECTIVITY_SERVICE));
        doReturn(null).when(manager).getActiveNetworkInfo();

        thrown.expect(NetworkPolicyException.class);
        thrown.expectMessage("Only allows downloading this task on the wifi network type!");
        strategy.inspectNetwork(task);

        final NetworkInfo info = mock(NetworkInfo.class);
        doReturn(info).when(manager).getActiveNetworkInfo();
        doReturn(ConnectivityManager.TYPE_WIFI).when(info).getType();
        strategy.inspectNetwork(task);
    }
}