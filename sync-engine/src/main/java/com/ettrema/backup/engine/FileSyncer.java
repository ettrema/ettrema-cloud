package com.ettrema.backup.engine;

import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import com.ettrema.common.Service;
import java.io.File;

/**
 *
 * @author brad
 */
public interface FileSyncer extends Service {
	
	/**
	 * Initiate a scan. This MUST NOT be a blocking call
	 */
	void scan();
	
	void onFileModified(File child, Root root);
	
    void onFileDeleted( File child, Job job, Root root ) ;

    void onFileMoved( String fullPathFrom, File dest, Job job, Root root );

	/**
	 * If a scan is running, cancel it
	 */
	void cancelScan();

	/**
	 * Is a scan currently running
	 * 
	 * @return 
	 */
	boolean isScanning();

	void setScanningDisabled(boolean state);
	
	/**
	 * Get the directory which is currently being scanned, or null if there is no
	 * scanning in progress
	 * 
	 * @return 
	 */
	File getCurrentScanDir();
	
	/**
	 * Duration, in seconds, until the next scan is scheduled to run
	 * 
	 * @return 
	 */
	long delayUntilNextScanSecs();
}
