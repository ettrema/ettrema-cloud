package com.ettrema.backup.queue;

import com.ettrema.backup.config.DeleteException;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.PermanentUploadException;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.engine.FileSyncer;
import java.util.Date;

/**
 *
 * @author brad
 */
public class DeletedFileHandler implements QueueItemHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(NewFileHandler.class);

    private final FileSyncer fileSyncer;

    public DeletedFileHandler(FileSyncer fileSyncer) {
        this.fileSyncer = fileSyncer;
    }
       
    
    @Override
    public boolean supports(QueueItem item) {
        return item instanceof DeletedFileQueueItem;
    }

    @Override
    public boolean requiresWait(QueueItem item) {
        return false;
    }

    @Override
    public void process(Repo r, QueueItem item) throws RepoNotAvailableException {
        log.debug("process");
        if (item.getFile().exists()) {
            log.info("file to be deleted exists locally, so won't do delete, but better check if its updated");
            fileSyncer.onFileModified( item.getFile());
        } else {
            item.setStarted(new Date());
            try {
                r.delete(item.getFile());
            } catch (DeleteException ex) {
                log.info("Couldnt delete item: " + item.getFileName());
            } catch (RepoNotAvailableException ex) {
                throw ex;
            } catch (PermanentUploadException ex) {
                log.info("error processing, will not retry: " + item.getFileName(), ex);
            }
        }
    }
}
