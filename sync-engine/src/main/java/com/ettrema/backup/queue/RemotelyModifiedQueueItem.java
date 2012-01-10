package com.ettrema.backup.queue;

import java.io.File;

/**
 * Bit of a mis-nomer, this class is used for whenever a remote item must
 * be downloaded. This can be both new or modified, and can apply to a single
 * file or a folder
 * 
 * In the case of a new folder containing files, only a single queue item should be
 * created to download the lot.
 *
 *
 * @author brad
 */
public class RemotelyModifiedQueueItem extends AbstractQueueItem {

    private final File file;
    private final Long bytesToDownload;
    private boolean conflicted;
    private long doneBytes;

    public RemotelyModifiedQueueItem(File file, Long bytesToDownload) {
        this.file = file;
        this.bytesToDownload = bytesToDownload;
    }

    @Override
    public String getActionDescription() {
        return "download";
    }

    /**
     * @return the file
     */
    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return "Download: " + file.getAbsolutePath();
    }

    @Override
    public long getLastModified() {
        return this.file.lastModified();
    }

    public Long getBytesToDownload() {
        return bytesToDownload;
    }

    @Override
    public long getBytesToUpload() {
        if (bytesToDownload == null) {
            return 0;
        }
        return getBytesToDownload(); // mis-match of terminology
    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    void setConflicted(boolean b) {
        this.conflicted = b;
    }

    public boolean isConflicted() {
        return conflicted;
    }

    @Override
    public long getProgressBytes() {
        return doneBytes;
    }

    @Override
    public void setProgressBytes(long bytes) {
        this.doneBytes = bytes;
    }
}
