package com.ettrema.backup.engine;

import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import com.ettrema.common.Service;
import java.io.File;

/**
 *
 * @author brad
 */
public interface FileSyncer {
	
	/**
	 * Initiate a scan.
	 */
	void scan(ScanStatus scanStatus);
	
	void onFileModified(File child, Root root);
	
    void onFileDeleted( File child, Job job, Root root ) ;

    void onFileMoved( String fullPathFrom, File dest, Job job, Root root );

}
