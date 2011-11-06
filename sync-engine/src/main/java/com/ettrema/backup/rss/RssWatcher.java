package com.ettrema.backup.rss;

import com.bradmcevoy.io.FileUtils;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.engine.Engine;
import com.ettrema.backup.queue.QueueInserter;
import com.ettrema.backup.utils.PathMunger;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.http.impl.cookie.DateUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

/**
 * Implements notification of remote changes by watching an RSS feed.
 *
 * @author brad
 */
public class RssWatcher {

	public static final String PATTERN_RESPONSE_HEADER = "E, dd MMM yyyy HH:mm:ss Z"; // Tue, 29 Jun 2010 10:37:14 +1200
	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RssWatcher.class);
	private final Config config;
	private final Engine engine;
	private final QueueInserter queueInserter;
	private final PathMunger pathMunger;
	private long dwellTime = 10000;
	private Thread watchThread;
	private boolean running;
	private final DateFormat df = new SimpleDateFormat(PATTERN_RESPONSE_HEADER);
	private Date latestUpdate;

	public RssWatcher(Config config, Engine engine, QueueInserter queueInserter, PathMunger pathMunger) {
		this.config = config;
		this.engine = engine;
		this.queueInserter = queueInserter;
		this.pathMunger = pathMunger;
	}

	public void start() {
		if (watchThread != null) {
			log.warn("already running");
			return;
		}
		running = true;
		watchThread = new Thread(new RssWatchRunnable(), "RssWatchThread");
		watchThread.setDaemon(true);
		watchThread.start();
	}

	public void stop() {
		running = false;
		if (watchThread != null) {
			watchThread.interrupt();
		}
		watchThread = null;
	}

	public long getDwellTime() {
		return dwellTime;
	}

	public void setDwellTime(long dwellTime) {
		this.dwellTime = dwellTime;
	}

	// need to redo this, don't know job or repo when we get the rss, have to deduce
	//if from the rss
	private void processRss(Job job, DavRepo dr, byte[] arr, Date dtRequested) throws SAXException, JDOMException, Exception {
		log.trace("processRss: " + dr.getDescription());
		ByteArrayInputStream bin = new ByteArrayInputStream(arr);
		Document doc = getJDomDocument(bin);
		Element elRss = doc.getRootElement();
		if (elRss == null) {
			throw new Exception("no rss element");
		}
		Element elChannel = elRss.getChild("channel");
		if (elChannel == null) {
			throw new Exception("no channel");
		}
		for (Object oChild : elChannel.getChildren()) {
			if (oChild instanceof Element) {
				Element elChild = (Element) oChild;
				String url = valueOf(elChild, "link");
				log.trace("got url: " + url);
				if (url != null) {
					String sPubDate = valueOf(elChild, "pubDate");
					Date pubDate = df.parse(sPubDate);
					String sLocalFile = pathMunger.findFileFromUrl(job.getRoots(), url, File.separator);
					if (sLocalFile != null) {
						log.trace("munged url to local path: " + sLocalFile);
						File localFile = new File(sLocalFile);
						if (elChild.getName().equals("item") || elChild.getName().equals("image")) {
							log.trace("is an updated file");

							queueInserter.onRemotelyUpdatedFile(dr, localFile, null);

						} else if (elChild.getName().equals("moved")) {
							String sMovedTo = valueOf(elChild, "movedTo");
							sMovedTo = pathMunger.findFileFromUrl(job.getRoots(), sMovedTo, File.separator);
							File movedTo = new File(sMovedTo);
							queueInserter.onRemotelyMoved(localFile, movedTo, job, dr);
						} else if (elChild.getName().equals("deleted")) {
							queueInserter.onRemotelyDeleted(localFile, job, dr);
						}
					} else {
						log.warn("Couldnt munge remote path: " + url);
					}
					setLatestDate(pubDate);
				}
			}
		}
		// Since we processed everything that was requested in this batch, we
		// can safely set the last date to the time the batch was requested
		setLatestDate(dtRequested);
	}

	public org.jdom.Document getJDomDocument(InputStream fin) throws JDOMException {
		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setExpandEntities(false);
			return builder.build(fin);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			FileUtils.close(fin);
		}
	}

	private String valueOf(Element el, String name) {
		Element elChild = el.getChild(name);
		if (elChild == null) {
			return null;
		} else {
			return elChild.getText();
		}
	}

	/**
	 * Record this so we will request update only after this
	 * 
	 * @param pubDate 
	 */
	private void setLatestDate(Date pubDate) {
		System.out.println("set latest date: " + pubDate);
		latestUpdate = pubDate; // TODO: persist it!!!
	}

	private class RssWatchRunnable implements Runnable {

		@Override
		public void run() {
			try {
				while (running) {
					checkRssFeeds();
					Thread.sleep(dwellTime);
				}
			} catch (InterruptedException ex) {
				log.info("finished");
			}

		}
	}

	private void checkRssFeeds() {
		for (Job j : config.getJobs()) {
			for (Repo r : j.getRepos()) {
				if (r instanceof DavRepo) {
					DavRepo dr = (DavRepo) r;
					checkFeed(j, dr);
				}
			}
		}
	}

	private void checkFeed(Job job, DavRepo dr) {
		try {
			String path = "_changelog.xml?since=" + formatDate(latestUpdate);
			System.out.println("checkFeed: " + latestUpdate + " - " + path);
			byte[] arr = dr.host().get(path);
			Date dtRequested = new Date();
			processRss(job, dr, arr, dtRequested);
		} catch (com.ettrema.httpclient.NotFoundException e) {
			// Just means no files have been uploaded yet
			log.trace("rss.xml not found, indicates that no files have been uploaded yet");
		} catch (RepoNotAvailableException ex) {
			log.trace("cant check feed, repository is offline");
		} catch (Exception e) {
			log.error("failed to check RSS for: " + dr.getHostName(), e);
		}
	}

	private String formatDate(Date latestUpdate) {
		if (latestUpdate != null) {
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
			return df.format(latestUpdate);
		} else {
			return "";
		}
	}
}
