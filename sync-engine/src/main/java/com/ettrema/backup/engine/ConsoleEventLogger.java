package com.ettrema.backup.engine;

import com.ettrema.backup.config.QueueItem;

/**
 *
 * @author brad
 */
public class ConsoleEventLogger implements EventLogger{

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ConsoleEventLogger.class);

    public void logStarted( QueueItem item ) {
        log.info("started: " + item.toString());
    }

    public void logCompleted( QueueItem item ) {
        log.info("completed: " + item.toString());
    }

}
