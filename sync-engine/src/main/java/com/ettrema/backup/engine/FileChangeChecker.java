package com.ettrema.backup.engine;

import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Repo;
import java.io.File;

/**
 *
 * @author brad
 */
public interface FileChangeChecker {

    public enum SyncStatus {

        LOCAL_NEWER,
        REMOTE_NEWER,
        CONFLICT,
        IDENITICAL
    }


    /**
     *
     * @param repo - the repo being checked against
     * @param remoteMeta - meta information about the remote (or repository) file
     * @param localFile - the local file being checked
     * @return
     */
    SyncStatus checkFile( Repo repo, FileMeta remoteMeta, File localFile );


    /**
     * Do a quick, partial, probably local only check of the file.
     *
     * @param repo
     * @param localFile
     * @return
     */
    SyncStatus checkFileFast( Repo repo, File localFile );
}
