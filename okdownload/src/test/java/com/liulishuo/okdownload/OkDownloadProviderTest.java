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

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class OkDownloadProviderTest {

    private OkDownloadProvider provider;
    @Mock Uri uri;

    @Before
    public void setup() {
        initMocks(this);
        provider = new OkDownloadProvider();
    }

    @Test
    public void onCreate() {
        assertThat(provider.onCreate()).isTrue();
    }

    @Test
    public void query() {
        assertThat(provider.query(uri, null, null, null, null)).isNull();
    }

    @Test
    public void getType() {
        assertThat(provider.getType(uri)).isNull();
    }

    @Test
    public void insert() {
        assertThat(provider.insert(uri, null)).isNull();
    }

    @Test
    public void delete() {
        assertThat(provider.delete(uri, null, null)).isZero();
    }

    @Test
    public void update() {
        assertThat(provider.update(uri, null, null, null)).isZero();
    }
}