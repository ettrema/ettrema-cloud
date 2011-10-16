package com.ettrema.backup.event;

import com.ettrema.event.Event;

/**
 *
 * @author brad
 */
public class ScanEvent implements Event{
    private final boolean started; // otherwise finished

    public ScanEvent( boolean started ) {
        this.started = started;
    }



    public boolean isStarted() {
        return started;
    }

    /*
     * just so there's no confusion..
     */
    public boolean isFinished() {
        return !started;
    }
}
