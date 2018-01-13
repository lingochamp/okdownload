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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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

    @Mock private RemitSyncToDBHelper.RemitAgent agent;

    @Before
    public void setup() {
        initMocks(this);

        helper = spy(new RemitSyncToDBHelper(agent));
    }

    @After
    public void tearDown() {
        helper.shutdown();
    }

    @Test
    public void dispatchDelayedMessage() {
        assertThat(helper.dispatchDelayedMessage(RemitSyncToDBHelper.WHAT_CLEAN_PARK)).isTrue();
    }


    @Test
    public void makeIdFreeToDatabase_infoOnDatabase() throws IOException {
        // info already on database case.
        when(agent.isInfoNotOnDatabase(1)).thenReturn(false);
        doNothing().when(helper).syncCacheToDB(1);
        helper.makeIdFreeToDatabase(1);
        verify(helper, never()).syncCacheToDB(eq(1));
        assertThat(helper.freeToDBIdList).containsExactly(1);
    }

    @Test
    public void makeIdFreeToDatabase_infoNotOnDatabase() throws IOException {
        when(agent.isInfoNotOnDatabase(1)).thenReturn(true);

        helper.handlingId = 10;
        helper.makeIdFreeToDatabase(1);

        assertThat(helper.freeToDBIdList).containsExactly(1);
        verify(helper).syncCacheToDB(eq(1));
        assertThat(helper.handlingId).isEqualTo(RemitSyncToDBHelper.INVALID_ID);
    }

    @Test
    public void isNotFreeToDatabase() {
        assertThat(helper.freeToDBIdList).isEmpty();

        // not contain --> not free to database as default.
        assertThat(helper.isNotFreeToDatabase(1)).isTrue();

        helper.freeToDBIdList.add(1);

        // contain --> free to database.
        assertThat(helper.isNotFreeToDatabase(1)).isFalse();
    }


    @Test
    public void onTaskEnd() {
        helper.freeToDBIdList.add(1);

        helper.onTaskEnd(1);

        assertThat(helper.freeToDBIdList).isEmpty();
    }

    @Test
    public void discardFlyingSyncOrEnsureSyncFinish() {
        // already finished
        helper.freeToDBIdList.add(1);
        helper.discardFlyingSyncOrEnsureSyncFinish(1);
        verify(helper, never()).discardDelayedId(eq(1));

        // discard success
        helper.freeToDBIdList.remove((Integer) 1);
        helper.discardFlyingSyncOrEnsureSyncFinish(1);
        verify(helper).discardDelayedId(eq(1));

        // discard failed
        helper.handlingId = 1;
        doNothing().when(helper).parkCurrentThread();
        helper.discardFlyingSyncOrEnsureSyncFinish(1);
        verify(helper).cleanThreadParkInNextLoop();
        verify(helper).parkCurrentThread();
    }

    @Test
    public void ensureCacheToDB() throws IOException {
        doNothing().when(helper).discardFlyingSyncOrEnsureSyncFinish(1);

        when(agent.isInfoNotOnDatabase(1)).thenReturn(false);
        helper.ensureCacheToDB(1);
        verify(helper).discardFlyingSyncOrEnsureSyncFinish(eq(1));
        verify(helper, never()).syncCacheToDB(eq(1));

        when(agent.isInfoNotOnDatabase(1)).thenReturn(true);
        helper.ensureCacheToDB(1);
        verify(helper).syncCacheToDB(eq(1));
    }
}