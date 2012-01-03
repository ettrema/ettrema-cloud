package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Dir;
import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.engine.FileChangeChecker.SyncStatus;
import com.ettrema.backup.event.RootChangedEvent;
import com.ettrema.backup.event.ScanDirEvent;
import com.ettrema.backup.event.ScanEvent;
import com.ettrema.backup.queue.QueueInserter;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.event.EventManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class DirectComparisonFileSyncer implements FileSyncer {

	private static final Logger log = LoggerFactory.getLogger(Root.class);
	private static final long SCAN_INTERVAL_MS = 1000 * 60 * 60 * 24; // once per day
	private final SyncExclusionsService exclusionsService;
	private final QueueInserter queueHandler;
	private final Config config;
	private final EventManager eventManager;
	private final FileChangeChecker fileChangeChecker;
	private final StatusService statusService;
	
	private boolean scanNow;
	private long nextScanTime;
	private boolean enabled;
	private boolean cancelled;
	private File currentScanDir;

	public DirectComparisonFileSyncer(SyncExclusionsService exclusionsService, QueueInserter queueHandler, Config config, EventManager eventManager, FileChangeChecker fileChangeChecker, StatusService statusService) {
		this.exclusionsService = exclusionsService;
		this.queueHandler = queueHandler;
		this.config = config;
		this.eventManager = eventManager;
		this.fileChangeChecker = fileChangeChecker;
		this.statusService = statusService;
	}

	/**
	 * Sets the scanNow flag so a scan will be initiated on next poll
	 */
	@Override
	public void scan() {
		log.info("scan requested...");
		scanNow = true;
	}
	
	private void _scan() {
		log.debug("scanning");

		EventUtils.fireQuietly(eventManager, new ScanEvent(true));


		// flush old cached data
		for (Repo reng : config.getAllRepos()) {
			reng.onScan();
			if (!reng.ping()) {
				log.info("setting repo offline because ping failed: " + reng.getDescription());
				reng.setOffline(false);
			}
		}
		for (Root root : config.getAllRoots()) {
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


		EventUtils.fireQuietly(eventManager, new ScanEvent(false));

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
	@Override
	public void onFileDeleted(File child, Job job, Root root) {
		log.debug("onFileDeleted: " + child.getAbsolutePath());
		if (!exclusionsService.isBackupable(child, root)) {
			return;
		}

		for (Repo r : job.getRepos()) {
			queueHandler.onFileDeleted(child, job, root, r);
		}
	}

	@Override
	public void onFileMoved(String fullPathFrom, File dest, Job job, Root root) {
		log.debug("onFileMoved: " + dest.getAbsolutePath());
		if (!exclusionsService.isBackupable(dest, root)) {
			return;
		}

		for (Repo r : job.getRepos()) {
			queueHandler.onMoved(fullPathFrom, dest, job, root, r);
		}
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
	@Override
	public void onFileModified(File child, Root root) {
		if (!exclusionsService.isBackupable(child, root)) {
			return;
		}
		root.addNonScanBytes(child.length());
		checkFileInRepos(child, false, root);
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

	@Override
	public void cancelScan() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isScanning() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setScanningDisabled(boolean state) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private class ScanStarter implements Runnable {

		@Override
		public void run() {
			while (enabled) {
				checkScanStart();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					return;
				}
			}
		}
	}

	private void checkScanStart() {
		if (System.currentTimeMillis() > nextScanTime || scanNow) {
			if (scanNow) {
				log.trace("manually initiated scan");
			} else {
				log.trace("kick off scheduled scan");
			}
			scanNow = false;

			nextScanTime = System.currentTimeMillis() + SCAN_INTERVAL_MS;
			try {
				_scan();
			} finally {
				nextScanTime = System.currentTimeMillis() + SCAN_INTERVAL_MS;
			}

		}
	}

	@Override
	public long delayUntilNextScanSecs() {
		return (nextScanTime - System.currentTimeMillis()) / 1000;
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
				for (Root r : roots) {
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

	private void scanAgainstRepo(File scanDir, Job job, List<Dir> dirs, Root root) {
		log.trace("scanAgainstRepo");
		if (!enabled()) {
			log.info("job cancelled");
			return;
		}
		if (statusService.isAllReposOffline(job)) {
			log.info("Cancelling scan because all repositories are offline");
			return;
		}
		if (isScanDirOtherRoot(scanDir, root, job)) {
			log.info("not scanning because is another root");
			return;
		}
		boolean isExcluded = !exclusionsService.isBackupable(scanDir, root);
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
		for (File child : files) {
			if (enabled()) {
				if (child.isDirectory()) {
					scanAgainstRepo(child, job, dirs, root);
				} else {
					try {
						long tm = System.currentTimeMillis();

						scanFileUpdated(child, root);
						if (log.isTraceEnabled()) {
							tm = System.currentTimeMillis() - tm;
							log.info("scanned file in: " + tm + "ms");
						}
						// have a little sleep to avoid saturating CPU
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							return;
						}


					} catch (Throwable e) {
						log.error("Exception scanning file: " + child.getAbsolutePath(), e);
					}
				}
			} else {
				log.info("paused, aborting scan");
				return;
			}
		}
		for (Repo r : job.getRepos()) {
			try {
				r.onScanDirComplete(scanDir.getAbsolutePath(), root.getFullPath(), root.getRepoName());
			} catch (RepoNotAvailableException ex) {
				log.info("repository has gone offline: " + r.getDescription(), ex);
				r.setOffline(true);
			}
		}
	}

	private void scanAgainstLocalDb(File scanDir, Job job, List<Dir> dirs, Root root) {
		log.trace("scanAgainstLocalDb.2:" + scanDir.getAbsolutePath());
		if (!enabled()) {
			log.info("job cancelled");
			return;
		}
		if (statusService.isAllReposOffline(job)) {
			log.info("Cancelling scan because all repositories are offline");
			return;
		}
		if (isScanDirOtherRoot(scanDir, root, job)) {
			log.info("not scanning because is another root");
			return;
		}
		boolean isExcluded = !exclusionsService.isBackupable(scanDir, root);
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
				if (!child.isDirectory()) {
					try {
						scanAgainstLocalDb(child, root);
					} catch (Throwable e) {
						log.error("Exception scanning file: " + child.getAbsolutePath(), e);
					}
				}
			} else {
				log.info("paused, aborting scan");
				return;
			}
		}
		// Scan subdirs
		for (File child : files) {
			if (enabled()) {
				if (child.isDirectory()) {
					scanAgainstLocalDb(child, job, dirs, root);
				}
			} else {
				log.info("paused, aborting scan");
				return;
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

	/**
	 * Check to see if the given file has been updated, by comparing it
	 * with the repository version
	 *
	 * Will enqueue the job if required
	 *
	 *
	 * @param child
	 * @param job
	 * @param root
	 */
	public void scanFileUpdated(File child, Root root) {
		if (!exclusionsService.isBackupable(child, root)) {
			return;
		}
		root.addScanBytes(child.length());
		checkFileInRepos(child, true, root);
	}

	public void scanAgainstLocalDb(File child, Root root) {
		if (!exclusionsService.isBackupable(child, root)) {
			return;
		}
		checkFileFast(child, root);

	}

	private void checkFileInRepos(File child, boolean isScan, Root root) {
		for (Repo r : root.getJob().getRepos()) {
			long tm = System.currentTimeMillis();
			if (!r.isOffline()) {
				try {
					if (!r.isExcludedFile(child, root)) {
						FileMeta meta = getMeta(r, child, root.getFullPath(), root.getRepoName(), isScan);
						checkFileUpdated(r, child, meta, root);
					} else {
						log.trace("file {} is excluded from {}", child.getAbsolutePath(), r.getDescription());
					}
				} catch (RepoNotAvailableException ex) {
					r.setOffline(true);
				}
			}
			if (log.isTraceEnabled()) {
				tm = System.currentTimeMillis() - tm;
				log.trace("scanned file in repo: " + r.getDescription() + " in " + tm + "ms");
			}
		}
	}

	private FileMeta getMeta(Repo r, File child, String localRootPath, String repoName, boolean isScan) throws RepoNotAvailableException {
		FileMeta meta;
		try {
			meta = r.getFileMeta(child.getAbsolutePath(), localRootPath, repoName, isScan);
		} catch (RepoNotAvailableException ex) {
			log.info("repository not available: " + r.getClass().getCanonicalName(), ex);
			log.trace("retrying..");
			try {
				meta = r.getFileMeta(child.getAbsolutePath(), localRootPath, repoName, isScan);
			} catch (RepoNotAvailableException ex1) {
				log.info("repo still not available");
				throw ex1;
			}
		}


		return meta;
	}

	private void checkFileFast(File child, Root root) {
		for (Repo r : root.getJob().getRepos()) {
			if (!r.isOffline()) {
				if (!r.isExcludedFile(child, root)) {
					SyncStatus syncStatus = fileChangeChecker.checkFileFast(r, child);
					if (syncStatus == SyncStatus.LOCAL_NEWER) {
						log.trace("checkFileUpdated - local file is new, so upload: " + child.getAbsolutePath());
						queueHandler.onUpdatedFile(r, child);
					}
				} else {
					log.trace("file {} is excluded from {}", child.getAbsolutePath(), r.getDescription());
				}
			}
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

	private boolean enabled() {
		return !cancelled && !config.isPaused();
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public File getCurrentScanDir() {
		return currentScanDir;
	}
	
	
}
