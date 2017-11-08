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

package cn.dreamtobe.okdownload.core;

import android.os.Build;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;

import cn.dreamtobe.okdownload.core.breakpoint.BlockInfo;

public class Util {

    public interface Logger {
        void w(String tag, String msg);

        void d(String tag, String msg);
    }

    private static Logger logger;

    public static void setLogger(Logger l) {
        logger = l;
    }

    public static void w(String tag, String msg) {
        if (logger != null) {
            logger.w(tag, msg);
            return;
        }

        Log.w(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (logger != null) {
            logger.d(tag, msg);
            return;
        }

        Log.d(tag, msg);
    }

    // For avoid mock whole android framework methods on unit-test.
    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable runnable) {
                final Thread result = new Thread(runnable, name);
                result.setDaemon(daemon);
                return result;
            }
        };
    }

    @Nullable
    public static String md5(String string) {
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ignored) {
        }

        if (hash != null) {
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                if ((b & 0xFF) < 0x10) hex.append("0");
                hex.append(Integer.toHexString(b & 0xFF));
            }
            return hex.toString();
        }

        return null;
    }

    public static boolean isCorrectFull(long fetchedLength, long contentLength) {
        return fetchedLength == contentLength;
    }

    public static boolean isFirstBlockMeetLenienceFull(long fetchedLength,
                                                       long contentLength) {
        return fetchedLength >= contentLength;
    }

    public static boolean isBlockComplete(int blockIndex, int blockCount, BlockInfo info) {
        if (blockCount == 1) {
            return isCorrectFull(info.getCurrentOffset(), info.getContentLength());
        } else {
            if (blockIndex == 0) {
                // first block
                return isFirstBlockMeetLenienceFull(info.getCurrentOffset(),
                        info.getContentLength());
            } else {
                return isCorrectFull(info.getCurrentOffset(), info.getContentLength());
            }
        }
    }

    public static void resetBlockIfDirty(int blockIndex, int blockCount, long totalLength,
                                         BlockInfo info) {
        boolean isDirty = false;

        if (info.getCurrentOffset() < 0) {
            isDirty = true;
        } else if (blockCount == 1) {
            if (info.getCurrentOffset() > info.getContentLength()) isDirty = true;
        } else {
            if (blockIndex == 0) {
                // first block
                if (info.getCurrentOffset() > totalLength) isDirty = true;
            } else if (blockIndex < blockCount - 1) {
                // middle blocks
                if (info.getCurrentOffset() > info.getContentLength()) isDirty = true;
            } else {
                // last block
                if (info.getCurrentOffset() > info.getContentLength()) isDirty = true;
            }
        }

        if (isDirty) {
            w("resetBlockIfDirty", "block is dirty so have to reset: " + info);
            info.resetBlock();
        }
    }

    public static long getFreeSpaceBytes(final String path) {
        // NEED CHECK PERMISSION?
        long freeSpaceBytes;
        final StatFs statFs = new StatFs(path);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            freeSpaceBytes = statFs.getAvailableBytes();
        } else {
            //noinspection deprecation
            freeSpaceBytes = statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
        }

        return freeSpaceBytes;
    }

    /**
     * @param si whether using SI unit refer to International System of Units.
     */
    public static String humanReadableBytes(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
