package com.ettrema.backup.queue;

import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Queue;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.event.QueueItemEvent;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.event.EventManager;
import java.io.File;

/**
 *
 * @author brad
 */
public class QueueInserter {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(QueueInserter.class);
    private final EventManager eventManager;

    public QueueInserter(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void onRemotelyUpdatedFile(Repo repo, File child, FileMeta meta) {
        log.info("queue remotely modified: " + child.getAbsolutePath());
        RemotelyModifiedQueueItem item = new RemotelyModifiedQueueItem(child, repo, meta);
        if (repo.getQueue().contains(item)) {
            log.trace("not adding item, already in queue: " + item);
        } else {
            enqueue(repo.getQueue(), item);
        }
    }

    public void onConflict(Repo repo, String fullPath, File child, FileMeta meta) {
        log.info("queue conflicted: " + child.getAbsolutePath());
        RemotelyModifiedQueueItem item = new RemotelyModifiedQueueItem(child, repo, meta);
        item.setConflicted(true);
        if (repo.getQueue().contains(item)) {
            log.trace("not adding item, already in queue: " + item);
        } else {
            enqueue(repo.getQueue(), item);
        }
    }

    public void onNewFile(Repo repo, File file) {
        log.info("queue new file: " + file.getAbsolutePath());

        NewFileQueueItem nf = new NewFileQueueItem(file, repo);

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
                onUpdatedFile(repo, contentFile);
            } else {
                log.trace("does not exist: " + contentFile.getAbsolutePath());
            }
        } else {
            log.trace("not meta file, check to see if there is an associated meta file");
            File metaFile = new File(s + ".meta.xml");
            if( metaFile.exists()) {
                log.trace("found meta file, so enqueue: " + metaFile.getAbsolutePath());
                onUpdatedFile(repo, metaFile);
            }
        }

        enqueue(repo.getQueue(), nf);
    }

    public void onUpdatedFile(Repo repo, File file) {
        NewFileQueueItem nf = new NewFileQueueItem(file, repo);
        if (repo.getQueue().contains(nf)) {
            log.debug("queue already contains item");
            return;
        }
        nf.setUpdated(true);
        enqueue(repo.getQueue(), nf);
    }

    public void onFileDeleted(File child, Job job, Root root, Repo repo) {
        DeletedFileQueueItem item = new DeletedFileQueueItem(child, repo, root);
        if (repo.getQueue().contains(item)) {
            log.debug("queue already contains item");
            return;
        }
        enqueue(repo.getQueue(), item);
    }

    public void onMoved(String fullPathFrom, File dest, Job job, Root root, Repo repo) {
        MovedQueueItem item = new MovedQueueItem(fullPathFrom, dest, repo);
        if (repo.getQueue().contains(item)) {
            log.debug("queue already contains item");
            return;
        }
        enqueue(repo.getQueue(), item);

    }

    private void enqueue(Queue queue, QueueItem item) {
        queue.addItem(item);
        EventUtils.fireQuietly(eventManager, new QueueItemEvent(queue, item, false));
    }

	public void onRemotelyMoved(File localFile, File movedTo, Job job, Repo dr) {
		RemotelyMovedQueueItem item = new RemotelyMovedQueueItem(localFile, movedTo, dr);
		enqueue(dr.getQueue(), item);
	}

	public void onRemotelyDeleted(File localFile, Job job, DavRepo dr) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
