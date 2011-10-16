package com.ettrema.backup;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.engine.Engine;
import com.ettrema.backup.event.QueueItemEvent;
import com.ettrema.backup.event.RepoChangedEvent;
import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.view.SummaryDetails;
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

import static com.ettrema.backup.BackupApplication._;

/**
 *
 * @author brad
 */
public class TrayController implements Observer<Config, Object>, EventListener {

    private static final Logger log = LoggerFactory.getLogger( TrayController.class );
    private final BackupApplication app;
    private final Config config;
    private final SummaryDetails summaryDetails;
    private final TrayIcon trayIcon;
    private final Image trayIconIdle;
    private final Image trayIconUploading;
    private final Image trayIconScanning;
    private final Image trayIconOffline;
    private final Engine engine;
    private MenuItem openItem;
    private MenuItem openWebItem;
    private MenuItem viewFilesItem;
    private CheckboxMenuItem paused;
    private CheckboxMenuItem disableScanning;
    private MenuItem exitItem;
    private Image current;

    public TrayController( BackupApplication app, Config config, EventManager eventManager, SummaryDetails summaryDetails, Engine engine ) {
        this.app = app;
        this.config = config;
        this.summaryDetails = summaryDetails;
        this.engine = engine;

        config.addObserver( this );

        eventManager.registerEventListener( this, QueueItemEvent.class );
        eventManager.registerEventListener( this, RepoChangedEvent.class );

        trayIconIdle = createImage( "/logo16x16.png", "idle" );
        trayIconUploading = createImage( "/upload16x16.png", "idle" );
        trayIconScanning = createImage( "/scanning16x16.png", "idle" );
        trayIconOffline = createImage( "/offline16x16.png", "idle" );

        trayIcon = new TrayIcon( trayIconIdle );
        trayIcon.setImageAutoSize( true );

    }

    public boolean show() {
        log.trace( "show" );
        if( !SystemTray.isSupported() ) {
            log.trace( "tray is not supported" );
            return false;
        } else {
            final PopupMenu popup = new PopupMenu();


            final SystemTray tray = SystemTray.getSystemTray();

            // Create a pop-up menu components
            openItem = new MenuItem( "Open ShmeGO" );
            openWebItem = new MenuItem( "Browse your media lounge" );
            viewFilesItem = new MenuItem( "View files on server" );
            paused = new CheckboxMenuItem( "Pause" );
            disableScanning = new CheckboxMenuItem( "Disable scanning" );
            exitItem = new MenuItem( "Exit" );
            setFont( openItem, paused, exitItem, openWebItem, viewFilesItem, disableScanning );

            //Add components to pop-up menu
            popup.add( openItem );
            popup.add( openWebItem );
            popup.add( viewFilesItem );
            popup.addSeparator();
            popup.add( paused );
            popup.add( disableScanning );
            popup.addSeparator();
            popup.add( exitItem );

            trayIcon.setPopupMenu( popup );

            try {
                tray.add( trayIcon );
            } catch( AWTException e ) {
                log.error( "couldnt add system tray", e );
                return false;
            }

            trayIcon.addActionListener( new ActionListener() {

                public void actionPerformed( ActionEvent e ) {
                    app.showView();
                }
            } );

            trayIcon.addMouseListener( new MouseListener() {

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

            } );

            openItem.addActionListener( new ActionListener() {

                public void actionPerformed( ActionEvent e ) {
                    app.showView();
                }
            } );

            openWebItem.addActionListener( new ActionListener() {

                public void actionPerformed( ActionEvent e ) {
                    _( BackupApplicationView.class ).openMediaLounge();
                }
            } );

            viewFilesItem.addActionListener( new ActionListener() {

                public void actionPerformed( ActionEvent e ) {
                    _( BackupApplicationView.class ).showRemoteBrowser();
                }
            } );

            paused.addItemListener( new ItemListener() {

                public void itemStateChanged( ItemEvent e ) {
                    log.debug( " paused : " + paused.getState() );
                    config.setPaused( paused.getState() );
                }
            } );

            disableScanning.addItemListener( new ItemListener() {

                public void itemStateChanged( ItemEvent e ) {
                    log.info( "set diabled scanning: " + disableScanning.getState() );
                    engine.setScanningDisabled( disableScanning.getState() );
                }
            } );

            exitItem.addActionListener( new ActionListener() {

                public void actionPerformed( ActionEvent e ) {
                    System.exit( 0 );
                }
            } );

            return true;
        }
    }

    private void setFont( MenuItem... menuItems ) {
        Font font = Font.decode( "Segoe UI-Plain-11" );
        for( MenuItem item : menuItems ) {
            item.setFont( font );
        }
    }

    protected static Image createImage( String path, String description ) {
        URL imageURL = TrayController.class.getResource( path );

        if( imageURL == null ) {
            return null;
        } else {
            return ( new ImageIcon( imageURL, description ) ).getImage();
        }
    }

    public void onAdded( Config t, Object parent ) {
    }

    public void onRemoved( Config t, Object parent, Integer indexOf ) {
    }

    public void onUpdated( Config t, Object parent ) {
        final boolean command = config.isPaused();
        boolean displayed = paused.getState();
        if( command != displayed ) {
            SwingUtilities.invokeLater( new Runnable() {

                public void run() {
                    System.out.println("TrayController-setstate");
                    paused.setState( command );
                }
            } );
        }
    }

    public void onEvent( Event e ) {
        checkState();
    }

    public void checkState() {
        //System.out.println("checkState");
        // update status
        // status: scanning, uploading, downloading, offline, otherwise null
        if( !summaryDetails.isAllOk() ) {
            setOffline();
        } else if( isUploading() ) {
            setUploading();
        } else if( isDownloading() ) {
            setDownloading();
        } else if( isScanning() ) {
            setScanning();
        } else {
            setIdle();
        }

    }

    private boolean isUploading() {
        for( Repo r : config.getAllRepos() ) {
            if( !r.getQueue().isEmpty() ) {
                return true;
            }
        }
        return false;
    }

    private boolean isDownloading() {
        return false; // todo
    }

    private void setOffline() {
        if( trayIcon.getImage() == trayIconOffline ) return;
        setIcon( trayIconOffline );
        trayIcon.setToolTip( "Unable to connect to the server" );
    }

    private void setUploading() {
        if( trayIcon.getImage() == trayIconUploading ) return;
        setIcon( trayIconUploading );
        trayIcon.setToolTip( "Uploading file(s) to the server" );
    }

    private void setDownloading() {
        if( trayIcon.getImage() == trayIconUploading ) return;
        setIcon( trayIconUploading );
    }

    private void setIdle() {
        if( trayIcon.getImage() == trayIconIdle ) return;
        setIcon( trayIconIdle );
        trayIcon.setToolTip( "Not uploading or scanning" );
    }

    private boolean isScanning() {
        return engine.isScanning();
    }

    private boolean isPaused() {
        return summaryDetails.isPaused();
    }


    private void setScanning() {
        if( trayIcon.getImage() == trayIconScanning ) return;
        setIcon( trayIconScanning );
        trayIcon.setToolTip( "Scanning for new and updated files..." );
    }

    private void setIcon( final Image image ) {
        SwingUtilities.invokeLater( new Runnable() {

            public void run() {
                System.out.println("TrayController: seticon");
                trayIcon.setImage( image );
            }
        } );
    }

}
