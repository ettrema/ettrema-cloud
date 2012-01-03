package com.ettrema.backup.engine;

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
}
