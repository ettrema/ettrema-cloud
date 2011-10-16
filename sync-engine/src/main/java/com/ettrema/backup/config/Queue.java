package com.ettrema.backup.config;

import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.observer.ObserverUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author brad
 */
public class Queue implements Iterable<QueueItem> {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Queue.class );
    private BlockingQueue<QueueItem> items = new LinkedBlockingQueue<QueueItem>();
    private List<QueueItem> listOfItems = new ArrayList<QueueItem>();
    private transient List<Observer<QueueItem, Queue>> observers = new CopyOnWriteArrayList<Observer<QueueItem, Queue>>();
    private transient MyObserver myObserver; // used for dispatching observations from queueitems to queue observers

    private MyObserver myObserver() {
        if( myObserver == null ) {
            myObserver = new MyObserver();
        }
        return myObserver;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public void addItem( QueueItem item ) {
//        log.debug( "addItem" );
        items.add( item );
        listOfItems.add( item );
        item.addObserver( myObserver() );
        ObserverUtils.notifyAdded( observers, item, this );
    }

    public void notifyObserversUpdated(QueueItem item) {
        ObserverUtils.notifyUpdated( observers, item, this );
    }

    public void addObserver( Observer<QueueItem, Queue> ob ) {
        if( observers == null ) {
            observers = new ArrayList<Observer<QueueItem, Queue>>();
        }
        observers.add( ob );
    }

	@Override
    public Iterator<QueueItem> iterator() {
        return items.iterator();
    }

    public boolean contains( QueueItem nf ) {
        if( items == null ) {
            return false;
        }
        return items.contains( nf );
    }

    public int size() {
        return items.size();
    }

    public QueueItem item( int row ) {
        if( row >= listOfItems.size() ) {
            throw new IndexOutOfBoundsException( "Row: " + row + " listOfItems: " + listOfItems.size() + " items:" + items.size() );
        }
        return listOfItems.get( row );

    }

    public synchronized int indexOf( QueueItem item ) {
        return listOfItems.indexOf( item );
    }

    public long getRemainingBytes() {
        long l = 0;
        for( QueueItem item : this ) {
            l += item.getBytesToUpload();
        }
        return l;
    }

    public void save() {
        // TODO
    }

    public QueueItem take() throws InterruptedException {
        QueueItem item = items.take();
        int removedIndex = indexOf( item );
        ObserverUtils.notifyRemoved( observers, item, this, removedIndex );
        listOfItems.remove( item );
        return item;
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
