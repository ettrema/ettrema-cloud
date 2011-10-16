package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.observer.Observer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;

/**
 *
 * @author brad
 */
public class FileWatcher implements Observer<Config, Object> {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( FileWatcher.class );
    private static final long SCAN_INTERVAL_MS = 1000 * 60 * 60 * 24; // once per day
    private final Set<Integer> watchIds = new HashSet<Integer>();
    private final Config config;
    private final Engine engine;
    private long nextScanTime;
    private boolean disabled;
    private List<WatchJob> watchJobs = Collections.EMPTY_LIST;

    private boolean scanNow;

    public FileWatcher( Config config, Engine engine ) {
        this.config = config;
        this.engine = engine;
        config.addObserver( this );
    }

    public void scanNow() {
        scanNow = true;
    }

    public void start() {

        nextScanTime = System.currentTimeMillis() + ( 1000 * 60 * 1 ); // after 1 minute

        Thread thNextScan = new Thread( new ScanStarter() );
        thNextScan.setName( "Next scan" );
        thNextScan.setDaemon( true );
        thNextScan.start();


        for( Integer watchId : watchIds ) {
            try {
                JNotify.removeWatch( watchId );
            } catch( JNotifyException ex ) {
                log.error( "Exception removing old watchid" );
            }
        }
        watchIds.clear();

        watchJobs = new CopyOnWriteArrayList<WatchJob>();
        for( Job job : config.getJobs() ) {
            for( Root root : job.getRoots() ) {
                watch( job, root );
            }
        }
    }

    private void watch( Job job, Root root ) {
        String path = root.getFullPath();

        // watch mask, specify events you care about,
        // or JNotify.FILE_ANY for all events.
        int mask = JNotify.FILE_CREATED
            | JNotify.FILE_DELETED
            | JNotify.FILE_MODIFIED
            | JNotify.FILE_RENAMED;

        // watch subtree?
        boolean watchSubtree = true;
        try {
            // add actual watch
            WatchJob watchJob = new WatchJob( job, root );
            watchJobs.add( watchJob );
            Integer watchID = JNotify.addWatch( path, mask, watchSubtree, watchJob );
            watchIds.add( watchID );
        } catch( JNotifyException ex ) {
            log.error( "error watching: " + root.getFullPath(), ex );
        }

    }

    @Override
    public void onAdded( Config t, Object parent ) {
    }

    @Override
    public void onRemoved( Config t, Object parent, Integer indexOf ) {
    }

    /**
     * Called when the config object changes. Will reset any previous watchers
     * and recreate them on the new roots
     *
     * @param t
     * @param parent
     */
    @Override
    public void onUpdated( Config t, Object parent ) {
        System.out.println("FileWatcher: onUpdated: config has changed so reload");
        start();
    }

    public long delayUntilNextScanSecs() {
        return ( nextScanTime - System.currentTimeMillis() ) / 1000;
    }

    private void checkScanStart() {
        if( System.currentTimeMillis() > nextScanTime || scanNow ) {
            if( scanNow ) {
                log.trace("manually initiated scan");
            } else {
                log.trace( "kick off scheduled scan" );
            }
            scanNow = false;
            
            nextScanTime = System.currentTimeMillis() + SCAN_INTERVAL_MS;
            try {
                engine.scan();
            } finally {
                nextScanTime = System.currentTimeMillis() + SCAN_INTERVAL_MS;
            }

        }
    }

    private class ScanStarter implements Runnable {

        @Override
        public void run() {
            while( true ) {
                checkScanStart();
                try {
                    Thread.sleep( 1000 );
                } catch( InterruptedException ex ) {
                    return;
                }
            }
        }
    }

    public void setDisabled( boolean disabled ) {
        this.disabled = disabled;
        for( WatchJob j : this.watchJobs ) {
            j.setDisabled( disabled );
        }
    }

    public boolean isDisabled() {
        return disabled;
    }
}
