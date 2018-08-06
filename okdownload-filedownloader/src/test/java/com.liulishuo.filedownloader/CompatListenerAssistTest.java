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

import android.os.Handler;

import com.liulishuo.filedownloader.exception.FileDownloadNetworkPolicyException;
import com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException;
import com.liulishuo.filedownloader.progress.ProgressAssist;
import com.liulishuo.filedownloader.retry.RetryAssist;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.exception.NetworkPolicyException;
import com.liulishuo.okdownload.core.exception.PreAllocateException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class CompatListenerAssistTest {

    @Mock
    private CompatListenerAssist.CompatListenerAssistCallback callback;
    @Mock
    private Handler uiHander;
    private CompatListenerAssist compatListenerAssist;

    @Before
    public void setup() {
        initMocks(this);

        compatListenerAssist = spy(new CompatListenerAssist(callback, uiHander));
    }

    @Test
    public void taskStart() {
        final DownloadTask mockDownloadTask = mock(DownloadTask.class);
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        when(mockDownloadTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(null);

        compatListenerAssist.taskStart(mockDownloadTask);
        verify(callback, never()).pending(any(DownloadTaskAdapter.class), anyLong(), anyLong());
        verify(callback, never()).started(any(DownloadTaskAdapter.class));

        when(mockDownloadTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(mockTaskAdapter);
        when(mockTaskAdapter.getSoFarBytesInLong()).thenReturn(1L);
        when(mockTaskAdapter.getTotalBytesInLong()).thenReturn(2L);

        compatListenerAssist.taskStart(mockDownloadTask);

        verify(callback).pending(mockTaskAdapter, 1L, 2L);
        verify(callback).started(mockTaskAdapter);
    }

    @Test
    public void connectStart() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(null);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);

        assertThat(compatListenerAssist.taskConnected.get()).isFalse();

        compatListenerAssist.connectStart(mockTask);
        assertThat(compatListenerAssist.taskConnected.get()).isTrue();
        verify(mockTaskAdapter, never()).getSoFarBytesInLong();
        verify(mockTaskAdapter, never()).getTotalBytesInLong();
        verify(mockTaskAdapter, never()).getProgressAssist();
        verify(callback, never()).connected(
                any(DownloadTaskAdapter.class), anyString(), anyBoolean(), anyLong(), anyLong());

        compatListenerAssist.taskConnected.set(false);
        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(mockTaskAdapter);
        when(mockTaskAdapter.getSoFarBytesInLong()).thenReturn(1L);
        when(mockTaskAdapter.getTotalBytesInLong()).thenReturn(2L);

        compatListenerAssist.connectStart(mockTask);

        verify(callback).connected(
                mockTaskAdapter,
                compatListenerAssist.getEtag(),
                compatListenerAssist.isResumable(),
                1L,
                2L);
        verify(mockProgressAssist).calculateCallbackMinIntervalBytes(2);
    }

    @Test
    public void fetchProgress() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(null);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);

        compatListenerAssist.fetchProgress(mockTask, 5);
        verify(mockTaskAdapter, never()).getProgressAssist();
        verify(mockProgressAssist, never()).onProgress(mockTaskAdapter, 5, callback);

        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(mockTaskAdapter);

        compatListenerAssist.fetchProgress(mockTask, 5);
        verify(mockProgressAssist).onProgress(mockTaskAdapter, 5, callback);
    }

    @Test
    public void taskEnd_doNothing() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(null);

        compatListenerAssist.taskEnd(mockTask, EndCause.CANCELED, null);
        verify(compatListenerAssist, never())
                .handleError(any(DownloadTaskAdapter.class), any(Exception.class));
        verify(compatListenerAssist, never())
                .handleCanceled(any(DownloadTaskAdapter.class));
        verify(compatListenerAssist, never())
                .handleWarn(
                        any(DownloadTaskAdapter.class), any(EndCause.class), any(Exception.class));
        verify(compatListenerAssist, never())
                .handleComplete(any(DownloadTaskAdapter.class));
        verify(compatListenerAssist, never()).onTaskFinish(any(DownloadTaskAdapter.class));
    }

    @Test
    public void taskEnd_handleError() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(mockTaskAdapter);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);
        doNothing().when(compatListenerAssist).handleError(mockTaskAdapter, null);
        doNothing().when(compatListenerAssist).onTaskFinish(mockTaskAdapter);

        compatListenerAssist.taskEnd(mockTask, EndCause.PRE_ALLOCATE_FAILED, null);
        compatListenerAssist.taskEnd(mockTask, EndCause.ERROR, null);

        verify(compatListenerAssist, times(2)).handleError(mockTaskAdapter, null);
        verify(compatListenerAssist, times(2)).onTaskFinish(mockTaskAdapter);
        verify(mockProgressAssist, times(2)).clearProgress();
    }

    @Test
    public void taskEnd_handleCanceled() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(mockTaskAdapter);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);
        doNothing().when(compatListenerAssist).handleCanceled(mockTaskAdapter);
        doNothing().when(compatListenerAssist).onTaskFinish(mockTaskAdapter);

        compatListenerAssist.taskEnd(mockTask, EndCause.CANCELED, null);

        verify(compatListenerAssist).handleCanceled(mockTaskAdapter);
        verify(compatListenerAssist).onTaskFinish(mockTaskAdapter);
        verify(mockProgressAssist).clearProgress();
    }

    @Test
    public void taskEnd_handleWarn() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(mockTaskAdapter);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);
        doNothing().when(compatListenerAssist).handleWarn(
                any(DownloadTaskAdapter.class), any(EndCause.class), any(Exception.class));
        doNothing().when(compatListenerAssist).onTaskFinish(mockTaskAdapter);

        compatListenerAssist.taskEnd(mockTask, EndCause.FILE_BUSY, null);
        compatListenerAssist.taskEnd(mockTask, EndCause.SAME_TASK_BUSY, null);

        verify(compatListenerAssist).handleWarn(mockTaskAdapter, EndCause.FILE_BUSY, null);
        verify(compatListenerAssist).handleWarn(mockTaskAdapter, EndCause.SAME_TASK_BUSY, null);
        verify(compatListenerAssist, times(2)).onTaskFinish(mockTaskAdapter);
        verify(mockProgressAssist, times(2)).clearProgress();
    }

    @Test
    public void taskEnd_handleComplete() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        when(mockTask.getTag(DownloadTaskAdapter.TAG_KEY)).thenReturn(mockTaskAdapter);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);
        doNothing().when(compatListenerAssist).handleComplete(mockTaskAdapter);
        doNothing().when(compatListenerAssist).onTaskFinish(mockTaskAdapter);

        compatListenerAssist.taskEnd(mockTask, EndCause.COMPLETED, null);

        verify(compatListenerAssist).handleComplete(mockTaskAdapter);
        verify(compatListenerAssist).onTaskFinish(mockTaskAdapter);
        verify(mockProgressAssist).clearProgress();
    }

    @Test
    public void handleCanceled() {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);
        when(mockProgressAssist.getSofarBytes()).thenReturn(1L);
        when(mockTaskAdapter.getTotalBytesInLong()).thenReturn(2L);

        compatListenerAssist.handleCanceled(mockTaskAdapter);
        verify(callback).paused(mockTaskAdapter, 1L, 2L);
    }

    @Test
    public void handleWarn() {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        compatListenerAssist.handleWarn(mockTaskAdapter, null, null);

        verify(callback).warn(mockTaskAdapter);
    }

    @Test
    public void handleError_interceptByRetry() {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        final RetryAssist mockRetryAssist = mock(RetryAssist.class);
        final DownloadTask mockTask = mock(DownloadTask.class);
        when(mockTaskAdapter.getDownloadTask()).thenReturn(mockTask);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);
        when(mockTaskAdapter.getRetryAssist()).thenReturn(mockRetryAssist);
        when(mockProgressAssist.getSofarBytes()).thenReturn(1L);
        when(mockRetryAssist.canRetry()).thenReturn(true);
        when(mockRetryAssist.getRetriedTimes()).thenReturn(1);

        compatListenerAssist.handleError(mockTaskAdapter, null);
        verify(callback).retry(mockTaskAdapter, null, 2, 1L);
        verify(mockRetryAssist).doRetry(mockTask);
        verify(callback, never()).error(any(DownloadTaskAdapter.class), any(Throwable.class));
    }

    @Test
    public void handleError_withNetWorkPolicyException() {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        when(mockTaskAdapter.getRetryAssist()).thenReturn(null);
        final Exception netWorkPolicyException = mock(NetworkPolicyException.class);

        compatListenerAssist.handleError(mockTaskAdapter, netWorkPolicyException);
        verify(callback, never()).retry(
                any(DownloadTaskAdapter.class), any(Throwable.class), anyInt(), anyLong());
        verify(callback).error(any(DownloadTaskAdapter.class),
                any(FileDownloadNetworkPolicyException.class));
    }

    @Test
    public void handleError_withPreAllocateException() {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final ProgressAssist mockProgressAssist = mock(ProgressAssist.class);
        when(mockTaskAdapter.getProgressAssist()).thenReturn(mockProgressAssist);
        when(mockTaskAdapter.getRetryAssist()).thenReturn(null);
        when(mockProgressAssist.getSofarBytes()).thenReturn(1L);
        final Exception mockException = mock(PreAllocateException.class);

        compatListenerAssist.handleError(mockTaskAdapter, mockException);

        verify(callback, never()).retry(
                any(DownloadTaskAdapter.class), any(Throwable.class), anyInt(), anyLong());
        final ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        final ArgumentCaptor<DownloadTaskAdapter> taskCaptor = ArgumentCaptor
                .forClass(DownloadTaskAdapter.class);
        verify(callback).error(taskCaptor.capture(), throwableCaptor.capture());
        assertThat(taskCaptor.getValue()).isEqualTo(mockTaskAdapter);
        assertThat(throwableCaptor.getValue())
                .isExactlyInstanceOf(FileDownloadOutOfSpaceException.class);
    }

    @Test
    public void handleError_withOtherException() {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        when(mockTaskAdapter.getRetryAssist()).thenReturn(null);
        final Exception exception = mock(Exception.class);

        compatListenerAssist.handleError(mockTaskAdapter, exception);
        verify(callback, never()).retry(
                any(DownloadTaskAdapter.class), any(Throwable.class), anyInt(), anyLong());
        verify(callback).error(any(DownloadTaskAdapter.class), any(Throwable.class));
    }

    @Test
    public void onTaskFinish() {
        final FileDownloadList fileDownloadList = spy(new FileDownloadList());
        FileDownloadList.setSingleton(fileDownloadList);
        final List<BaseDownloadTask.FinishListener> finishListeners = new ArrayList<>();
        final BaseDownloadTask.FinishListener mockFinishListener =
                mock(BaseDownloadTask.FinishListener.class);
        finishListeners.add(mockFinishListener);
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        when(mockTaskAdapter.getFinishListeners()).thenReturn(finishListeners);

        compatListenerAssist.onTaskFinish(mockTaskAdapter);

        verify(mockFinishListener).over(mockTaskAdapter);
        verify(fileDownloadList).remove(mockTaskAdapter);
    }

    @Test
    public void handleComplete_syncCallback() throws Throwable {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final DownloadTask mockTask = mock(DownloadTask.class);
        when(mockTaskAdapter.getDownloadTask()).thenReturn(mockTask);
        when(mockTask.isAutoCallbackToUIThread()).thenReturn(false);

        compatListenerAssist.taskConnected.set(true);
        compatListenerAssist.handleComplete(mockTaskAdapter);

        verify(callback).blockComplete(mockTaskAdapter);
        verify(callback).completed(mockTaskAdapter);
        assertThat(compatListenerAssist.isReuseOldFile()).isFalse();
    }

    @Test
    public void handleComplete_syncCallback_error() throws Throwable {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        final DownloadTask mockTask = mock(DownloadTask.class);
        final Exception mockException =  mock(Exception.class);
        when(mockTaskAdapter.getDownloadTask()).thenReturn(mockTask);
        when(mockTask.isAutoCallbackToUIThread()).thenReturn(false);
        doNothing().when(compatListenerAssist)
                .handleError(any(DownloadTaskAdapter.class), any(Exception.class));
        doThrow(mockException).when(callback).blockComplete(mockTaskAdapter);

        compatListenerAssist.taskConnected.set(false);
        compatListenerAssist.handleComplete(mockTaskAdapter);

        verify(callback, never()).completed(mockTaskAdapter);
        verify(compatListenerAssist).handleError(
                any(DownloadTaskAdapter.class), any(Exception.class));
        assertThat(compatListenerAssist.isReuseOldFile()).isTrue();
    }

    @Test
    public void handleBlockComplete() throws Throwable {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                final Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        }).when(uiHander).post(any(Runnable.class));

        compatListenerAssist.handleBlockComplete(mockTaskAdapter);

        verify(callback).blockComplete(mockTaskAdapter);
        verify(callback).completed(mockTaskAdapter);
    }

    @Test
    public void handleBlockComplete_error() throws Throwable {
        final DownloadTaskAdapter mockTaskAdapter = mock(DownloadTaskAdapter.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                final Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        }).when(uiHander).post(any(Runnable.class));
        doThrow(mock(Exception.class)).when(callback).blockComplete(mockTaskAdapter);
        doNothing().when(compatListenerAssist)
                .handleError(any(DownloadTaskAdapter.class), any(Exception.class));

        compatListenerAssist.handleBlockComplete(mockTaskAdapter);

        verify(compatListenerAssist)
                .handleError(any(DownloadTaskAdapter.class), any(Exception.class));
        verify(callback, never()).completed(mockTaskAdapter);
    }
}
