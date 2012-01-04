package com.ettrema.backup.engine;

import com.ettrema.httpclient.Resource;
import java.io.File;
import java.util.List;

/**
 * This is used to notify the user of conflicts once conflicting remote files
 * have been downloaded. It should prompt the user to resolve the conflict, usually
 * by merging changes between the old and new files
 *
 * @author brad
 */
public interface ConflictManager {
    void onConflict(File currentLocalFile, File remoteFile);

	List<File> getConflicts();

	/**
	 * Called when the conflict is between the type of resource ie file vs directory
	 * 
	 * @param l
	 * @param childFile 
	 */
	void onTreeConflict(File l, Resource childFile);
}
