package com.ettrema.backup.queue;

import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.Root;
import java.io.File;

/**
 *
 * @author brad
 */
public class DeletedFileQueueItem extends AbstractQueueItem{

    private final File file;
    private final long timeLogged;
    private final Root root;

    private transient Repo repo;

    public DeletedFileQueueItem( File file, Repo repo, Root root ) {
        this.file = file;
        this.repo = repo;
        this.root = root;
        this.timeLogged = System.currentTimeMillis();
    }

    @Override
    public String getActionDescription() {
        return "delete";
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

    public Root getRoot() {
        return root;
    }
        
}
