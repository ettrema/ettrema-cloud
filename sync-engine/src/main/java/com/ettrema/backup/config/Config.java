package com.ettrema.backup.config;

import com.ettrema.backup.observer.AbstractObservable;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class Config extends AbstractObservable {

	private static final Logger log = LoggerFactory.getLogger(Config.class);
	private List<Job> jobs;
	private String baseDomain;
	private int port;
	private String sites;
	private transient boolean paused;
	private transient Integer throttlePerc;
	private transient Configurator configurator;

	public void save() {
		System.out.println("save, notify objservres");
		setChanged();
		configurator.save(this);
		notifyObservers();
	}

	public synchronized void saveData() {
		// todo
		setChanged();
		notifyObservers();
	}

	public String getBaseDomain() {
		if (baseDomain == null) {
			baseDomain = "test.com";
		}
		return baseDomain;
	}

	public synchronized void setBaseDomain(String baseDomain) {
		this.baseDomain = baseDomain;
	}

	public synchronized void clearJobs() {
		setChanged();
		jobs.clear();
		notifyObservers();
	}

	public synchronized List<Job> getJobs() {
		if (jobs == null) {
			jobs = new ArrayList<Job>();
		}
		return jobs;
	}

	public synchronized void setJobs(List<Job> jobs) {
		setChanged();
		this.jobs = jobs;
	}

	public boolean isPaused() {
		return paused;
	}

	public synchronized void setPaused(boolean paused) {
		setChanged();
		this.paused = paused;
		notifyObservers();
	}

	public synchronized Integer getThrottlePerc() {
		if (throttlePerc == null) {
			throttlePerc = 99;
		}
		return throttlePerc;
	}

	public synchronized void setThrottlePerc(int throttlePerc) {
		setChanged();
		this.throttlePerc = throttlePerc;
		notifyObservers();
	}

	public synchronized List<Repo> getAllRepos() {
		List<Repo> list = new ArrayList<Repo>();
		if (this.jobs != null) {
			for (Job j : this.getJobs()) {
				for (Repo r : j.getRepos()) {
					list.add(r);
				}
			}
		}
		return list;
	}

	public synchronized List<Root> getAllRoots() {
		List<Root> list = new ArrayList<Root>();
		for (Job j : this.getJobs()) {
			for (Root r : j.getRoots()) {
				list.add(r);
			}
		}
		return list;
	}

	void setConfigurator(Configurator configurator) {
		this.configurator = configurator;
	}

	void onChildChanged() {
		setChanged();
	}

	public boolean isConfigured() {
		for (Job j : jobs) {
			if (j.isConfigured()) {
				return true;
			}
		}
		return false;
	}

	public int getPort() {
		if (port == 0) {
			port = 80;
		}
		return port;
	}

	public synchronized void setPort(int port) {
		this.port = port;
	}

	public String getSites() {
		if (sites == null) {
			sites = "users";
		}
		return sites;
	}

	public String getMediaLoungeUrl() {
		for (Repo r : getAllRepos()) {
			if (r instanceof DavRepo) {
				DavRepo dr = (DavRepo) r;
				return getMediaLoungeUrl(dr);
			}
		}
		return null;
	}

	public String getMediaLoungeUrl(DavRepo dr) {
		return "http://" + dr.getHostName() + "/" + getMediaLoungePath(dr.getUser());
	}	
	
	public String getMediaLoungePath(String user) {
		return getSites() + "/" + user;
	}
}
