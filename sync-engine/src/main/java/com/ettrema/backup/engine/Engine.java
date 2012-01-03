package com.ettrema.backup.engine;

import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.queue.QueueInserter;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Configurator;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Dir;
import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.engine.FileChangeChecker.SyncStatus;
import com.ettrema.backup.event.RepoChangedEvent;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.event.EventManager;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One per application
 *
 * @author brad
 */
public class Engine {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Engine.class );
    private final Config config;
    private final Configurator configurator;
    private final QueueInserter queueHandler;
    private final FileChangeChecker fileChangeChecker;
    private final EventManager eventManager;
    private final ThrottleFactory throttleFactory;
    private final CrcCalculator crcCalculator;

    private static Scanner scanner;
    private List<File> conflicts = new CopyOnWriteArrayList<File>();
    private boolean disableScanning;

    public Engine( ThrottleFactory throttleFactory, Config config, Configurator configurator, EventManager eventManager, FileChangeChecker fileChangeChecker,CrcCalculator crcCalculator) {
        this.throttleFactory = throttleFactory;
        this.config = config;
        this.configurator = configurator;
        this.eventManager = eventManager;
        this.queueHandler = new QueueInserter( eventManager );
        this.fileChangeChecker = fileChangeChecker;
        this.crcCalculator = crcCalculator;
    }

    /**
     * may be null if not scanning
     *
     * @return
     */
    public Scanner getScanner() {
        return scanner;
    }

    public void scan() {
        if( scanner != null ) {
            log.info( "not doing scan, because a scan is already running" );
            return;
        }
        if( throttleFactory.isPaused() ) {
            log.trace( "throttle is off, so cant scan" );
            return;
        }

        if( disableScanning ) {
            log.trace( "not scanning because is disabled" );
            return;
        }

        log.trace( "scan" );
        try {
            scanner = new Scanner( this, config, eventManager, crcCalculator);
            scanner.scan();
            if( !config.isPaused() ) {
                configurator.save( config );
            }
        } catch( Throwable e ) {
            log.error( "Exception doing scan", e );
        } finally {
            log.trace( "completed scan, reseting scanner" );
            scanner = null;
        }
    }

    /**
     * Called from the FileWatcher when a file deletion event has occurred.
     * 
     * @param f
     * @param job
     * @param root
     */
    public void onFileDeleted( File child, Job job, Root root ) {
        log.debug( "onFileDeleted: " + child.getAbsolutePath() );
        if( !isBackupable( child, root ) ) {
            return;
        }

        for( Repo r : job.getRepos() ) {
            queueHandler.onFileDeleted( child, job, root, r );
        }
    }

    public void onFileMoved( String fullPathFrom, File dest, Job job, Root root ) {
        log.debug( "onFileMoved: " + dest.getAbsolutePath() );
        if( !isBackupable( dest, root ) ) {
            return;
        }

        for( Repo r : job.getRepos() ) {
            queueHandler.onMoved( fullPathFrom, dest, job, root, r );
        }

    }

    /**
     * For those times you don't know the meta
     * @param r
     * @param file
     * @param root 
     */
    public void checkFileUpdated(Repo r, File file, Root root) throws RepoNotAvailableException {
        FileMeta meta = r.getFileMeta(file.getAbsolutePath(), root.getFullPath(), root.getRepoName(), false);
        checkFileUpdated(r, file, meta, root);
    }    

    public void checkFileUpdatedFast(Repo r, File child, Root root) {
        SyncStatus syncStatus = fileChangeChecker.checkFileFast(r, child);
        if( syncStatus == SyncStatus.LOCAL_NEWER) {
            log.trace( "checkFileUpdated - local file is new, so upload: " + child.getAbsolutePath() );
            queueHandler.onUpdatedFile( r, child );
        }
    }

    /**
     *
     * @param r
     * @param child
     * @param meta
     * @param root
     * @return - true if the file was uploaded
     */
    public void checkFileUpdated( Repo r, File child, FileMeta meta, Root root) {
        if( meta == null ) {
            log.trace( "checkFileUpdated - no meta, new file:" + child.getAbsolutePath() );
            queueHandler.onNewFile( r, child );
        } else {
            SyncStatus syncStatus = fileChangeChecker.checkFile( r, meta, child );
            switch( syncStatus ) {
                case LOCAL_NEWER:
                    log.trace( "checkFileUpdated - local file is new, so upload: " + child.getAbsolutePath() );
                    queueHandler.onUpdatedFile( r, child );
                    break;
                case REMOTE_NEWER:                    
                    log.trace( "checkFileUpdated - remote file is newer, so download: " + child.getAbsolutePath() );
                    queueHandler.onRemotelyUpdatedFile( r, child, meta );
                    
//                    if( r.isSync() ) {
//                        log.trace( "checkFileUpdated - remote file is newer, so download: " + child.getAbsolutePath() );
//                        queueHandler.onRemotelyUpdatedFile( r, root.getFullPath(), child, meta );
//                    } else {
//                        log.trace("remote file is newer, but sync is off so will upload");
//                        queueHandler.onUpdatedFile( r, root.getFullPath(), child );
//                    }
                    break;
                case CONFLICT:                    
                    log.trace( "checkFileUpdated - conflict: " + child.getAbsolutePath() );
                    queueHandler.onConflict( r, root.getFullPath(), child, meta );
                    
//                    if( r.isSync() ) {
//                        log.trace( "checkFileUpdated - conflict: " + child.getAbsolutePath() );
//                        conflicts.add( child );
//                        queueHandler.onConflict( r, root.getFullPath(), child, meta );
//                    } else {
//                        log.trace("local and remote files differ, but sync is false so will upload");
//                        queueHandler.onUpdatedFile( r, root.getFullPath(), child );
//                    }
                    break;
                default:
                    log.trace( "checkFileUpdated - files are identical: " + child.getAbsolutePath() );
            }
        }
    }

    public boolean isBackupable( File child, Root root ) {
        File f = child;
        if( f.isFile() && f.length() == 0 ) {
            log.trace( "not uploading empty file: " + f.getAbsolutePath() );
            return false;
        }
        if( f.isFile() && f.getName().contains( ".conflicted." ) ) {
            log.trace( "not uploading conflict file: " + f.getAbsolutePath() );
            return false;
        }
        while( f.getAbsolutePath().startsWith( root.getFullPath() ) ) {
            if( f.getName().equals( "Thumb.db" ) ) {
                log.trace( "is thumbs.db" );
                return false;
            }
            if( f.isHidden() ) {
                //log.trace( "ishidden" );
                return false;
            }
            if( f.getName().startsWith( "." ) ) {
                log.trace( "starts with ." );
                return false;
            }
            if( f.getName().endsWith( ".tmp" ) ) {
                log.trace( "ends with .tmp" );
                return false;
            }
            if( f.getName().startsWith( "~" ) ) {
                log.trace( "starts with tilda" );
                return false;
            }
            f = f.getParentFile();
        }

        return true;
    }

    public boolean isExcludedFolder( File dir, Root root ) {
        if( dir.getAbsolutePath().equals( root.getFullPath() ) ) {
            // can't exclude a root
            return false;
        }
        List<Dir> exclusions = root.getExclusions();
        String s = dir.getAbsolutePath();
        for( Dir d : exclusions ) {
            if( s.startsWith( d.getFullPath() ) ) {
                return true;
            }
        }
        return false;
    }

    public void setOffline( Job j, Repo r ) {
        log.info( "setOffline: " + r.toString() );
        r.setOffline( true );
        EventUtils.fireQuietly( eventManager, new RepoChangedEvent( r ) );
    }

    public boolean isOffline( Repo r ) {
        return r.isOffline();
    }

    public boolean isAllReposOffline( Job job ) {
        for( Repo repo : job.getRepos() ) {
            if( !repo.isOffline() ) {
                return false;
            }
        }
        return true;
    }

    public DavRepo getFirstRepo() {
        for( Job j : config.getJobs() ) {
            for( Repo r : j.getRepos() ) {
                if( r instanceof DavRepo ) {
                    return (DavRepo) r;
                }
            }
        }
        return null;
    }

    public Config getConfig() {
        return config;
    }

    public boolean isScanning() {
        return scanner != null;
    }

    public void cancelScan() {
        if( scanner != null ) {
            scanner.setCancelled(true);
        }
    }

    public List<File> getConflicts() {
        return conflicts;
    }

    public void setScanningDisabled( boolean state ) {
        this.disableScanning = state;
        if( disableScanning ) {
            if( isScanning() ) {
                log.info( "cancelling scan because of disabled scanning command" );
                scanner.setCancelled( true );
                scanner = null;
            }
        }
    }


}
