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

import android.net.Uri;

/**
 * Created by Jacksgong on 24/09/2017.
 */

public class DefaultProcessFileStrategy implements ProcessFileStrategy {
    @Override
    public DownloadOutputStream createProcessFile(Uri targetFileUri) {
        return null;
    }

    @Override
    public void renameToTargetFile(DownloadOutputStream processOutputStream, Uri targetFileUri) {

    }

    @Override
    public int buffSize() {
        return 0;
    }
}
