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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.liulishuo.okdownload.DownloadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BreakpointStoreOnCache implements BreakpointStore {
    private final SparseArray<BreakpointInfo> storedInfos;
    private final HashMap<String, String> responseFilenameMap;

    private final SparseArray<DownloadTask> unStoredTasks;
    private final List<Integer> sortedOccupiedIds;

    public BreakpointStoreOnCache() {
        this(new SparseArray<BreakpointInfo>(), new HashMap<String, String>());
    }

    BreakpointStoreOnCache(SparseArray<BreakpointInfo> storedInfos,
                           HashMap<String, String> responseFilenameMap,
                           SparseArray<DownloadTask> unStoredTasks,
                           List<Integer> sortedOccupiedIds) {
        this.unStoredTasks = unStoredTasks;
        this.storedInfos = storedInfos;
        this.responseFilenameMap = responseFilenameMap;
        this.sortedOccupiedIds = sortedOccupiedIds;
    }

    public BreakpointStoreOnCache(SparseArray<BreakpointInfo> storedInfos,
                                  HashMap<String, String> responseFilenameMap) {
        this.unStoredTasks = new SparseArray<>();
        this.storedInfos = storedInfos;
        this.responseFilenameMap = responseFilenameMap;

        final int count = storedInfos.size();

        sortedOccupiedIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sortedOccupiedIds.add(storedInfos.valueAt(i).id);
        }
        Collections.sort(sortedOccupiedIds);
    }

    @Override
    public BreakpointInfo get(int id) {
        return storedInfos.get(id);
    }

    @Override
    public BreakpointInfo createAndInsert(@NonNull DownloadTask task) {
        final int id = task.getId();

        BreakpointInfo newInfo = new BreakpointInfo(id, task.getUrl(), task.getParentPath(),
                task.getFilename());
        storedInfos.put(id, newInfo);
        unStoredTasks.remove(id);
        return newInfo;
    }

    @Override public void onSyncToFilesystemSuccess(@NonNull BreakpointInfo info, int blockIndex,
                                                    long increaseLength) {
        final BreakpointInfo onCacheOne = this.storedInfos.get(info.id);
        if (info != onCacheOne) throw new IllegalArgumentException("Info not on store!");

        onCacheOne.getBlock(blockIndex).increaseCurrentOffset(increaseLength);
    }

    @Override
    public boolean update(@NonNull BreakpointInfo breakpointInfo) {
        final String filename = breakpointInfo.getFilename();
        if (breakpointInfo.isTaskOnlyProvidedParentPath() && filename != null) {
            this.responseFilenameMap.put(breakpointInfo.getUrl(), filename);
        }

        final BreakpointInfo onCacheOne = this.storedInfos.get(breakpointInfo.id);
        if (onCacheOne != null) {
            if (onCacheOne == breakpointInfo) return true;

            // replace
            this.storedInfos.put(breakpointInfo.id, breakpointInfo.copy());
            return true;
        }

        return false;
    }

    @Override
    public synchronized void completeDownload(int id) {
        storedInfos.remove(id);
        if (unStoredTasks.get(id) == null) sortedOccupiedIds.remove(Integer.valueOf(id));
    }

    @Override public synchronized void discard(int id) {
        storedInfos.remove(id);
        if (unStoredTasks.get(id) == null) sortedOccupiedIds.remove(Integer.valueOf(id));
    }

    @Override
    public synchronized int findOrCreateId(@NonNull DownloadTask task) {
        final SparseArray<BreakpointInfo> clonedMap = storedInfos.clone();
        final int size = clonedMap.size();
        for (int i = 0; i < size; i++) {
            final BreakpointInfo info = clonedMap.valueAt(i);
            if (info != null && info.isSameFrom(task)) {
                return info.id;
            }
        }

        final int unStoredSize = unStoredTasks.size();
        for (int i = 0; i < unStoredSize; i++) {
            final DownloadTask another = unStoredTasks.valueAt(i);
            if (another == null) continue;
            if (another.compareIgnoreId(task)) return another.getId();
        }

        final int id = allocateId();
        unStoredTasks.put(id, task);
        return id;
    }

    // info maybe turn to equal to another one after get filename from response.
    @Override
    public BreakpointInfo findAnotherInfoFromCompare(DownloadTask task, BreakpointInfo ignored) {
        final SparseArray<BreakpointInfo> clonedMap = storedInfos.clone();
        final int size = clonedMap.size();
        for (int i = 0; i < size; i++) {
            final BreakpointInfo info = clonedMap.valueAt(i);
            if (info == ignored) continue;

            if (info.isSameFrom(task)) {
                return info;
            }
        }

        return null;
    }

    @Nullable @Override public String getResponseFilename(String url) {
        return responseFilenameMap.get(url);
    }

    private static final int FIRST_ID = 1;

    synchronized int allocateId() {
        int newId = 0;

        int index = 0;

        int preId = 0;
        int curId;

        for (int i = 0; i < sortedOccupiedIds.size(); i++) {
            final Integer curIdObj = sortedOccupiedIds.get(i);
            if (curIdObj == null) {
                index = i;
                newId = preId + 1;
                break;
            }

            curId = curIdObj;
            if (preId == 0) {
                if (curId != FIRST_ID) {
                    newId = FIRST_ID;
                    index = 0;
                    break;
                }
                preId = curId;
                continue;
            }

            if (curId != preId + 1) {
                newId = preId + 1;
                index = i;
                break;
            }

            preId = curId;
        }

        if (newId == 0) {
            if (sortedOccupiedIds.isEmpty()) {
                newId = FIRST_ID;
            } else {
                newId = sortedOccupiedIds.get(sortedOccupiedIds.size() - 1) + 1;
                index = sortedOccupiedIds.size();
            }
        }

        sortedOccupiedIds.add(index, newId);

        return newId;
    }

}