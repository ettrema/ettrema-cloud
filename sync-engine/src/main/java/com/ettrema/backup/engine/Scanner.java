package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Dir;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.event.RootChangedEvent;
import com.ettrema.backup.event.ScanDirEvent;
import com.ettrema.backup.event.ScanEvent;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.event.EventManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Executes a scan over files and folders
 *
 * @author brad
 */
public class Scanner {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Scanner.class );
    private final Config config;
    private final Engine engine;
    private final EventManager eventManager;
    private final LocalTokenScanner localTokenScanner;
    
    private boolean cancelled;
    private File scanDir;

    public Scanner( Engine engine, Config config, EventManager eventManager, CrcCalculator crcCalculator ) {
        this.engine = engine;
        this.config = config;
        this.eventManager = eventManager;
        localTokenScanner = new LocalTokenScanner(engine, config, eventManager, crcCalculator );
    }

    public File getScanDir() {
        return scanDir;
    }

    public void scan() throws Exception {
        log.debug( "scanning" );
                
        EventUtils.fireQuietly( eventManager, new ScanEvent( true ) );

        localTokenScanner.scan();
        
        // flush old cached data
        for( Repo reng : config.getAllRepos() ) {
            reng.onScan();
            if( !reng.ping() ) {
                log.info( "setting repo offline because ping failed: " + reng.getDescription() );
                reng.setOffline( false );
            }
        }
        for( Root root : config.getAllRoots() ) {
            root.onScan();
        }

        log.trace("**** PHASE 1: Fast Scan, local data only ****");
        if (scanAgainstLocalDb()) {
            return;
        }

        log.trace("**** PHASE 2: Thorough Scan, compare with server ****");
        if (scanAgainstRepos()) {
            return;
        }


        EventUtils.fireQuietly( eventManager, new ScanEvent( false ) );

        for( Root root : config.getAllRoots() ) {
            root.onScanCompletedOk();
        }
        for( Repo reng : config.getAllRepos() ) {
            reng.onScanComplete();
        }


        log.trace( "finished scanning" );
    }

    private boolean scanAgainstRepos() {
        // begin scanning at roots
        for (Job j : config.getJobs()) {
            log.trace("scan job: " + j.toString());
            if (enabled()) {
                for (Root r : j.getRoots()) {
                    if (enabled()) {
                        log.trace("scan root: " + r.getFullPath());
                        File dir = new File(r.getFullPath());
                        scanAgainstRepo(dir, j, r.getExclusions(), r);
                        EventUtils.fireQuietly(eventManager, new RootChangedEvent(r));
                    } else {
                        log.info("paused, abort scan");
                        return true;
                    }
                }
            } else {
                log.info("paused, abort scan");
                return true;
            }
        }
        return false;
    }


    private boolean scanAgainstLocalDb() {
        log.trace("scanAgainstLocalDb.1");
        // begin scanning at roots
        for (Job j : config.getJobs()) {
            log.trace("scan job: " + j.toString());
            if (enabled()) {
                Collection<Root> roots = new ArrayList<Root>(j.getRoots());
                for (Root r : roots ) {
                    if (enabled()) {
                        log.trace("scan root: " + r.getFullPath());
                        File dir = new File(r.getFullPath());
                        scanAgainstLocalDb(dir, j, r.getExclusions(), r);
                        EventUtils.fireQuietly(eventManager, new RootChangedEvent(r));
                    } else {
                        log.info("paused, abort scan");
                        return true;
                    }
                }
            } else {
                log.info("paused, abort scan");
                return true;
            }
        }
        return false;
    }

    private void scanAgainstRepo( File scanDir, Job job, List<Dir> dirs, Root root ) {
        log.trace("scanAgainstRepo");
        if( !enabled() ) {
            log.info( "job cancelled" );
            return;
        }
        if( engine.isAllReposOffline( job ) ) {
            log.info( "Cancelling scan because all repositories are offline" );
            return;
        }
        if( isScanDirOtherRoot( scanDir, root, job ) ) {
            log.info( "not scanning because is another root" );
            return;
        }
        boolean isExcluded = !engine.isBackupable( scanDir, root );
        if( isExcluded ) {
            log.trace( "is excluded: " + scanDir.getAbsolutePath() );
            return;
        }

        setCurrentScanDir( scanDir );

        // Have a little sleep to make sure we don't saturate the CPU
        try {
            Thread.sleep( 500 );
        } catch( InterruptedException e ) {
            return;
        }

        File[] files = scanDir.listFiles();
        if( files == null || files.length == 0 ) {
            return;
        }
        for( File child : files ) {
            if( enabled() ) {
                if( child.isDirectory() ) {
                    scanAgainstRepo( child, job, dirs, root );
                } else {
                    try {
                        long tm = System.currentTimeMillis();

                        root.scanFileUpdated( child );
                        if( log.isTraceEnabled() ) {
                            tm = System.currentTimeMillis() - tm;
                            log.info( "scanned file in: " + tm + "ms" );
                        }
                        // have a little sleep to avoid saturating CPU
                        try {
                            Thread.sleep( 100 );
                        } catch( InterruptedException e ) {
                            return;
                        }


                    } catch( Throwable e ) {
                        log.error( "Exception scanning file: " + child.getAbsolutePath(), e );
                    }
                }
            } else {
                log.info( "paused, aborting scan" );
                return;
            }
        }
        for( Repo r : job.getRepos() ) {
            try {
                r.onScanDirComplete( scanDir.getAbsolutePath(), root.getFullPath(), root.getRepoName() );
            } catch( RepoNotAvailableException ex ) {
                log.info( "repository has gone offline: " + r.getDescription(), ex );
                r.setOffline( true );
            }
        }
    }


    private void scanAgainstLocalDb( File scanDir, Job job, List<Dir> dirs, Root root ) {
        log.trace("scanAgainstLocalDb.2:" + scanDir.getAbsolutePath());
        if( !enabled() ) {
            log.info( "job cancelled" );
            return;
        }
        if( engine.isAllReposOffline( job ) ) {
            log.info( "Cancelling scan because all repositories are offline" );
            return;
        }
        if( isScanDirOtherRoot( scanDir, root, job ) ) {
            log.info( "not scanning because is another root" );
            return;
        }
        boolean isExcluded = !engine.isBackupable( scanDir, root );
        if( isExcluded ) {
            log.trace( "is excluded: " + scanDir.getAbsolutePath() );
            return;
        }

        setCurrentScanDir( scanDir );

        // Have a little sleep to make sure we don't saturate the CPU
        try {
            Thread.sleep( 500 );
        } catch( InterruptedException e ) {
            return;
        }

        File[] files = scanDir.listFiles();
        if( files == null || files.length == 0 ) {
            return;
        }
        // Scan local files
        for( File child : files ) {
            if( enabled() ) {
                if( !child.isDirectory() ) {
                    try {
                        root.scanAgainstLocalDb( child );
                    } catch( Throwable e ) {
                        log.error( "Exception scanning file: " + child.getAbsolutePath(), e );
                    }
                }
            } else {
                log.info( "paused, aborting scan" );
                return;
            }
        }
        // Scan subdirs
        for( File child : files ) {
            if( enabled() ) {
                if( child.isDirectory() ) {
                    scanAgainstLocalDb( child, job, dirs, root );
                }
            } else {
                log.info( "paused, aborting scan" );
                return;
            }
        }
    }

    private void setCurrentScanDir( File scanDir ) {
        this.scanDir = scanDir;
        EventUtils.fireQuietly( eventManager, new ScanDirEvent( scanDir ) );
    }

    /**
     * Check to see if the directory about to be scanned is another root on the same
     * job. If it is, we don't scan it because it will be scanned when the other
     * root is processed
     * 
     * @param scanDir
     * @param root
     * @param job
     * @return
     */
    private boolean isScanDirOtherRoot( File scanDir, Root root, Job job ) {
        for( Root otherRoot : job.getRoots() ) {
            if( otherRoot == root ) {
                // thats cool
            } else {
                if( scanDir.getAbsolutePath().equals( otherRoot.getFullPath() ) ) {
                    log.trace( "same dir as other root: " + scanDir.getAbsolutePath() + " == " + otherRoot.getFullPath() );
                    return true;
                }
            }
        }
        return false;
    }

    private boolean enabled() {
        return !cancelled && !config.isPaused();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled( boolean cancelled ) {
        this.cancelled = cancelled;
    }
}
