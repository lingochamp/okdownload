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

package com.liulishuo.okdownload.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

public abstract class IdentifiedTask {

    public abstract int getId();

    @NonNull public abstract String getUrl();

    @NonNull protected abstract File getProvidedPathFile();

    @NonNull public abstract File getParentFile();

    @Nullable public abstract String getFilename();

    public boolean compareIgnoreId(IdentifiedTask another) {
        if (!getUrl().equals(another.getUrl())) return false;

        if (getProvidedPathFile().equals(another.getProvidedPathFile())) return true;

        if (!getParentFile().equals(another.getParentFile())) return false;

        // cover the case of filename is provided by response.
        final String filename = getFilename();
        final String anotherFilename = another.getFilename();
        return anotherFilename != null && filename != null && anotherFilename.equals(filename);
    }
}