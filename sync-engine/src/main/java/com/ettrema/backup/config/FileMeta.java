package com.ettrema.backup.config;

import java.util.Date;
import java.util.List;

/**
 * Basic information about a repository resource
 *
 * @author brad
 */
public interface FileMeta {

    /**
     * @return the modifiedDate
     */
    Date getModifiedDate();

    /**
     * @return the length
     */
    long getLength();

    boolean isDirectory();

    Long getCrc();

    /**
     * The name of the repository resource within its folder
     *
     * @return
     */
    String getName();

    /**
     * If this is a directory, return its direct children. Otherwise, return
     * null
     *
     * @return
     */
    List<FileMeta> getChildren() throws RepoNotAvailableException;
}
