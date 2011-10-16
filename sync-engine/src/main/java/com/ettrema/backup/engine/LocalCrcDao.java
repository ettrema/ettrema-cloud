/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ettrema.backup.engine;

import com.ettrema.backup.config.Repo;
import com.ettrema.backup.engine.DateAndLong;
import java.io.File;

/**
 *
 * @author brad
 */
public interface LocalCrcDao {

    /**
     * Look in the versions table for an entry of this file backed up to
     * the given repo.
     *
     * If none found return null
     *
     * @param localFile
     * @param repo
     * @return
     */
    DateAndLong getLocalBackedupCrc(final File localFile, final Repo repo);

    /**
     * Check the current table for a cached CRC. If none present, or is out of date
     * calculate a new CRC and persist it
     *
     * @param localFile
     * @return
     */
    long getLocalCurrentCrc(final File localFile);

    /**
     * Called after a file has been backed up, or we've found that a local file
     * has already been backed up and is identical
     *
     * @param localFile
     * @param repo
     * @param crc
     */
    void setLocalBackedupCrc(final File localFile, final Repo repo, final long crc);

}
