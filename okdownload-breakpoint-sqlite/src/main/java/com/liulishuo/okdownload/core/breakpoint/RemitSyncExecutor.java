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
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.core.Util;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class RemitSyncExecutor implements Handler.Callback {
    private static final String TAG = "RemitSyncExecutor";

    static final int WHAT_SYNC_BUNCH_ID = BreakpointStoreOnCache.FIRST_ID - 1;
    static final int WHAT_REMOVE_FREE_BUNCH_ID = BreakpointStoreOnCache.FIRST_ID - 2;
    static final int WHAT_REMOVE_FREE_ID = BreakpointStoreOnCache.FIRST_ID - 3;
    static final int WHAT_REMOVE_INFO = BreakpointStoreOnCache.FIRST_ID - 4;


    @NonNull private final Handler handler;

    @NonNull private final Set<Integer> freeToDBIdList;

    private
    @NonNull final RemitAgent agent;

    RemitSyncExecutor(@NonNull RemitAgent agent) {
        this.agent = agent;
        this.freeToDBIdList = new HashSet<>();

        final HandlerThread thread = new HandlerThread("OkDownload RemitHandoverToDB");
        thread.start();

        handler = new Handler(thread.getLooper(), this);
    }

    RemitSyncExecutor(@NonNull RemitAgent agent, @Nullable Handler handler,
                      @NonNull Set<Integer> freeToDBIdList) {
        this.agent = agent;
        this.handler = handler;
        this.freeToDBIdList = freeToDBIdList;
    }

    void shutdown() {
        this.handler.getLooper().quit();
    }

    boolean isFreeToDatabase(int id) {
        return freeToDBIdList.contains(id);
    }

    public void postSyncInfoDelay(int id, long delayMillis) {
        handler.sendEmptyMessageDelayed(id, delayMillis);
    }

    public void postSync(int id) {
        handler.sendEmptyMessage(id);
    }

    public void postSync(List<Integer> idList) {
        Message message = handler.obtainMessage(WHAT_SYNC_BUNCH_ID);
        message.obj = idList;
        handler.sendMessage(message);
    }

    public void postRemoveInfo(int id) {
        Message message = handler.obtainMessage(WHAT_REMOVE_INFO);
        message.arg1 = id;
        handler.sendMessage(message);
    }

    public void postRemoveFreeIds(List<Integer> idList) {
        final Message message = handler.obtainMessage(WHAT_REMOVE_FREE_BUNCH_ID);
        message.obj = idList;
        handler.sendMessage(message);
    }

    public void postRemoveFreeId(int id) {
        final Message message = handler.obtainMessage(WHAT_REMOVE_FREE_ID);
        message.arg1 = id;
        handler.sendMessage(message);
    }

    void removePostWithId(int id) {
        handler.removeMessages(id);
    }

    void removePostWithIds(int[] ids) {
        for (int id : ids) {
            handler.removeMessages(id);
        }
    }

    public boolean handleMessage(Message msg) {
        List<Integer> idList;
        int id;
        switch (msg.what) {
            case WHAT_REMOVE_INFO:
                id = msg.arg1;
                freeToDBIdList.remove(id);
                this.agent.removeInfo(id);
                Util.d(TAG, "remove info " + id);
                break;
            case WHAT_REMOVE_FREE_BUNCH_ID:
                // remove bunch free-ids
                idList = (List<Integer>) msg.obj;
                freeToDBIdList.removeAll(idList);
                Util.d(TAG, "remove free bunch ids " + idList);
                break;
            case WHAT_REMOVE_FREE_ID:
                // remove free-id
                id = msg.arg1;
                freeToDBIdList.remove(id);
                Util.d(TAG, "remove free bunch id " + id);
                break;
            case WHAT_SYNC_BUNCH_ID:
                // sync bunch id
                idList = (List<Integer>) msg.obj;
                try {
                    this.agent.syncCacheToDB(idList);
                    freeToDBIdList.addAll(idList);
                    Util.d(TAG, "sync bunch info with ids: " + idList);
                } catch (IOException e) {
                    Util.w(TAG, "sync info to db failed for ids: " + idList);
                }
                break;
            default:
                // sync id
                id = msg.what;
                try {
                    this.agent.syncCacheToDB(id);
                    freeToDBIdList.add(id);
                    Util.d(TAG, "sync info with id: " + id);
                } catch (IOException e) {
                    Util.w(TAG, "sync cache to db failed for id: " + id);
                }
                break;
        }
        return true;
    }

    interface RemitAgent {
        void syncCacheToDB(List<Integer> idList) throws IOException;

        void syncCacheToDB(int id) throws IOException;

        void removeInfo(int id);
    }
}
