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

package com.liulishuo.filedownloader.stream;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.okdownload.core.file.DownloadOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DownloadOutputStreamAdapter implements DownloadOutputStream {

    @NonNull
    private final FileDownloadOutputStream fileDownloadOutputStream;

    public DownloadOutputStreamAdapter(@NonNull FileDownloadOutputStream outputStream) {
        this.fileDownloadOutputStream = outputStream;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        fileDownloadOutputStream.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        fileDownloadOutputStream.close();
    }

    @Override
    public void flushAndSync() throws IOException {
        fileDownloadOutputStream.flushAndSync();
    }

    @Override
    public void seek(long offset) throws IOException {
        try {
            fileDownloadOutputStream.seek(offset);
        } catch (IllegalAccessException e) {
            throw new IOException("illegal access", e);
        }
    }

    @Override
    public void setLength(long newLength) throws IOException {
        try {
            fileDownloadOutputStream.setLength(newLength);
        } catch (IllegalAccessException e) {
            throw new IOException("illegal access", e);
        }
    }

    public static class Factory implements DownloadOutputStream.Factory {

        @NonNull
        private final FileDownloadHelper.OutputStreamCreator creator;

        public Factory(@NonNull FileDownloadHelper.OutputStreamCreator creator) {
            this.creator = creator;
        }

        @Override
        public DownloadOutputStream create(Context context, File file, int flushBufferSize) throws
                FileNotFoundException {
            if (file == null) throw new FileNotFoundException("file is null");
            try {
                return new DownloadOutputStreamAdapter(creator.create(file));
            } catch (IOException e) {
                throw new FileNotFoundException("create filedownloader output stream error: "
                        + e.getMessage());
            }
        }

        @Override
        public DownloadOutputStream create(Context context, Uri uri, int flushBufferSize) throws
                FileNotFoundException {
            if (uri == null) throw new FileNotFoundException("file is null");
            if ("file".equals(uri.getScheme())) {
                final String filePath = uri.getPath();
                if (filePath == null) throw new FileNotFoundException("file path is null");
                return create(context, new File(filePath), flushBufferSize);
            }
            throw new FileNotFoundException("filedownloader supports file schema only");
        }

        @Override
        public boolean supportSeek() {
            return true;
        }
    }
}