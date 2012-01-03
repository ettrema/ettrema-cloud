package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Dir;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.event.RootChangedEvent;
import com.ettrema.backup.event.ScanDirEvent;
import com.ettrema.backup.event.ScanEvent;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.event.EventManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Scans local files to maintain a database of CRC's
 * 
 * For files, the CRC is just of its content. For folders the CRC is the hash
 * of the CRC's of its members and their names in the format name + ":" + crc
 *
 * @author brad
 */
public class LocalTokenScanner {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LocalTokenScanner.class);
    private final Config config;
    private final Engine engine;
    private final EventManager eventManager;
    private final StateTokenDaoImpl stateTokenDao = new StateTokenDaoImpl();
    private final CrcCalculator crcCalculator;
    private boolean cancelled;
    private File currentScanDir;

    public LocalTokenScanner(Engine engine, Config config, EventManager eventManager, CrcCalculator crcCalculator) {
        this.engine = engine;
        this.config = config;
        this.eventManager = eventManager;
        this.crcCalculator = crcCalculator;
    }

    public void scan() throws Exception {
        log.debug("scanning");
        System.out.println("----------- local token scanning");

//        EventUtils.fireQuietly(eventManager, new ScanEvent(true));
//
//        // flush old cached data
//        for (Repo reng : config.getAllRepos()) {
//            reng.onScan();
//            if (!reng.ping()) {
//                log.info("setting repo offline because ping failed: " + reng.getDescription());
//                reng.setOffline(false);
//            }
//        }
//        for (Root root : config.getAllRoots()) {
//            root.onScan();
//        }

        if (scanAllJobs()) {
            return;
        }
        EventUtils.fireQuietly(eventManager, new ScanEvent(false));

//        for (Root root : config.getAllRoots()) {
//            root.onScanCompletedOk();
//        }
//        for (Repo reng : config.getAllRepos()) {
//            reng.onScanComplete();
//        }


        log.trace("finished scanning");
    }

    private boolean scanAllJobs() {
        System.out.println("scanAllJobs");
        log.trace("scanAgainstLocalDb.1");
        // begin scanning at roots
        for (Job j : config.getJobs()) {
            log.trace("scan job: " + j.toString());
            if (enabled()) {
                Collection<Root> roots = new ArrayList<Root>(j.getRoots());
                for (Root r : roots) {
                    if (enabled()) {
                        log.trace("scan root: " + r.getFullPath());
                        File dir = new File(r.getFullPath());
                        scan(dir, j, r.getExclusions(), r);
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

    private void scan(File scanDir, Job job, List<Dir> dirs, Root root) {
        log.trace("scanAgainstLocalDb.2:" + scanDir.getAbsolutePath());
        if (!enabled()) {
            log.info("job cancelled");
            return;
        }
        if (engine.isAllReposOffline(job)) {
            log.info("Cancelling scan because all repositories are offline");
            return;
        }
        if (isScanDirOtherRoot(scanDir, root, job)) {
            log.info("not scanning because is another root");
            return;
        }
        boolean isExcluded = !engine.isBackupable(scanDir, root);
        if (isExcluded) {
            log.trace("is excluded: " + scanDir.getAbsolutePath());
            return;
        }

        setCurrentScanDir(scanDir);

        // Have a little sleep to make sure we don't saturate the CPU
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            return;
        }

        File[] files = scanDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        // Scan local files
        for (File child : files) {
            if (enabled()) {
                try {
                    scanFile(root, child);
                } catch (Throwable e) {
                    log.error("Exception scanning file: " + child.getAbsolutePath(), e);
                }
            } else {
                log.info("paused, aborting scan");
                return;
            }
        }
        // Check for removed files. ie those in the database but not present locally
        // remove them from the database
        List<StateToken> filesInDb = stateTokenDao.findForParent(scanDir);
        for (StateToken st  : filesInDb) {
            File dbFile = new File(st.getFullPath());
            if( !dbFile.exists()) {
                stateTokenDao.remove(dbFile);
            }
        }
    }

    private void scanFile(Root root, File child) {
        if (!engine.isBackupable(child, root)) {
            return;
        }
        checkFile(root, child);
    }

    private void checkFile(Root root, File child) {
        StateToken stateToken = stateTokenDao.get(child);
        if (stateToken == null) {
            // have not recorded crc, so generate
            generateCrc(root, child);
        } else {
            long actualModTime = child.lastModified();
            if (actualModTime != stateToken.getTime()) {
                // file has changed since crc was generated, so refresh
                generateCrc(root, child);
            }
        }
    }

    private void setCurrentScanDir(File scanDir) {
        this.currentScanDir = scanDir;
        EventUtils.fireQuietly(eventManager, new ScanDirEvent(scanDir));
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
    private boolean isScanDirOtherRoot(File scanDir, Root root, Job job) {
        for (Root otherRoot : job.getRoots()) {
            if (otherRoot == root) {
                // thats cool
            } else {
                if (scanDir.getAbsolutePath().equals(otherRoot.getFullPath())) {
                    log.trace("same dir as other root: " + scanDir.getAbsolutePath() + " == " + otherRoot.getFullPath());
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

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    private void generateCrc(Root root, File child) {
        System.out.println("generateCrc: " + child.getAbsolutePath());
        long crc = crcCalculator.getLocalCrc(child);
        stateTokenDao.set(child, crc, child.lastModified());
        File parent = child.getParentFile();
        if (root.contains(parent)) {
            generateCrc(root, parent);
        }
    }
}
