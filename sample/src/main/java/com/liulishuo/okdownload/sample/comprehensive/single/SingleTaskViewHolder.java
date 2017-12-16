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

package com.liulishuo.okdownload.sample.comprehensive.single;

import android.widget.ProgressBar;
import android.widget.TextView;

class SingleTaskViewHolder {
    final TextView titleTv;
    final TextView speedTv;
    final ProgressBar pb;

    SingleTaskViewHolder(TextView titleTv, TextView speedTv, ProgressBar pb) {
        this.titleTv = titleTv;
        this.speedTv = speedTv;
        this.pb = pb;
    }
}