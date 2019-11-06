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

package com.liulishuo.filedownloader.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.liulishuo.filedownloader.DownloadTaskAdapter;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.Util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The utils for FileDownloader.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class FileDownloadUtils {

    private static String defaultSaveRootPath;

    private static final String TAG = "FileDownloadUtils";
    private static final String FILEDOWNLOADER_PREFIX = "FileDownloader";
    // note on https://tools.ietf.org/html/rfc5987
    private static final Pattern CONTENT_DISPOSITION_WITH_ASTERISK_PATTERN =
            Pattern.compile("attachment;\\s*filename\\*\\s*=\\s*\"*([^\"]*)'\\S*'([^\"]*)\"*");
    // note on http://www.ietf.org/rfc/rfc1806.txt
    private static final Pattern CONTENT_DISPOSITION_WITHOUT_ASTERISK_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*\"*([^\"\\n]*)\"*");

    public static String getDefaultSaveRootPath() {
        if (!TextUtils.isEmpty(defaultSaveRootPath)) {
            return defaultSaveRootPath;
        }

        if (FileDownloadHelper.getAppContext().getExternalCacheDir() == null) {
            return Environment.getDownloadCacheDirectory().getAbsolutePath();
        } else {
            //noinspection ConstantConditions
            return FileDownloadHelper.getAppContext().getExternalCacheDir().getAbsolutePath();
        }
    }

    /**
     * The path is used as the default directory in the case of the task without set path.
     *
     * @param path default root path for save download file.
     * @see com.liulishuo.filedownloader.BaseDownloadTask#setPath(String, boolean)
     */
    public static void setDefaultSaveRootPath(final String path) {
        defaultSaveRootPath = path;
    }

    public static String getDefaultSaveFilePath(final String url) {
        return generateFilePath(getDefaultSaveRootPath(), generateFileName(url));
    }

    public static String generateFileName(final String url) {
        return md5(url);
    }

    public static String generateFilePath(String directory, String filename) {
        if (filename == null) {
            throw new IllegalStateException("can't generate real path, the file name is null");
        }

        if (directory == null) {
            throw new IllegalStateException("can't generate real path, the directory is null");
        }

        return String.format("%s%s%s", directory, File.separator, filename);
    }

    public static String md5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append(0);
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    @Nullable
    public static DownloadTaskAdapter findDownloadTaskAdapter(DownloadTask downloadTask) {
        if (downloadTask == null) {
            Util.w(TAG, "download task is null when find DownloadTaskAdapter");
            return null;
        }
        final Object o = downloadTask.getTag(DownloadTaskAdapter.KEY_TASK_ADAPTER);
        if (o == null) {
            Util.w(TAG, "no tag with DownloadTaskAdapter.KEY_TASK_ADAPTER");
            return null;
        }
        if (o instanceof DownloadTaskAdapter) {
            return (DownloadTaskAdapter) o;
        }
        Util.w(TAG, "download task's tag is not DownloadTaskAdapter");
        return null;
    }

    public static byte convertDownloadStatus(final StatusUtil.Status status) {
        switch (status) {
            case COMPLETED:
                return FileDownloadStatus.completed;
            case IDLE:
                return FileDownloadStatus.paused;
            case PENDING:
                return FileDownloadStatus.pending;
            case RUNNING:
                return FileDownloadStatus.progress;
            default:
                return FileDownloadStatus.INVALID_STATUS;
        }
    }

    /**
     * Temp file pattern is deprecated in OkDownload.
     */
    @Deprecated
    public static String getTempPath(final String targetPath) {
        return String.format(Locale.ENGLISH, "%s.temp", targetPath);
    }

    @Deprecated
    public static String getThreadPoolName(String name) {
        return FILEDOWNLOADER_PREFIX + "-" + name;
    }

    @Deprecated
    public static void setMinProgressStep(int minProgressStep) throws IllegalAccessException {
        // do nothing
    }

    @Deprecated
    public static void setMinProgressTime(long minProgressTime) throws IllegalAccessException {
        // do nothing
    }

    @Deprecated
    public static int getMinProgressStep() {
        return 0;
    }

    @Deprecated
    public static long getMinProgressTime() {
        return 0;
    }

    @Deprecated
    public static boolean isFilenameValid(String filename) {
        return true;
    }

    @Deprecated
    public static int generateId(final String url, final String path) {
        return new DownloadTask.Builder(url, new File(path)).build().getId();
    }

    @Deprecated
    public static int generateId(final String url, final String path,
                                 final boolean pathAsDirectory) {
        if (pathAsDirectory) {
            return new DownloadTask.Builder(url, path, null).build().getId();
        }

        return generateId(url, path);
    }

    public static String getStack() {
        return getStack(true);
    }

    public static String getStack(final boolean printLine) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        return getStack(stackTrace, printLine);
    }

    public static String getStack(final StackTraceElement[] stackTrace, final boolean printLine) {
        if ((stackTrace == null) || (stackTrace.length < 4)) {
            return "";
        }

        StringBuilder t = new StringBuilder();

        for (int i = 3; i < stackTrace.length; i++) {
            if (!stackTrace[i].getClassName().contains("com.liulishuo.filedownloader")) {
                continue;
            }
            t.append('[');
            t.append(stackTrace[i].getClassName()
                    .substring("com.liulishuo.filedownloader".length()));
            t.append(':');
            t.append(stackTrace[i].getMethodName());
            if (printLine) {
                t.append('(').append(stackTrace[i].getLineNumber()).append(")]");
            } else {
                t.append(']');
            }
        }
        return t.toString();
    }

    @Deprecated
    public static boolean isDownloaderProcess(final Context context) {
        return false;
    }

    public static String[] convertHeaderString(final String nameAndValuesString) {
        final String[] lineString = nameAndValuesString.split("\n");
        final String[] namesAndValues = new String[lineString.length * 2];

        for (int i = 0; i < lineString.length; i++) {
            final String[] nameAndValue = lineString[i].split(": ");
            /**
             * @see Headers#toString()
             * @see Headers#name(int)
             * @see Headers#value(int)
             */
            namesAndValues[i * 2] = nameAndValue[0];
            namesAndValues[i * 2 + 1] = nameAndValue[1];
        }

        return namesAndValues;
    }

    public static long getFreeSpaceBytes(final String path) {
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

    public static String formatString(final String msg, Object... args) {
        return String.format(Locale.ENGLISH, msg, args);
    }

    @Deprecated
    public static long parseContentRangeFoInstanceLength(String contentRange) {
        return -1;
    }

    /**
     * The same to com.android.providers.downloads.Helpers#parseContentDisposition.
     * </p>
     * Parse the Content-Disposition HTTP Header. The format of the header
     * is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be
     * downloaded to the file system. We only support the attachment type.
     */
    public static String parseContentDisposition(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }

        try {
            Matcher m = CONTENT_DISPOSITION_WITH_ASTERISK_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                String charset = m.group(1);
                String encodeFileName = m.group(2);
                return URLDecoder.decode(encodeFileName, charset);
            }

            m = CONTENT_DISPOSITION_WITHOUT_ASTERISK_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalStateException | UnsupportedEncodingException ignore) {
            // This function is defined as returning null when it can't parse the header
        }
        return null;
    }

    /**
     * @param path            If {@code pathAsDirectory} is true, the {@code path} would be the
     *                        absolute directory to settle down the file;
     *                        If {@code pathAsDirectory} is false, the {@code path} would be the
     *                        absolute file path.
     * @param pathAsDirectory whether the {@code path} is a directory.
     * @param filename        the file's name.
     * @return the absolute path of the file. If can't find by params, will return {@code null}.
     */
    public static String getTargetFilePath(String path, boolean pathAsDirectory, String filename) {
        if (path == null) {
            return null;
        }

        if (pathAsDirectory) {
            if (filename == null) {
                return null;
            }

            return FileDownloadUtils.generateFilePath(path, filename);
        } else {
            return path;
        }
    }

    /**
     * The same to {@link File#getParent()}, for non-creating a file object.
     *
     * @return this file's parent pathname or {@code null}.
     */
    public static String getParent(final String path) {
        int length = path.length(), firstInPath = 0;
        if (File.separatorChar == '\\' && length > 2 && path.charAt(1) == ':') {
            firstInPath = 2;
        }
        int index = path.lastIndexOf(File.separatorChar);
        if (index == -1 && firstInPath > 0) {
            index = 2;
        }
        if (index == -1 || path.charAt(length - 1) == File.separatorChar) {
            return null;
        }
        if (path.indexOf(File.separatorChar) == index
                && path.charAt(firstInPath) == File.separatorChar) {
            return path.substring(0, index + 1);
        }
        return path.substring(0, index);
    }

    @Deprecated
    public static boolean isNetworkNotOnWifiType() {
        return false;
    }

    public static boolean checkPermission(String permission) {
        final int perm = FileDownloadHelper.getAppContext()
                .checkCallingOrSelfPermission(permission);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    public static void deleteTaskFiles(String targetFilepath, String tempFilePath) {
        deleteTempFile(tempFilePath);
        deleteTargetFile(targetFilepath);
    }

    public static void deleteTempFile(String tempFilePath) {
        if (tempFilePath != null) {
            final File tempFile = new File(tempFilePath);
            if (tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    public static void deleteTargetFile(String targetFilePath) {
        if (targetFilePath != null) {
            final File targetFile = new File(targetFilePath);
            if (targetFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                targetFile.delete();
            }
        }
    }
}