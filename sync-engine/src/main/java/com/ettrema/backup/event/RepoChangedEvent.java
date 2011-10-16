package com.ettrema.backup.event;

import com.ettrema.backup.config.Repo;
import com.ettrema.event.Event;

/**
 *
 * @author brad
 */
public class RepoChangedEvent implements Event{

    private final Repo repo;

    public RepoChangedEvent( Repo repoEngine ) {
        this.repo = repoEngine;
    }

    public Repo getRepo() {
        return repo;
    }


    

}
