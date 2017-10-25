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

package cn.dreamtobe.okdownload;

import android.os.SystemClock;

import cn.dreamtobe.okdownload.core.Util;

public class SpeedCalculator {

    long timestamp;
    long increaseBytes;

    private long bytesPerSecond;

    long beginTimestamp;
    long endTimestamp;
    long allIncreaseBytes;

    // convenience for unit test
    long nowMillis() {
        return SystemClock.uptimeMillis();
    }

    public synchronized void downloading(long increaseBytes) {
        if (timestamp == 0) {
            this.timestamp = nowMillis();
            this.beginTimestamp = timestamp;
        }

        this.increaseBytes += increaseBytes;
        this.allIncreaseBytes += increaseBytes;
    }

    public synchronized void flush() {
        final long nowMillis = nowMillis();
        final long sinceNowIncreaseBytes = increaseBytes;
        final long durationMillis = Math.max(1, nowMillis - timestamp);

        increaseBytes = 0;
        timestamp = nowMillis;

        // precision loss
        bytesPerSecond = (long) ((float) sinceNowIncreaseBytes / durationMillis * 1000f);
    }

    public long getBytesPerSecondAndFlush() {
        flush();
        return bytesPerSecond;
    }

    public synchronized long getBytesPerSecondFromBegin() {
        final long endTimestamp = this.endTimestamp == 0 ? nowMillis() : this.endTimestamp;
        final long sinceNowIncreaseBytes = allIncreaseBytes;
        final long durationMillis = Math.max(1, endTimestamp - beginTimestamp);

        // precision loss
        return (long) ((float) sinceNowIncreaseBytes / durationMillis * 1000f);
    }

    public void endTask() {
        endTimestamp = nowMillis();
    }

    public String speed() {
        return getSpeedWithSIAndFlush();
    }


    /**
     * With wikipedia: https://en.wikipedia.org/wiki/Kibibyte
     * <p>
     * 1KiB = 2^10B = 1024B
     * 1MiB = 2^10KB = 1024KB
     */
    public String getSpeedWithBinaryAndFlush() {
        return humanReadableSpeed(getBytesPerSecondAndFlush(), false);
    }

    /**
     * With wikipedia: https://en.wikipedia.org/wiki/Kilobyte
     * <p>
     * 1KB = 1000B
     * 1MB = 1000KB
     */
    public String getSpeedWithSIAndFlush() {
        return humanReadableSpeed(getBytesPerSecondAndFlush(), true);
    }

    public String speedFromBegin() {
        return humanReadableSpeed(getBytesPerSecondFromBegin(), true);
    }

    private static String humanReadableSpeed(long bytes, boolean si) {
        return Util.humanReadableBytes(bytes, si) + "/s";
    }
}
