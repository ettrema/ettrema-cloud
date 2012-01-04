package com.ettrema.backup.config;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class Configurator {

    private static final Logger log = LoggerFactory.getLogger(Configurator.class);
    private final FilePersister filePersister;
    private final File fConfigDir;
    private final File fConfigMain;
    private final File fData;

    public Configurator(File fConfigDir) {
        filePersister = new FilePersister();
        this.fConfigDir = fConfigDir;
        this.fConfigMain = new File(this.fConfigDir, "config.xml");
        this.fData = new File(fConfigDir, "data.xml");
    }

    public Config load() {
        Config config = (Config) filePersister.loadFromXml(fConfigMain);
        if (config == null) {
            config = new Config();
            config.setConfigurator(this);
        }
        initParentReferences(config);
        loadData(config);
        if (config.getThrottlePerc() == null) {
            config.setThrottlePerc(99);
        }
        return config;
    }

    private void loadData(Config config) {
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx - loaddata");
        for (Job j : config.getJobs()) {
            for (Repo r : j.getRepos()) {
                String repoStateFileName = r.getId();
                if (repoStateFileName != null && repoStateFileName.length() > 0) {
                    File fRepoState = new File(fConfigDir, repoStateFileName);
                    Object state = filePersister.loadFromXml(fRepoState);
                    r.setState(state);
                    System.out.println("  inited repo: " + r.getId() + " - " + r.getState());
                } else {
                    log.warn("Couldnt load repostory data because its id is null: " + r);
                }
                if (r.getState() == null) {
                    throw new RuntimeException("state should have been set");
                }
            }
        }
    }

    /**
     * Saves configuration and state
     * 
     * @param c 
     */
    public void save(Config c) {
        filePersister.saveToXml(c, fConfigMain);
        saveState(c);
    }

    /**
     * Saves only the state data
     */
    public void saveState(Config c) {
        for (Job j : c.getJobs()) {
            for (Repo r : j.getRepos()) {
                saveState(r);
            }
        }
    }

    public void saveState(Repo r) {
        String repoStateFileName = r.getId();
        File fRepoState = new File(fConfigDir, repoStateFileName);
        filePersister.saveToXml(r.getState(), fRepoState);

    }

    private void initParentReferences(Config config) {
        config.setConfigurator(this);
        for (Job j : config.getJobs()) {
            j.setConfig(config);
            for (Root r : j.getRoots()) {
                r.setJob(j);
            }
            for (Repo r : j.getRepos()) {
                r.setJob(j);
            }
        }
    }
}
