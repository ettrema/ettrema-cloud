package com.ettrema.backup.view;

import com.ettrema.backup.engine.BandwidthService;
import com.ettrema.backup.BackupApplicationView;
import com.ettrema.backup.engine.ThrottleFactory;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.event.RepoChangedEvent;
import com.ettrema.backup.event.QueueItemEvent;
import com.ettrema.backup.event.QueueProcessEvent;
import com.ettrema.backup.event.RootChangedEvent;
import com.ettrema.backup.event.ScanDirEvent;
import com.ettrema.backup.event.ScanEvent;
import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.utils.TimeUtils;
import com.ettrema.event.Event;
import com.ettrema.event.EventListener;
import com.ettrema.event.EventManager;
import com.ettrema.httpclient.ProgressListener;
import java.io.File;
import java.util.Date;

/**
 * Listens to events and progress notifications, to provide simpler
 * UI event data to the UI
 *
 * @author brad
 */
public class SummaryDetails implements EventListener, ProgressListener {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SummaryDetails.class);
	// Service fields
	private final Config config;
	private final BandwidthService bandwidthService;
	private final BackupApplicationView view;
	private final ThrottleFactory throttleFactory;
	// data fields
	private String timeRemaining; // the calculated time remaining
	private String progress; // total bytes in the remote backup locations
	private String usage; // total bytes in the local backup locations
	private String currentFileName;
	private int currentFilePerc; // the bytes of outstanding queue items;
	private boolean allOk;
	private String problemDescription;
	private Date lastProgress;

	public SummaryDetails(ThrottleFactory throttleFactory, BackupApplicationView view, EventManager eventManager, Config config, BandwidthService bandwidthService) {
		this.throttleFactory = throttleFactory;
		this.view = view;
		this.bandwidthService = bandwidthService;
		this.config = config;
		eventManager.registerEventListener(this, QueueItemEvent.class);
		eventManager.registerEventListener(this, QueueProcessEvent.class);
		eventManager.registerEventListener(this, ScanEvent.class);
		eventManager.registerEventListener(this, ScanDirEvent.class);
		eventManager.registerEventListener(this, RootChangedEvent.class);
		eventManager.registerEventListener(this, RepoChangedEvent.class);
		config.addObserver(new Observer() {

			public void onAdded(Object t, Object parent) {
				refresh();
			}

			public void onRemoved(Object t, Object parent, Integer indexOf) {
				refresh();
			}

			public void onUpdated(Object t, Object parent) {
				refresh();
			}
		});

	}

	public String getBandWidthFormatted() {
		int bytesPerSec = bandwidthService.getBytesPerSec();
		return TimeUtils.formatBandwidth(bytesPerSec);
	}

	public void onEvent(Event e) {
		refresh();
	}

	public boolean isPaused() {
		return throttleFactory.isPaused();
	}

	public void refresh() {
		long remainingBytes = getQueuedBytes();
		if (remainingBytes == 0) {
			timeRemaining = "All done";
		} else {
			int bw = bandwidthService.getBytesPerSec();
			if (bw > 0) {
				long timeRemainingSecs = remainingBytes / bw;
				timeRemaining = TimeUtils.formatSecsAsTime(timeRemainingSecs);

			} else {
				timeRemaining = "Unknown";
			}
		}

		progress = formatOverallProgress(getDavBytesBackedup(), getLocalTotalBytes(), getQueuedBytes());
		usage = formatUsage(getAccountUsedBytes(), getDavMaxBytes());
		calcNewStatus();
	}

	/**
	 * Called by the HTTP client when uploading
	 * 
	 * @param percent
	 * @param fileName
	 * @param bytesPerSec
	 */
	public void onProgress(final long bytesRead, final Long totalBytes, String fileName) {
		System.out.println("SummaryDetails: onProgress: " + fileName + " bytesRead: " + bytesRead);
		fileName = nameOnly(fileName);
		this.currentFileName = fileName;
		this.lastProgress = new Date();		
		if (totalBytes != null) {
			int percent = (int) (bytesRead * 100 / totalBytes);
			this.currentFilePerc = percent;
		} else {
			this.currentFilePerc = 0;
		}
	}

	/**
	 * Called on the progress listener by the HTTP client after upload has completed
	 *
	 * @param fileName
	 */
	public void onComplete(String fileName) {
		this.currentFileName = "";
		this.currentFilePerc = 0;
		this.lastProgress = new Date();
	}

	public boolean isCancelled() {
		return false;
	}

	private String nameOnly(String fileName) {
		int pos = fileName.lastIndexOf(File.separator);
		if (pos > 0) {
			return fileName.substring(pos + 1);
		} else {
			return fileName;
		}
	}

	private String formatUsage(Long backedUp, Long max) {
		return formatPercentage(backedUp, max);
	}

	private String formatPercentage(Long backedUp, Long max) {
		if (backedUp == null || max == null || max == 0) {
			return "Unknown";
		} else {
			return (backedUp * 100) / max + "%";
		}
	}

	private String formatOverallProgress(Long remaining, Long total, long queuedBytes) {
		if (this.throttleFactory.isPaused()) {
			if (queuedBytes > 0) {
				return "Paused, " + TimeUtils.formatBytes(queuedBytes) + " queued";
			} else {
				return "Paused";
			}
		} else {
			if (queuedBytes > 0) {
				return "Uploading: " + TimeUtils.formatBytes(queuedBytes) + " at " + getBandWidthFormatted();
			} else {
				if (total == null) {
					return "Scanning...";
				} else {
					return " Backed up " + TimeUtils.formatBytes(total);
				}
			}
		}
	}

	/**
	 * Total bytes currently in the queue for all jobs and all repos
	 * @return
	 */
	private long getQueuedBytes() {
		long n = 0;
		for (Repo r : config.getAllRepos()) {
			n += r.getQueue().getRemainingBytes();
		}
		return n;
	}

	/**
	 * Total bytes permitted in DAV accounts
	 *
	 * @return
	 */
	private long getDavMaxBytes() {
		long n = 0;
		for (Repo r : config.getAllRepos()) {
			if (r instanceof DavRepo) {
				if (r.getMaxBytes() != null) {
					n += r.getMaxBytes();
				} else {
					//log.warn( "no account max info" );
				}
			}
		}
		return n;
	}

	private Long getAccountUsedBytes() {
		Long n = null;
		for (Repo r : config.getAllRepos()) {
			if (r instanceof DavRepo) {
				DavRepo dr = (DavRepo) r;
				if (dr.getMaxBytes() != null) {
					if (n == null) {
						n = 0l;
					}
					n += dr.getAccountUsedBytes();
				} else {
					//log.warn( "no account max info" );
				}
			}
		}
		return n;
	}

	private void calcNewStatus() {
		if (throttleFactory.isPaused()) {
			allOk = false;
			problemDescription = "The backup is paused";
			return;
		}
		for (Repo r : config.getAllRepos()) {
			if (r instanceof DavRepo) {
				DavRepo dr = (DavRepo) r;
				if (dr.isOffline()) {
					log.info("first dav repo is offline");
					allOk = false;
					problemDescription = "Not connected to the backup website";
					return;
				}
			}
		}

		// todo: other checks
		allOk = true;

	}

	/**
	 * Total bytes in all backup jobs to backup
	 * @return
	 */
	private Long getLocalTotalBytes() {
		Long n = null;
		for (Root r : config.getAllRoots()) {
			Long rootBytes = r.getTotalLocalBytes();
			if (rootBytes != null) {
				if (n == null) {
					n = rootBytes;
				} else {
					n += rootBytes;
				}
			}
		}
		return n;
	}

	/**
	 * total bytes backed up to DAV repositories
	 * @return
	 */
	private Long getDavBytesBackedup() {
		Long n = null;
		for (Repo r : config.getAllRepos()) {
			if (r instanceof DavRepo) {
				Long l = r.getBackedUpBytes();
				if (l != null) {
					if (n == null) {
						n = l;
					} else {
						n += l;
					}
				}
			}
		}
		return n;
	}

	public String getCurrentFileName() {
		return currentFileName;
	}

	public int getCurrentFilePerc() {
		return currentFilePerc;
	}

	public String getProgress() {
		return progress;
	}

	public String getUsage() {
		return usage;
	}

	public String getTimeRemaining() {
		return timeRemaining;
	}

	public boolean isAllOk() {
		return allOk;
	}

	public String getProblemDescription() {
		return problemDescription;
	}

	public Date getLastProgress() {
		return lastProgress;
	}

	public void onRead(int bytes) {

	}
}
