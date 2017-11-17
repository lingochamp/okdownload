/*
 * Copyright (C) 2017 Jacksgong(jacksgong.com)
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

import android.support.annotation.NonNull;

import java.io.IOException;

import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.exception.ResumeFailedException;
import com.liulishuo.okdownload.core.file.MultiPointOutputStream;

public class DownloadCache {
    private String redirectLocation;
    private final MultiPointOutputStream outputStream;

    private volatile boolean isPreconditionFailed;
    private volatile boolean isUserCanceled;
    private volatile boolean isServerCanceled;
    private volatile boolean isUnknownError;
    private volatile boolean isFileBusyAfterRun;
    private volatile boolean isPreAllocateFailed;
    private volatile IOException realCause;

    DownloadCache(@NonNull MultiPointOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    private DownloadCache() {
        this.outputStream = null;
    }

    @NonNull MultiPointOutputStream getOutputStream() {
        if (outputStream == null) throw new IllegalArgumentException();
        return outputStream;
    }

    void setRedirectLocation(String redirectLocation) {
        this.redirectLocation = redirectLocation;
    }

    String getRedirectLocation() {
        return redirectLocation;
    }

    boolean isPreconditionFailed() {
        return isPreconditionFailed;
    }

    public boolean isUserCanceled() {
        return isUserCanceled;
    }

    boolean isServerCanceled() {
        return isServerCanceled;
    }

    boolean isUnknownError() {
        return isUnknownError;
    }

    boolean isFileBusyAfterRun() {
        return isFileBusyAfterRun;
    }

    public boolean isPreAllocateFailed() {
        return isPreAllocateFailed;
    }

    IOException getRealCause() {
        return realCause;
    }

    ResumeFailedCause getResumeFailedCause() {
        return ((ResumeFailedException) realCause).getResumeFailedCause();
    }

    public boolean isInterrupt() {
        return isPreconditionFailed || isUserCanceled || isServerCanceled || isUnknownError
                || isFileBusyAfterRun || isPreAllocateFailed;
    }

    public void setPreconditionFailed(IOException realCause) {
        this.isPreconditionFailed = true;
        this.realCause = realCause;
    }

    void setUserCanceled() {
        this.isUserCanceled = true;
    }

    public void setFileBusyAfterRun() {
        this.isFileBusyAfterRun = true;
    }

    public void setServerCanceled(IOException realCause) {
        this.isServerCanceled = true;
        this.realCause = realCause;
    }

    public void setUnknownError(IOException realCause) {
        this.isUnknownError = true;
        this.realCause = realCause;
    }

    public void setPreAllocateFailed(IOException realCause) {
        this.isPreAllocateFailed = true;
        this.realCause = realCause;
    }

    static class PreError extends DownloadCache {
        PreError(IOException realCause) {
            super(null);
            setUnknownError(realCause);
        }
    }
}