package com.ettrema.backup.engine;

import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.event.RepoChangedEvent;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.event.EventManager;

/**
 *
 * @author brad
 */
public class StatusService {
	
	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( StatusService.class );
	
	private final EventManager eventManager;

	public StatusService(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	
    public boolean isAllReposOffline( Job job ) {
        for( Repo repo : job.getRepos() ) {
            if( !repo.isOffline() ) {
                return false;
            }
        }
        return true;
    }
	
    public void setOffline( Job j, Repo r ) {
        log.info( "setOffline: " + r.toString() );
        r.setOffline( true );
        EventUtils.fireQuietly( eventManager, new RepoChangedEvent( r ) );
    }
	
}
