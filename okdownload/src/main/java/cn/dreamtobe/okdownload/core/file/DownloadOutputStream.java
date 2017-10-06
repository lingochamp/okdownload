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

package cn.dreamtobe.okdownload.core.file;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Jacksgong on 29/09/2017.
 */

public interface DownloadOutputStream {
    void write(byte[] b, int off, int len) throws IOException;

    void close() throws IOException;

    void flushAndSync() throws IOException;

    boolean supportSeek();

    void seek(long offset) throws IOException;

    void setLength(long newLength) throws IOException;

    interface Factory {
        DownloadOutputStream create(Context context, File file) throws FileNotFoundException;

        DownloadOutputStream create(Context context, Uri uri) throws FileNotFoundException;
    }
}
