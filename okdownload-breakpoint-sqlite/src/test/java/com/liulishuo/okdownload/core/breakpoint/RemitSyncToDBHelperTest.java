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

package com.liulishuo.okdownload.core.breakpoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class RemitSyncToDBHelperTest {

    private RemitSyncToDBHelper helper;

    @Mock private RemitSyncExecutor executor;

    @Before
    public void setup() {
        initMocks(this);

        helper = spy(new RemitSyncToDBHelper(executor));
    }

    @Test
    public void shutdown() {
        helper.shutdown();
        verify(executor).shutdown();
    }

    @Test
    public void isNotFreeToDatabase() {
        when(executor.isFreeToDatabase(1)).thenReturn(true);
        assertThat(helper.isNotFreeToDatabase(1)).isFalse();

        when(executor.isFreeToDatabase(1)).thenReturn(false);
        assertThat(helper.isNotFreeToDatabase(1)).isTrue();
    }

    @Test
    public void onTaskStart() {
        helper.onTaskStart(1);

        verify(executor).removePostWithId(eq(1));
        verify(executor).postSyncInfoDelay(eq(1), eq(helper.delayMillis));
    }

    @Test
    public void endAndEnsureToDB() {
        when(executor.isFreeToDatabase(1)).thenReturn(true);
        helper.endAndEnsureToDB(1);

        verify(executor).removePostWithId(eq(1));
        verify(executor).postRemoveFreeId(eq(1));
        verify(executor, never()).postSync(eq(1));

        when(executor.isFreeToDatabase(2)).thenReturn(false);
        helper.endAndEnsureToDB(2);
        verify(executor).postSync(eq(2));
        verify(executor).postRemoveFreeId(eq(2));
    }

    @Test
    public void discard() {
        helper.discard(1);
        verify(executor).removePostWithId(eq(1));
        verify(executor).postRemoveInfo(eq(1));
    }
}