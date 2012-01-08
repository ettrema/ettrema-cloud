package com.ettrema.cloudsync.view;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.engine.ScanService;
import com.ettrema.backup.event.QueueItemEvent;
import com.ettrema.backup.event.RepoChangedEvent;
import com.ettrema.backup.observer.Observer;
import com.ettrema.cloudsync.ModuleFactory;
import com.ettrema.event.Event;
import com.ettrema.event.EventListener;
import com.ettrema.event.EventManager;
import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ettrema.cloudsync.ModuleFactory._;
import org.openide.LifecycleManager;

/**
 *
 * @author brad
 */
public class TrayController implements Observer<Config, Object> {

    private static final Logger log = LoggerFactory.getLogger(TrayController.class);
    private final ScanService scanService;
    private final WindowController windowController;
    private final Config config;
    private final SummaryDetails summaryDetails;
    private final TrayIcon trayIcon;
    private final Image trayIconIdle;
    private final Image trayIconUploading;
    private final Image trayIconScanning;
    private final Image trayIconOffline;
    private MenuItem openItem;
    private MenuItem openWebItem;
    private MenuItem viewFilesItem;
    private CheckboxMenuItem paused;
    private CheckboxMenuItem disableScanning;
    private MenuItem exitItem;
    private Image current;

    public TrayController(ScanService scanService, WindowController windowController, Config config, EventManager eventManager, SummaryDetails summaryDetails) {
        this.scanService = scanService;
        this.windowController = windowController;
        this.config = config;
        this.summaryDetails = summaryDetails;

        config.addObserver(this);

        TrayControllerEventListener tcel = new TrayControllerEventListener();
        eventManager.registerEventListener(tcel, QueueItemEvent.class);
        eventManager.registerEventListener(tcel, RepoChangedEvent.class);

        trayIconIdle = createImage("/com/ettrema/cloudsync/logo16x16.png", "idle");
        trayIconUploading = createImage("/com/ettrema/cloudsync/upload16x16.png", "idle");
        trayIconScanning = createImage("/com/ettrema/cloudsync/scanning16x16.png", "idle");
        trayIconOffline = createImage("/com/ettrema/cloudsync/offline16x16.png", "idle");

        trayIcon = new TrayIcon(trayIconIdle);
        trayIcon.setImageAutoSize(true);

    }

    public boolean show() {
        log.trace("show");
        if (!SystemTray.isSupported()) {
            log.trace("tray is not supported");
            return false;
        } else {
            final PopupMenu popup = new PopupMenu();


            final SystemTray tray = SystemTray.getSystemTray();

            // Create a pop-up menu components
            openItem = new MenuItem("Open ShmeGO");
            openWebItem = new MenuItem("Browse your media lounge");
            viewFilesItem = new MenuItem("View files on server");
            paused = new CheckboxMenuItem("Pause");
            disableScanning = new CheckboxMenuItem("Disable scanning");
            exitItem = new MenuItem("Exit");
            setFont(openItem, paused, exitItem, openWebItem, viewFilesItem, disableScanning);

            //Add components to pop-up menu
            popup.add(openItem);
            popup.add(openWebItem);
            popup.add(viewFilesItem);
            popup.addSeparator();
            popup.add(paused);
            popup.add(disableScanning);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error("couldnt add system tray", e);
                return false;
            }

            trayIcon.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    windowController.showMain();
                }
            });

            trayIcon.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    windowController.showMain();
                }

                public void mousePressed(MouseEvent e) {
                }

                public void mouseReleased(MouseEvent e) {
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }
            });

            openItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    windowController.showMain();
                }
            });

            openWebItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    windowController.openMediaLounge();
                }
            });

            viewFilesItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    windowController.showRemoteBrowser();
                }
            });

            paused.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    log.debug(" paused : " + paused.getState());
                    config.setPaused(paused.getState());
                }
            });

            disableScanning.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    log.info("set diabled scanning: " + disableScanning.getState());
                    scanService.setScanningDisabled(disableScanning.getState());
                }
            });

            exitItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    LifecycleManager.getDefault().exit();
                }
            });

            return true;
        }
    }

    private void setFont(MenuItem... menuItems) {
        Font font = Font.decode("Segoe UI-Plain-11");
        for (MenuItem item : menuItems) {
            item.setFont(font);
        }
    }

    protected static Image createImage(String path, String description) {
        URL imageURL = TrayController.class.getResource(path);

        if (imageURL == null) {
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

    public void onAdded(Config t, Object parent) {
    }

    public void onRemoved(Config t, Object parent, Integer indexOf) {
    }

    public void onUpdated(Config t, Object parent) {
        final boolean command = config.isPaused();
        boolean displayed = paused.getState();
        if (command != displayed) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    System.out.println("TrayController-setstate");
                    paused.setState(command);
                }
            });
        }
    }

    public void checkState() {
        //System.out.println("checkState");
        // update status
        // status: scanning, uploading, downloading, offline, otherwise null
        if (!summaryDetails.isAllOk()) {
            setOffline();
        } else if (isUploading()) {
            setUploading();
        } else if (isDownloading()) {
            setDownloading();
        } else if (isScanning()) {
            setScanning();
        } else {
            setIdle();
        }

    }

    private boolean isUploading() {
        for (Repo r : config.getAllRepos()) {
            if (!r.getQueue().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isDownloading() {
        return false; // todo
    }

    private void setOffline() {
        if (trayIcon.getImage() == trayIconOffline) {
            return;
        }
        setIcon(trayIconOffline);
        trayIcon.setToolTip("Unable to connect to the server");
    }

    private void setUploading() {
        if (trayIcon.getImage() == trayIconUploading) {
            return;
        }
        setIcon(trayIconUploading);
        trayIcon.setToolTip("Uploading file(s) to the server");
    }

    private void setDownloading() {
        if (trayIcon.getImage() == trayIconUploading) {
            return;
        }
        setIcon(trayIconUploading);
    }

    private void setIdle() {
        if (trayIcon.getImage() == trayIconIdle) {
            return;
        }
        setIcon(trayIconIdle);
        trayIcon.setToolTip("Not uploading or scanning");
    }

    private boolean isScanning() {
        return scanService.isScanning();
    }

    private boolean isPaused() {
        return summaryDetails.isPaused();
    }

    private void setScanning() {
        if (trayIcon.getImage() == trayIconScanning) {
            return;
        }
        setIcon(trayIconScanning);
        trayIcon.setToolTip("Scanning for new and updated files...");
    }

    private void setIcon(final Image image) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                System.out.println("TrayController: seticon");
                trayIcon.setImage(image);
            }
        });
    }

    private class TrayControllerEventListener implements EventListener {

        @Override
        public void onEvent(Event e) {
            checkState();
        }
    }
}
