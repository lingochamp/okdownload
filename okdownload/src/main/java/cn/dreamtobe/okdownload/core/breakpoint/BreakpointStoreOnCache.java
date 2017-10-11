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

package cn.dreamtobe.okdownload.core.breakpoint;

import android.util.SparseArray;

import java.util.concurrent.atomic.AtomicInteger;

import cn.dreamtobe.okdownload.DownloadTask;

public class BreakpointStoreOnCache implements BreakpointStore {
    private AtomicInteger identifyGenerator = new AtomicInteger(1);

    private SparseArray<BreakpointInfo> breakpointMap = new SparseArray<>();

    @Override
    public BreakpointInfo get(int id) {
        return breakpointMap.get(id);
    }

    @Override
    public BreakpointInfo createAndInsert(DownloadTask task) {
        return null;
    }

    @Override public void onSyncToFilesystemSuccess(BreakpointInfo info, int blockIndex, long increaseLength) {
        info.getBlock(blockIndex).increaseCurrentOffset(increaseLength);
    }

    @Override
    public boolean update(BreakpointInfo breakpointInfo) {
        final BreakpointInfo onCacheOne = this.breakpointMap.get(breakpointInfo.id);
        if (onCacheOne != null) {
            onCacheOne.etag = breakpointInfo.etag;
            onCacheOne.blockInfoList.clear();
            // we don't need to deep clone this list, because of the block info only contain val params.
            // todo maybe we need crash when  add all failed.
            onCacheOne.blockInfoList.addAll(breakpointInfo.blockInfoList);
            return true;
        }

        return false;
    }

    @Override
    public void completeDownload(int id) {
        breakpointMap.remove(id);
    }

    @Override public void discard(int id) {
        breakpointMap.remove(id);
    }

    @Override
    public int createId(DownloadTask task) {
        return identifyGenerator.getAndIncrement();
    }
}