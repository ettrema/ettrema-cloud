package com.ettrema.backup.queue;

import com.ettrema.backup.config.*;

/**
 *
 * @author brad
 */
public interface QueueItemHandler {

    /**
     * Can this handler process the given item
     *
     * @param item
     * @return
     */
    boolean supports(QueueItem item);

    /**
     * Return true if the item cannot be processed yet.
     * 
     * @param item
     * @return
     */
    boolean requiresWait(QueueItem item);

    /**
     * Process the item, usually asynchronously by creating a task
     * with the given application
     *
     * @param repo
     * @param job
     * @param item
     */
    void process(Repo repo, QueueItem item) throws RepoNotAvailableException, PermanentUploadException;
}
