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
import com.liulishuo.okdownload.core.IdentifiedTask;
import com.liulishuo.okdownload.core.cause.EndCause;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BreakpointStoreOnCache implements DownloadStore {
    private final SparseArray<BreakpointInfo> storedInfos;
    private final HashMap<String, String> responseFilenameMap;

    @NonNull private final KeyToIdMap keyToIdMap;

    private final SparseArray<IdentifiedTask> unStoredTasks;
    private final List<Integer> sortedOccupiedIds;
    private final List<Integer> fileDirtyList;

    public BreakpointStoreOnCache() {
        this(new SparseArray<BreakpointInfo>(), new ArrayList<Integer>(),
                new HashMap<String, String>());
    }

    BreakpointStoreOnCache(SparseArray<BreakpointInfo> storedInfos,
                           List<Integer> fileDirtyList,
                           HashMap<String, String> responseFilenameMap,
                           SparseArray<IdentifiedTask> unStoredTasks,
                           List<Integer> sortedOccupiedIds,
                           KeyToIdMap keyToIdMap) {
        this.unStoredTasks = unStoredTasks;
        this.fileDirtyList = fileDirtyList;
        this.storedInfos = storedInfos;
        this.responseFilenameMap = responseFilenameMap;
        this.sortedOccupiedIds = sortedOccupiedIds;
        this.keyToIdMap = keyToIdMap;
    }

    public BreakpointStoreOnCache(SparseArray<BreakpointInfo> storedInfos,
                                  List<Integer> fileDirtyList,
                                  HashMap<String, String> responseFilenameMap) {
        this.unStoredTasks = new SparseArray<>();
        this.storedInfos = storedInfos;
        this.fileDirtyList = fileDirtyList;
        this.responseFilenameMap = responseFilenameMap;
        this.keyToIdMap = new KeyToIdMap();

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

    @NonNull @Override
    public BreakpointInfo createAndInsert(@NonNull DownloadTask task) {
        final int id = task.getId();

        BreakpointInfo newInfo = new BreakpointInfo(id, task.getUrl(), task.getParentFile(),
                task.getFilename());
        synchronized (this) {
            storedInfos.put(id, newInfo);
            unStoredTasks.remove(id);
        }
        return newInfo;
    }

    @Override public void onTaskStart(int id) {
    }

    @Override public void onSyncToFilesystemSuccess(@NonNull BreakpointInfo info, int blockIndex,
                                                    long increaseLength) throws IOException {
        final BreakpointInfo onCacheOne = this.storedInfos.get(info.id);
        if (info != onCacheOne) throw new IOException("Info not on store!");

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
            synchronized (this) {
                this.storedInfos.put(breakpointInfo.id, breakpointInfo.copy());
            }
            return true;
        }

        return false;
    }

    @Override
    public void onTaskEnd(int id, @NonNull EndCause cause, @Nullable Exception exception) {
        if (cause == EndCause.COMPLETED) {
            remove(id);
        }
    }

    @Nullable @Override public BreakpointInfo getAfterCompleted(int id) {
        return null;
    }

    @Override
    public boolean markFileDirty(int id) {
        if (!fileDirtyList.contains(id)) {
            synchronized (fileDirtyList) {
                if (!fileDirtyList.contains(id)) {
                    fileDirtyList.add(id);
                    return true;
                }
            }
        }
        return false;
    }

    @Override public boolean markFileClear(int id) {
        synchronized (fileDirtyList) {
            return fileDirtyList.remove(Integer.valueOf(id));
        }
    }

    @Override public synchronized void remove(int id) {
        storedInfos.remove(id);
        if (unStoredTasks.get(id) == null) sortedOccupiedIds.remove(Integer.valueOf(id));
        keyToIdMap.remove(id);
    }

    @Override
    public synchronized int findOrCreateId(@NonNull DownloadTask task) {
        final Integer candidate = keyToIdMap.get(task);
        if (candidate != null) return candidate;

        final int size = storedInfos.size();
        for (int i = 0; i < size; i++) {
            final BreakpointInfo info = storedInfos.valueAt(i);
            if (info != null && info.isSameFrom(task)) {
                return info.id;
            }
        }

        final int unStoredSize = unStoredTasks.size();
        for (int i = 0; i < unStoredSize; i++) {
            final IdentifiedTask another = unStoredTasks.valueAt(i);
            if (another == null) continue;
            if (another.compareIgnoreId(task)) return another.getId();
        }

        final int id = allocateId();
        unStoredTasks.put(id, task.mock(id));
        keyToIdMap.add(task, id);
        return id;
    }

    // info maybe turn to equal to another one after get filename from response.
    @Override
    public BreakpointInfo findAnotherInfoFromCompare(@NonNull DownloadTask task,
                                                     @NonNull BreakpointInfo ignored) {
        final SparseArray<BreakpointInfo> clonedMap;
        synchronized (this) {
            clonedMap = storedInfos.clone();
        }
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

    @Override public boolean isOnlyMemoryCache() {
        return true;
    }

    @Override public boolean isFileDirty(int id) {
        return fileDirtyList.contains(id);
    }

    @Nullable @Override public String getResponseFilename(String url) {
        return responseFilenameMap.get(url);
    }

    public static final int FIRST_ID = 1;

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