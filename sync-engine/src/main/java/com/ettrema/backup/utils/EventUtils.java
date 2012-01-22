package com.ettrema.backup.utils;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import static com.ettrema.backup.engine.Services._;
import com.ettrema.event.Event;
import com.ettrema.event.EventManager;

/**
 *
 * @author brad
 */
public class EventUtils {

    public static void fireQuietly( Event e) {
        EventManager eventManager = _(EventManager.class);
        fireQuietly( eventManager, e );
    }

    public static void fireQuietly(EventManager eventManager, Event e) {
        try {
            eventManager.fireEvent( e );
        } catch( ConflictException ex ) {
            throw new RuntimeException( ex );
        } catch( BadRequestException ex ) {
            throw new RuntimeException( ex );
        } catch( NotAuthorizedException ex ) {
            throw new RuntimeException( ex );
        }
    }
}
