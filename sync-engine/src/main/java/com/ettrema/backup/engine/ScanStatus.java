package com.ettrema.backup.engine;

import java.io.File;

/**
 *
 * @author brad
 */
public class ScanStatus {
	boolean cancelled;
	File currentScanDir;	
	
	public boolean enabled() {
		return !cancelled;
	}
}
