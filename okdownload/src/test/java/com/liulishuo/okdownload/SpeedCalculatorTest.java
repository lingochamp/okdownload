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

package com.liulishuo.okdownload;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class SpeedCalculatorTest {

    private SpeedCalculator calculator;

    @Before
    public void setup() {
        calculator = spy(new SpeedCalculator());
    }

    @Test
    public void calculate() throws InterruptedException {
        long now = 66;
        long firstTimestamp = now;
        doReturn(now).when(calculator).nowMillis();

        calculator.downloading(10);

        assertThat(calculator.timestamp).isEqualTo(firstTimestamp);
        assertThat(calculator.beginTimestamp).isEqualTo(firstTimestamp);
        assertThat(calculator.increaseBytes).isEqualTo(10);
        assertThat(calculator.allIncreaseBytes).isEqualTo(10);

        calculator.downloading(20);

        assertThat(calculator.timestamp).isEqualTo(now);
        assertThat(calculator.beginTimestamp).isEqualTo(now);
        assertThat(calculator.increaseBytes).isEqualTo(30);
        assertThat(calculator.allIncreaseBytes).isEqualTo(30);

        // 30/1*1000
        assertThat(calculator.getInstantBytesPerSecondAndFlush()).isEqualTo(30000);

        calculator.downloading(60);

        now = 86;
        doReturn(now).when(calculator).nowMillis();

        // 60/(86-66)*1000
        assertThat(calculator.getInstantBytesPerSecondAndFlush()).isEqualTo(3000);
        assertThat(calculator.timestamp).isEqualTo(now);
        assertThat(calculator.beginTimestamp).isEqualTo(firstTimestamp);

        now = 96;
        doReturn(now).when(calculator).nowMillis();

        calculator.downloading(10);
        calculator.endTask();

        // 10/(96-86)*1000
        assertThat(calculator.getInstantBytesPerSecondAndFlush()).isEqualTo(1000);
        //(10+20+60+10)
        assertThat(calculator.allIncreaseBytes).isEqualTo(100);
        assertThat(calculator.endTimestamp).isEqualTo(96);
        assertThat(calculator.beginTimestamp).isEqualTo(firstTimestamp);
        // (10+20+60+10)/(96-66)*1000
        assertThat(calculator.getBytesPerSecondFromBegin()).isEqualTo(3333);

        calculator.reset();
        assertThat(calculator.getBytesPerSecondFromBegin()).isEqualTo(0);
    }

    @Test
    public void speed() {
        long now = 66;
        doReturn(now).when(calculator).nowMillis();

        calculator.downloading(1000);

        now = 166;
        doReturn(now).when(calculator).nowMillis();
        assertThat(calculator.speed()).isEqualTo("10.0 kB/s");

        now = 1066;
        doReturn(now).when(calculator).nowMillis();
        // because than 1 second and last speed is 10.0
        assertThat(calculator.speed()).isEqualTo("10.0 kB/s");
    }
}
