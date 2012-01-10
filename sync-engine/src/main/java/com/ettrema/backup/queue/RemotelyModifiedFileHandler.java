package com.ettrema.backup.queue;

import com.bradmcevoy.io.FileUtils;
import com.ettrema.backup.config.Config;
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
import com.ettrema.common.Withee;
import com.ettrema.logging.LogUtils;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 *
 * @author brad
 */
public class RemotelyModifiedFileHandler implements QueueItemHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RemotelyModifiedFileHandler.class);
    private final Config config;
    private final CrcCalculator crcCalculator;
    private final LocalCrcDaoImpl localCrcDao;
    private final ConflictManager conflictManager;
    private final FileChangeChecker fileChangeChecker;
    private final QueueInserter queueInserter;
    private final PathMunger pathMunger;

    public RemotelyModifiedFileHandler(Config config, CrcCalculator crcCalculator, LocalCrcDaoImpl localCrcDao, ConflictManager conflictManager, FileChangeChecker fileChangeChecker, QueueInserter queueInserter, PathMunger pathMunger) {
        this.config = config;
        this.crcCalculator = crcCalculator;
        this.localCrcDao = localCrcDao;
        this.conflictManager = conflictManager;
        this.fileChangeChecker = fileChangeChecker;
        this.queueInserter = queueInserter;
        this.pathMunger = pathMunger;
    }

    @Override
    public boolean supports(QueueItem item) {
        return item instanceof RemotelyModifiedQueueItem;
    }

    @Override
    public boolean requiresWait(QueueItem item) {
        // TODO: check how long since last uploaded and compare with file size
        // TODO: check if file is open
        return false;
    }

    @Override
    public void process(final Repo r, final QueueItem item) throws RepoNotAvailableException, PermanentUploadException {
        item.setStarted(new Date());

        final RemotelyModifiedQueueItem remoteModItem = (RemotelyModifiedQueueItem) item;
        final File fLocalFile = item.getFile();
        log.info("process remotely modified file: " + fLocalFile.getAbsolutePath());
        String localPath = fLocalFile.getAbsolutePath();
        final Job job = config.findJob(r);
        Root root = pathMunger.findRootFromFile(job.getRoots(), fLocalFile);
        if (root == null) {
            throw new RuntimeException("Couldnt find root for: " + fLocalFile.getAbsolutePath());
        }
        String rootPath = root.getFullPath();
        String rootName = root.getRepoName();
        r.withFileMeta(localPath, rootPath, rootName, new Withee<FileMeta>() {

            @Override
            public void with(final FileMeta t) throws RepoNotAvailableException, PermanentUploadException {
                doDownloadAndVerify(r, t, remoteModItem, job, item);
            }
        });


    }

    private boolean doDownloadAndVerify(Repo r, FileMeta remoteMeta, RemotelyModifiedQueueItem remoteModItem, Job job, QueueItem item) throws RepoNotAvailableException, PermanentUploadException {
        LogUtils.trace(log, "doDownloadAndVerify", item.getFile().getAbsolutePath(), "directory?", remoteMeta.isDirectory());
        if (remoteMeta.isDirectory()) {
            return doDownloadDirectory(item.getFile(), r, remoteMeta, remoteModItem, job, item);
        } else {
            return doDownloadAndVerifyFile(item.getFile(), r, remoteMeta, remoteModItem, job, item);
        }
    }

    private boolean doDownloadAndVerifyFile(final File fLocalFile, Repo r, FileMeta remoteMeta, RemotelyModifiedQueueItem remoteModItem, Job job, QueueItem item) throws RepoNotAvailableException, PermanentUploadException {
        SyncStatus syncStatus;
        if (fLocalFile.exists()) {
            syncStatus = fileChangeChecker.checkFile(r, remoteMeta, remoteModItem.getFile());
            log.trace("local file exists, sync status: " + syncStatus);
        } else {
            log.trace("local file does not exist, so download");
            syncStatus = SyncStatus.REMOTE_NEWER;
        }
        switch (syncStatus) {
            case IDENITICAL:
                log.info("Not downloading because files are identical");
                remoteModItem.setNotes("Files are identical, so nothing to do");
                remoteModItem.setCompleted(new Date());
                return true;
            case LOCAL_NEWER:
                log.info("Local file is newer, so queue upload");
                queueInserter.enqueueUpload(r, fLocalFile);
                remoteModItem.setNotes("Local file is newer, will upload");
                remoteModItem.setCompleted(new Date());
                return true;
            case CONFLICT:
                log.info("Files are conflicted");
                remoteModItem.setConflicted(true);
                remoteModItem.setNotes("Conflict detected");
                break;
            case REMOTE_NEWER:
                log.info("remote file is newer and files are not in conflict, so download");
                break;
        }
        return downloadFile(fLocalFile, r, job, remoteMeta, item, remoteModItem);
    }

    private boolean downloadFile(final File fLocalFile, Repo r, Job job, FileMeta remoteMeta, QueueItem item, RemotelyModifiedQueueItem remoteModItem) throws RepoNotAvailableException, PermanentUploadException {
        try {
            File fTemp = new File(fLocalFile.getParentFile(), ".ettrema-download." + fLocalFile.getName());
            log.trace("downloading to: " + fTemp.getAbsolutePath());
            r.download(fTemp, fLocalFile, job, null);
            if (!verifyDownload(fTemp, remoteMeta.getCrc())) {
                log.error("crc of downloaded file does not match expected crc: " + fTemp.getAbsolutePath() + " - remote:" + remoteMeta.getName());
                item.setNotes("Downloaded file was corrupt. Did not update the local file");
            }
            if (fLocalFile.exists()) {
                if (fLocalFile.isDirectory()) {
                    log.error("can't delete a directory: " + fLocalFile.getAbsolutePath());
                    item.setNotes("remote file corresponds to a local directory, which we won't delete: " + fLocalFile.getAbsolutePath());
                    return true;
                } else {
                    if (remoteModItem.isConflicted()) {
                        log.trace("is conflicted, so will rename remote file");
                        String ext = FileUtils.getExtension(fLocalFile);
                        File conflictedPath = new File(fLocalFile.getParent(), fLocalFile.getName() + ".conflicted." + ext);
                        if (!fTemp.renameTo(conflictedPath)) {
                            log.error("Couldnt rename conflicted file to: " + conflictedPath.getAbsolutePath());
                            item.setNotes("Couldnt rename conflicted file to: " + conflictedPath.getAbsolutePath());
                            return true;
                        }
                        conflictManager.onConflict(fLocalFile, conflictedPath);
                    } else {
                        log.trace("is remotely modified, so move file into old versions folder");
                        if (!moveToOldVersions(fLocalFile)) {
                            log.error("Couldnt delete local file: " + item.getFile().getAbsolutePath());
                            item.setNotes("Couldnt delete local file: " + item.getFile().getAbsolutePath());
                            item.setCompleted(new Date());
                            return true;
                        } else {
                            log.trace("moved previous version into old versions OK");
                        }
                        boolean movedOk = false;
                        for (int i = 0; i < 5; i++) {
                            if (fTemp.renameTo(fLocalFile)) {
                                log.trace("moved to real file location: " + fLocalFile.getAbsolutePath());
                                movedOk = true;
                                break;
                            } else {
                                log.error("Error moving temp download file to: " + fLocalFile.getAbsolutePath() + " attempt " + i);
                                Thread.sleep(50);
                            }
                        }
                        if (!movedOk) {
                            log.error("Couldnt rename temp file: " + fTemp.getAbsolutePath() + " to dest file: " + fLocalFile.getAbsolutePath());
                            item.setNotes("Couldnt rename temp file: " + fTemp.getAbsolutePath() + " to dest file: " + fLocalFile.getAbsolutePath());
                            return true;
                        }
                    }
                }
            } else {
                if (!fTemp.renameTo(fLocalFile)) {
                    log.error("Error moving temp download file to: " + fLocalFile.getAbsolutePath());
                    item.setNotes("Couldnt rename temp file to dest file");
                    return true;
                } else {
                    log.trace("renamed temp to: " + fLocalFile.getAbsolutePath());
                }
            }
            long crc = crcCalculator.getLocalCrc(fLocalFile);
            localCrcDao.setLocalBackedupCrc(fLocalFile, r, crc);
        } catch (RepoNotAvailableException e) {
            log.error("repo excetion");
            throw e;
        } catch (PermanentUploadException e) {
            log.error("perm upload excetion");
            throw e;

        } catch (Exception e) {
            log.error("Exception transferring file", e);
            item.setNotes("error uploading file: " + e.getMessage());
        } finally {
            item.setCompleted(new Date());
            log.debug("completed remotely modified task");
        }
        return false;
    }

    /**
     * Rather then delete updated files, move them into a subfolder
     *
     * @param f
     * @return
     */
    public static boolean moveToOldVersions(File f) {
        try {
            File dir = f.getParentFile();
            File oldVersions = new File(dir, ".ettrema.oldversions");
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
        } catch (Throwable e) {
            log.error("Exception moving file to old versions: " + f.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    private boolean verifyDownload(File fTemp, Long expectedcrc) {
        long localCrc = crcCalculator.getLocalCrc(fTemp);
        return localCrc == expectedcrc;
    }

    private boolean doDownloadDirectory(File fLocalFile, Repo r, FileMeta remoteMeta, RemotelyModifiedQueueItem remoteModItem, Job job, QueueItem item) throws RepoNotAvailableException, PermanentUploadException {
        if (fLocalFile.exists() && fLocalFile.isFile()) {
            log.warn("Wanted to download directory, but local is a file");
            // TODO: probably should just rename the local file ... ?
            conflictManager.onTreeConflict(fLocalFile, null);
            return true;
        } else {
            LogUtils.trace(log, "doDownloadDirectory: download: ", fLocalFile.getAbsolutePath());
            if( !fLocalFile.exists() ) {
                if( !fLocalFile.mkdirs()) {
                    log.error("doDownloadDirectory: Unable to create local directory: " + fLocalFile.getAbsolutePath());
                    return false;
                }
            }
            List<FileMeta> children = remoteMeta.getChildren();
            if (children != null) {
                LogUtils.trace(log, "doDownloadDirectory: downloading children: ", children.size());
                for (FileMeta child : remoteMeta.getChildren()) {
                    File localChild = new File(fLocalFile, child.getName());
                    if (child.isDirectory()) {
                        if (!doDownloadDirectory(localChild, r, child, remoteModItem, job, item)) {
                            log.error("Failed to download: " + child.getName());
                            return false;
                        } else {
                            LogUtils.trace(log, "doDownloadDirectory - downloaded child dir", child.getName());
                        }
                    } else {
                        if (!downloadFile(localChild, r, job, child, item, remoteModItem)) {
                            log.error("Failed to download file: " + localChild.getAbsolutePath());
                            return false;
                        } else {
                            LogUtils.trace(log, "doDownloadDirectory - downloaded child file", child.getName());
                        }
                    }
                }
            } else {
                log.warn("Remote resource has null children, which might indicate it a tree conflict: " + fLocalFile.getAbsolutePath());
            }
            return true;
        }
    }
}
