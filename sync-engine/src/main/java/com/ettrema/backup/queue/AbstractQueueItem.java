package com.ettrema.backup.queue;

import com.ettrema.backup.config.Queue;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.observer.ObserverUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author brad
 */
public abstract class AbstractQueueItem implements QueueItem {

    private transient List<Observer<QueueItem, Queue>> observers;

    private final UUID id;
    
    private Date started;

    private Date completed;

    private String notes;

    public AbstractQueueItem() {
        id = UUID.randomUUID();
    }

    public void onStatusChanged() {
        ObserverUtils.notifyUpdated( observers(), this, null);
    }

    private List<Observer<QueueItem, Queue>> observers() {
        if( observers == null ) {
            observers = new ArrayList<Observer<QueueItem, Queue>>();
        }
        return observers;
    }
    
    @Override
    public void addObserver( Observer<QueueItem, Queue> ob ) {
        observers().add( ob );
    }

    @Override
    public Date getStarted() {
        return started;
    }

    @Override
    public void setStarted( Date started ) {
        this.started = started;
    }

    @Override
    public Date getCompleted() {
        return completed;
    }

    @Override
    public void setCompleted( Date finished ) {
        this.completed = finished;

    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public final void setNotes( String notes ) {
        System.out.println("setNotes: " + notes);
        this.notes = notes;
        onStatusChanged();
    }

    
    @Override
    public final boolean equals( Object obj ) {
        if( obj == null ) {
            return false;
        }
        if( getClass() != obj.getClass() ) {
            return false;
        }
        final AbstractQueueItem other = (AbstractQueueItem) obj;
        return id.equals(other.getId());
    }

    public UUID getId() {
        return id;
    }
    
    

    @Override
    public final  int hashCode() {
        return id.hashCode();
    }



    


}

