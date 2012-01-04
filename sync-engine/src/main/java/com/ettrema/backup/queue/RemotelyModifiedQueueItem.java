package com.ettrema.backup.queue;

import com.ettrema.backup.config.FileMeta;
import java.io.File;

/**
 *
 * @author brad
 */
public class RemotelyModifiedQueueItem extends AbstractQueueItem {

	private final File file;
	private final Long bytesToDownload;
	private boolean conflicted;
	private long doneBytes;

	public RemotelyModifiedQueueItem(File file, FileMeta fileMeta) {
		this.file = file;
		if (fileMeta != null) {
			this.bytesToDownload = fileMeta.getLength();
		} else {
			bytesToDownload = null;
		}
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
		if( bytesToDownload == null ) {
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
