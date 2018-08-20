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

package com.liulishuo.filedownloader.stream;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.SyncFailedException;

/**
 * The output stream used to write the file for download.
 */

public interface FileDownloadOutputStream {

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this file.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    void write(byte b[], int off, int len) throws IOException;

    /**
     * Flush all buffer to system and force all system buffers to synchronize with the underlying
     * device.
     * <p>
     * This method must ensure all data whatever on buffers of VM or buffers of system for this
     * output stream must persist on the physical media, otherwise the breakpoint will not be
     * integrity.
     *
     * @throws SyncFailedException Thrown when the buffers cannot be flushed,
     *                             or because the system cannot guarantee that all the
     *                             buffers have been synchronized with physical media.
     * @see OutputStream#flush()
     * @see FileDescriptor#sync()
     */
    void flushAndSync() throws IOException;

    /**
     * Closes this output stream and releases any system resources associated with this stream. The
     * general contract of <code>close</code> is that it closes the output stream. A closed stream
     * cannot perform output operations and cannot be reopened.
     * <p>
     * The <code>close</code> method of <code>OutputStream</code> does nothing.
     *
     * @throws IOException if an I/O error occurs.
     */
    void close() throws IOException;

    /**
     * Sets the file-pointer offset, measured from the beginning of this file, at which the next
     * read or write occurs.  The offset may be set beyond the end of the file. Setting the offset
     * beyond the end of the file does not change the file length.  The file length will change only
     * by writing after the offset has been set beyond the end of the file.
     *
     * @param offset the offset position, measured in bytes from the
     *               beginning of the file, at which to set the file
     *               pointer.
     * @throws IOException            if <code>offset</code> is less than
     *                                <code>0</code> or if an I/O error occurs.
     * @throws IllegalAccessException if in this output stream doesn't support this function.
     *                                You can return {@code false} in
     *                                FileDownloadHelper.OutputStreamCreator#supportSeek()
     *                                let the internal mechanism can predict this situation, and
     *                                handle it smoothly.
     * @see java.io.RandomAccessFile#seek(long)
     * @see java.nio.channels.FileChannel#position(long)
     */
    void seek(long offset) throws IOException, IllegalAccessException;

    /**
     * Sets the length of this file.
     * <p>
     * <p> If the present length of the file as returned by the <code>length</code> method is
     * greater than the <code>newLength</code> argument then the file will be truncated.  In this
     * case, if the file offset as returned by the <code>getFilePointer</code> method is greater
     * than <code>newLength</code> then after this method returns the offset will be equal to
     * <code>newLength</code>.
     * <p>
     * <p> If the present length of the file as returned by the <code>length</code> method is
     * smaller than the <code>newLength</code> argument then the file will be extended.  In this
     * case, the contents of the extended portion of the file are not defined.
     *
     * @param newLength The desired length of the file
     * @throws IOException            If an I/O error occurs
     * @throws IllegalAccessException If in this output stream doesn't support this function.
     * @see java.io.RandomAccessFile#setLength(long)
     */
    void setLength(long newLength) throws IOException, IllegalAccessException;
}
