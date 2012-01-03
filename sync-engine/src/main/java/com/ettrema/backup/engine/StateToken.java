package com.ettrema.backup.engine;

/**
 *
 * @author brad
 */
public class StateToken {
    private String fullPath;
    private long crc;
    private long time;

    public StateToken(String fullPath, long crc, long time) {
        this.fullPath = fullPath;
        this.crc = crc;
        this.time = time;
    }

    public long getCrc() {
        return crc;
    }

    public String getFullPath() {
        return fullPath;
    }

    public long getTime() {
        return time;
    }            
}
