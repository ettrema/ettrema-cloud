package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.event.ScanDirEvent;
import com.ettrema.backup.event.ScanEvent;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.common.Service;
import com.ettrema.event.EventManager;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ScanService implements Service {

	private static final Logger log = LoggerFactory.getLogger(DirectComparisonFileSyncer.class);
	private static final long SCAN_INTERVAL_MS = 1000 * 60 * 60 * 24; // once per day
	private final FileSyncer fileSyncer;
	private final ExclusionsService exclusionsService;
	private final Config config;
	private final EventManager eventManager;
	private final ScanHelper scanHelper = new ScanHelper();
	private ScanStatus scanStatus;
	private boolean scanNow;
	private long nextScanTime;
	private boolean enabled;

	public ScanService(FileSyncer fileSyncer, ExclusionsService exclusionsService, Config config, EventManager eventManager) {
		this.fileSyncer = fileSyncer;
		this.exclusionsService = exclusionsService;
		this.config = config;
		this.eventManager = eventManager;
	}

	/**
	 * Sets the scanNow flag so a scan will be initiated on next poll
	 */
	public void scan() {
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
	public void onFileDeleted(File child, Job job, Root root) {
		log.debug("onFileDeleted: " + child.getAbsolutePath());
		if (!exclusionsService.isBackupable(child, root)) {
			return;
		}
		fileSyncer.onFileDeleted(child, job, root);
	}

	public void onFileMoved(String fullPathFrom, File dest, Job job, Root root) {
		log.debug("onFileMoved: " + dest.getAbsolutePath());
		if (!exclusionsService.isBackupable(dest, root)) {
			return;
		}
		fileSyncer.onFileMoved(fullPathFrom, dest, job, root);
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
	public void onFileModified(File child, Root root) {
		if (!exclusionsService.isBackupable(child, root)) {
			return;
		}
		root.addNonScanBytes(child.length());
		fileSyncer.onFileModified(child, root);
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
				scan();
			} finally {
				nextScanTime = System.currentTimeMillis() + SCAN_INTERVAL_MS;
			}

		}
	}

	public long delayUntilNextScanSecs() {
		return (nextScanTime - System.currentTimeMillis()) / 1000;
	}

	public File getCurrentScanDir() {
		return scanStatus.currentScanDir;
	}

	public void cancelScan() {
		scanStatus.cancelled = true;
	}

	public boolean isScanning() {
		return scanStatus != null;
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
