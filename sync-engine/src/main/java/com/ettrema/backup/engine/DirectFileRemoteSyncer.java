package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.LocalRepo;
import com.ettrema.backup.config.LocalRepo.LocalRepoState;
import com.ettrema.backup.config.Repo;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class DirectFileRemoteSyncer implements RemoteSyncer {

	private static final Logger log = LoggerFactory.getLogger(DirectFileRemoteSyncer.class);
	
	private final Config config;

	public DirectFileRemoteSyncer(Config config) {
		this.config = config;
	}
	

	@Override
	public void ping() {
		for( Repo r : config.getAllRepos()) {
			if( r instanceof LocalRepo) {
				pingRepo((LocalRepo)r);
			}
		}		
	}
	
	private void pingRepo(LocalRepo localRepo) {
		LocalRepoState state;
		try {
			File target = localRepo.getTarget();
			if (!target.exists()) {
				log.trace("repo is offline because does not exist: " + target.getAbsolutePath());
				return;
			} else {
				state = localRepo.getState();
				state.setMaxBytes((Long) target.getTotalSpace());
				state.setUsedBytes((Long) state.getMaxBytes() - target.getFreeSpace()); 
				log.trace("repo is online");
				return;
			}
		} catch (Exception e) {
			log.trace("repo is offline because there was an exception listing files");
			return;
		}
	}
}
