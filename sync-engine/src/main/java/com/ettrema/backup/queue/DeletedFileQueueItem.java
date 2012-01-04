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

    public DeletedFileQueueItem( File file ) {
        this.file = file;
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
	public void setProgressBytes(long bytes) {
		
	}

	@Override
	public long getProgressBytes() {
		return 0;
	}
        
}
