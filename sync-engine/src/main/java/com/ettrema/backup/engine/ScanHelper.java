package com.ettrema.backup.engine;

import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.config.Root;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
class ScanHelper {
	
	private static final Logger log = LoggerFactory.getLogger(ScanHelper.class);
	
	
	
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
	public boolean isScanDirOtherRoot(File scanDir, Root root, Job job) {
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
	
	public FileMeta getMeta(Repo r, File child, String localRootPath, String repoName, boolean isScan) throws RepoNotAvailableException {
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
}
