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

package cn.dreamtobe.okdownload.core.exception;

import java.io.IOException;

public class PreAllocateException extends IOException {
    private final long requireSpace;
    private final long freeSpace;

    public PreAllocateException(long requireSpace, long freeSpace) {
        super("There is Free space less than Require space: " + freeSpace + " < " + requireSpace);
        this.requireSpace = requireSpace;
        this.freeSpace = freeSpace;
    }

    public long getRequireSpace() {
        return requireSpace;
    }

    public long getFreeSpace() {
        return freeSpace;
    }
}
