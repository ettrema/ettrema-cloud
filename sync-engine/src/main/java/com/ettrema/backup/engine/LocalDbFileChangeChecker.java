package com.ettrema.backup.engine;

import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Repo;
import java.io.File;
import java.sql.SQLException;
import java.util.Date;

/**
 * A FileChangeChecker which works by comparing normalised modified dates.
 *
 * This is for repositories which don't support CRC meta data
 *
 * @author brad
 */
public class LocalDbFileChangeChecker implements FileChangeChecker {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( LocalDbFileChangeChecker.class );
    private final FileChangeChecker wrapped;
    private final LocalCrcDaoImpl localCrcDao;

    public LocalDbFileChangeChecker( LocalCrcDaoImpl localCrcDao, FileChangeChecker wrapped ) throws SQLException {
        this.wrapped = wrapped;
        this.localCrcDao = localCrcDao;
    }

    @Override
    public SyncStatus checkFile( Repo repo, FileMeta remoteMeta, File localFile ) {
//        return SyncStatus.IDENITICAL;
        long tm = System.currentTimeMillis();
        try {
            if( remoteMeta.getCrc() != null ) {
                // Get current CRC for the local file. This should be cached in DB
                long localCurrentCrc = localCrcDao.getLocalCurrentCrc( localFile );
                log.trace("local crc: " + localCurrentCrc + " remote: " + remoteMeta.getCrc());
                if( localCurrentCrc == remoteMeta.getCrc() ) {
                    // Check that we know the backed up CRC
                    // Only thing is this might be a bit inefficient. Means hitting the DB lots even when nothing's happening
                    DateAndLong localBackedupCrc = localCrcDao.getLocalBackedupCrc( localFile, repo );
                    if( localBackedupCrc == null ) {
                        log.trace( "files are identical, but no backed up CRC so update it" );
                        localCrcDao.setLocalBackedupCrc( localFile, repo, localCurrentCrc );
                    }
                    log.trace( "checkFile: identical: " + localFile.getAbsolutePath() );
                    return SyncStatus.IDENITICAL;
                } else {
                    log.trace( "local crc differs from remote: local: " + localCurrentCrc + " != " + remoteMeta.getCrc() );
                    DateAndLong localBackedupCrc = localCrcDao.getLocalBackedupCrc( localFile, repo );
                    if( localBackedupCrc == null ) {
                        log.trace( "no local backedup crc, indicates file has not been backed up, but is on server - treat as conflict;" );
                        // means that it has never been backed up from this device
                        // its a new file, but someone else has made a new file too!
                        return SyncStatus.CONFLICT;
                    } else {
                        if( localBackedupCrc.getLong().equals( remoteMeta.getCrc()) ) {
                            // the server holds what was previously backedup, but this differs from current
                            // This is the normal case, the user has updated the local file
                            log.trace( "local backed up crc == remote crc, but current local differs. So locally updated" );
                            return SyncStatus.LOCAL_NEWER;
                        } else {
                            // The server version differs from what was backed up, meaning another
                            // user or device has uploaded a version to the server
                            log.trace("local backed up != remote crc - " + localBackedupCrc.getLong() + " != " + remoteMeta.getCrc());
                            if( localCurrentCrc == localBackedupCrc.getLong() ) {
                                // the backedup and current are the same, so user has not made edits
                                log.trace( "local current crc == local backed up crc, but remote differs. So remote is updated" );
                                return SyncStatus.REMOTE_NEWER;
                            } else {
                                // use has made local edits, but server has been updated too
                                log.trace( "local current differs from local backed up, which differs from remote. So conflict: " +  localCurrentCrc + " != " + localBackedupCrc );
                                return SyncStatus.CONFLICT;
                            }
                        }
                    }
                }
            } else {
                // if no CRC fall through to default checking
                log.trace("no crc from remote host, can't check file contents: " + localFile.getAbsolutePath());
                return wrapped.checkFile( repo, remoteMeta, localFile );
            }
        } finally {
            tm = System.currentTimeMillis() - tm;
            log.trace("checkFile time: " + tm + "ms");
        }
    }


    /**
     * Just does mininal checks of the file against the local database
     *
     * @param repo
     * @param localFile
     * @return
     */
    @Override
    public SyncStatus checkFileFast( Repo repo, File localFile ) {
        DateAndLong dateAndCrc = localCrcDao.getLocalBackedupCrc(localFile, repo);
        if( dateAndCrc == null ) {
            log.trace("no crc so is localy new");
            return SyncStatus.LOCAL_NEWER;
        } else if( isModifiedSince(localFile, dateAndCrc.getDate())) {
            // means that the file has been modified since it was backed up. This
            // doesnt mean the content was modified, but the update file handler
            // will check that.
			log.trace("Local file has a modified date later then the remote file.");
            return SyncStatus.LOCAL_NEWER;
        } else {
            return SyncStatus.IDENITICAL;
        }
    }

    private boolean isModifiedSince(File localFile, Date date) {
        return localFile.lastModified() > date.getTime();
    }
}
