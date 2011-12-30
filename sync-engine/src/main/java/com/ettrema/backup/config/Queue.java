package com.ettrema.backup.config;

import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.observer.ObserverUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author brad
 */
public class Queue implements Iterable<QueueItem> {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Queue.class );
    private List<QueueItem> listOfItems = new java.util.concurrent.CopyOnWriteArrayList<QueueItem>();
    private transient List<Observer<QueueItem, Queue>> observers = new CopyOnWriteArrayList<Observer<QueueItem, Queue>>();
    private transient MyObserver myObserver; // used for dispatching observations from queueitems to queue observers

    private MyObserver myObserver() {
        if( myObserver == null ) {
            myObserver = new MyObserver();
        }
        return myObserver;
    }

    public boolean isEmpty() {
        return listOfItems == null || listOfItems.isEmpty();
    }

    public void addItem( QueueItem item ) {
//        log.debug( "addItem" );
        listOfItems.add( item );
        item.addObserver( myObserver() );
        ObserverUtils.notifyAdded( observers(), item, this );
    }

    public void notifyObserversUpdated(QueueItem item) {
        ObserverUtils.notifyUpdated( observers(), item, this );
    }

	public List<Observer<QueueItem, Queue>> observers() {
        if( observers == null ) {
            observers = new ArrayList<Observer<QueueItem, Queue>>();
        }
		return observers;
	}
	
    public void addObserver( Observer<QueueItem, Queue> ob ) {
        observers().add( ob );
    }

	@Override
    public Iterator<QueueItem> iterator() {
        return listOfItems.iterator();
    }

    public boolean contains( QueueItem nf ) {
        if( listOfItems == null ) {
            return false;
        }
        return listOfItems.contains( nf );
    }

    public int size() {
        return listOfItems.size();
    }

    public QueueItem item( int row ) {
        if( row >= listOfItems.size() ) {
            throw new IndexOutOfBoundsException( "Row: " + row + " listOfItems: " + listOfItems.size() + " items:" + listOfItems.size() );
        }
        return listOfItems.get( row );

    }

    public synchronized int indexOf( QueueItem item ) {
        return listOfItems.indexOf( item );
    }

    public long getRemainingBytes() {
        long l = 0;
        for( QueueItem item : listOfItems ) {
            l += item.getBytesToUpload();
        }
        return l;
    }

    public void save() {
        // TODO
    }

    public synchronized QueueItem take() throws InterruptedException {
		if( listOfItems.size() > 0 ) {
			QueueItem item = listOfItems.remove(0);
			int removedIndex = indexOf( item );
			ObserverUtils.notifyRemoved( observers(), item, this, removedIndex );
			listOfItems.remove( item );
			return item;
		} else {
			return null;
		}
    }

    private class MyObserver implements Observer<QueueItem, Queue> {

        @Override
        public void onAdded( QueueItem t, Queue parent ) {
        }

        @Override
        public void onUpdated( QueueItem t, Queue parent ) {
            ObserverUtils.notifyUpdated( observers, t, Queue.this );
        }

        @Override
        public void onRemoved( QueueItem t, Queue parent, Integer indexOf ) {
        }
    }
}
