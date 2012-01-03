package com.ettrema.backup;

import com.ettrema.backup.config.Repo;
import com.ettrema.backup.engine.ThrottleFactory;
import com.ettrema.backup.account.AccountCreator;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Configurator;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.engine.CrcCalculator;
import com.ettrema.backup.engine.DbInitialiser;
import com.ettrema.backup.engine.FileChangeChecker;
import com.ettrema.backup.engine.ModifiedDateFileChangeChecker;
import com.ettrema.backup.engine.FileWatcher;
import com.ettrema.backup.engine.LocalCrcDaoImpl;
import com.ettrema.backup.engine.LocalDbFileChangeChecker;
import com.ettrema.backup.history.HistoryDao;
import com.ettrema.backup.history.HistoryService;
import com.ettrema.backup.queue.DeletedFileHandler;
import com.ettrema.backup.queue.MovedHandler;
import com.ettrema.backup.queue.NewFileHandler;
import com.ettrema.backup.queue.QueueInserter;
import com.ettrema.backup.queue.QueueManager;
import com.ettrema.backup.queue.RemotelyModifiedFileHandler;
import com.ettrema.backup.utils.PathMunger;
import com.ettrema.backup.engine.BandwidthService;
import com.ettrema.backup.engine.ConflictManager;
import com.ettrema.backup.engine.DirectComparisonFileSyncer;
import com.ettrema.backup.engine.FileSyncer;
import com.ettrema.backup.engine.Services;
import com.ettrema.backup.engine.SimpleConflictManager;
import com.ettrema.backup.engine.StatusService;
import com.ettrema.backup.engine.ExclusionsService;
import com.ettrema.backup.engine.ScanService;
import com.ettrema.backup.queue.QueueItemHandler;
import com.ettrema.backup.queue.RemotelyDeletedHandler;
import com.ettrema.backup.queue.RemotelyMovedHandler;
import com.ettrema.backup.view.SummaryDetails;
import com.ettrema.client.BrowserView;
import com.ettrema.context.RootContext;
import com.ettrema.event.EventManager;
import com.ettrema.event.EventManagerImpl;
import java.io.File;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.swing.JOptionPane;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class of the application.
 */
public class BackupApplication extends SingleFrameApplication implements Application.ExitListener {

	private static final Logger log = LoggerFactory.getLogger(BackupApplication.class);
	private static boolean isStarted;

	/**
	 * A convenient static getter for the application instance.
	 * @return the instance of DesktopApplication1
	 */
	public static BackupApplication getApplication() {
		return Application.getInstance(BackupApplication.class);
	}

	/**
	 * Main method launching the application.
	 */
	public static void main(String[] args) {
		try {
			launch(BackupApplication.class, args);
		} catch (Throwable e) {
			JOptionPane.showInputDialog("EXception: " + e.toString());
		}
	}
	private RootContext context;
	private Configurator configurator;
	private Config config;
	private ExclusionsService exclusionsService;
	private StatusService statusService;
	private ScanService scanService;
	private FileSyncer fileSyncer;
	private AccountCreator accountCreator;
	private BandwidthService bandwidthService;
	private ThrottleFactory throttleFactory;
	private HistoryService historyService;
	private EventManager eventManager;
	private QueueInserter queueHandler;
	private FileWatcher fileWatcher;
	private QueueManager queueManager;
	private PathMunger pathMunger;
	private SummaryDetails summaryDetails;
	private BrowserView browserView; // TODO: should be associated with davrepo
	private BackupApplicationView view;
	private BrowserController browserController;
	private TrayController trayController;
	private FileChangeChecker fileChangeChecker;
	private CrcCalculator crcCalculator;
	private ScreenUpdateService screenUpdateService;
	private HistoryDao historyDao;
	private ConflictManager conflictManager;
	private boolean runningInSystemTray = false;

	/**
	 * At startup create and show the main frame of the application.
	 */
	@Override
	protected void startup() {
		log.trace("startup");

		if (isStarted) {
			throw new RuntimeException("EEEK");
		}
		isStarted = true;

		addExitListener(this);
		try {
			File configDir = initConfig();

			ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(3);

			crcCalculator = new CrcCalculator();

			File dbDir = new File(configDir, "db");
			dbDir.mkdir();
			File dbFile = new File(dbDir, "versions");
			ModifiedDateFileChangeChecker modifiedDateFileChangeChecker = new ModifiedDateFileChangeChecker();
			DbInitialiser dbInit = new DbInitialiser(dbFile);
			LocalCrcDaoImpl crcDao = new LocalCrcDaoImpl(dbInit.getUseConnection(), dbInit.getDialect(), crcCalculator);
			fileChangeChecker = new LocalDbFileChangeChecker(crcDao, modifiedDateFileChangeChecker);
			browserController = new BrowserController();
			pathMunger = new PathMunger();
			bandwidthService = new BandwidthService();
			throttleFactory = new ThrottleFactory(bandwidthService);
			historyService = new HistoryService();
			eventManager = new EventManagerImpl();
			historyDao = new HistoryDao(dbInit.getUseConnection(), dbInit.getDialect(), eventManager);
			accountCreator = new AccountCreator(config);
			queueHandler = new QueueInserter(eventManager);
			exclusionsService = new ExclusionsService();
			statusService = new StatusService(eventManager);
			fileSyncer = new DirectComparisonFileSyncer(exclusionsService, queueHandler, config, eventManager, fileChangeChecker, statusService);			
			scanService = new ScanService(fileSyncer, exclusionsService, config, eventManager);
			fileWatcher = new FileWatcher(config, fileSyncer);
			conflictManager = new SimpleConflictManager();
			RemotelyModifiedFileHandler remoteModHandler = new RemotelyModifiedFileHandler(crcCalculator, crcDao, conflictManager, fileChangeChecker, queueHandler, pathMunger);
			RemotelyMovedHandler remotelyMovedHandler = new RemotelyMovedHandler();
			RemotelyDeletedHandler remotelyDeletedHandler = new RemotelyDeletedHandler();
			List<QueueItemHandler> handlers = Arrays.asList(new NewFileHandler(crcCalculator, crcDao), new DeletedFileHandler(fileSyncer), new MovedHandler(), remoteModHandler, remotelyMovedHandler, remotelyDeletedHandler);
			queueManager = new QueueManager(config, eventManager, historyDao, handlers, executorService, configurator);

			view = new BackupApplicationView(this, config, scanService, accountCreator, eventManager, queueManager, browserController, historyDao, conflictManager);
			summaryDetails = new SummaryDetails(throttleFactory, view, eventManager, config, bandwidthService, queueManager);

			initContext();

			queueManager.startThread();

			summaryDetails.refresh();

			trayController = new TrayController(scanService, this, config, eventManager, summaryDetails);
			runningInSystemTray = trayController.show();
			screenUpdateService = new ScreenUpdateService(view, trayController, config, eventManager);

			if (config.isConfigured()) {
				if (!runningInSystemTray) {
					show(view); // configured, so don't need wizard, but can't display in tray so show main screen
				} else {
					// this is normal path. when configured and tray icon working don't show any screens
				}
			} else {
				view.showNewAccount();  // not configured so always show wizard
			}

			fileWatcher.start();
			screenUpdateService.start();
			//rssWatcher.start();


		} catch (Exception e) {
			log.error("couldnt start", e);
			JOptionPane.showInputDialog("Couldnt start: " + e.toString());
			System.exit(9);
		}
	}

	public void showView() {
		show(view);
	}

	private void initContext() {
		context = new RootContext();
		context.put(config);
		context.put(pathMunger);
		context.put(bandwidthService);
		context.put(throttleFactory);
		context.put(historyService);
		context.put(eventManager);
		context.put(queueHandler);
		context.put(fileSyncer);
		context.put(fileWatcher);
		context.put(crcCalculator);

		context.put(queueManager);
		context.put(summaryDetails);
		context.put(fileChangeChecker);
		context.put(view);
		Services.initInstance(context);
	}

	/**
	 * This method is to initialize the specified window by injecting resources.
	 * Windows shown in our application come fully initialized from the GUI
	 * builder, so this additional configuration is not needed.
	 */
	@Override
	protected void configureWindow(java.awt.Window root) {
	}

	/**
	 *
	 * @return - the configuration directory
	 */
	private File initConfig() {
		File fConfigDir = new File(System.getProperty("user.home") + "/.ettrema");
		configurator = new Configurator(fConfigDir);
		config = configurator.load();
		return fConfigDir;
	}

	public Configurator getConfigurator() {
		return configurator;
	}

	public BackupApplicationView getView() {
		return (BackupApplicationView) this.getMainView();
	}

	public BrowserView getBrowser() {
		if (browserView == null) {
			log.trace("create new browser");

			browserView = new BrowserView(getFirstRepo(), this);
		}
		return browserView;
	}

	public DavRepo getFirstRepo() {
		List<Repo> repos = config.getAllRepos();
		if( repos != null ) {
			for(Repo r : repos) {
				if( r instanceof DavRepo) {
					return (DavRepo) r;
				}
			}			
		}
		return null;
	}

	public RootContext getRootContext() {
		return context;
	}

	public static <T> T _(Class<T> c) {
		if (getApplication() == null) {
			return null;
		}
		if (getApplication().getRootContext() == null) {
			return null;
		}
		return getApplication().getRootContext().get(c);
	}

	public boolean canExit(EventObject e) {
//        Object source = ( e != null ) ? e.getSource() : null;
//        Component owner = ( source instanceof Component ) ? (Component) source : null;
//        int option = JOptionPane.showConfirmDialog( owner, "Really Exit?" );
//        return option == JOptionPane.YES_OPTION;
		if (runningInSystemTray) {
			getMainFrame().setVisible(false);
			return false;
		} else {
			return true;
		}
	}

	public void willExit(EventObject event) {
	}
}
