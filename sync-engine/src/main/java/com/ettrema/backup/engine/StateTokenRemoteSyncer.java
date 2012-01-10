package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.config.Root;
import com.ettrema.common.LogUtils;
import com.ettrema.httpclient.Folder;
import com.ettrema.httpclient.Host;
import com.ettrema.httpclient.HttpException;
import com.ettrema.httpclient.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class StateTokenRemoteSyncer implements RemoteSyncer {

    private static final Logger log = LoggerFactory.getLogger(StateTokenRemoteSyncer.class);
    private final Config config;
    private final StateTokenDaoImpl stateTokenDao;
    private final TransferAuthorisationService transferAuthorisationService;
    private final ConflictManager conflictManager;
    private final CrcCalculator crcCalculator;
    private final StateTokenFileSyncer stateTokenFileSyncer;
    private final ExclusionsService exclusionsService;

    public StateTokenRemoteSyncer(Config config, TransferAuthorisationService transferAuthorisationService, ConflictManager conflictManager, CrcCalculator crcCalculator, StateTokenDaoImpl stateTokenDao, StateTokenFileSyncer stateTokenFileSyncer, ExclusionsService exclusionsService) {
        this.config = config;
        this.transferAuthorisationService = transferAuthorisationService;
        this.conflictManager = conflictManager;
        this.crcCalculator = crcCalculator;
        this.stateTokenDao = stateTokenDao;
        this.stateTokenFileSyncer = stateTokenFileSyncer;
        this.exclusionsService = exclusionsService;
    }

    @Override
    public void ping() {
        for (Job j : config.getJobs()) {
            for (Repo r : j.getRepos()) {
                if (!stateTokenFileSyncer.isUptodate()) {
                    return;
                }
                if (r instanceof DavRepo) {
                    pingDavRepo(j, (DavRepo) r);
                }
            }
        }
    }

    private void pingDavRepo(Job job, DavRepo davRepo) {
        if (!stateTokenFileSyncer.isUptodate()) {
            log.info("aborting sync check because file syncer is not up to date");
            return;
        }
        String h = davRepo.getHostName();
        if (h == null || h.length() == 0) {
            LogUtils.trace(log, "pingDavRepo: Host not configured so aborting scan");
            return;
        }
        // Note that we MUST not call through to the host to do a check if there
        // is a task in progress because the call will block on the Host methods
        // And, if there is a task running we must be online
        if (davRepo.getState().current != null) {
            LogUtils.trace(log, "pingDavRepo: there is an operation in progress so abort scan", davRepo.getState().current);
            return;
        }

        try {
            try {
                Host host = davRepo.host(true);
                davRepo.updateAccountInfo(host);

                for (Root root : job.getRoots()) {
                    Resource remote = host.find(root.getRepoName());
                    File dir = new File(root.getFullPath());
                    LogUtils.trace(log, "pingDavRepo: root:", dir.getAbsolutePath(), "crc:", remote.getCrc());
                    if (remote instanceof Folder) {
                        if (!compareFolder(davRepo, (Folder) remote, dir)) {
                            log.trace("comparison aborted");
                            return;
                        }
                    } else {
                        log.warn("remote root is not a folder: " + remote.href());
                    }
                }
            } catch (RepoNotAvailableException ex) {
                log.trace("repo exeption", ex);
            }
        } catch (Throwable e) {
            log.trace("unknown exception", e);
        }
    }

    /**
     *
     *
     * @param repo
     * @param remoteFolder
     * @param dir
     * @return - false indicates the comparison has been aborted
     * @throws RepoNotAvailableException
     */
    private boolean compareFolder(DavRepo repo, Folder remoteFolder, File dir) throws RepoNotAvailableException {
        if (!stateTokenFileSyncer.isUptodate()) {
            log.info("aborting sync check because file syncer is not up to date");
            return false;
        }

        if (remoteFolder.getCrc() == null) {
            log.trace("No crc: " + remoteFolder.href());
            return true;
        }
        StateToken token = stateTokenDao.get(dir);
        List<StateToken> tokens;
        if (token == null) {
            log.info("New remote folder; " + remoteFolder.href() + " - local:" + dir.getAbsolutePath());
            try {
                transferAuthorisationService.requestDownload(repo, remoteFolder);
            } catch (IOException ex) {
                throw new RepoNotAvailableException(remoteFolder.encodedUrl(), ex);
            } catch (HttpException ex) {
                java.util.logging.Logger.getLogger(StateTokenRemoteSyncer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            if (!remoteFolder.getCrc().equals(token.currentCrc)) {
                LogUtils.trace(log, "compareFolder: crc's are not equal", remoteFolder.href(), dir.getAbsolutePath(), remoteFolder.getCrc(), token.currentCrc);
                if (log.isTraceEnabled()) {
                    tokens = stateTokenDao.findForFolder(dir);
                    log.trace(crcCalculator.getLocalCrcForDirectoryData(tokens));
                }
                try {
                    // remote folder contains different contents to the local folder
                    compareChildren(repo, remoteFolder, dir);
                } catch (IOException ex) {
                    throw new RepoNotAvailableException(ex);
                } catch (HttpException ex) {
                    throw new RepoNotAvailableException(ex);
                }
            } else {
                // Identical, nothing to do
                LogUtils.trace(log, "Local and remote folders are in perfect sync", remoteFolder.href(), dir.getAbsolutePath());
            }
        }
        return true;
    }

    private void compareChildren(DavRepo repo, Folder remoteFolder, File dir) throws IOException, HttpException, RepoNotAvailableException {
        LogUtils.trace(log, "compareChildren", remoteFolder.encodedUrl(), dir.getAbsolutePath());
        List<? extends Resource> remoteChildren = remoteFolder.children();
        File[] localChildren = dir.listFiles();

        // Iterate through remote resources reconciling them against local resources
        // This will download new remote files when local file are missing, and will delete
        // remote files if local files have been removed
        for (Resource r : remoteChildren) {
            File local = child(r.name, localChildren);
            if (local == null) {
                local = new File(dir, r.name);
            }
            if (exclusionsService.isBackupable(local)) {
                StateToken token = stateTokenDao.get(local);
                if (!local.exists()) {
                    // Doesnt exist, so authorise download
                    if (token == null) {
                        LogUtils.debug(log, "compareChildren: Found new remote resource: " + r.href());
                        transferAuthorisationService.requestDownload(repo, r);
                    } else {
                        LogUtils.debug(log, "compareChildren: Found deleted local resource: " + r.href());
                        transferAuthorisationService.requestRemoteDelete(repo, r);
                    }
                } else {
                    // A local resource exists with the same name
                    if (r instanceof Folder) {
                        Folder childFolder = (Folder) r;
                        compareFolder(repo, childFolder, local);
                    } else {
                        // remote is a file and a local resource exists with the same name
                        com.ettrema.httpclient.File childFile = (com.ettrema.httpclient.File) r;
                        if (local.isDirectory()) {
                            // ok, this is a bit weird. Remote is a file but local is a directory. Definitely a conflict
                            LogUtils.warn(log, "treeConflict", local.getAbsolutePath());
                            conflictManager.onTreeConflict(local, childFile);
                        } else {
                            // local and remote both exist and are files, so just compare crc's
                            if (r.getCrc() == null) {
                                log.warn("No remote crc, so can't check files: " + r.href());
                            } else {
                                compareFiles(repo, local, childFile);
                            }
                        }
                    }
                }
            }
        }
        // Now look for local file which don't match against a remote resource, these will
        // either be uploaded or deleted locally
        if (localChildren != null) {
            for (File local : localChildren) {
                if (exclusionsService.isBackupable(local)) {
                    Resource r = child(local.getName(), remoteChildren);
                    if (r == null) {
                        // we have a local resource with no corresponding server resource
                        // has either been added locally or removed remotely
                        // The stateToken will tell us if it has been added locally
                        StateToken token = stateTokenDao.get(local);
                        if (token == null || token.backedupCrc == null) {
                            // is added locally so upload
                            LogUtils.trace(log, "compareChildren: found local resource which has not been backed up, and no corresponding server resource. Upload", local.getAbsolutePath());
                            transferAuthorisationService.requestUpload(local);
                        } else {
                            // we have a state token and it has been backed up previously, but is now not on server
                            // so check if crc has changed, which indicates local mods, otherwise delete local
                            LogUtils.trace(log, "compareChildren: found local resource which has a pristine backup, and no corresponding server resource. Delete local.", local.getAbsolutePath());
                            transferAuthorisationService.requestDeleteLocal(local);
                        }
                    }
                }
            }
        }
    }

    private File child(String name, File[] localChildren) {
        if (localChildren != null) {
            for (File f : localChildren) {
                if (f.getName().equals(name)) {
                    return f;
                }
            }
        }
        return null;
    }

    private Resource child(String name, List<? extends Resource> remoteChildren) {
        if (remoteChildren != null) {
            for (Resource f : remoteChildren) {
                if (f.name.equals(name)) {
                    return f;
                }
            }
        }
        return null;
    }

    private void compareFiles(DavRepo repo, File localFile, com.ettrema.httpclient.File remoteFile) throws IOException, HttpException {
        StateToken token = stateTokenDao.get(localFile);
        long localCrc;
        if (token == null) {
            log.warn("No local crc for existing file: " + localFile.getAbsolutePath());
            localCrc = crcCalculator.getLocalCrc(localFile);
        } else {
            localCrc = token.currentCrc;
        }
        if (localCrc != remoteFile.getCrc().longValue()) {
            log.info("Different crc's: " + localFile.getAbsolutePath());
            if (token != null) {
                if (token.backedupCrc == null) {
                    LogUtils.trace(log, "Local and remote CRC's differ. Local has never backed up. Conflict", localFile.getAbsolutePath(), remoteFile.encodedUrl());
                    transferAuthorisationService.resolveConflict(remoteFile, localFile);
                } else if (token.backedupCrc == token.currentCrc) {
                    // local has been backed up, so can overwrite
                    LogUtils.trace(log, "CRC's differ. local has been backed up and is pristine, so can overwrite local. Download", localFile.getAbsolutePath());
                    transferAuthorisationService.requestDownload(repo, remoteFile);
                } else {
                    if (token.backedupCrc == remoteFile.getCrc()) {
                        LogUtils.trace(log, "Local and remote CRC's differ. Remote is identical to last backup, so local has been updated. Upload", localFile.getAbsolutePath());
                        transferAuthorisationService.requestUpload(localFile);
                    } else {
                        LogUtils.trace(log, "Local and remote CRC's differ. Remote is different to last backup, so local has been updated. Upload", localFile.getAbsolutePath());
                        transferAuthorisationService.resolveConflict(remoteFile, localFile);
                    }
                }
            }
        } else {
            LogUtils.trace(log, "compareFiles: files are identical", localFile.getAbsolutePath(), remoteFile.encodedUrl());
        }
    }
}
