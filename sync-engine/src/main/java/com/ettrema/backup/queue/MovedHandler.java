package com.ettrema.backup.queue;

import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.PermanentUploadException;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.config.UploadException;
import java.util.Date;

/**
 *
 * @author brad
 */
public class MovedHandler implements QueueItemHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MovedHandler.class);

    @Override
    public boolean supports(QueueItem item) {
        return item instanceof MovedQueueItem;
    }

    @Override
    public boolean requiresWait(QueueItem item) {
        return false;
    }

    @Override
    public void process(Repo repo, QueueItem i) throws RepoNotAvailableException {
        MovedQueueItem item = (MovedQueueItem) i;
        item.setStarted(new Date());
        try {
            repo.move(item.getFullPathFrom(), item.getFile(), i);
            i.setCompleted(new Date());
        } catch (RepoNotAvailableException ex) {
            throw ex;
        } catch (PermanentUploadException ex) {
            log.info("error processing, will not retry: " + item.getFileName(), ex);
        } catch (UploadException ex) {
            log.info("error processing, will not retry: " + item.getFileName(), ex);
            i.setNotes("Failed to upload");
        }
    }
}
