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

	private static final Logger log = LoggerFactory.getLogger(DirectComparisonFileSyncer.class);
	private static final long SCAN_INTERVAL_MS = 1000 * 60 * 60 * 24; // once per day
	private final ExclusionsService exclusionsService;
	private final QueueInserter queueHandler;
	private final Config config;
	private final EventManager eventManager;
	private final FileChangeChecker fileChangeChecker;
	private final StatusService statusService;
	private final ScanHelper scanHelper = new ScanHelper();
	
	public DirectComparisonFileSyncer(ExclusionsService exclusionsService, QueueInserter queueHandler, Config config, EventManager eventManager, FileChangeChecker fileChangeChecker, StatusService statusService) {
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
	public void scan(ScanStatus scanStatus) {
		log.debug("scanning");

		log.trace("**** PHASE 1: Fast Scan, local data only ****");
		if (scanAgainstLocalDb(scanStatus)) {
			return;
		}

		log.trace("**** PHASE 2: Thorough Scan, compare with server ****");
		if (scanAgainstRepos(scanStatus)) {
			return;
		}
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
		for (Repo r : job.getRepos()) {
			queueHandler.onFileDeleted(child, job, root, r);
		}
	}

	@Override
	public void onFileMoved(String fullPathFrom, File dest, Job job, Root root) {
		log.debug("onFileMoved: " + dest.getAbsolutePath());
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
		root.addNonScanBytes(child.length());
		checkFileInRepos(child, false, root);
	}


	private boolean scanAgainstRepos(ScanStatus scanStatus) {
		// begin scanning at roots
		for (Job j : config.getJobs()) {
			log.trace("scan job: " + j.toString());
			if (scanStatus.enabled()) {
				for (Root r : j.getRoots()) {
					if (scanStatus.enabled()) {
						log.trace("scan root: " + r.getFullPath());
						File dir = new File(r.getFullPath());
						scanAgainstRepo(dir, j, r.getExclusions(), r, scanStatus);
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

	private boolean scanAgainstLocalDb(ScanStatus scanStatus) {
		log.trace("scanAgainstLocalDb.1");
		// begin scanning at roots
		for (Job j : config.getJobs()) {
			log.trace("scan job: " + j.toString());
			if (scanStatus.enabled()) {
				Collection<Root> roots = new ArrayList<Root>(j.getRoots());
				for (Root r : roots) {
					if (scanStatus.enabled()) {
						log.trace("scan root: " + r.getFullPath());
						File dir = new File(r.getFullPath());
						scanAgainstLocalDb(dir, j, r.getExclusions(), r, scanStatus);
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

	private void scanAgainstRepo(File scanDir, Job job, List<Dir> dirs, Root root, ScanStatus scanStatus) {
		log.trace("scanAgainstRepo");
		if (!scanStatus.enabled()) {
			log.info("job cancelled");
			return;
		}
		if (statusService.isAllReposOffline(job)) {
			log.info("Cancelling scan because all repositories are offline");
			return;
		}
		if (scanHelper.isScanDirOtherRoot(scanDir, root, job)) {
			log.info("not scanning because is another root");
			return;
		}
		boolean isExcluded = !exclusionsService.isBackupable(scanDir, root);
		if (isExcluded) {
			log.trace("is excluded: " + scanDir.getAbsolutePath());
			return;
		}

		scanStatus.currentScanDir = scanDir;

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
			if (scanStatus.enabled()) {
				if (child.isDirectory()) {
					scanAgainstRepo(child, job, dirs, root, scanStatus);
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

	private void scanAgainstLocalDb(File scanDir, Job job, List<Dir> dirs, Root root, ScanStatus scanStatus) {
		log.trace("scanAgainstLocalDb.2:" + scanDir.getAbsolutePath());
		if (!scanStatus.enabled()) {
			log.info("job cancelled");
			return;
		}
		if (statusService.isAllReposOffline(job)) {
			log.info("Cancelling scan because all repositories are offline");
			return;
		}
		if (scanHelper.isScanDirOtherRoot(scanDir, root, job)) {
			log.info("not scanning because is another root");
			return;
		}
		boolean isExcluded = !exclusionsService.isBackupable(scanDir, root);
		if (isExcluded) {
			log.trace("is excluded: " + scanDir.getAbsolutePath());
			return;
		}

		scanStatus.currentScanDir = scanDir;

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
			if (scanStatus.enabled()) {
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
			if (scanStatus.enabled()) {
				if (child.isDirectory()) {
					scanAgainstLocalDb(child, job, dirs, root, scanStatus);
				}
			} else {
				log.info("paused, aborting scan");
				return;
			}
		}
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
	private void scanFileUpdated(File child, Root root) {
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
						FileMeta meta = scanHelper.getMeta(r, child, root.getFullPath(), root.getRepoName(), isScan);
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
}
