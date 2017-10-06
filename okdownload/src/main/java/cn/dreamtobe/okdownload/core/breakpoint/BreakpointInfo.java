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

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BreakpointInfo {
    final int id;
    public final Profile profile;
    final List<BlockInfo> blockInfoList;

    BreakpointInfo(int id, Profile profile) {
        this.id = id;
        this.profile = profile;
        this.blockInfoList = new ArrayList<>();
    }

    public boolean isLastBlock(int blockIndex) {
        return blockInfoList.size() == blockIndex + 1;
    }

    public void addBlock(BlockInfo blockInfo) {
        this.blockInfoList.add(blockInfo);
    }

    public BlockInfo getBlock(int blockIndex) {
        return blockInfoList.get(blockIndex);
    }

    public void resetBlockInfos() {
        this.blockInfoList.clear();
    }

    public int getBlockCount() {
        return blockInfoList.size();
    }

    public static class Profile {
        public final String url;
        String etag;
        public final Uri uri;

        Profile(String url, Uri uri) {
            this.url = url;
            this.uri = uri;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }

        public @Nullable
        String getEtag() {
            return this.etag;
        }
    }
}
