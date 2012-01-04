package com.ettrema.backup.queue;

import com.ettrema.backup.config.Repo;
import java.io.File;

/**
 * Represents an action to take when a resource is moved remotely, meaning that
 * we should probably make a corresponding change to the local resource
 *
 * @author bradm
 */
public class RemotelyMovedQueueItem extends AbstractQueueItem {

	private final File file;
	private final File movedToFile;
	private transient Repo repo;

	public RemotelyMovedQueueItem(File file, File movedToFile, Repo repo) {
		this.file = file;
		this.movedToFile = movedToFile;
		this.repo = repo;
	}

	@Override
	public String getActionDescription() {
		return "move local";
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

	public File getMovedToFile() {
		return movedToFile;
	}

	@Override
	public void setProgressBytes(long bytes) {
		
	}

	@Override
	public long getProgressBytes() {
		return 0;
	}
	
	
}
