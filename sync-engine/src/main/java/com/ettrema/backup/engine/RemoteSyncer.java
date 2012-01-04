package com.ettrema.backup.engine;

/**
 * Performs syncronisation with remote repositories with appropriate mechanisms
 * , such as state token comparisons or direct file comparisons
 *
 * @author brad
 */
public interface RemoteSyncer {
	/**
	 * Just check to see if the repositories which are used by this filesyncer are
	 * online. Repo.setOffline should be called as appropriate
	 */
	void ping();
	
}
