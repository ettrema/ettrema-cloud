package com.ettrema.backup.engine;

import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import java.io.File;

/**
 *
 * @author brad
 */
public interface FileSyncer {
	
	/**
	 * Initiate a scan. This is called infrequently so is suitable for long
	 * running tasks such as scanning the whole file system for changed files
	 */
	void scan(ScanStatus scanStatus);
		
	void onFileModified(File child, Root root);
	
    void onFileDeleted( File child, Job job, Root root ) ;

    void onFileMoved( String fullPathFrom, File dest, Job job, Root root );

}
