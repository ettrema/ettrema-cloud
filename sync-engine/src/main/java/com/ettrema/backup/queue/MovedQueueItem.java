package com.ettrema.backup.queue;

import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import java.io.File;

/**
 *
 * @author brad
 */
public class MovedQueueItem extends AbstractQueueItem {

    private final File file;
    private final String fullPathFrom;
    private final long timeLogged;
    private transient Repo repo;

    public MovedQueueItem( String fullPathFrom, File dest, Repo repo ) {
        this.fullPathFrom = fullPathFrom;
        this.file = dest;
        this.repo = repo;
        this.timeLogged = System.currentTimeMillis();
    }

    public String getFullPathFrom() {
        return fullPathFrom;
    }

    public String getActionDescription() {
        return "move";
    }

    public long getLastModified() {
        return timeLogged;
    }

    public File getFile() {
        return file;
    }

    public long getBytesToUpload() {
        return 0;
    }

    public String getFileName() {
        return file.getName();
    }

    public Repo getRepo() {
        return repo;
    }
}
