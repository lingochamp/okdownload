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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class PreAllocateExceptionTest {

    private PreAllocateException exception;
    private long freeSpace = 1;
    private long requireSpace = 2;

    @Before
    public void setup() {
        exception = new PreAllocateException(requireSpace, freeSpace);
    }

    @Test
    public void construct() {
        assertThat(exception.getMessage()).isEqualTo(
                "There is Free space less than Require space: " + freeSpace + " < " + requireSpace);
        assertThat(exception.getRequireSpace()).isEqualTo(requireSpace);
        assertThat(exception.getFreeSpace()).isEqualTo(freeSpace);
    }
}