package com.ettrema.backup.queue;

import java.io.File;

/**
 *
 * @author brad
 */
public class NewFileQueueItem extends  AbstractQueueItem {

    private final File file;
    private boolean updated;
    private final long bytesToUpload;
	private long doneBytes;

    public NewFileQueueItem( File file ) {
        this.file = file;
        this.bytesToUpload = file.length();
    }

    @Override
    public String getActionDescription() {
        return "upload";
    }



    /**
     * @return the file
     */
    @Override
    public File getFile() {
        return file;
    }

    public void setUpdated( boolean b ) {
        this.updated = b;
    }

    public boolean isUpdated() {
        return updated;
    }

    @Override
    public String toString() {
        return "Upload: " + file.getAbsolutePath();
    }


    @Override
    public long getLastModified() {
        return this.file.lastModified();
    }

    @Override
    public long getBytesToUpload() {
        return bytesToUpload;
    }

    @Override
    public String getFileName() {
        return file.getName();
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
