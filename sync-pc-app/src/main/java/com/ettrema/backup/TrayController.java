package com.ettrema.backup;

import static com.ettrema.backup.BackupApplication._;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.engine.ScanService;
import com.ettrema.backup.event.QueueItemEvent;
import com.ettrema.backup.event.QueueProcessEvent;
import com.ettrema.backup.event.RepoChangedEvent;
import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.queue.QueueManager;
import com.ettrema.backup.view.SummaryDetails;
import com.ettrema.event.Event;
import com.ettrema.event.EventListener;
import com.ettrema.event.EventManager;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class TrayController implements Observer<Config, Object> {

    private static final Logger log = LoggerFactory.getLogger(TrayController.class);
    private final QueueManager queueManager;
    private final ScanService scanService;
    private final BackupApplication app;
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

    public TrayController(QueueManager queueManager, ScanService scanService, BackupApplication app, Config config, EventManager eventManager, SummaryDetails summaryDetails) {
        this.queueManager = queueManager;
        this.scanService = scanService;
        this.app = app;
        this.config = config;
        this.summaryDetails = summaryDetails;

        config.addObserver(this);

        TrayControllerEventListener tcel = new TrayControllerEventListener();
        eventManager.registerEventListener(tcel, QueueItemEvent.class);
        eventManager.registerEventListener(tcel, RepoChangedEvent.class);
        eventManager.registerEventListener(tcel, QueueProcessEvent.class);

        trayIconIdle = createImage("/logo16x16.png", "idle");
        trayIconUploading = createImage("/upload16x16.png", "idle");
        trayIconScanning = createImage("/scanning16x16.png", "idle");
        trayIconOffline = createImage("/offline16x16.png", "idle");

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

                public void actionPerformed(ActionEvent e) {
                    app.showView();
                }
            });

            trayIcon.addMouseListener(new MouseListener() {

                public void mouseClicked(MouseEvent e) {
                    app.showView();
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

                public void actionPerformed(ActionEvent e) {
                    app.showView();
                }
            });

            openWebItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    _(BackupApplicationView.class).openMediaLounge();
                }
            });

            viewFilesItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    _(BackupApplicationView.class).showRemoteBrowser();
                }
            });

            paused.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    log.debug(" paused : " + paused.getState());
                    config.setPaused(paused.getState());
                }
            });

            disableScanning.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    log.info("set diabled scanning: " + disableScanning.getState());
                    scanService.setScanningDisabled(disableScanning.getState());
                }
            });

            exitItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
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
        // update status
        // status: scanning, uploading, downloading, offline, otherwise null
        if (!summaryDetails.isAllOk()) {
            System.out.println("checkState: offline --------------------------------");
            setOffline();
        } else if (isUploading()) {
            System.out.println("checkState: uploading --------------------------------");
            setUploading();
        } else if (isDownloading()) {
            System.out.println("checkState: downloading --------------------------------");
            setDownloading();
        } else if (isScanning()) {
            System.out.println("checkState: scanning --------------------------------");
            setScanning();
        } else {
            System.out.println("checkState: idle --------------------------------");
            setIdle();
        }

    }

    private boolean isUploading() {
        return queueManager.isInProgress(QueueManager.TransferDirection.UPLOAD);
    }

    private boolean isDownloading() {
        return queueManager.isInProgress(QueueManager.TransferDirection.DOWNLOAD);
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

            public void run() {
                System.out.println("TrayController: seticon");
                trayIcon.setImage(image);
            }
        });
    }

    private class TrayControllerEventListener implements EventListener {

        public void onEvent(Event e) {
            checkState();
        }
    }
}
