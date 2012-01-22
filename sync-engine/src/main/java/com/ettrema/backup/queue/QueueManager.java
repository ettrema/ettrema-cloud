package com.ettrema.backup.queue;

import com.ettrema.backup.config.*;
import com.ettrema.backup.history.HistoryDao;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class QueueManager {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(QueueManager.class);
    
    public enum TransferDirection {
        UPLOAD,
        DOWNLOAD
    }
    
    private final Config config;
    private final QueueProcessor proc;
    // working fields
    private long minStableMs = 1000;

    public QueueManager(Config config, HistoryDao historyDao, List<QueueItemHandler> handlers, Configurator configurator) {
        this.config = config;
        this.proc = new QueueProcessor(handlers, historyDao, configurator);
    }

    public void checkQueues() throws InterruptedException {
        for (Job job : config.getJobs()) {
            for (Repo repo : job.getRepos()) {
                proc.processQueue(repo);
            }
        }
    }
    
    public boolean isInProgress(TransferDirection direction) {
        for (Job job : config.getJobs()) {
            for (Repo repo : job.getRepos()) {
                QueueItem current = repo.getCurrent();
                if( current != null && getDirection(current).equals(direction) ) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private TransferDirection getDirection(QueueItem current) {
        if( current instanceof RemotelyDeletedQueueItem || current instanceof RemotelyModifiedQueueItem || current instanceof RemotelyMovedQueueItem) {
            return TransferDirection.DOWNLOAD;           
        } else {
            return TransferDirection.UPLOAD;
        }
    }
    
    
    public int getQueueSize() {
        int i = 0;
        for (Job job : config.getJobs()) {
            for (Repo repo : job.getRepos()) {
                i += repo.getQueue().size();
                if( repo.getCurrent() != null ) {
                    i++;
                }
            }
        }
        return i;        
    }

    public long getMinStableMs() {
        return minStableMs;
    }

    public void setMinStableMs(long minStableMs) {
        this.minStableMs = minStableMs;
    }
    
    public List<ProgressSummary> getCurrentQueueItems() {
        List<ProgressSummary> summaryList = new ArrayList<ProgressSummary>();
        for (Job job : config.getJobs()) {
            for (Repo repo : job.getRepos()) {
                QueueItem item = repo.getCurrent();
                if (item != null) {
                    ProgressSummary progressSummary = new ProgressSummary(item.getFileName(), item.getBytesToUpload(), item.getProgressBytes());
                    summaryList.add(progressSummary);
                }
            }
        }
        return summaryList;
    }

    public class ProgressSummary {

        public final String filename;
        public final long bytesTotal;
        public final long bytesDone;

        public ProgressSummary(String filename, long bytesTotal, long bytesDone) {
            this.filename = filename;
            this.bytesTotal = bytesTotal;
            this.bytesDone = bytesDone;
        }
    }
}
