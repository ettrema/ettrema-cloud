package com.ettrema.backup.config;

/**
 *
 * @author brad
 */
public class AccountPathService {

    private final Config config;

    public AccountPathService(Config config) {
        this.config = config;
    }
        
    
    public String getMediaLoungeUrl() {
        for (Repo r : config.getAllRepos()) {
            if (r instanceof DavRepo) {
                DavRepo dr = (DavRepo) r;
                return getMediaLoungeUrl(dr);
            }
        }
        return null;
    }

    private String getMediaLoungeUrl(DavRepo dr) {
        return "http://" + dr.getHostName() + "/" + getMediaLoungePath(dr.getUser());
    }

    public String getMediaLoungePath(String user) {
        return config.getSites() + "/" + user + "/";
    }
}
