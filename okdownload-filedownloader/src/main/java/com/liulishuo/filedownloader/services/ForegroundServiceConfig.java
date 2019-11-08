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

package com.liulishuo.filedownloader.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;

/**
 * No service in OkDownload, so this configuration class is useless.
 */
@Deprecated
@TargetApi(26)
public class ForegroundServiceConfig {

    private ForegroundServiceConfig() {
    }

    private Notification buildDefaultNotification(Context context) {
        return null;
    }

    public static class Builder {

        public Builder notificationId(int notificationId) {
            return this;
        }

        public Builder notificationChannelId(String notificationChannelId) {
            return this;
        }

        public Builder notificationChannelName(String notificationChannelName) {
            return this;
        }

        public Builder notification(Notification notification) {
            return this;
        }

        public Builder needRecreateChannelId(boolean needRecreateChannelId) {
            return this;
        }

        public ForegroundServiceConfig build() {
            return new ForegroundServiceConfig();
        }
    }
}