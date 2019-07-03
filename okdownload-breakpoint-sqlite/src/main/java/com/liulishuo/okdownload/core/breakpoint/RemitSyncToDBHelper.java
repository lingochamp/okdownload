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

import androidx.annotation.NonNull;

class RemitSyncToDBHelper {

    private final RemitSyncExecutor executor;

    long delayMillis;

    RemitSyncToDBHelper(@NonNull final RemitSyncExecutor.RemitAgent agent) {
        this(new RemitSyncExecutor(agent));
    }

    RemitSyncToDBHelper(@NonNull final RemitSyncExecutor executor) {
        this.executor = executor;
        this.delayMillis = 1500;
    }

    void shutdown() {
        this.executor.shutdown();
    }

    boolean isNotFreeToDatabase(int id) {
        return !executor.isFreeToDatabase(id);
    }

    void onTaskStart(int id) {
        // discard pending sync if we can
        executor.removePostWithId(id);

        executor.postSyncInfoDelay(id, delayMillis);
    }

    void endAndEnsureToDB(int id) {
        executor.removePostWithId(id);

        try {
            // already synced
            if (executor.isFreeToDatabase(id)) return;

            // force sync for ids
            executor.postSync(id);
        } finally {
            // remove free state
            executor.postRemoveFreeId(id);
        }
    }

    void discard(int id) {
        executor.removePostWithId(id);
        executor.postRemoveInfo(id);
    }
}
