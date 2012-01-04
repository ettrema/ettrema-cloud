package com.ettrema.backup.engine;

/**
 *
 * @author brad
 */
public class StateToken {
	
	
	/**
	 * Absolute file path
	 */
	public String filePath;
	/**
	 * The CRC of the file or directory. If a directory, this is a CRC of the
	 * concatencation of its member's names and CRC values
	 */
	public long currentCrc;
	/**
	 * The time at which the current CRC was generated
	 */
	public long currentTime;	
	/**
	 * The CRC of the file at the time it was last uploaded
	 */
	public Long backedupCrc;
	/**
	 * The time at which the file was uploaded
	 */
	public Long backedupTime;
	
	public StateToken(String filePath) {
		this.filePath = filePath;
	}
	
}
