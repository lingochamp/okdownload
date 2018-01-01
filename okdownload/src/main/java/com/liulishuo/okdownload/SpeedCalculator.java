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

import android.os.SystemClock;

import com.liulishuo.okdownload.core.Util;

public class SpeedCalculator {

    long timestamp;
    long increaseBytes;

    private long bytesPerSecond;

    long beginTimestamp;
    long endTimestamp;
    long allIncreaseBytes;

    public void reset() {
        timestamp = 0;
        increaseBytes = 0;
        bytesPerSecond = 0;

        beginTimestamp = 0;
        endTimestamp = 0;
        allIncreaseBytes = 0;
    }

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

    /**
     * Get instant bytes per-second.
     */
    public long getInstantBytesPerSecondAndFlush() {
        flush();
        return bytesPerSecond;
    }

    /**
     * Get bytes per-second and only if duration is greater than or equal to 1 second will flush and
     * re-calculate speed.
     */
    public long getBytesPerSecondAndFlush() {
        if (nowMillis() - timestamp < 1000) return bytesPerSecond;

        return getInstantBytesPerSecondAndFlush();
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

    /**
     * Get instant speed
     */
    public String instantSpeed() {
        return getSpeedWithSIAndFlush();
    }

    /**
     * Get speed with at least one second duration.
     */
    public String speed() {
        return humanReadableSpeed(getBytesPerSecondAndFlush(), true);
    }

    /**
     * Get last time calculated speed.
     */
    public String lastSpeed() {
        return humanReadableSpeed(bytesPerSecond, true);
    }

    public long getInstantSpeedDurationMillis() {
        return nowMillis() - timestamp;
    }


    /**
     * With wikipedia: https://en.wikipedia.org/wiki/Kibibyte
     * <p>
     * 1KiB = 2^10B = 1024B
     * 1MiB = 2^10KB = 1024KB
     */
    public String getSpeedWithBinaryAndFlush() {
        return humanReadableSpeed(getInstantBytesPerSecondAndFlush(), false);
    }

    /**
     * With wikipedia: https://en.wikipedia.org/wiki/Kilobyte
     * <p>
     * 1KB = 1000B
     * 1MB = 1000KB
     */
    public String getSpeedWithSIAndFlush() {
        return humanReadableSpeed(getInstantBytesPerSecondAndFlush(), true);
    }

    public String averageSpeed() {
        return speedFromBegin();
    }

    public String speedFromBegin() {
        return humanReadableSpeed(getBytesPerSecondFromBegin(), true);
    }

    private static String humanReadableSpeed(long bytes, boolean si) {
        return Util.humanReadableBytes(bytes, si) + "/s";
    }
}
