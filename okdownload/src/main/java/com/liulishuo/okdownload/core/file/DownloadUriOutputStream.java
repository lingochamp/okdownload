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

package com.liulishuo.okdownload.core.file;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import com.liulishuo.okdownload.core.Util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class DownloadUriOutputStream implements DownloadOutputStream {

    @NonNull private final FileChannel channel;
    @NonNull final ParcelFileDescriptor pdf;
    @NonNull final BufferedOutputStream out;
    @NonNull final FileOutputStream fos;

    public DownloadUriOutputStream(Context context, Uri uri, int bufferSize) throws
            FileNotFoundException {
        final ParcelFileDescriptor pdf = context.getContentResolver().openFileDescriptor(uri, "rw");
        if (pdf == null) throw new FileNotFoundException("result of " + uri + " is null!");
        this.pdf = pdf;

        this.fos = new FileOutputStream(pdf.getFileDescriptor());
        this.channel = fos.getChannel();
        this.out = new BufferedOutputStream(fos, bufferSize);
    }

    DownloadUriOutputStream(@NonNull FileChannel channel, @NonNull ParcelFileDescriptor pdf,
                            @NonNull FileOutputStream fos,
                            @NonNull BufferedOutputStream out) {
        this.channel = channel;
        this.pdf = pdf;
        this.fos = fos;
        this.out = out;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        out.close();
        fos.close();
    }

    @Override
    public void flushAndSync() throws IOException {
        out.flush();
        pdf.getFileDescriptor().sync();
    }

    @Override
    public void seek(long offset) throws IOException {
        channel.position(offset);
    }
    @Override
    public void setLength(long newLength) {
        final String tag = "DownloadUriOutputStream";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.posix_fallocate(pdf.getFileDescriptor(),0, newLength);
            } catch (Throwable e) {
                if (e instanceof ErrnoException) {
                    if (((ErrnoException) e).errno == OsConstants.ENOSYS || ((ErrnoException) e).errno == OsConstants.ENOTSUP) {
                        Util.w(tag, "fallocate() not supported; falling back to ftruncate()");
                        try {
                            Os.ftruncate(pdf.getFileDescriptor(), newLength);
                        } catch (Throwable e1) {
                            Util.w(tag, "It can't pre-allocate length(" + newLength + ") on the sdk"
                                    + " version(" + Build.VERSION.SDK_INT + "), because of " + e1);
                        }
                    }
                }else {
                    Util.w(tag, "It can't pre-allocate length(" + newLength + ") on the sdk"
                            + " version(" + Build.VERSION.SDK_INT + "), because of " + e);
                }
            }
        } else {
            Util.w(tag,
                    "It can't pre-allocate length(" + newLength + ") on the sdk "
                            + "version(" + Build.VERSION.SDK_INT + ")");
        }
    }

    public static class Factory implements DownloadOutputStream.Factory {

        @Override
        public DownloadOutputStream create(Context context, File file, int flushBufferSize) throws
                FileNotFoundException {
            return new DownloadUriOutputStream(context, Uri.fromFile(file), flushBufferSize);
        }

        @Override
        public DownloadOutputStream create(Context context, Uri uri, int flushBufferSize) throws
                FileNotFoundException {
            return new DownloadUriOutputStream(context, uri, flushBufferSize);
        }

        @Override public boolean supportSeek() {
            return true;
        }
    }
}
