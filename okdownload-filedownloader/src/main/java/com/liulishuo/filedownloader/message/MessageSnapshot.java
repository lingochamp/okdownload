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

package com.liulishuo.filedownloader.message;

import android.os.Parcel;

import com.liulishuo.filedownloader.model.FileDownloadStatus;

import java.util.Locale;

/**
 * The message snapshot.
 */
public abstract class MessageSnapshot implements IMessageSnapshot {
    private final int id;
    protected boolean largeFile;

    MessageSnapshot(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Throwable getThrowable() {
        throw new NoFieldException("getThrowable", this);
    }

    @Override
    public int getRetryingTimes() {
        throw new NoFieldException("getRetryingTimes", this);
    }

    @Override
    public boolean isResuming() {
        throw new NoFieldException("isResuming", this);
    }

    @Override
    public String getEtag() {
        throw new NoFieldException("getEtag", this);
    }

    @Override
    public long getLargeSofarBytes() {
        throw new NoFieldException("getLargeSofarBytes", this);
    }

    @Override
    public long getLargeTotalBytes() {
        throw new NoFieldException("getLargeTotalBytes", this);
    }

    @Override
    public int getSmallSofarBytes() {
        throw new NoFieldException("getSmallSofarBytes", this);
    }

    @Override
    public int getSmallTotalBytes() {
        throw new NoFieldException("getSmallTotalBytes", this);
    }

    @Override
    public boolean isReusedDownloadedFile() {
        throw new NoFieldException("isReusedDownloadedFile", this);
    }

    @Override
    public String getFileName() {
        throw new NoFieldException("getFileName", this);
    }

    @Override
    public boolean isLargeFile() {
        return largeFile;
    }


    public interface IWarnMessageSnapshot {
        MessageSnapshot turnToPending();
    }


    MessageSnapshot(Parcel in) {
        this.id = in.readInt();
    }

    public static class NoFieldException extends IllegalStateException {
        NoFieldException(String methodName, MessageSnapshot snapshot) {
            super(String.format(Locale.ENGLISH, "There isn't a field for '%s' in this message"
                            + " %d %d %s",
                    methodName, snapshot.getId(), snapshot.getStatus(),
                    snapshot.getClass().getName()));
        }
    }

    // Started Snapshot
    public static class StartedMessageSnapshot extends MessageSnapshot {

        StartedMessageSnapshot(int id) {
            super(id);
        }

        StartedMessageSnapshot(Parcel in) {
            super(in);
        }

        @Override
        public byte getStatus() {
            return FileDownloadStatus.started;
        }
    }
}
