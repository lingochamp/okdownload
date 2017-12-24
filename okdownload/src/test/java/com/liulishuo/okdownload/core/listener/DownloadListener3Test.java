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

package com.liulishuo.okdownload.core.listener;

import android.support.annotation.NonNull;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class DownloadListener3Test {
    private DownloadListener3 listener3;

    @Mock private DownloadTask task;
    @Mock private Exception realCause;

    @Before
    public void setup() {
        initMocks(this);

        listener3 = spy(new DownloadListener3() {
            @Override protected void started(DownloadTask task) {
            }

            @Override protected void completed(DownloadTask task) {
            }

            @Override protected void canceled(DownloadTask task) {
            }

            @Override protected void error(DownloadTask task, Exception e) {
            }

            @Override protected void warn(DownloadTask task) {
            }

            @Override
            public void connected(DownloadTask task, int blockCount, long currentOffset,
                                     long totalLength) {
            }

            @Override public void progress(DownloadTask task, long currentOffset) {
            }

            @Override public void retry(DownloadTask task, @NonNull ResumeFailedCause cause) {
            }
        });
    }

    @Test
    public void end() {
        listener3.taskStart(task);
        verify(listener3).started(eq(task));


        listener3.taskEnd(task, EndCause.COMPLETE, realCause);
        verify(listener3).completed(eq(task));

        listener3.taskEnd(task, EndCause.CANCELED, realCause);
        verify(listener3).canceled(eq(task));

        listener3.taskEnd(task, EndCause.ERROR, realCause);
        verify(listener3).error(eq(task), eq(realCause));

        listener3.taskEnd(task, EndCause.PRE_ALLOCATE_FAILED, realCause);
        verify(listener3, times(2)).error(eq(task), eq(realCause));

        listener3.taskEnd(task, EndCause.FILE_BUSY, realCause);
        verify(listener3).warn(eq(task));

        listener3.taskEnd(task, EndCause.SAME_TASK_BUSY, realCause);
        verify(listener3, times(2)).warn(eq(task));
    }
}