package com.ettrema.backup.queue;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.history.HistoryDao;
import com.ettrema.backup.observer.Observer;
import com.ettrema.event.EventManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 *
 * @author brad
 */
public class QueueManager implements Observer {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( QueueManager.class );
    private final Config config;
    private final List<QueueItemHandler> handlers;
    private final EventManager eventManager;
    private final HistoryDao historyDao;
    private final ScheduledExecutorService svc;
    // working fields
    private long minStableMs = 1000;
    //private ScheduledFuture scheduledTask;
    private List<Thread> queueProcessors = new ArrayList<Thread>();
	
	private boolean isRunning;

    public QueueManager( Config config, EventManager eventManager, HistoryDao historyDao, List<QueueItemHandler> handlers, ScheduledExecutorService svc ) {
        this.svc = svc;
        this.historyDao = historyDao;
        this.config = config;
        this.eventManager = eventManager;
        this.handlers = handlers;
        config.addObserver( this );
    }

    public synchronized void startThread() {
		if( isRunning ) {
			log.warn("QueueManager is already running");
			return ;
		}
        log.trace( "starting queue processing thread" );
		isRunning = true;
        for( Job job : config.getJobs() ) {
            for( Repo repo : job.getRepos() ) {
                QueueProcessor proc = new QueueProcessor(eventManager, job, repo, repo.getQueue(), handlers, historyDao );
                Thread thProc = new Thread( proc, "Queue Processor: " + repo.getDescription() );
                queueProcessors.add( thProc );
                thProc.start();
            }
        }
    }

    public synchronized void stopThread() {
        log.info( "stopping queue processors" );
        for( Thread t : queueProcessors ) {
            t.interrupt();
        }
		isRunning = false;
    }

    public long getMinStableMs() {
        return minStableMs;
    }

    public void setMinStableMs( long minStableMs ) {
        this.minStableMs = minStableMs;
    }

    public boolean isPaused() {
        return config.isPaused();
    }

    public void setPaused( boolean newPauseState ) {
		boolean oldPauseState = !isRunning;
        if( newPauseState != oldPauseState ) {
            log.trace( "setPaused : " + newPauseState );
            if( newPauseState ) {
                stopThread();
            } else {
                startThread();
            }
        }
    }

	@Override
    public void onAdded( Object t, Object parent ) {
    }

	@Override
    public void onRemoved( Object t, Object parent, Integer indexOf ) {
    }

	@Override
    public void onUpdated( Object t, Object parent ) {
        if( t instanceof Config ) {
            setPaused( config.isPaused() );
        }
    }
}
