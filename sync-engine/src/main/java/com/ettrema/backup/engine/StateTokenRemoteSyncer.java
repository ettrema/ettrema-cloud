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

    public StateTokenRemoteSyncer(Config config, TransferAuthorisationService transferAuthorisationService, ConflictManager conflictManager, CrcCalculator crcCalculator, StateTokenDaoImpl stateTokenDao) {
        this.config = config;
        this.transferAuthorisationService = transferAuthorisationService;
        this.conflictManager = conflictManager;
        this.crcCalculator = crcCalculator;
        this.stateTokenDao = stateTokenDao;
    }

    @Override
    public void ping() {
        for (Job j : config.getJobs()) {
            for (Repo r : j.getRepos()) {
                if (r instanceof DavRepo) {
                    pingDavRepo(j, (DavRepo) r);
                }
            }
        }
    }

    private void pingDavRepo(Job job, DavRepo davRepo) {
        String h = davRepo.getHostName();
        LogUtils.trace(log, "pingDavRepo", h);
        if (h == null || h.length() == 0) {
            log.trace("Not configured");
            return;
        }
        // Note that we MUST not call through to the host to do a check if there
        // is a task in progress because the call will block on the Host methods
        // And, if there is a task running we must be online
        if (davRepo.getState().current != null) {
            return;
        }

        try {
            try {
                Host host = davRepo.host(true);
                davRepo.updateAccountInfo(host);

                for (Root root : job.getRoots()) {
                    Resource remote = host.find(root.getRepoName());
                    File dir = new File(root.getFullPath());
                    if (remote instanceof Folder) {
                        compareFolder((Folder) remote, dir);
                    } else {
                        log.warn("remote root is not a folder: " + remote.href());
                    }
                }
                return;
            } catch (RepoNotAvailableException ex) {
                log.trace("repo exeption", ex);
                return;
            }
        } catch (Throwable e) {
            log.trace("unknown exception", e);
            return;
        }
    }

    private void compareFolder(Folder remoteFolder, File dir) throws RepoNotAvailableException {
        if (remoteFolder.getCrc() == null) {
            log.trace("No crc: " + remoteFolder.href());
            return;
        }
        StateToken token = stateTokenDao.get(dir);
        if (token == null) {
            log.info("New remote folder; " + remoteFolder.href() + " - local:" + dir.getAbsolutePath());
            transferAuthorisationService.requestDownload(remoteFolder);
        } else {
            if (!remoteFolder.getCrc().equals(token.currentCrc)) {
                LogUtils.trace(log, "compareFolder: crc's are not equal", remoteFolder.href(), dir.getAbsolutePath());
                try {
                    // remote folder contains different contents to the local folder
                    compareChildren(remoteFolder, dir);
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
    }

    private void compareChildren(Folder remoteFolder, File dir) throws IOException, HttpException, RepoNotAvailableException {
        List<? extends Resource> remoteChildren = remoteFolder.children();
        File[] localChildren = dir.listFiles();

        // Iterate through remote resources reconciling them against local resources
        // This will download new remote files when local file are missing, and will delete
        // remote files if local files have been removed
        for (Resource r : remoteChildren) {
            File local = child(r.name, localChildren);
            StateToken token = stateTokenDao.get(local);
            if (local == null) {
                // Doesnt exist, so authorise download
                if( token == null ) {
                    transferAuthorisationService.requestDownload(r);
                } else {
                    transferAuthorisationService.requestRemoteDelete(r);
                }
            } else {
                // A local resource exists with the same name
                if (r instanceof Folder) {
                    Folder childFolder = (Folder) r;
                    compareFolder(childFolder, local);
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
                            compareFiles(local, r);
                        }
                    }
                }
            }
        }
        // Now look for local file which don't match against a remote resource, these will
        // either be uploaded or deleted locally
        if(localChildren != null) {
            for(File local : localChildren) {
                Resource r = child(local.getName(), remoteChildren);
                if( r == null ) {
                    // we have a local resource with no corresponding server resource
                    // has either been added locally or removed remotely
                    // The stateToken will tell us if it has been added locally
                    StateToken token = stateTokenDao.get(local);
                    if( token == null || token.backedupCrc == null ) {
                        // is added locally so upload
                        LogUtils.trace(log, "compareChildren: found local resource which has not been backed up, and no corresponding server resource. Upload", local.getAbsolutePath());
                        transferAuthorisationService.requestUpload(local);
                    } else {
                        // we have a state token and it has been backed up previously, but is now not on server
                        // so check if crc has changed, which indicates local mods, otherwise delete local
                        if( token.backedupCrc.equals(token.currentCrc)) {
                            // no mods, so can be safely deleted
                            LogUtils.trace(log, "compareChildren: found local resource which has a pristine backup, and no corresponding server resource. Delete local.", local.getAbsolutePath());
                            transferAuthorisationService.requestDeleteLocal(local);
                        } else {
                            LogUtils.trace(log, "compareChildren: found local resource which has been backed up, but then modified, and no corresponding server resource. Tree conflict.");
                            conflictManager.onTreeConflict(local, r);
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

    private void compareFiles(File l, Resource r) {
        StateToken token = stateTokenDao.get(l);
        long localCrc;
        if (token == null) {
            log.warn("No local crc for existing file: " + l.getAbsolutePath());
            localCrc = crcCalculator.getLocalCrc(l);
        } else {
            localCrc = token.currentCrc;
        }
        if (localCrc != r.getCrc().longValue()) {
            log.info("Different crc's: " + l.getAbsolutePath());
            if (token != null) {
                if (token.backedupCrc == null) {
                    LogUtils.trace(log, "Local and remote CRC's differ. Local has never backed up. Conflict", l.getAbsolutePath());
                    transferAuthorisationService.resolveConflict(r, l);
                } else if (token.backedupCrc == token.currentCrc) {
                    // local has been backed up, so can overwrite
                    LogUtils.trace(log, "CRC's differ. local has been backed up and is pristine, so can overwrite local. Download", l.getAbsolutePath());
                    transferAuthorisationService.requestDownload(r);
                } else {
                    if (token.backedupCrc == r.getCrc()) {
                        LogUtils.trace(log, "Local and remote CRC's differ. Remote is identical to last backup, so local has been updated. Upload", l.getAbsolutePath());
                        transferAuthorisationService.requestUpload(l);
                    } else {
                        LogUtils.trace(log, "Local and remote CRC's differ. Remote is different to last backup, so local has been updated. Upload", l.getAbsolutePath());
                        transferAuthorisationService.resolveConflict(r, l);
                    }
                }
            }
        }
    }
}
