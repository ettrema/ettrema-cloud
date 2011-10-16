package com.ettrema.backup.event;

import com.ettrema.backup.config.Root;
import com.ettrema.event.Event;

/**
 *
 * @author brad
 */
public class RootChangedEvent implements Event {
    private final Root root;

    public RootChangedEvent( Root root ) {
        this.root = root;
    }

    public Root getRoot() {
        return root;
    }

    
}
