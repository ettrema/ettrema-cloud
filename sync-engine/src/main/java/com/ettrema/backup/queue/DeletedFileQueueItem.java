package com.ettrema.backup.queue;

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

    public DeletedFileQueueItem( File file, Root root ) {
        this.file = file;
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

    public Root getRoot() {
        return root;
    }

	@Override
	public void setProgressBytes(long bytes) {
		
	}

	@Override
	public long getProgressBytes() {
		return 0;
	}
        
}
