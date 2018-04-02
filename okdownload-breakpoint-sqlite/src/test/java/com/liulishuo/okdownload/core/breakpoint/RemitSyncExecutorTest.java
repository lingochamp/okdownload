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

package com.liulishuo.okdownload.core.breakpoint;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.liulishuo.okdownload.core.breakpoint.RemitSyncExecutor.WHAT_REMOVE_FREE_BUNCH_ID;
import static com.liulishuo.okdownload.core.breakpoint.RemitSyncExecutor.WHAT_REMOVE_FREE_ID;
import static com.liulishuo.okdownload.core.breakpoint.RemitSyncExecutor.WHAT_REMOVE_INFO;
import static com.liulishuo.okdownload.core.breakpoint.RemitSyncExecutor.WHAT_SYNC_BUNCH_ID;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class RemitSyncExecutorTest {

    private RemitSyncExecutor executor;
    private Set<Integer> freeToDBIdList = new HashSet<>();

    @Mock RemitSyncExecutor.RemitAgent agent;
    @Mock Handler handler;

    private ArgumentCaptor<Message> messageCaptor;
    private List<Integer> idList;

    @Before
    public void setup() {
        initMocks(this);
        this.executor = spy(new RemitSyncExecutor(agent, handler, freeToDBIdList));
        doReturn(mock(Looper.class)).when(handler).getLooper();

        messageCaptor = ArgumentCaptor.forClass(Message.class);
        idList = new ArrayList<>();
        idList.add(1);
        idList.add(2);
        idList.add(3);

        Message msg = new Message();
        msg.what = WHAT_SYNC_BUNCH_ID;
        when(handler.obtainMessage(WHAT_SYNC_BUNCH_ID)).thenReturn(msg);
        msg = new Message();
        msg.what = WHAT_REMOVE_FREE_BUNCH_ID;
        when(handler.obtainMessage(WHAT_REMOVE_FREE_BUNCH_ID)).thenReturn(msg);
        msg = new Message();
        msg.what = WHAT_REMOVE_FREE_ID;
        when(handler.obtainMessage(WHAT_REMOVE_FREE_ID)).thenReturn(msg);
        msg = new Message();
        msg.what = WHAT_REMOVE_INFO;
        when(handler.obtainMessage(WHAT_REMOVE_INFO)).thenReturn(msg);
    }

    @Test
    public void shutdown() {
        executor.shutdown();
        verify(handler.getLooper()).quit();
    }

    @Test
    public void isFreeToDatabase() {
        freeToDBIdList.add(1);
        assertThat(executor.isFreeToDatabase(1)).isTrue();
        freeToDBIdList.remove(1);
        assertThat(executor.isFreeToDatabase(1)).isFalse();
    }

    @Test
    public void postSyncInfoDelay() {
        executor.postSyncInfoDelay(1, 10);
        verify(handler).sendEmptyMessageDelayed(eq(1), eq(10L));
    }

    @Test
    public void postSync() {
        executor.postSync(1);
        verify(handler).sendEmptyMessage(eq(1));

        executor.postSync(idList);
        verify(handler).sendMessage(messageCaptor.capture());

        final Message message = messageCaptor.getValue();
        assertThat(message.obj).isEqualTo(idList);
        assertThat(message.what).isEqualTo(WHAT_SYNC_BUNCH_ID);
    }

    @Test
    public void postRemoveInfo() {
        executor.postRemoveInfo(1);
        verify(handler).sendMessage(messageCaptor.capture());

        final Message message = messageCaptor.getValue();
        assertThat(message.arg1).isEqualTo(1);
        assertThat(message.what).isEqualTo(WHAT_REMOVE_INFO);
    }

    @Test
    public void postRemoveFreeIds() {
        executor.postRemoveFreeIds(idList);

        verify(handler).sendMessage(messageCaptor.capture());

        final Message message = messageCaptor.getValue();
        assertThat(message.obj).isEqualTo(idList);
        assertThat(message.what).isEqualTo(WHAT_REMOVE_FREE_BUNCH_ID);
    }

    @Test
    public void postRemoveFreeId() {
        executor.postRemoveFreeId(1);
        verify(handler).sendMessage(messageCaptor.capture());

        final Message message = messageCaptor.getValue();
        assertThat(message.arg1).isEqualTo(1);
        assertThat(message.what).isEqualTo(WHAT_REMOVE_FREE_ID);
    }

    @Test
    public void removePostWithId() {
        executor.removePostWithId(1);
        verify(handler).removeMessages(eq(1));
    }

    @Test
    public void removePostWithIds() {
        final int[] ids = new int[3];
        ids[0] = 1;
        ids[1] = 2;
        ids[2] = 3;
        executor.removePostWithIds(ids);
        verify(handler).removeMessages(eq(1));
        verify(handler).removeMessages(eq(2));
        verify(handler).removeMessages(eq(3));
    }

    @Test
    public void handleMessage_removeInfo() {
        final Message message = new Message();
        message.what = WHAT_REMOVE_INFO;
        message.arg1 = 1;
        freeToDBIdList.add(1);

        executor.handleMessage(message);

        verify(agent).removeInfo(eq(1));
        assertThat(freeToDBIdList).isEmpty();
    }


    @Test
    public void handleMessage_removeFreeBunchId() {
        final Message message = new Message();
        message.what = WHAT_REMOVE_FREE_BUNCH_ID;
        message.obj = idList;
        freeToDBIdList.addAll(idList);

        executor.handleMessage(message);

        assertThat(freeToDBIdList).isEmpty();
    }

    @Test
    public void handleMessage_removeFreeId() {
        final Message message = new Message();
        message.what = WHAT_REMOVE_FREE_ID;
        message.arg1 = 1;
        freeToDBIdList.add(1);

        executor.handleMessage(message);

        assertThat(freeToDBIdList).isEmpty();
    }

    @Test
    public void handleMessage_syncBunchId() throws IOException {
        final Message message = new Message();
        message.what = WHAT_SYNC_BUNCH_ID;
        message.obj = idList;

        executor.handleMessage(message);

        verify(agent).syncCacheToDB(eq(idList));
        assertThat(freeToDBIdList).containsExactly(1, 2, 3);
    }

    @Test
    public void handleMessage_syncId() throws IOException {
        final Message message = new Message();
        message.what = 1;

        executor.handleMessage(message);

        verify(agent).syncCacheToDB(eq(1));
        assertThat(freeToDBIdList).containsExactly(1);
    }
}