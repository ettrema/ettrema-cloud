package com.ettrema.cloudsync.view;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Repo;
import org.openide.windows.WindowManager;

/**
 *
 * @author brad
 */
public class WindowController {

    private final AccountView accountView;
    private final BrowserController browserController;
    private final Config config;

    public WindowController(AccountView accountView, BrowserController browserController, Config config) {
        this.accountView = accountView;
        this.browserController = browserController;
        this.config = config;
    }

    public void hideMain() {
        WindowManager.getDefault().getMainWindow().setVisible(false);
    }

    public void showMain() {
        WindowManager.getDefault().getMainWindow().setVisible(true);
    }

    public void showNewAccount() {
        accountView.setVisible(true);

    }

    public void openMediaLounge() {
        String url = getMediaLoungeUrl();
        browserController.openUrl(url);
    }

    public void showRemoteBrowser() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String getMediaLoungeUrl() {
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
