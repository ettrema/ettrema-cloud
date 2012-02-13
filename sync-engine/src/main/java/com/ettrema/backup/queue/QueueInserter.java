package com.ettrema.backup.queue;

import com.ettrema.backup.config.*;
import com.ettrema.backup.event.QueueItemEvent;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.common.LogUtils;
import com.ettrema.event.EventManager;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class QueueInserter {

    private static final Logger log = LoggerFactory.getLogger(QueueInserter.class);
    private final EventManager eventManager;

    public QueueInserter(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void enqueueDownload(Repo repo, File child, Long bytesToDownload) {
        log.info("queue remotely modified: " + child.getAbsolutePath());
        RemotelyModifiedQueueItem item = new RemotelyModifiedQueueItem(child, bytesToDownload);
        if (repo.getQueue().contains(item)) {
            log.trace("not adding item, already in queue: " + item);
        } else {
            enqueue(repo.getQueue(), item);
        }
    }

    public void onConflict(Repo repo, String fullPath, File child, FileMeta meta) {
        log.info("queue conflicted: " + child.getAbsolutePath());
        RemotelyModifiedQueueItem item = new RemotelyModifiedQueueItem(child, meta.getLength());
        item.setConflicted(true);
        if (repo.getQueue().contains(item)) {
            log.trace("not adding item, already in queue: " + item);
        } else {
            enqueue(repo.getQueue(), item);
        }
    }

    public void enqueueUpload(Repo repo, File file) {
        log.info("queue new file: " + file.getAbsolutePath());

        NewFileQueueItem nf = new NewFileQueueItem(file);

        if (repo.getQueue().contains(nf)) {
            log.debug("queue already contains item");
            return;
        } else {
//            log.debug( "adding item with hash: " + nf.hashCode() );
        }

        String s = file.getAbsolutePath();
        if (file.getName().endsWith(".meta.xml")) {
            log.trace("is meta file, so look for content file");
            s = s.substring(0, s.length() - ".meta.xml".length());
            File contentFile = new File(s);
            if (contentFile.exists()) {
                log.trace("content file exists, so enqueue: " + contentFile.getAbsolutePath());
                _enqueueUpload(repo, contentFile);
            } else {
                log.trace("does not exist: " + contentFile.getAbsolutePath());
            }
        } else {
            log.trace("not meta file, check to see if there is an associated meta file");
            File metaFile = new File(s + ".meta.xml");
            if (metaFile.exists()) {
                log.trace("found meta file, so enqueue: " + metaFile.getAbsolutePath());
                _enqueueUpload(repo, metaFile);
            }
        }

        enqueue(repo.getQueue(), nf);
    }

    private void _enqueueUpload(Repo repo, File file) {
        NewFileQueueItem nf = new NewFileQueueItem(file);
        if (repo.getQueue().contains(nf)) {
            log.debug("queue already contains item");
            return;
        }
        nf.setUpdated(true);
        enqueue(repo.getQueue(), nf);
    }

    public void enqueueRemoteDelete(File child, Repo repo) {
        DeletedFileQueueItem item = new DeletedFileQueueItem(child);
        if (repo.getQueue().contains(item)) {
            log.debug("queue already contains item");
            return;
        }
        enqueue(repo.getQueue(), item);
    }

    public void enqueueRemoteMove(String fullPathFrom, File dest, Job job, Root root, Repo repo) {
        MovedQueueItem item = new MovedQueueItem(fullPathFrom, dest);
        if (repo.getQueue().contains(item)) {
            log.debug("queue already contains item");
            return;
        }
        enqueue(repo.getQueue(), item);

    }

    private void enqueue(Queue queue, QueueItem item) {
        queue.addItem(item);
        LogUtils.trace(log, "enqueue: size:", queue.size());
        EventUtils.fireQuietly(eventManager, new QueueItemEvent(queue, item, false));
    }

    public void enqueueLocalMove(File localFile, File movedTo, Job job, Repo dr) {
        RemotelyMovedQueueItem item = new RemotelyMovedQueueItem(localFile, movedTo, dr);
        enqueue(dr.getQueue(), item);
    }

    public void enqueueLocalDelete(File localFile, Repo dr) {
        RemotelyDeletedQueueItem deletedQueueItem = new RemotelyDeletedQueueItem(localFile);
        enqueue(dr.getQueue(), deletedQueueItem);
        
    }
}
