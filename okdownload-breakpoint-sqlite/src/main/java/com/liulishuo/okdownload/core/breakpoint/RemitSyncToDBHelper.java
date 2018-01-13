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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.core.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

class RemitSyncToDBHelper {
    @NonNull private final RemitAgent agent;
    @NonNull private final Handler handler;

    long delayMillis;

    final List<Integer> freeToDBIdList = new ArrayList<>();
    @Nullable private volatile Thread parkThread;

    static final int INVALID_ID = BreakpointStoreOnCache.FIRST_ID - 1;
    static final int WHAT_CLEAN_PARK = INVALID_ID;

    volatile int handlingId = INVALID_ID;

    RemitSyncToDBHelper(@NonNull final RemitAgent agent) {
        this.agent = agent;
        this.delayMillis = 1500;

        final HandlerThread thread = new HandlerThread("OkDownload RemitHandoverToDB");
        thread.start();
        handler = new Handler(thread.getLooper(), new Handler.Callback() {
            @Override public boolean handleMessage(Message msg) {

                if (dispatchDelayedMessage(msg.what)) return true;

                final int id = msg.what;
                makeIdFreeToDatabase(id);

                return true;
            }
        });
    }

    void shutdown() {
        this.handler.getLooper().quit();
    }

    boolean dispatchDelayedMessage(int what) {
        if (what == WHAT_CLEAN_PARK) {
            if (parkThread != null) {
                LockSupport.unpark(parkThread);
                parkThread = null;
            }
            return true;
        }

        return false;
    }

    void makeIdFreeToDatabase(int id) {
        try {
            handlingId = id;
            if (agent.isInfoNotOnDatabase(id)) {
                syncCacheToDB(id);
            }
            freeToDBIdList.add(id);
        } catch (IOException e) {
            Util.e("RemitSyncToDBHelper", "sync cache to database failed!", e);
        } finally {
            handlingId = INVALID_ID;
            if (parkThread != null) {
                LockSupport.unpark(parkThread);
                parkThread = null;
            }
        }
    }


    boolean isNotFreeToDatabase(int id) {
        return !freeToDBIdList.contains(id);
    }

    void onTaskStart(int id) {
        handler.sendEmptyMessageDelayed(id, delayMillis);
    }

    void onTaskEnd(int id) {
        discardDelayedId(id);
        freeToDBIdList.remove((Integer) id);
    }

    void discardFlyingSyncOrEnsureSyncFinish(int id) {
        // is already finished
        if (freeToDBIdList.contains(id)) {
            // already finished delayed message
            return;
        }

        // try to discard
        discardDelayedId(id);

        // is discard success
        if (handlingId == id) {
            // discard failed, so make sure sync finish
            cleanThreadParkInNextLoop();
            parkCurrentThread();
        }
    }


    void ensureCacheToDB(int id) {
        discardFlyingSyncOrEnsureSyncFinish(id);

        if (agent.isInfoNotOnDatabase(id)) {
            try {
                syncCacheToDB(id);
            } catch (IOException e) {
                Util.e("RemitSyncToDBHelper", "sync cache to database failed!", e);
            }
        }
    }

    void syncCacheToDB(int id) throws IOException {
        this.agent.syncCacheToDB(id);
    }

    interface RemitAgent {
        void syncCacheToDB(int id) throws IOException;

        boolean isInfoNotOnDatabase(int id);
    }

    void parkCurrentThread() {
        LockSupport.park();
    }

    void cleanThreadParkInNextLoop() {
        parkThread = Thread.currentThread();
        handler.sendEmptyMessage(WHAT_CLEAN_PARK);
    }

    void discardDelayedId(int id) {
        handler.removeMessages(id);
    }

}
