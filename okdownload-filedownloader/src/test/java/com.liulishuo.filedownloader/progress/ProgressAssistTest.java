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

package com.liulishuo.filedownloader.progress;

import com.liulishuo.filedownloader.CompatListenerAssist;
import com.liulishuo.filedownloader.DownloadTaskAdapter;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.okdownload.SpeedCalculator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ProgressAssistTest {

    @Test
    public void calculateCallbackMinIntervalBytes_noCallback() {
        final ProgressAssist progressAssist = new ProgressAssist(0);

        progressAssist.calculateCallbackMinIntervalBytes(0);
        assertThat(progressAssist.callbackMinIntervalBytes)
                .isEqualTo(ProgressAssist.NO_ANY_PROGRESS_CALLBACK);

        progressAssist.calculateCallbackMinIntervalBytes(100);
        assertThat(progressAssist.callbackMinIntervalBytes)
                .isEqualTo(ProgressAssist.NO_ANY_PROGRESS_CALLBACK);
    }

    @Test
    public void calculateCallbackMinIntervalBytes_minIntervalBytes() {
        ProgressAssist progressAssist = new ProgressAssist(10);

        progressAssist.calculateCallbackMinIntervalBytes(-1);
        assertThat(progressAssist.callbackMinIntervalBytes)
                .isEqualTo(ProgressAssist.CALLBACK_SAFE_MIN_INTERVAL_BYTES);

        progressAssist.calculateCallbackMinIntervalBytes(1);
        assertThat(progressAssist.callbackMinIntervalBytes)
                .isEqualTo(ProgressAssist.CALLBACK_SAFE_MIN_INTERVAL_BYTES);
    }

    @Test
    public void calculateCallbackMinIntervalBytes() {
        ProgressAssist progressAssist = new ProgressAssist(5);

        progressAssist.calculateCallbackMinIntervalBytes(100);

        assertThat(progressAssist.callbackMinIntervalBytes).isEqualTo(20);
    }

    @Test
    public void canCallbackProgress() {
        final ProgressAssist progressAssist = new ProgressAssist(5);
        progressAssist.callbackMinIntervalBytes = ProgressAssist.NO_ANY_PROGRESS_CALLBACK;

        assertThat(progressAssist.canCallbackProgress(Long.MAX_VALUE)).isEqualTo(false);
        assertThat(progressAssist.incrementBytes.get()).isEqualTo(0);

        progressAssist.callbackMinIntervalBytes = 10;

        assertThat(progressAssist.canCallbackProgress(5)).isEqualTo(false);
        assertThat(progressAssist.incrementBytes.get()).isEqualTo(5);
        assertThat(progressAssist.canCallbackProgress(5)).isEqualTo(true);
        assertThat(progressAssist.incrementBytes.get()).isEqualTo(0);
        assertThat(progressAssist.canCallbackProgress(12)).isEqualTo(true);
        assertThat(progressAssist.incrementBytes.get()).isEqualTo(2);
    }

    @Test
    public void onProgress() {
        final ProgressAssist progressAssist = new ProgressAssist(5);
        progressAssist.calculateCallbackMinIntervalBytes(100);

        assertThat(progressAssist.callbackMinIntervalBytes).isEqualTo(20);

        final DownloadTaskAdapter mockTask = spy(FileDownloader.getImpl().create("url"));
        final CompatListenerAssist.CompatListenerAssistCallback callback =
                mock(CompatListenerAssist.CompatListenerAssistCallback.class);
        doReturn(100L).when(mockTask).getTotalBytesInLong();

        for (int i = 0; i < 100; i++) {
            progressAssist.onProgress(mockTask, 1, callback);
        }

        verify(callback, times(5)).progress(eq(mockTask), anyLong(), eq(100L));
        assertThat(progressAssist.getSofarBytes()).isEqualTo(100);
    }

    @Test
    public void onProgress_noAnyProgress() {
        final SpeedCalculator mockSpeedCalculator = mock(SpeedCalculator.class);
        final ProgressAssist progressAssist = new ProgressAssist(-1, mockSpeedCalculator);
        progressAssist.calculateCallbackMinIntervalBytes(100);

        assertThat(progressAssist.callbackMinIntervalBytes)
                .isEqualTo(ProgressAssist.NO_ANY_PROGRESS_CALLBACK);

        final DownloadTaskAdapter mockTask = spy(FileDownloader.getImpl().create("url"));
        final CompatListenerAssist.CompatListenerAssistCallback callback =
                mock(CompatListenerAssist.CompatListenerAssistCallback.class);
        doReturn(100L).when(mockTask).getTotalBytesInLong();

        for (int i = 0; i < 100; i++) {
            progressAssist.onProgress(mockTask, 1, callback);
        }

        verify(mockSpeedCalculator, times(100)).downloading(1);
        verify(callback, never()).progress(any(DownloadTaskAdapter.class), anyLong(), anyLong());
        assertThat(progressAssist.getSofarBytes()).isEqualTo(100);
    }

    @Test
    public void clearProgress() {
        final ProgressAssist progressAssist = new ProgressAssist(5);
        progressAssist.incrementBytes.set(100);
        progressAssist.sofarBytes.set(100);

        progressAssist.clearProgress();

        assertThat(progressAssist.incrementBytes.get()).isEqualTo(0);
        assertThat(progressAssist.sofarBytes.get()).isEqualTo(0);
    }

    @Test
    public void initSofarBytes() {
        final ProgressAssist progressAssist = new ProgressAssist(5);
        progressAssist.initSofarBytes(100);
        assertThat(progressAssist.sofarBytes.get()).isEqualTo(100);
    }
}
