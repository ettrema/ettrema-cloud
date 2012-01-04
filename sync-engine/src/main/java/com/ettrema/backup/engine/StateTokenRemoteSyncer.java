package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.config.Root;
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
	private final StateTokenDaoImpl stateTokenDao = new StateTokenDaoImpl();
	private final TransferAuthorisationService transferAuthorisationService;
	private final ConflictManager conflictManager;
	private final CrcCalculator crcCalculator;

	public StateTokenRemoteSyncer(Config config, TransferAuthorisationService transferAuthorisationService, ConflictManager conflictManager, CrcCalculator crcCalculator) {
		this.config = config;
		this.transferAuthorisationService = transferAuthorisationService;
		this.conflictManager = conflictManager;
		this.crcCalculator = crcCalculator;
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
			log.info("New remote folder; " + remoteFolder.href());
			transferAuthorisationService.requestDownload(remoteFolder);
		} else {
			if (!remoteFolder.getCrc().equals(token.currentCrc)) {
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
			}
		}
	}

	private void compareChildren(Folder remoteFolder, File dir) throws IOException, HttpException, RepoNotAvailableException {
		List<? extends Resource> remoteChildren = remoteFolder.children();
		File[] localChildren = dir.listFiles();
		StateToken token;

		for (Resource r : remoteChildren) {
			File l = child(r.name, localChildren);
			if (l == null) {
				// Doesnt exist, so authorise download
				transferAuthorisationService.requestDownload(r);
			} else {
				// A local resource exists with the same name
				if (r instanceof Folder) {
					Folder childFolder = (Folder) r;
					compareFolder(childFolder, l);
				} else {
					// remote is a file and a local resource exists with the same name
					com.ettrema.httpclient.File childFile = (com.ettrema.httpclient.File) r;
					if (l.isDirectory()) {
						// ok, this is a bit weird. Remote is a file but local is a directory. Definitely a conflict
						conflictManager.onTreeConflict(l, childFile);
					} else {
						// local and remote both exist and are files, so just compare crc's
						if (r.getCrc() == null) {
							log.warn("No remote crc, so can't check files: " + r.href());
						} else {
							compareFiles(l, r);
						}
					}
				}
			}
		}
	}

	private File child(String name, File[] localChildren) {
		for (File f : localChildren) {
			if (f.getName().equals(name)) {
				return f;
			}
		}
		return null;
	}
	

	private void compareFiles(File l, Resource r) {
		StateToken token;
		token = stateTokenDao.get(l);
		long localCrc;
		if (token == null) {
			log.warn("No local crc for existing file: " + l.getAbsolutePath());
			localCrc = crcCalculator.getLocalCrc(l);
		} else {
			localCrc = token.currentCrc;
		}
		if (localCrc != r.getCrc().longValue()) {
			log.info("Different crc's: " + l.getAbsolutePath());
			if( token != null ) {
				if( token.backedupCrc == null ) {
					// Never backed up, so definitely conflict. 
					transferAuthorisationService.resolveConflict(r,l);
				} else if( token.backedupCrc == token.currentCrc) {
					// local has been backed up, so can overwrite
					transferAuthorisationService.requestDownload(r);
				} else {
					if( token.backedupCrc == r.getCrc()) {
						// remote is same as what we last backed up, so local has been updated
						transferAuthorisationService.requestUpload(l);
					} else {
						// local has changed since last backup, but server also differs = conflict
						transferAuthorisationService.resolveConflict(r,l);
					}
				}
			}
		}
	}
	
}
