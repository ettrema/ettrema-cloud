package com.ettrema.backup.engine;

/**
 *
 * @author brad
 */
public class StateToken implements Comparable<StateToken> {
	
	
	/**
	 * Absolute file path
	 */
	public String filePath;
	/**
	 * The CRC of the file or directory. If a directory, this is a CRC of the
	 * concatencation of its member's names and CRC values
         * 
         * A null value indicates that the file was present but has been removed
	 */
	public Long currentCrc;
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

	@Override
	public int compareTo(StateToken o) {
		// Comparison must be same as on the server
		String path1 = this.filePath.toUpperCase();
		String path2 = o.filePath.toUpperCase();
		return path1.compareTo(path2);
	}
	
}
