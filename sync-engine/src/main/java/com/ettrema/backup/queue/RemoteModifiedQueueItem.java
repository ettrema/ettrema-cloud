package com.ettrema.backup.queue;

import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Repo;
import java.io.File;

/**
 *
 * @author brad
 */
public class RemoteModifiedQueueItem extends AbstractQueueItem {

	private final File file;
	private final Long bytesToDownload;
	private boolean conflicted;
	private transient Repo repo;

	public RemoteModifiedQueueItem(File file, Repo repo, FileMeta fileMeta) {
		this.file = file;
		this.repo = repo;
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

	@Override
	public Repo getRepo() {
		return repo;
	}

	void setConflicted(boolean b) {
		this.conflicted = b;
	}

	public boolean isConflicted() {
		return conflicted;
	}
}
