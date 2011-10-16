package com.ettrema.backup.config;

import java.util.List;


/**
 *
 * @author brad
 */
public class Job {
    private final String id;
    private final List<? extends Repo> repos;
    private final List<Root> roots;

    transient Config config;

    public Job( String id, List<? extends Repo> repos, List<Root> roots ) {
        this.id = id;
        this.repos = repos;
        this.roots = roots;
    }

    public String getId() {
        return id;
    }
    
    public Iterable<? extends Repo> getRepos() {
        return repos;
    }

    public List<Root> getRoots() {
        return roots;
    }

    public void setConfig( Config config ) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }
    
    public String getFirstDavRepo() {
        for(Repo r : repos ) {
            if( r instanceof DavRepo) {
                return ((DavRepo)r).getHostName();
            }
        }
        return "";
    }

	public boolean isConfigured() {
		// Must be at least one root and a configured repo
		boolean hasRepo = false;
		for( Repo r : repos ) {
			if( r.isConfigured() ) { 
				hasRepo = true;
				break;
			}
		}
		boolean hasRoot = false;
		for( Root r : roots ) {
			if( r.isConfigured() ) {
				hasRoot = true;
				break;
			}
		}
		return hasRepo && hasRoot;
	}
}
