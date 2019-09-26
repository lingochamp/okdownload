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

package com.liulishuo.okdownload.core.exception;

import com.liulishuo.okdownload.core.NamedRunnable;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class InterruptExceptionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void printStackTrace() {
        assertThat(InterruptException.SIGNAL.getMessage()).isEqualTo("Interrupted");

        thrown.expect(IllegalAccessError.class);
        thrown.expectMessage("Stack is ignored for signal");
        InterruptException.SIGNAL.printStackTrace();
    }

    @Test
    public void testInterruptedStatus() {
        final CountDownLatch latch = new CountDownLatch(1);
        final Thread r1 = new Thread(new NamedRunnable("test runnable") {
            @Override
            protected void execute() throws InterruptedException {
                latch.countDown();
                Thread.sleep(100);
            }

            @Override
            protected void interrupted(InterruptedException e) {
            }

            @Override
            protected void finished() {
                Assert.assertTrue(Thread.currentThread().isInterrupted());
            }
        });
        r1.start();
        try {
            latch.await(100, TimeUnit.MILLISECONDS);
            r1.interrupt();
            r1.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
