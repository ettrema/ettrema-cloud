package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import com.ettrema.common.LogUtils;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class StateTokenFileSyncer implements FileSyncer {

	private static final Logger log = LoggerFactory.getLogger(StateTokenFileSyncer.class);
	private final ExclusionsService exclusionsService;
	private final Config config;
	private final StateTokenDaoImpl stateTokenDao = new StateTokenDaoImpl();
	private final CrcCalculator crcCalculator = new CrcCalculator();
	private final ScanHelper scanHelper = new ScanHelper();

	public StateTokenFileSyncer(ExclusionsService exclusionsService, Config config) {
		this.exclusionsService = exclusionsService;
		this.config = config;
	}

	/**
	 * Sets the scanNow flag so a scan will be initiated on next poll
	 */
	@Override
	public void scan(ScanStatus scanStatus) {
		log.debug("scanning");
		try {
			if (scanAllRoots(scanStatus)) {
				return;
			}
		} catch (InterruptedException ex) {
			log.info("scan has been interrupted");
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
		try {
			scanDirectory(child.getParentFile(), new ScanStatus());
		} catch (InterruptedException ex) {
		}
	}

	@Override
	public void onFileMoved(String fullPathFrom, File dest, Job job, Root root) {
		log.debug("onFileMoved: " + dest.getAbsolutePath());
		try {
			File fromDir = new File(fullPathFrom).getParentFile();
			scanDirectory(fromDir, new ScanStatus());
			scanDirectory(dest.getParentFile(), new ScanStatus());
		} catch (InterruptedException ex) {
		}
	}

	@Override
	public void onFileModified(File child, Root root) {
		log.debug("onFileModified: " + child.getAbsolutePath());
		try {
			scanDirectory(child.getParentFile(), new ScanStatus());
		} catch (InterruptedException ex) {
		}
	}

	private boolean scanAllRoots(ScanStatus scanStatus) throws InterruptedException {
		// begin scanning at roots
		Set<File> rootDirs = new HashSet<File>();

		for (Job j : config.getJobs()) {
			log.trace("scan job: " + j.toString());
			if (scanStatus.enabled()) {
				for (Root r : j.getRoots()) {
					File rootDir = new File(r.getFullPath());
					rootDirs.add(rootDir);
				}
			} else {
				log.info("paused, abort scan");
				return true;
			}
		}
		for (File rootDir : rootDirs) {
			if (scanStatus.enabled()) {
				log.trace("scan root: " + rootDir.getAbsolutePath());
				scanDirectory(rootDir, scanStatus);
			} else {
				log.info("paused, abort scan");
				return true;
			}
		}

		return false;
	}

	private boolean scanDirectory(File scanDir, ScanStatus scanStatus) throws InterruptedException {
		LogUtils.trace(log, "scanDirectory", scanDir.getAbsolutePath());
		if (!scanStatus.enabled()) {
			log.info("job cancelled");
			return false;
		}
		if (scanHelper.isOutsideAnyRoot(scanDir, config)) {
			log.info("not scanning because is another root");
			return false;
		}
		boolean isExcluded = !exclusionsService.isBackupable(scanDir);
		if (isExcluded) {
			log.trace("is excluded: " + scanDir.getAbsolutePath());
			return false;
		}

		scanStatus.currentScanDir = scanDir;
		List<StateToken> tokens = stateTokenDao.findForFolder(scanDir);
		File[] files = scanDir.listFiles();
		boolean didChange = false;
		if (files != null) {
			for (File child : files) {
				if (scanStatus.enabled()) {
					if (child.isDirectory()) {
						if (scanDirectory(child, scanStatus)) {
							didChange = true;
						}
					} else {
						if (checkCrcFile(child, tokens)) {
							didChange = true;
						}
					}
					// have a little sleep to avoid saturating CPU
					//Thread.sleep(100);
				} else {
					log.info("paused, aborting scan");
					return false;
				}
			}
		}

		// Now remove rows for any items no longer present in the filesystem
		tokens = stateTokenDao.findForFolder(scanDir);
		if (tokens != null) {
			for (StateToken t : tokens) {
				File f = new File(t.filePath);
				if (!f.exists()) {
					stateTokenDao.delete(t);
					didChange = true;
				}
			}
		}

		if (didChange) {
			// Need to update directory CRC
			updateDirectoryCrc(scanDir);
		}
		return didChange;
	}

	/**
	 * Check to see if the CRC for the given file (not directory) is up to date,
	 * and update it if not.
	 * 
	 * @return - true if a change was made
	 * 
	 * @param child 
	 */
	private boolean checkCrcFile(File child, List<StateToken> tokens) {
		StateToken token = findToken(tokens, child);
		long modTime = child.lastModified();
		if (token == null) {
			token = new StateToken(child.getAbsolutePath());
		}
		if (token.currentTime != child.lastModified()) {
			long crc = crcCalculator.getLocalCrc(child);
			token.currentCrc = crc;
			token.currentTime = modTime;
			stateTokenDao.saveOrUpdate(token);
			LogUtils.trace(log, "checkCrcFile: updated", child.getAbsolutePath(), crc);
			return true;
		} else {
			LogUtils.trace(log, "checkCrcFile: not updated", child.getAbsolutePath());
			return false;
		}
	}

	private StateToken findToken(List<StateToken> tokens, File child) {
		if (tokens == null) {
			return null;
		}
		for (StateToken t : tokens) {
			if (t.filePath.equals(child.getAbsolutePath())) {
				return t;
			}
		}
		return null;
	}

	private void updateDirectoryCrc(File dir) {
		List<StateToken> childTokens = stateTokenDao.findForFolder(dir);
		long crc = crcCalculator.getLocalCrcDirectory(childTokens);

		StateToken dirToken = stateTokenDao.get(dir);
		if (dirToken == null) {
			dirToken = new StateToken(dir.getAbsolutePath());
		}
		long oldCrc = dirToken.currentCrc;
		dirToken.currentCrc = crc;
		dirToken.currentTime = dir.lastModified();
		LogUtils.trace(log, "updateDirectoryCrc", dir.getAbsolutePath(), "new value", crc, "old value", oldCrc);
		stateTokenDao.saveOrUpdate(dirToken);				
	}

	

}
