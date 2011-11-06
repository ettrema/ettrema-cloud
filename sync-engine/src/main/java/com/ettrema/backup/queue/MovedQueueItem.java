package com.ettrema.backup.queue;

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

	@Override
    public String getActionDescription() {
        return "move";
    }

	@Override
    public long getLastModified() {
        return timeLogged;
    }

	@Override
    public File getFile() {
        return file;
    }

	@Override
    public long getBytesToUpload() {
        return 0;
    }

	@Override
    public String getFileName() {
        return file.getName();
    }

	@Override
    public Repo getRepo() {
        return repo;
    }
}
