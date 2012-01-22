package com.ettrema.backup.queue;

import com.ettrema.backup.config.*;
import com.ettrema.backup.engine.ScanService;
import com.ettrema.backup.event.QueueProcessEvent;
import com.ettrema.backup.history.HistoryDao;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.common.LogUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class QueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(QueueProcessor.class);
    private final List<QueueItemHandler> handlers;
    private final HistoryDao historyDao;
    private final Configurator configurator;
    private QueueItem retry;

    public QueueProcessor(List<QueueItemHandler> handlers, HistoryDao historyDao, Configurator configurator) {
        this.handlers = handlers;
        this.historyDao = historyDao;
        this.configurator = configurator;
    }

    public void processQueue(Repo repo) throws InterruptedException {
        Queue queue = repo.getQueue();
        while (!queue.isEmpty()) {
            try {
                if (repo.isOffline()) {
                    // check if repo is still offline
                    LogUtils.trace(log, "processQueue: repo is offline: ", repo.getDescription());
                } else {
                    if (retry == null) {
                        QueueItem item = queue.take();
                        if (item != null) {
                            LogUtils.trace(log, "processQueue: process new item for repo: ", repo.getDescription(), "queue item:", item);                            
                            start(item, repo);
                        } else {
                            LogUtils.trace(log, "processQueue: empty queue for repo: ", repo.getDescription());
                        }
                    } else {
                        LogUtils.trace(log, "processQueue: retrying: ", retry.getFileName() + " - " + retry.getActionDescription());
                        try {
                            EventUtils.fireQuietly(new QueueProcessEvent(retry, QueueProcessEvent.Status.RETRY, repo));
                            start(retry, repo);
                        } finally {
                            if (retry != null) {
                                log.info("processQueue: retry failed, abandoning queue item: " + retry.getActionDescription() + " - " + retry.getFileName());
                            }
                            retry = null;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("exception in queue processor", e);
                retry = null; // just in case poison item
            }
        }
    }

    private void start(QueueItem item, Repo repo) {
        QueueItemHandler hnd = findHandler(item);
        //long timeStableMs = System.currentTimeMillis() - item.getLastModified();
        LogUtils.trace(log, "queue processing started", item, item.getFile().getAbsolutePath());
        repo.setCurrent(item);
        EventUtils.fireQuietly(new QueueProcessEvent(item, QueueProcessEvent.Status.PROCESSING, repo));
        try {
            hnd.process(repo, item);
            historyDao.success(item, repo);
            repo.setCurrent(null);
            EventUtils.fireQuietly(new QueueProcessEvent(item, QueueProcessEvent.Status.COMPLETED_OK, repo));
            configurator.saveState(repo);
        } catch (PermanentUploadException ex) {
            log.error("Failed uploading", ex);
            historyDao.failed(item, repo);
            retry = null; // no retry
            repo.setCurrent(null);
            EventUtils.fireQuietly(new QueueProcessEvent(item, QueueProcessEvent.Status.ERROR, repo));
        } catch (RepoNotAvailableException ex) {
            log.error("Failed uploading", ex);
            retry = item;
            repo.setOffline(true);
            repo.setCurrent(null);
            EventUtils.fireQuietly(new QueueProcessEvent(item, QueueProcessEvent.Status.ERROR, repo));
        } finally {            
            LogUtils.trace(log, "queue processing completed", item, item.getNotes());
            repo.getQueue().notifyObserversUpdated(item);
        }
    }

    private QueueItemHandler findHandler(QueueItem item) {
        for (QueueItemHandler hnd : handlers) {
            if (hnd.supports(item)) {
                return hnd;
            }
        }
        throw new RuntimeException("No suitable handler: " + item.getClass());
    }
}
