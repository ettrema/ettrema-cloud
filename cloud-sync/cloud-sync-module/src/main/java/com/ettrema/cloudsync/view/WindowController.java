package com.ettrema.cloudsync.view;

import com.ettrema.backup.config.AccountPathService;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.engine.ScanService;
import com.ettrema.cloudsync.account.AccountCreator;
import org.openide.windows.WindowManager;

/**
 *
 * @author brad
 */
public class WindowController {

    private final BrowserController browserController;
    private final Config config;
    private final ScanService scanService;
    private final AccountCreator accountCreator;
    private final AccountPathService accountPathService;

    public WindowController(ScanService scanService, BrowserController browserController, Config config, AccountCreator accountCreator, AccountPathService accountPathService) {
        this.scanService = scanService;
        this.browserController = browserController;
        this.config = config;
        this.accountCreator = accountCreator;
        this.accountPathService = accountPathService;                
    }

    public void hideMain() {
        WindowManager.getDefault().getMainWindow().setVisible(false);
    }

    public void showMain() {
        WindowManager.getDefault().getMainWindow().setVisible(true);
    }

    public void showNewAccount() {        
        AccountView accountView = new AccountView(scanService, config, accountCreator, null, accountPathService);
        accountView.setVisible(true);
    }

    public void openMediaLounge() {
        String url = accountPathService.getMediaLoungeUrl();
        browserController.openUrl(url);
    }

    public void showRemoteBrowser() {
        throw new UnsupportedOperationException("Not yet implemented");
    }   
}
