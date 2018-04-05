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

package com.liulishuo.okdownload.sample;


import android.os.Bundle;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.sample.base.BaseListActivity;

public class MainActivity extends BaseListActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.enableConsoleLog();
    }

    @Override protected void setupAdapter(BaseListActivity.ItemsHolder holder) {
        holder.addItem(R.string.single_download_title, R.string.single_download_desc,
                SingleActivity.class);
        holder.addItem(R.string.each_block_progress_title, R.string.each_block_progress_desc,
                EachBlockProgressActivity.class);
        holder.addItem(R.string.queue_download_title, R.string.queue_download_desc,
                QueueActivity.class);
        holder.addItem(R.string.bunch_download_title, R.string.bunch_download_desc,
                BunchActivity.class);
//        holder.addItem(R.string.task_manager_title, R.string.task_manager_desc,
//                ManagerActivity.class);
        holder.addItem(R.string.title_content_uri, R.string.content_uri_desc,
                ContentUriActivity.class);
        holder.addItem(R.string.title_notification, R.string.notification_desc,
                NotificationActivity.class);
//        holder.addItem(R.string.comprehensive_case_title, R.string.comprehensive_case_desc,
//                ComprehensiveActivity.class);
    }

    @Override public int titleRes() {
        return R.string.app_name;
    }
}
