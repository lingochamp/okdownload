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

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ServerCanceledExceptionTest {

    @Test
    public void construct() {
        int responseCode = 1;
        long offset = 2;

        ServerCanceledException exception = new ServerCanceledException(responseCode, offset);
        assertThat(exception.getMessage())
                .isEqualTo("Response code can't handled on internal " + responseCode
                        + " with current offset "
                        + offset);
        assertThat(exception.getResponseCode()).isEqualTo(responseCode);
    }
}