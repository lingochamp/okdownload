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

import com.liulishuo.okdownload.core.exception.FileBusyAfterRunException;
import com.liulishuo.okdownload.core.exception.PreAllocateException;
import com.liulishuo.okdownload.core.exception.ResumeFailedException;
import com.liulishuo.okdownload.core.exception.ServerCanceledException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DownloadCacheTest {
    private DownloadCache cache;
    @Mock private MultiPointOutputStream outputStream;
    @Mock private IOException realCause;

    @Before
    public void setup() {
        initMocks(this);

        cache = spy(new DownloadCache(outputStream));
    }

    @Test
    public void getOutputStream() throws Exception {
        assertThat(cache.getOutputStream()).isEqualTo(outputStream);
    }

    @Test
    public void setRedirectLocation() throws Exception {
        final String redirectLocation = "redirectLocation";
        cache.setRedirectLocation(redirectLocation);
        assertThat(cache.getRedirectLocation()).isEqualTo(redirectLocation);
    }

    @Test
    public void setPreconditionFailed() throws Exception {
        assertThat(cache.isInterrupt()).isFalse();
        cache.setPreconditionFailed(realCause);
        assertThat(cache.getRealCause()).isEqualTo(realCause);
        assertThat(cache.isInterrupt()).isTrue();
    }

    @Test
    public void setUserCanceled() throws Exception {
        assertThat(cache.isInterrupt()).isFalse();
        assertThat(cache.isUserCanceled()).isFalse();
        cache.setUserCanceled();
        assertThat(cache.isUserCanceled()).isTrue();
        assertThat(cache.isInterrupt()).isTrue();
    }

    @Test
    public void setFileBusyAfterRun() throws Exception {
        assertThat(cache.isInterrupt()).isFalse();
        cache.setFileBusyAfterRun();
        assertThat(cache.isInterrupt()).isTrue();
    }

    @Test
    public void setServerCanceled() throws Exception {
        assertThat(cache.isInterrupt()).isFalse();
        cache.setServerCanceled(realCause);
        assertThat(cache.isInterrupt()).isTrue();
    }

    @Test
    public void setUnknownError() throws Exception {
        assertThat(cache.isInterrupt()).isFalse();
        cache.setUnknownError(realCause);
        assertThat(cache.isInterrupt()).isTrue();
    }

    @Test
    public void setPreAllocateFailed() throws Exception {
        assertThat(cache.isInterrupt()).isFalse();
        cache.setPreAllocateFailed(realCause);
        assertThat(cache.isInterrupt()).isTrue();
    }

    @Test
    public void catchException_userCanceled() {
        when(cache.isUserCanceled()).thenReturn(true);
        final IOException ioException = mock(IOException.class);

        cache.catchException(ioException);

        verify(cache, never()).setUnknownError(ioException);
    }

    @Test
    public void catchException_ResumeFailed() {
        final ResumeFailedException exception = mock(ResumeFailedException.class);

        cache.catchException(exception);

        verify(cache).setPreconditionFailed(eq(exception));
    }

    @Test
    public void catchException_ServerCanceled() {
        final ServerCanceledException exception = mock(ServerCanceledException.class);

        cache.catchException(exception);

        verify(cache).setServerCanceled(eq(exception));
    }

    @Test
    public void catchException_fileBusy() throws IOException {
        final FileBusyAfterRunException exception = FileBusyAfterRunException.SIGNAL;

        cache.catchException(exception);

        verify(cache).setFileBusyAfterRun();
    }

    @Test
    public void catchException_preAllocateFailed() throws IOException {
        final PreAllocateException exception = mock(PreAllocateException.class);

        cache.catchException(exception);

        verify(cache).setPreAllocateFailed(eq(exception));
    }

    @Test
    public void catchException_othersException() throws IOException {
        final IOException exception = mock(IOException.class);

        cache.catchException(exception);

        verify(cache).setUnknownError(eq(exception));
    }

}