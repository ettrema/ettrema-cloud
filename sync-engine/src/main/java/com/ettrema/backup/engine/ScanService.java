package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.event.ScanEvent;
import com.ettrema.backup.queue.QueueManager;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.common.LogUtils;
import com.ettrema.common.Service;
import com.ettrema.event.EventManager;
import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ScanService implements Service {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);
    private static final long SCAN_INTERVAL_MS = 1000 * 60 * 60 * 24; // once per day
    private final FileSyncer fileSyncer;
    private final List<RemoteSyncer> remoteSyncers;
    private final ExclusionsService exclusionsService;
    private final Config config;
    private final EventManager eventManager;
    private final QueueManager queueManager;
    private ScanStatus scanStatus;
    private boolean scanNow;
    private long nextScanTime;
    private boolean enabled;

    public ScanService(FileSyncer fileSyncer, ExclusionsService exclusionsService, Config config, EventManager eventManager, List<RemoteSyncer> remoteSyncers, QueueManager queueManager) {
        this.fileSyncer = fileSyncer;
        this.exclusionsService = exclusionsService;
        this.config = config;
        this.eventManager = eventManager;
        this.remoteSyncers = remoteSyncers;
        this.queueManager = queueManager;
    }

    public void initiateScan() {
        scanNow = true;
    }

    /**
     * Do a scan of local and remote filesystems. This is now a syncronous call.
     * to initiate a scan without blocking call initiateScan
     */
    public void scan() throws InterruptedException {
        log.debug("scanning");

        EventUtils.fireQuietly(eventManager, new ScanEvent(true));

        try {
            scanStatus = new ScanStatus();
            fileSyncer.scan(scanStatus);
            EventUtils.fireQuietly(eventManager, new ScanEvent(false));
        } finally {
            scanStatus = null;
        }
        for (Root root : config.getAllRoots()) {
            root.onScanCompletedOk();
        }
        for (Repo reng : config.getAllRepos()) {
            reng.onScanComplete();
        }

        log.trace("finished scanning");
    }

    /**
     * Called from the FileWatcher when a file deletion event has occurred.
     *
     * @param f
     * @param job
     * @param root
     */
    public void onFileDeleted(File child) {
        log.debug("onFileDeleted: " + child.getAbsolutePath());
        if (!exclusionsService.isBackupable(child)) {
            return;
        }
        fileSyncer.onFileDeleted(child);
    }

    public void onFileMoved(String fullPathFrom, File dest) {
        log.debug("onFileMoved: " + dest.getAbsolutePath());
        if (!exclusionsService.isBackupable(dest)) {
            return;
        }
        fileSyncer.onFileMoved(fullPathFrom, dest);
    }

    /**
     * Called from the file system watcher on a modified event.
     *
     * Check to see if the given file has been updated, or requires an update
     *
     * Will enqueue the job if required
     *
     * @param child
     * @param job
     * @param root
     */
    public void onFileModified(File child) {
        if (!exclusionsService.isBackupable(child)) {
            return;
        }
        fileSyncer.onFileModified(child);
    }

    @Override
    public void start() {
        enabled = true;
        nextScanTime = System.currentTimeMillis() + (1000 * 60 * 1); // after 1 minute

        Thread thNextScan = new Thread(new ScanStarter());
        thNextScan.setName("Next scan");
        thNextScan.setDaemon(true);
        thNextScan.start();
    }

    @Override
    public void stop() {
        enabled = false;
    }

    private class ScanStarter implements Runnable {

        @Override
        public void run() {
            while (enabled) {
                try {
                    checkScanStart();
                    if( queueManager.getQueueSize() == 0 ) {
                        Thread.sleep(3000);
                    } else {
                        Thread.sleep(100); // just in case ... so we don't end up in a busy loop
                    }
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }
    }

    private void pingRepos() {
        for (RemoteSyncer rs : remoteSyncers) {
            rs.ping();
        }
    }

    private void checkScanStart() throws InterruptedException {
        LogUtils.trace(log, "checkScanStart: 1: check queues...");
        queueManager.checkQueues(); // always check queues for outstanding actions to process
        int size = queueManager.getQueueSize();
        if( size > 0 ) {
            LogUtils.trace(log, "checkScanStart: 1: exiting scan because there are already queued items: size:", size);
            return ;
        } else {
            LogUtils.trace(log, "checkScanStart: 1: all queues are empty so continue scan");
        }
        if (System.currentTimeMillis() > nextScanTime || scanNow) {
            if (scanNow) {
                log.trace("checkScanStart: manually initiated local file system scan");
            } else {
                log.trace("checkScanStart: kick off scheduled local file system scan");
            }
            scanNow = false;

            nextScanTime = System.currentTimeMillis() + SCAN_INTERVAL_MS;
            try {
                scan();
            } finally {
                nextScanTime = System.currentTimeMillis() + SCAN_INTERVAL_MS;
            }
        }
        LogUtils.trace(log, "checkScanStart: 3: check remote repositories...");
        pingRepos();        
    }

    public long delayUntilNextScanSecs() {
        return (nextScanTime - System.currentTimeMillis()) / 1000;
    }

    public File getCurrentScanDir() {
        if (scanStatus != null) {
            return scanStatus.currentScanDir;
        } else {
            return null;
        }
    }

    public void cancelScan() {
        if (scanStatus != null) {
            scanStatus.cancelled = true;
        }
    }

    public boolean isScanning() {
        return scanStatus != null && !scanStatus.cancelled;
    }

    public void setScanningDisabled(boolean state) {
        if (state) {
            if (!enabled) {
                start();
            }
        } else {
            if (enabled) {
                stop();
            }
        }
    }
}
