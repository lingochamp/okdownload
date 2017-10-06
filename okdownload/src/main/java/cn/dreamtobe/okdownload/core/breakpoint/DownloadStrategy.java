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

package cn.dreamtobe.okdownload.core.breakpoint;

import java.io.File;
import java.util.List;
import java.util.Map;

import cn.dreamtobe.okdownload.task.DownloadTask;

public class DownloadStrategy {

    // 1 connection: [0, 1MB)
    private final static long ONE_CONNECTION_UPPER_LIMIT = 1024 * 1024; // 1MB
    // 2 connection: [1MB, 5MB)
    private final static long TWO_CONNECTION_UPPER_LIMIT = 5 * 1024 * 1024; // 5MB
    // 3 connection: [5MB, 50MB)
    private final static long THREE_CONNECTION_UPPER_LIMIT = 50 * 1024 * 1024; // 50MB
    // 4 connection: [50MB, 100MB)
    private final static long FOUR_CONNECTION_UPPER_LIMIT = 100 * 1024 * 1024; // 100MB

    public boolean isAvailable(DownloadTask task, BreakpointInfo info) {
        return info.getBlockCount() > 0 && new File(task.getUri().getPath()).exists();
    }

    public int determineBlockCount(DownloadTask task, long totalLength, Map<String, List<String>> responseHeaderFields) {
        if (totalLength < ONE_CONNECTION_UPPER_LIMIT) {
            return 1;
        }

        if (totalLength < TWO_CONNECTION_UPPER_LIMIT) {
            return 2;
        }

        if (totalLength < THREE_CONNECTION_UPPER_LIMIT) {
            return 3;
        }

        if (totalLength < FOUR_CONNECTION_UPPER_LIMIT) {
            return 4;
        }

        return 5;
    }
}
