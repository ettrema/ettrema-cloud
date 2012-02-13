package com.ettrema.backup.queue;

import com.ettrema.backup.config.PermanentUploadException;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.engine.StateTokenFileSyncer;
import java.io.File;
import java.util.Date;

/**
 *
 * @author brad
 */
public class RemotelyDeletedHandler implements QueueItemHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RemotelyDeletedHandler.class);

    private final StateTokenFileSyncer fileSyncer;

    public RemotelyDeletedHandler(StateTokenFileSyncer fileSyncer) {
        this.fileSyncer = fileSyncer;
    }
    
    
    
    @Override
    public boolean supports(QueueItem item) {
        return item instanceof RemotelyDeletedQueueItem;
    }

    @Override
    public boolean requiresWait(QueueItem item) {
        return false;
    }

    @Override
    public void process(Repo r, QueueItem item) throws RepoNotAvailableException, PermanentUploadException {
        item.setStarted(new Date());

        RemotelyDeletedQueueItem remoteModItem = (RemotelyDeletedQueueItem) item;
        File fLocalFile = item.getFile();
        log.debug("delete local: " + fLocalFile.getAbsolutePath());
        try {
            if (!fLocalFile.exists()) {
                remoteModItem.setNotes("No local file/folder to move");
                log.trace("No local file/folder to delete, so will do nothing");
                return;
            }
            fileSyncer.deleteLocalCrc(fLocalFile);
            RemotelyModifiedFileHandler.moveToOldVersions(fLocalFile);

            remoteModItem.setNotes("Deleted: " + fLocalFile.getAbsolutePath());
        } finally {
            item.setCompleted(new Date());
            log.debug("completed remotely modified task");
        }
    }
}
