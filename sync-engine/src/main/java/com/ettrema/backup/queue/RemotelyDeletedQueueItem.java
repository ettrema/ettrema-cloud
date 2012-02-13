package com.ettrema.backup.queue;

import com.ettrema.backup.config.Repo;
import java.io.File;

/**
 *
 * @author bradm
 */
public class RemotelyDeletedQueueItem extends AbstractQueueItem {

    private final File file;

    public RemotelyDeletedQueueItem(File file) {
        this.file = file;
    }

    @Override
    public String getActionDescription() {
        return "delete local";
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
        return "Delete: " + file.getAbsolutePath();
    }

    @Override
    public long getLastModified() {
        return this.file.lastModified();
    }

    public Long getBytesToDownload() {
        return 0l;
    }

    @Override
    public long getBytesToUpload() {
        return getBytesToDownload(); // mis-match of terminology
    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    @Override
    public void setProgressBytes(long bytes) {
    }

    @Override
    public long getProgressBytes() {
        return 0;
    }
}
