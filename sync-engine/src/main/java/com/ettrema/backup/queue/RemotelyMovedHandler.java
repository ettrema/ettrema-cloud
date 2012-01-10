package com.ettrema.backup.queue;

import com.ettrema.backup.config.PermanentUploadException;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import java.io.File;
import java.util.Date;

/**
 *
 * @author brad
 */
public class RemotelyMovedHandler implements QueueItemHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RemotelyMovedHandler.class);

    @Override
    public boolean supports(QueueItem item) {
        return item instanceof RemotelyMovedQueueItem;
    }

    @Override
    public boolean requiresWait(QueueItem item) {
        return false;
    }

    @Override
    public void process(Repo r, QueueItem item) throws RepoNotAvailableException, PermanentUploadException {
        item.setStarted(new Date());

        RemotelyMovedQueueItem remoteModItem = (RemotelyMovedQueueItem) item;
        File fLocalFile = item.getFile();
        log.debug("move: " + fLocalFile.getAbsolutePath() + " to " + remoteModItem.getMovedToFile().getAbsolutePath());
        try {
            if (!fLocalFile.exists()) {
                remoteModItem.setNotes("No local file/folder to move");
                log.trace("No local file/folder to move, so will do nothing");
                return;
            }
            if (remoteModItem.getMovedToFile().exists()) {
                remoteModItem.setNotes("Destination already exists, so will not move");
                log.trace("Move destination already exists, so will do nothing");
                return;
            }
            fLocalFile.renameTo(remoteModItem.getMovedToFile());
            remoteModItem.setNotes("Moved to: " + remoteModItem.getMovedToFile().getAbsolutePath());
        } finally {
            item.setCompleted(new Date());
            log.debug("completed remotely modified task");
        }
    }
}
