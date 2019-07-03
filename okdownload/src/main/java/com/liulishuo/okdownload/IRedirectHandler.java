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

package com.liulishuo.okdownload;

import androidx.annotation.Nullable;

import com.liulishuo.okdownload.core.connection.DownloadConnection;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IRedirectHandler {

    /**
     * handle redirect during connection
     *
     * @param originalConnection original connection of original url, contain connection info
     *
     * @param originalConnected  connected connection of original url, contain request response
     *                           of first connect
     *
     * @param headerProperties   request headers of the connection, these headers should be added in
     *                           the new connection during handle redirect
     */
    void handleRedirect(
            DownloadConnection originalConnection,
            DownloadConnection.Connected originalConnected,
            Map<String, List<String>> headerProperties
    ) throws IOException;

    @Nullable
    String getRedirectLocation();
}
