package com.ettrema.backup.config;

/**
 * Represents an excluded
 *
 *
 * @author brad
 */
public class Dir {

    private String fullPath;
    
    public Dir() {
    }

    public Dir( String fullPath ) {
        this.fullPath = fullPath;
    }



    /**
     * @return the fullPath
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * @param fullPath the fullPath to set
     */
    public void setFullPath( String fullPath ) {
        this.fullPath = fullPath;
    }
}
