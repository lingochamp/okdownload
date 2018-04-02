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

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class IdentifiedTaskTest {
    private IdentifiedTask task;
    private IdentifiedTask another;
    private String url = "https://jacksgong.com";
    private File providedFile = new File("/provided-path/filename");
    private File parentFile = new File("/provided-path");
    private String filename = "filename";

    @Before
    public void setUp() throws Exception {
        task = spy(new IdentifiedTask() {
            @Override public int getId() {
                return 0;
            }

            @NonNull @Override public String getUrl() {
                return url;
            }

            @NonNull @Override protected File getProvidedPathFile() {
                return providedFile;
            }

            @NonNull @Override public File getParentFile() {
                return parentFile;
            }

            @Nullable @Override public String getFilename() {
                return filename;
            }
        });

        another = spy(new IdentifiedTask() {
            @Override public int getId() {
                return 0;
            }

            @NonNull @Override public String getUrl() {
                return url;
            }

            @NonNull @Override protected File getProvidedPathFile() {
                return providedFile;
            }

            @NonNull @Override public File getParentFile() {
                return parentFile;
            }

            @Nullable @Override public String getFilename() {
                return filename;
            }
        });
    }

    @Test
    public void compareIgnoreId_url() {
        when(another.getUrl()).thenReturn("another-url");
        assertThat(task.compareIgnoreId(another)).isFalse();

        when(another.getUrl()).thenReturn(url);
        assertThat(task.compareIgnoreId(another)).isTrue();
    }

    @Test
    public void compareIgnoreId_providedPathFile() {
        when(another.getProvidedPathFile()).thenReturn(new File("/another-provided-path"));
        when(another.getParentFile()).thenReturn(new File("/another-provided-path"));
        assertThat(task.compareIgnoreId(another)).isFalse();

        when(another.getProvidedPathFile()).thenReturn(providedFile);
        assertThat(task.compareIgnoreId(another)).isTrue();
    }

    @Test
    public void compareIgnoreId_parentPath() {
        when(another.getProvidedPathFile()).thenReturn(new File("/another-parent-path"));

        when(another.getParentFile()).thenReturn(new File("/another-parent-path"));
        assertThat(task.compareIgnoreId(another)).isFalse();

        when(another.getParentFile()).thenReturn(parentFile);
        assertThat(task.compareIgnoreId(another)).isTrue();
    }

    @Test
    public void compareIgnoreId_filename() {
        when(another.getProvidedPathFile()).thenReturn(new File("/another-parent-path"));

        when(another.getFilename()).thenReturn(null);
        assertThat(task.compareIgnoreId(another)).isFalse();

        when(another.getFilename()).thenReturn("another-filename");
        assertThat(task.compareIgnoreId(another)).isFalse();

        when(another.getFilename()).thenReturn(filename);
        assertThat(task.compareIgnoreId(another)).isTrue();
    }
}