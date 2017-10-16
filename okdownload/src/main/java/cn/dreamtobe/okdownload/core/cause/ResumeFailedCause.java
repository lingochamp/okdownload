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

package cn.dreamtobe.okdownload.core.cause;

public enum ResumeFailedCause {
    INFO_DIRTY,
    FILE_NOT_EXIST,
    OUTPUT_STREAM_NOT_SUPPORT,
    RESPONSE_ETAG_CHANGED,
    RESPONSE_PRECONDITION_FAILED,
    RESPONSE_CREATED_RANGE_NOT_FROM_0,
    RESPONSE_RESET_RANGE_NOT_FROM_0,
}