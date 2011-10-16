package com.ettrema.backup.config;

import java.util.Date;

/**
 * Basic information about a repository resource
 *
 * @author brad
 */
public class FileMeta {
    private final String name;
    private Date modifiedDate;
    private Long length;
    private boolean directory;
    private Long crc;

    public FileMeta(String name) {
        this.name = name;
    }



    /**
     * @return the modifiedDate
     */
    public Date getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @param modifiedDate the modifiedDate to set
     */
    public void setModifiedDate( Date modifiedDate ) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * @return the length
     */
    public long getLength() {
        if( length == null ) {
            return 0;
        } else {
            return length;
        }
    }

    /**
     * @param length the length to set
     */
    public void setLength( Long length ) {
        this.length = length;
    }

    public void setDirectory( boolean directory ) {
        this.directory = directory;
    }

    public boolean isDirectory() {
        return directory;
    }

    public Long getCrc() {
        return crc;
    }

    public void setCrc( Long crc ) {
        this.crc = crc;
    }

    /**
     * The name of the repository resource within its folder
     * 
     * @return
     */
    public String getName() {
        return name;
    }


}
