package com.ettrema.backup.engine;

import java.io.IOException;
import java.util.Date;
import org.apache.commons.io.FileUtils;

import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Repo;
import java.io.File;

/**
 *
 * @author brad
 */
public class ModifiedDateFileChangeChecker implements FileChangeChecker {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ModifiedDateFileChangeChecker.class );

    @Override
    public SyncStatus checkFileFast(Repo repo, File localFile) {
        return SyncStatus.IDENITICAL;
    }


    @Override
    public SyncStatus checkFile( Repo repo, FileMeta remoteMeta, File localFile ) {
        log.trace("checkFile: " + localFile.getAbsolutePath());
        if( !localFile.exists() ) {
            // this might not actually be true. if the local file has been deleted
            // then logically that deletion should be applied to the repository
            // But we'll need local version history to know that
            log.trace("local file does not exist");
            return SyncStatus.REMOTE_NEWER;
        }
        if( remoteMeta.getModifiedDate() != null ) {
            log.trace("remote modified date: " + remoteMeta.getModifiedDate());
            long remoteMod = remoteMeta.getModifiedDate().getTime();            
            if( remoteMod >= localFile.lastModified() ) {
                // note that we can't say that remote is newer as you might expect;
                // because when we copy a file it will always have a later mod date then local
                // basically, we can't do bi-directional sync with a date based algorithm
                log.trace("remote file is newer, but there's no way to know if its actually been modified after the local");
                return SyncStatus.IDENITICAL;
            } else {
                if( log.isTraceEnabled() ) {
                    Date localDate = new Date( localFile.lastModified() );
                    Date remoteDate = remoteMeta.getModifiedDate();
                    log.trace( "local file is newer: local date: " + localDate + "  remote date: " + remoteDate );
                }

                // If the mod date is in the future touch it to reset the mod date
                if( localFile.lastModified() > System.currentTimeMillis() ) {
                    try {
                        // modified in the future
                        FileUtils.touch( localFile );
                    } catch( IOException ex ) {
                        log.warn( "Failed to touch file with future mod date: " + localFile.getAbsolutePath() );
                    }
                }
                return SyncStatus.LOCAL_NEWER;
            }
        } else {
            // no remote mod date, assume we need to upload
            log.warn( "no remote mod date!! - " + localFile.getAbsolutePath() );
            return SyncStatus.LOCAL_NEWER;
        }
    }

    /**
     *
     * @return - true indicates that the files are identical
     */
//    public boolean isIdentical( FileMeta meta, File localFile ) {
//        if( meta.getCrc() == null ) {
//            return false; // can't tell
//        }
//        long remoteCrc = meta.getCrc();
//        long localCrc = getLocalCrc( localFile );
//        log.trace( "is equal: " + remoteCrc + " == " + localCrc);
//        return remoteCrc == localCrc;
//    }



}
