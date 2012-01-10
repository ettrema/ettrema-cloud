package com.ettrema.cloudsync;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Configurator;
import com.ettrema.backup.engine.*;
import com.ettrema.backup.history.HistoryDao;
import com.ettrema.backup.history.HistoryService;
import com.ettrema.backup.queue.*;
import com.ettrema.backup.utils.PathMunger;
import com.ettrema.cloudsync.account.AccountCreator;
import com.ettrema.cloudsync.view.*;
import com.ettrema.context.RootContext;
import com.ettrema.event.EventManager;
import com.ettrema.event.EventManagerImpl;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ModuleFactory {

    private static final Logger log = LoggerFactory.getLogger(ModuleFactory.class);

    public static <T> T _(Class<T> c) {
        try {
            return get().getRootContext().get(c);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private static ModuleFactory moduleFactory;

    public static ModuleFactory get() throws Exception {
        if (moduleFactory == null) {
            moduleFactory = new ModuleFactory();
        }
        return moduleFactory;
    }
    private final RootContext context;
    private final Configurator configurator;
    private final Config config;
    private final ExclusionsService exclusionsService;
    private final StatusService statusService;
    private final ScanService scanService;
    private final StateTokenDaoImpl stateTokenDao;
    private final StateTokenFileSyncer fileSyncer;
    private final AccountCreator accountCreator;
    private final BandwidthService bandwidthService;
    private final ThrottleFactory throttleFactory;
    private final HistoryService historyService;
    private final EventManager eventManager;
    private final QueueInserter queueInserter;
    private final FileWatcher fileWatcher;
    private final QueueManager queueManager;
    private final PathMunger pathMunger;
    private final SummaryDetails summaryDetails;
//    private final mainTopComponent view;
    private final BrowserController browserController;
    private final TrayController trayController;
    private final FileChangeChecker fileChangeChecker;
    private final CrcCalculator crcCalculator;
    private final HistoryDao historyDao;
    private final ConflictManager conflictManager;
    private final TransferAuthorisationService transferAuthorisationService;
    private final List<RemoteSyncer> remoteSyncers;
    private final WindowController windowController;
    private final AccountView accountView;
    private boolean runningInSystemTray = false;

    public ModuleFactory() throws Exception {
        File configDir = new File(System.getProperty("user.home") + "/.ettrema");
        configurator = new Configurator(configDir);
        config = configurator.load();

        ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(3);

        crcCalculator = new CrcCalculator();

        File dbDir = new File(configDir, "db");
        dbDir.mkdir();
        File dbFile = new File(dbDir, "versions");
        accountView = new AccountView(null, true);
        browserController = new BrowserController();
        windowController = new WindowController(accountView, browserController, config);
        ModifiedDateFileChangeChecker modifiedDateFileChangeChecker = new ModifiedDateFileChangeChecker();
        DbInitialiser dbInit = new DbInitialiser(dbFile);
        LocalCrcDaoImpl crcDao = new LocalCrcDaoImpl(dbInit.getUseConnection(), dbInit.getDialect(), crcCalculator);
        fileChangeChecker = new LocalDbFileChangeChecker(crcDao, modifiedDateFileChangeChecker);
        pathMunger = new PathMunger();
        bandwidthService = new BandwidthService();
        throttleFactory = new ThrottleFactory(bandwidthService);
        historyService = new HistoryService();
        eventManager = new EventManagerImpl();
        historyDao = new HistoryDao(dbInit.getUseConnection(), dbInit.getDialect(), eventManager);
        accountCreator = new AccountCreator(config, windowController);
        queueInserter = new QueueInserter(eventManager);
        exclusionsService = new ExclusionsService(config);
        statusService = new StatusService(eventManager);
        stateTokenDao = new StateTokenDaoImpl(dbInit.getUseConnection(), dbInit.getDialect());
        fileSyncer = new StateTokenFileSyncer(exclusionsService, config, stateTokenDao);
        conflictManager = new SimpleConflictManager();
        transferAuthorisationService = new GuiTransferAuthorisationService(queueInserter, pathMunger, config);
        RemoteSyncer stateTokenRemoteSyncer = new StateTokenRemoteSyncer(config, transferAuthorisationService, conflictManager, crcCalculator, stateTokenDao, fileSyncer, exclusionsService);
        RemoteSyncer directFileRemoteSyncer = new DirectFileRemoteSyncer(config);
        remoteSyncers = Arrays.asList(stateTokenRemoteSyncer, directFileRemoteSyncer);
        fileWatcher = new FileWatcher(config, fileSyncer);
        RemotelyModifiedFileHandler remoteModHandler = new RemotelyModifiedFileHandler(config, crcCalculator, crcDao, conflictManager, fileChangeChecker, queueInserter, pathMunger);
        RemotelyMovedHandler remotelyMovedHandler = new RemotelyMovedHandler();
        RemotelyDeletedHandler remotelyDeletedHandler = new RemotelyDeletedHandler();
        List<QueueItemHandler> handlers = Arrays.asList(new NewFileHandler(crcCalculator, crcDao), new DeletedFileHandler(fileSyncer), new MovedHandler(), remoteModHandler, remotelyMovedHandler, remotelyDeletedHandler);
        queueManager = new QueueManager(config, eventManager, historyDao, handlers, executorService, configurator);
        scanService = new ScanService(fileSyncer, exclusionsService, config, eventManager, remoteSyncers, queueManager);

        summaryDetails = new SummaryDetails(throttleFactory, eventManager, config, bandwidthService, queueManager);

        context = new RootContext();
        context.put(config);
        context.put(pathMunger);
        context.put(bandwidthService);
        context.put(throttleFactory);
        context.put(historyService);
        context.put(eventManager);
        context.put(queueInserter);
        context.put(fileSyncer);
        context.put(fileWatcher);
        context.put(crcCalculator);

        context.put(queueManager);
        context.put(summaryDetails);
        context.put(fileChangeChecker);
//            context.put(view);
        Services.initInstance(context);

        summaryDetails.refresh();

        trayController = new TrayController(scanService, windowController, config, eventManager, summaryDetails);
        runningInSystemTray = trayController.show();

        if (config.isConfigured()) {
            if (!runningInSystemTray) {
                windowController.showMain(); // configured, so don't need wizard, but can't display in tray so show main screen
            } else {
                // this is normal path. when configured and tray icon working don't show any screens
                windowController.hideMain();
            }
        } else {
            windowController.showNewAccount();  // not configured so always show wizard
        }
    }

    public void startAll() {
        try {
            fileWatcher.start();
            scanService.start();
            scanService.scan(); // just for dev                 
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public RootContext getRootContext() {
        return context;
    }
}
