package com.ettrema.backup.observer;

import java.util.List;

/**
 *
 * @author brad
 */
public class ObserverUtils {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ObserverUtils.class );

    public static <T,P> void notifyAdded( List<Observer<T,P>> observers, T item, P parent ) {
        for( Observer<T,P> ob : observers ) {
//            log.trace("notifyAdded: " + ob.getClass());
            ob.onAdded( item, parent );
        }
    }

    public static <T,P> void notifyUpdated( List<Observer<T,P>> observers, T item, P parent  ) {
        for( Observer<T,P> ob : observers ) {
//            log.trace("notifyUpdated: " + ob.getClass());
            System.out.println("notigy: " + ob + " about " + item);
            ob.onUpdated( item, parent );
        }
    }


    public static <T,P> void notifyRemoved( List<Observer<T,P>> observers, T item, P parent, Integer index  ) {
        for( Observer<T,P> ob : observers ) {
//            log.trace("notifyUpdated: " + ob.getClass());
            ob.onRemoved(item, parent, index);
        }
    }
}
