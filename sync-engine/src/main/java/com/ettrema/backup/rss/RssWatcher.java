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
	private void processRss(Job job, DavRepo dr, byte[] arr) throws SAXException, JDOMException, Exception {
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
				String url = null;
				if (elChild.getName().equals("item")) {
					url = valueOf(elChild, "link");
				} else if (elChild.getName().equals("image")) {
					url = valueOf(elChild, "url");
				}

				if (url != null) {
					log.trace("got url: " + url);
					String sPubDate = valueOf(elChild, "pubDate");
					Date pubDate = df.parse(sPubDate);
					if (latestUpdate == null || pubDate.after(latestUpdate)) {
						System.out.println("pubDate is after latestDate");
						latestUpdate = pubDate;
						String sLocalFile = pathMunger.findFileFromUrl(job.getRoots(), url, File.separator);
						File localFile = new File(sLocalFile);
						queueInserter.onRemotelyUpdatedFile(dr, localFile, null);
					} else {
						System.out.println("date is not after latest local");
					}
				}
			}
		}
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
			byte[] arr = dr.host().get("Recent/rss.xml");
			processRss(job, dr, arr);
		} catch (com.ettrema.httpclient.NotFoundException e) {
			// Just means no files have been uploaded yet
			log.trace("rss.xml not found, indicates that no files have been uploaded yet");
		} catch (RepoNotAvailableException ex) {
			log.trace("cant check feed, repository is offline");
		} catch (Exception e) {
			log.error("failed to check RSS for: " + dr.getHostName(), e);
		}
	}
}
