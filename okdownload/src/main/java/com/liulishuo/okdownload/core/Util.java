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

package com.liulishuo.okdownload.core;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStoreOnCache;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.connection.DownloadUrlConnection;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    // request method
    public static final String METHOD_HEAD = "HEAD";

    // request header fields.
    public static final String RANGE = "Range";
    public static final String IF_MATCH = "If-Match";

    // response header fields.
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_RANGE = "Content-Range";
    public static final String ETAG = "Etag";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    // response header value.
    public static final String VALUE_CHUNKED = "chunked";
    public static final int CHUNKED_CONTENT_LENGTH = -1;

    public interface Logger {
        void e(String tag, String msg, Exception e);

        void w(String tag, String msg);

        void d(String tag, String msg);

        void i(String tag, String msg);
    }

    public static class EmptyLogger implements Logger {
        @Override public void e(String tag, String msg, Exception e) { }

        @Override public void w(String tag, String msg) { }

        @Override public void d(String tag, String msg) { }

        @Override public void i(String tag, String msg) { }
    }

    private static Logger logger = new EmptyLogger();

    /**
     * Enable logger used for okdownload, and print each log with {@link Log}.
     */
    public static void enableConsoleLog() {
        logger = null;
    }

    /**
     * Set the logger which using on okdownload.
     * default one is {@link EmptyLogger}.
     *
     * @param l if provide logger is {@code null} we will using {@link Log} as default.
     */
    public static void setLogger(@Nullable Logger l) {
        logger = l;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void e(String tag, String msg, Exception e) {
        if (logger != null) {
            logger.e(tag, msg, e);
            return;
        }

        Log.e(tag, msg, e);
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

    public static void i(String tag, String msg) {
        if (logger != null) {
            logger.i(tag, msg);
            return;
        }

        Log.i(tag, msg);
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

    public static void resetBlockIfDirty(BlockInfo info) {
        boolean isDirty = false;

        if (info.getCurrentOffset() < 0) {
            isDirty = true;
        } else if (info.getCurrentOffset() > info.getContentLength()) {
            isDirty = true;
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

    public static @NonNull DownloadStore createDefaultDatabase(Context context) {
        // You can import through com.liulishuo.okdownload:sqlite:{version}
        final String storeOnSqliteClassName
                = "com.liulishuo.okdownload.core.breakpoint.BreakpointStoreOnSQLite";

        try {
            final Constructor constructor = Class.forName(storeOnSqliteClassName)
                    .getDeclaredConstructor(Context.class);
            return (DownloadStore) constructor.newInstance(context);
        } catch (ClassNotFoundException ignored) {
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException ignored) {
        }

        return new BreakpointStoreOnCache();
    }

    public static @NonNull DownloadStore createRemitDatabase(DownloadStore originStore) {
        try {
            final Method createRemitSelf = originStore.getClass()
                    .getMethod("createRemitSelf");
            return (DownloadStore) createRemitSelf.invoke(originStore);
        } catch (IllegalAccessException ignored) {
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException ignored) {
        }

        return originStore;
    }

    public static @NonNull DownloadConnection.Factory createDefaultConnectionFactory() {
        final String okhttpConnectionClassName
                = "com.liulishuo.okdownload.core.connection.DownloadOkHttp3Connection$Factory";
        try {
            final Constructor constructor = Class.forName(okhttpConnectionClassName)
                    .getDeclaredConstructor();
            return (DownloadConnection.Factory) constructor.newInstance();
        } catch (ClassNotFoundException ignored) {
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException ignored) {
        }

        return new DownloadUrlConnection.Factory();
    }

    public static void assembleBlock(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                     long instanceLength,
                                     boolean isAcceptRange) {
        final int blockCount;
        if (OkDownload.with().downloadStrategy().isUseMultiBlock(isAcceptRange)) {
            blockCount = OkDownload.with().downloadStrategy()
                    .determineBlockCount(task, instanceLength);
        } else {
            blockCount = 1;
        }

        info.resetBlockInfos();
        final long eachLength = instanceLength / blockCount;
        long startOffset = 0;
        long contentLength = 0;
        for (int i = 0; i < blockCount; i++) {
            startOffset = startOffset + contentLength;
            if (i == 0) {
                // first block
                final long remainLength = instanceLength % blockCount;
                contentLength = eachLength + remainLength;
            } else {
                contentLength = eachLength;
            }

            final BlockInfo blockInfo = new BlockInfo(startOffset, contentLength);
            info.addBlock(blockInfo);
        }
    }

    public static long parseContentLength(@Nullable String contentLength) {
        if (contentLength == null) return CHUNKED_CONTENT_LENGTH;

        return Long.parseLong(contentLength);
    }

    public static boolean isNetworkNotOnWifiType() {
        final ConnectivityManager manager = (ConnectivityManager) OkDownload.with().context()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            Util.w("Util", "failed to get connectivity manager!");
            return true;
        }

        //noinspection MissingPermission, because we check permission accessable when invoked
        @SuppressLint("MissingPermission") final NetworkInfo info = manager.getActiveNetworkInfo();

        return info == null || info.getType() != ConnectivityManager.TYPE_WIFI;
    }

    public static boolean checkPermission(String permission) {
        final int perm = OkDownload.with().context().checkCallingOrSelfPermission(permission);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    public static long parseContentLengthFromContentRange(@Nullable String contentRange) {
        if (contentRange == null || contentRange.length() == 0) return CHUNKED_CONTENT_LENGTH;
        final String pattern = "bytes (\\d+)-(\\d+)/\\d+";
        try {
            final Pattern r = Pattern.compile(pattern);
            final Matcher m = r.matcher(contentRange);
            if (m.find()) {
                final long rangeStart = Long.parseLong(m.group(1));
                final long rangeEnd = Long.parseLong(m.group(2));
                return rangeEnd - rangeStart + 1;
            }
        } catch (Exception e) {
            Util.w("Util", "parse content-length from content-range failed " + e);
        }
        return CHUNKED_CONTENT_LENGTH;
    }

    public static boolean isUriContentScheme(@NonNull Uri uri) {
        return uri.getScheme().equals(ContentResolver.SCHEME_CONTENT);
    }

    public static boolean isUriFileScheme(@NonNull Uri uri) {
        return uri.getScheme().equals(ContentResolver.SCHEME_FILE);
    }

    @Nullable public static String getFilenameFromContentUri(@NonNull Uri contentUri) {
        final ContentResolver resolver = OkDownload.with().context().getContentResolver();
        final Cursor cursor = resolver.query(contentUri, null, null, null, null);
        if (cursor != null) {
            try {
                cursor.moveToFirst();
                return cursor
                        .getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    @NonNull public static File getParentFile(final File file) {
        final File candidate = file.getParentFile();
        return candidate == null ? new File("/") : candidate;
    }
}
