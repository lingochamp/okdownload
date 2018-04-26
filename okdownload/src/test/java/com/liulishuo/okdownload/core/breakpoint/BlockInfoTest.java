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

package com.liulishuo.okdownload.core.breakpoint;

import org.junit.Before;
import org.junit.Test;

import static com.liulishuo.okdownload.core.Util.CHUNKED_CONTENT_LENGTH;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class BlockInfoTest {

    private BlockInfo info;

    @Before
    public void setup() {
        info = new BlockInfo(0, 1000);
    }

    @Test
    public void increase() {
        info.increaseCurrentOffset(123);
        assertThat(info.getCurrentOffset()).isEqualTo(123);
    }

    @Test
    public void copyNotClone() {
        info.increaseCurrentOffset(1);
        final BlockInfo copy = info.copy();
        copy.increaseCurrentOffset(1);

        assertThat(info.getCurrentOffset()).isEqualTo(1);
        assertThat(copy.getCurrentOffset()).isEqualTo(2);
    }

    @Test
    public void getRangeRight() {
        BlockInfo info = new BlockInfo(0, 3, 1);
        assertThat(info.getRangeRight()).isEqualTo(2);

        info = new BlockInfo(12, 6, 2);
        assertThat(info.getRangeRight()).isEqualTo(17);
    }

    @Test
    public void chunked() {
        BlockInfo info = new BlockInfo(0, CHUNKED_CONTENT_LENGTH);
        assertThat(info.getContentLength()).isEqualTo(CHUNKED_CONTENT_LENGTH);
    }
}