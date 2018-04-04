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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class NamedRunnableTest {

    private NamedRunnable runnable;

    @Before
    public void setup() {
        String name = "name";
        runnable = spy(new NamedRunnable(name) {
            @Override protected void execute() {
            }

            @Override protected void canceled(InterruptedException e) {
            }

            @Override protected void finished() {
            }
        });
    }

    @Test
    public void run_nonInterrupt() throws InterruptedException {
        Thread.currentThread().setName("oldName");
        runnable.run();

        verify(runnable).execute();
        verify(runnable, never()).canceled(any(InterruptedException.class));
        verify(runnable).finished();
        assertThat(Thread.currentThread().getName()).isEqualTo("oldName");
    }

    @Test
    public void run_interrupt() throws InterruptedException {
        doThrow(InterruptedException.class).when(runnable).execute();

        runnable.run();
        verify(runnable).execute();
        verify(runnable).canceled(any(InterruptedException.class));
        verify(runnable).finished();
    }
}