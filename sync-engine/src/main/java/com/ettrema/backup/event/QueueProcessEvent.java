package com.ettrema.backup.event;

import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import com.ettrema.event.Event;

/**
 *
 * @author brad
 */
public class QueueProcessEvent implements Event{

    public enum Status {
        PROCESSING("processing"),
        COMPLETED_OK("Completed OK"),
        ERROR("Error"),
        RETRY("Retrying..");

        private String text;

        private Status( String text ) {
            this.text = text;
        }


    }

    private final QueueItem item;
    private final Status status;
	private final Repo repo;

    public QueueProcessEvent( QueueItem item, Status status, Repo repo ) {
        this.item = item;
        this.status = status;
		this.repo = repo;
    }

    public QueueItem getItem() {
        return item;
    }

    public Status getStatus() {
        return status;
    }

	public Repo getRepo() {
		return repo;
	}    
}
