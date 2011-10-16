package com.ettrema.backup.event;

import com.ettrema.backup.config.Queue;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.event.Event;

/**
 * Raised when an item is enqueued or finished
 *
 * @author brad
 */
public class QueueItemEvent implements Event {

    private final Queue q;
    private final QueueItem item;
    private final boolean finished;

    public QueueItemEvent( Queue q, QueueItem item, boolean finished ) {
        this.q = q;
        this.item = item;
        this.finished = finished;
    }

    public QueueItem getItem() {
        return item;
    }

    public Queue getQ() {
        return q;
    }

    public boolean isFinished() {
        return finished;
    }
}
