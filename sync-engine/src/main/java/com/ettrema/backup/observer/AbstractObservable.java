package com.ettrema.backup.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author brad
 */
public class AbstractObservable {

    private transient boolean changed;

    private transient List<Observer> observers;

    public void addObserver(Observer ob){
        if( observers == null ) {
            observers = new CopyOnWriteArrayList<Observer>();
        }
        observers.add( ob );
    }

    protected void setChanged() {
        changed = true;
    }

    protected void notifyObservers() {
        if( !changed ) {
            return ;
        }
        if( observers == null ) {
            return ;
        } else {
            for( Observer ob : observers){
				System.out.println("notify: " + ob);
                ob.onUpdated( this, null);
            }
        }
    }
}
