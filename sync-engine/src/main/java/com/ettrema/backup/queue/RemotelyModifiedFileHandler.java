package com.ettrema.backup.queue;

import com.bradmcevoy.io.FileUtils;
import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.PermanentUploadException;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.engine.ConflictManager;
import com.ettrema.backup.engine.CrcCalculator;
import com.ettrema.backup.engine.FileChangeChecker;
import com.ettrema.backup.engine.FileChangeChecker.SyncStatus;
import com.ettrema.backup.engine.LocalCrcDaoImpl;
import com.ettrema.backup.utils.PathMunger;
import java.io.File;
import java.util.Date;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author brad
 */
public class RemotelyModifiedFileHandler implements QueueItemHandler {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RemotelyModifiedFileHandler.class);
	private final CrcCalculator crcCalculator;
	private final LocalCrcDaoImpl localCrcDao;
	private final ConflictManager conflictManager;
	private final FileChangeChecker fileChangeChecker;
	private final QueueInserter queueInserter;
	private final PathMunger pathMunger;

	public RemotelyModifiedFileHandler(CrcCalculator crcCalculator, LocalCrcDaoImpl localCrcDao, ConflictManager conflictManager, FileChangeChecker fileChangeChecker, QueueInserter queueInserter, PathMunger pathMunger) {
		this.crcCalculator = crcCalculator;
		this.localCrcDao = localCrcDao;
		this.conflictManager = conflictManager;
		this.fileChangeChecker = fileChangeChecker;
		this.queueInserter = queueInserter;
		this.pathMunger = pathMunger;
	}

	@Override
	public boolean supports(QueueItem item) {
		return item instanceof RemoteModifiedQueueItem;
	}

	@Override
	public boolean requiresWait(QueueItem item) {
		// TODO: check how long since last uploaded and compare with file size
		// TODO: check if file is open
		return false;
	}

	@Override
	public void process(Repo r, Job job, QueueItem item) throws RepoNotAvailableException, PermanentUploadException {
		item.setStarted(new Date());

		RemoteModifiedQueueItem remoteModItem = (RemoteModifiedQueueItem) item;
		File fLocalFile = item.getFile();
		log.debug("process: " + fLocalFile.getAbsolutePath());
		SyncStatus syncStatus;
		if (fLocalFile.exists()) {
			String localPath = fLocalFile.getAbsolutePath();
			Root root = pathMunger.findRootFromFile(job.getRoots(), fLocalFile);
			if( root == null ) {
				throw new RuntimeException("Couldnt find root for: " + fLocalFile.getAbsolutePath());
			}
			String rootPath = root.getFullPath();
			String rootName = root.getRepoName();
			FileMeta remoteMeta = r.getFileMeta(localPath, rootPath, rootName, false);
			syncStatus = fileChangeChecker.checkFile(r, remoteMeta, remoteModItem.getFile());
			log.trace("local file exists, sync status: " + syncStatus);
		} else {
			log.trace("local file does not exist, so download");
			syncStatus = SyncStatus.REMOTE_NEWER;
		}		
		switch (syncStatus) {
			case IDENITICAL:
				log.info("Not downloading because files are identical");
				return;
			case LOCAL_NEWER:
				log.info("Local file is newer, so queue upload");
				queueInserter.onUpdatedFile(r, fLocalFile);
				return;
			case CONFLICT:
				log.info("Files are conflicted");
				remoteModItem.setConflicted(true);
				break;
			case REMOTE_NEWER:
				log.info("remote file is newer and files are not in conflict, so download");
				break;
		}

		try {
			File fTemp = File.createTempFile("shmego-download", item.getFileName());
			log.trace("downloading to: " + fTemp.getAbsolutePath());
			r.download(fTemp, fLocalFile, job, null);

			if (fLocalFile.exists()) {
				if (fLocalFile.isDirectory()) {
					log.error("can't delete a directory: " + fLocalFile.getAbsolutePath());
					item.setNotes("remote file corresponds to a local directory, which we won't delete: " + fLocalFile.getAbsolutePath());
					return;
				} else {
					if (remoteModItem.isConflicted()) {
						log.trace("is conflicted, so will rename remote file");
						String ext = FileUtils.getExtension(fLocalFile);
						File conflictedPath = new File(fLocalFile.getParent(), fLocalFile.getName() + ".conflicted." + ext);
						if (!fTemp.renameTo(conflictedPath)) {
							log.error("Couldnt rename conflicted file to: " + conflictedPath.getAbsolutePath());
							item.setNotes("Couldnt rename conflicted file to: " + conflictedPath.getAbsolutePath());
							return;
						}
						conflictManager.onConflict(fLocalFile, conflictedPath);
					} else {
						log.trace("is remotely modified, so move file into old versions folder");
						if (!moveToOldVersions(fLocalFile)) {
							log.error("Couldnt delete local file: " + item.getFile().getAbsolutePath());
							item.setNotes("Couldnt delete local file: " + item.getFile().getAbsolutePath());
							return;
						}
						if (!fTemp.renameTo(fLocalFile)) {
							log.error("Error moving temp download file to: " + fLocalFile.getAbsolutePath());
							item.setNotes("Couldnt rename temp file to dest file");
							return;
						}
					}
				}
			} else {
				if (!fTemp.renameTo(fLocalFile)) {
					log.error("Error moving temp download file to: " + fLocalFile.getAbsolutePath());
					item.setNotes("Couldnt rename temp file to dest file");
					return;
				}
			}


			long crc = crcCalculator.getLocalCrc(fLocalFile);

			localCrcDao.setLocalBackedupCrc(fLocalFile, r, crc);

		} catch (RepoNotAvailableException e) {
			throw e;
		} catch (PermanentUploadException e) {
			throw e;

		} catch (Exception e) {
			log.error("Exception transferring file", e);
			item.setNotes("error uploading file: " + e.getMessage());
		} finally {
			item.setCompleted(new Date());
			log.debug("completed remotely modified task");
		}
	}

	/**
	 * Rather then delete updated files, move them into a subfolder
	 * 
	 * @param f
	 * @return
	 */
	private boolean moveToOldVersions(File f) {
		File dir = f.getParentFile();
		File oldVersions = new File(dir, ".shmego.oldversions");
		if (!oldVersions.exists()) {
			if (!oldVersions.mkdir()) {
				log.error("Couldnt create old versions folder: " + oldVersions.getAbsolutePath());
				return false;
			}
		}

		File dest = new File(oldVersions, f.getName());
		while (dest.exists()) {
			String destName = FileUtils.incrementFileName(f.getName(), true);
			dest = new File(oldVersions, destName);
		}

		if (!f.renameTo(dest)) {
			log.error("Couldnt move updated file to old versions folder");
			return false;
		}
		log.trace("done move to: " + dest.getAbsolutePath());
		return true;
	}
}
