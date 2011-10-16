package com.ettrema.backup.engine;

import com.ettrema.backup.config.QueueItem;

/**
 *
 * @author brad
 */
public interface EventLogger {
    void logStarted(QueueItem item);

    void logCompleted(QueueItem item);
}
