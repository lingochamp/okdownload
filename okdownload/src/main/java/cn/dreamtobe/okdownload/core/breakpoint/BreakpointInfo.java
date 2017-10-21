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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.dreamtobe.okdownload.DownloadTask;
import cn.dreamtobe.okdownload.core.Util;
import cn.dreamtobe.okdownload.core.download.DownloadStrategy;

public class BreakpointInfo {
    final int id;
    private final String url;
    private String etag;

    private final String parentPath;
    private final DownloadStrategy.FilenameHolder filenameHolder;

    private final List<BlockInfo> blockInfoList;
    private final boolean isTaskOnlyProvidedParentPath;

    public BreakpointInfo(int id, @NonNull String url, @NonNull String parentPath,
                          @Nullable String filename) {
        this.id = id;
        this.url = url;
        this.parentPath = parentPath;
        this.blockInfoList = new ArrayList<>();

        if (Util.isEmpty(filename)) {
            filenameHolder = new DownloadStrategy.FilenameHolder();
            isTaskOnlyProvidedParentPath = true;
        } else {
            filenameHolder = new DownloadStrategy.FilenameHolder(filename);
            isTaskOnlyProvidedParentPath = false;
        }
    }

    private BreakpointInfo(int id, @NonNull String url, @NonNull String parentPath,
                           @Nullable String filename, boolean isTaskOnlyProvidedParentPath) {
        this.id = id;
        this.url = url;
        this.parentPath = parentPath;
        this.blockInfoList = new ArrayList<>();

        if (Util.isEmpty(filename)) {
            filenameHolder = new DownloadStrategy.FilenameHolder();
        } else {
            filenameHolder = new DownloadStrategy.FilenameHolder(filename);
        }

        this.isTaskOnlyProvidedParentPath = isTaskOnlyProvidedParentPath;
    }

    public void addBlock(BlockInfo blockInfo) {
        this.blockInfoList.add(blockInfo);
    }

    public boolean isLastBlock(int blockIndex) {
        return blockIndex == blockInfoList.size() - 1;
    }

    public boolean isSingleBlock() {
        return blockInfoList.size() == 1;
    }

    public BlockInfo getBlock(int blockIndex) {
        return blockInfoList.get(blockIndex);
    }

    public void resetInfo() {
        this.blockInfoList.clear();
        this.etag = null;
    }

    public void resetBlockInfos() {
        this.blockInfoList.clear();
    }

    public int getBlockCount() {
        return blockInfoList.size();
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getTotalLength() {
        long length = 0;
        ArrayList<BlockInfo> list = (ArrayList<BlockInfo>) ((ArrayList) blockInfoList).clone();
        for (BlockInfo info : list) {
            length += info.getContentLength();
        }

        return length;
    }

    public @Nullable
    String getEtag() {
        return this.etag;
    }

    public String getUrl() {
        return url;
    }

    public String getFilename() {
        return filenameHolder.get();
    }

    public DownloadStrategy.FilenameHolder getFilenameHolder() {
        return filenameHolder;
    }

    public String getPath() {
        final String filename = this.filenameHolder.get();
        return filename == null ? null : new File(parentPath, filename).getAbsolutePath();
    }

    public BreakpointInfo copy() {
        final BreakpointInfo info = new BreakpointInfo(id, url, parentPath, filenameHolder.get(),
                isTaskOnlyProvidedParentPath);
        for (BlockInfo blockInfo : blockInfoList) {
            info.blockInfoList.add(blockInfo.copy());
        }
        return info;
    }

    public BreakpointInfo copyWithReplaceId(int replaceId) {
        final BreakpointInfo info = new BreakpointInfo(replaceId, url, parentPath,
                filenameHolder.get(), isTaskOnlyProvidedParentPath);
        for (BlockInfo blockInfo : blockInfoList) {
            info.blockInfoList.add(blockInfo.copy());
        }
        return info;
    }

    /**
     * You can use this method to replace url for using breakpoint info from another task.
     */
    public BreakpointInfo copyWithReplaceIdAndUrl(int replaceId, String newUrl) {
        final BreakpointInfo info = new BreakpointInfo(replaceId, newUrl, parentPath,
                filenameHolder.get(), isTaskOnlyProvidedParentPath);
        for (BlockInfo blockInfo : blockInfoList) {
            info.blockInfoList.add(blockInfo.copy());
        }
        return info;
    }

    public boolean isSameFrom(DownloadTask task) {
        if (!new File(parentPath).equals(new File(task.getParentPath()))) {
            return false;
        }

        if (!url.equals(task.getUrl())) return false;

        final String otherFilename = task.getFilename();
        if (isTaskOnlyProvidedParentPath) {
            // filename is provided by response.
            if (!task.isUriIsDirectory()) return false;

            return otherFilename == null || otherFilename.equals(filenameHolder.get());
        } else {
            // filename is provided by task.
            return otherFilename != null && otherFilename.equals(filenameHolder.get());
        }
    }

    @Override public String toString() {
        return "id[" + id + "]" + " url[" + url + "]" + " etag[" + etag + "]"
                + " parent path[" + parentPath + "]" + " filename[" + filenameHolder.get() + "]"
                + " block(s):" + blockInfoList.toString();
    }
}
