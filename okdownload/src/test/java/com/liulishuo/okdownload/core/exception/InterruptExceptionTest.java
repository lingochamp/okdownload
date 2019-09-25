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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
        Thread r1 = new Thread(new NamedRunnable("test runnable") {
            @Override
            protected void execute() throws InterruptedException {
                Thread.sleep(1000);
            }

            @Override
            protected void interrupted(InterruptedException e) {
            }

            @Override
            protected void finished() {
                if (!Thread.currentThread().isInterrupted()) {
                    assertThat("").isEqualTo("Failed in get interrupted status");
                }
            }
        });
        r1.start();
        try {
            Thread.sleep(100);
            r1.interrupt();
            r1.join();
        } catch (Exception e) {
            assertThat("").isEqualTo("Failed in unknown exception");
            e.printStackTrace();
        }
    }
}
