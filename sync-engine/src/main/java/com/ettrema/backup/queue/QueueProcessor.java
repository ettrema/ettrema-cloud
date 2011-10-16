package com.ettrema.backup.queue;

import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.PermanentUploadException;
import com.ettrema.backup.config.Queue;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.event.QueueProcessEvent;
import com.ettrema.backup.history.HistoryDao;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.event.EventManager;
import java.util.List;

/**
 *
 * @author brad
 */
public class QueueProcessor implements Runnable {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( QueueProcessor.class );
    private final EventManager eventManager;
    private final Job job;
    private final Repo repo;
    private final Queue queue;
    private final List<QueueItemHandler> handlers;
    private final HistoryDao historyDao;
    private boolean running = true;
    private QueueItem retry;

    public QueueProcessor( EventManager eventManager, Job job, Repo repo, Queue queue, List<QueueItemHandler> handlers, HistoryDao historyDao) {
        this.eventManager = eventManager;
        this.job = job;
        this.repo = repo;
        this.queue = queue;
        this.handlers = handlers;
        this.historyDao = historyDao;
    }

    public void run() {
        try {
            go();
        } catch( Exception e ) {
            log.info( "processing stopped: " + repo.getDescription() );
        }
    }

    public void go() throws InterruptedException {
        log.info( "go: " + repo.getDescription() );
        while( running ) {
			System.out.println("QUEUE: go");
            try {
                if( repo.isOffline() ) {
					System.out.println("QUEUE: offline");
                    // check if repo is still offline
                    if( log.isTraceEnabled() ) {
                        log.trace( "repo is offline:" + repo.getDescription() );
                    }
                    if( repo.ping() ) {
                        log.info( "repository is back online: " + repo.getDescription() );
                        repo.setOffline( false );
                    } else {
                        doSleep( 3000 );
                    }
                } else {
                    if( retry == null ) {
						System.out.println("QUEUE: take");
                        QueueItem item = queue.take();
                        EventUtils.fireQuietly( new QueueProcessEvent( item, QueueProcessEvent.Status.PROCESSING ) );
                        start( item );
                    } else {
						System.out.println("QUEUE: retry");
                        log.trace( "retry: " + retry.getFileName() );
                        try {
                            EventUtils.fireQuietly( new QueueProcessEvent( retry, QueueProcessEvent.Status.RETRY ) );
                            start( retry );
                        } finally {
                            if( retry != null ) {
                                log.info( "retry failed, abandoning queue item: " + retry.getActionDescription() + " - " + retry.getFileName() );
                            }
                            retry = null;
                        }
                    }
                }
            } catch( InterruptedException e ) {
                throw e;
            } catch( Exception e ) {
                log.error( "exception in queue processor", e );
                retry = null; // just in case poison item
            }
        }
    }

    private void start( QueueItem item ) {
        QueueItemHandler hnd = findHandler( item );
        //long timeStableMs = System.currentTimeMillis() - item.getLastModified();

        repo.setCurrent( item );
        try {
            hnd.process( repo, job, item );
            historyDao.success(item);
            EventUtils.fireQuietly( new QueueProcessEvent( item, QueueProcessEvent.Status.COMPLETED_OK ) );
        } catch( PermanentUploadException ex ) {
			log.error("Failed uploading", ex);
            historyDao.failed(item); 
            retry = null; // no retry
            EventUtils.fireQuietly( new QueueProcessEvent( item, QueueProcessEvent.Status.ERROR ) );
        } catch( RepoNotAvailableException ex ) {
			log.error("Failed uploading", ex);
            retry = item;
            repo.setOffline( true );
            EventUtils.fireQuietly( new QueueProcessEvent( item, QueueProcessEvent.Status.ERROR ) );
        } finally {
            log.trace("notify updated: " + item.getCompleted());
            queue.notifyObserversUpdated( item );
        }
    }

    private QueueItemHandler findHandler( QueueItem item ) {
        for( QueueItemHandler hnd : handlers ) {
            if( hnd.supports( item ) ) {
                return hnd;
            }
        }
        throw new RuntimeException( "No suitable handler: " + item.getClass() );

    }

    private void doSleep( int i ) throws InterruptedException {
        Thread.sleep( i );
    }
}
