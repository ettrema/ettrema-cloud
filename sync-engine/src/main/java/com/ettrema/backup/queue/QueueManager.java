package com.ettrema.backup.queue;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Configurator;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.QueueItem;
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
	private final Configurator configurator;
    // working fields
    private long minStableMs = 1000;
    //private ScheduledFuture scheduledTask;
    private List<Thread> queueProcThreads = new ArrayList<Thread>();
	private List<QueueProcessor> queueProcessors = new ArrayList<QueueProcessor>();
	
	
	private boolean isRunning;

    public QueueManager( Config config, EventManager eventManager, HistoryDao historyDao, List<QueueItemHandler> handlers, ScheduledExecutorService svc, Configurator configurator ) {
        this.svc = svc;
        this.historyDao = historyDao;
        this.config = config;
        this.eventManager = eventManager;
        this.handlers = handlers;
		this.configurator = configurator;
        config.addObserver( this );
    }

    public synchronized void startThread() {
		if( isRunning ) {
			log.warn("QueueManager is already running");
			return ;
		}
        log.trace( "starting queue processing thread" );
		isRunning = true;
		queueProcThreads.clear();
		queueProcessors.clear();
        for( Job job : config.getJobs() ) {
            for( Repo repo : job.getRepos() ) {
                QueueProcessor proc = new QueueProcessor(eventManager, job, repo, repo.getQueue(), handlers, historyDao, configurator );
				queueProcessors.add(proc);
                Thread thProc = new Thread( proc, "Queue Processor: " + repo.getDescription() );
                queueProcThreads.add( thProc );
                thProc.start();
            }
        }
    }

    public synchronized void stopThread() {
        log.info( "stopping queue processors" );
        for( Thread t : queueProcThreads ) {
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
	
	public List<ProgressSummary> getCurrentQueueItems() {
		List<ProgressSummary> summaryList = new ArrayList<ProgressSummary>();
		for(QueueProcessor qp : queueProcessors) {
			QueueItem item = qp.getCurrentQueueItem();
			if( item != null ) {				
				ProgressSummary progressSummary = new ProgressSummary(item.getFileName(), item.getBytesToUpload(), item.getProgressBytes());
				summaryList.add(progressSummary);
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
