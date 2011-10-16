package com.ettrema.backup;

import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.Repo;
import com.ettrema.common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ScreenUpdateService implements Service {

    private static final Logger log = LoggerFactory.getLogger( ScreenUpdateService.class );
    private final BackupApplicationView view;
    private final TrayController trayController;
    private final Config config;
    private Thread thUpdateScreen;
    private boolean running;
    private int counter;

    public ScreenUpdateService( BackupApplicationView view, TrayController trayController, Config config ) {
        this.view = view;
        this.trayController = trayController;
        this.config = config;
    }

    public void start() {
        log.info( "starting screen update service" );
        running = true;
        thUpdateScreen = new Thread( new Runnable() {

            public void run() {
                while( running ) {
                    doChecks();
                    try {
                        Thread.sleep( 3000 );
                    } catch( InterruptedException ex ) {
                        log.info( "refresh thread interrupted" );
                        break;
                    } catch(Exception e) {
                        log.error("Caught unexpected exception in screen update service", e);
                        log.info("continuing despite error...");
                    }

                }
            }
        } );
        thUpdateScreen.setName( "Online status checker" );
        thUpdateScreen.setDaemon( true );
        thUpdateScreen.start();
    }

    public void stop() {
        running = false;
        if( thUpdateScreen != null ) {
            thUpdateScreen.interrupt();
        }
    }

    private void doChecks() {
        view.doUpdateScreen();
        trayController.checkState();
        boolean doOnlineChecks = false;
        if( counter++ > 2 ) {
            counter = 0;
            doOnlineChecks = true;
        }
        if( config.isPaused() ) {
            return ;
        }
        for( Repo r : config.getAllRepos() ) {
            if( r.isOffline() ) {
                if( r.ping() ) {
                    log.info( "repository is online: " + r.getDescription() );
                    r.setOffline( false );
                }
            } else if( doOnlineChecks ) {  // todo: increase to reduce traffic
                if( !r.ping() ) {
                    log.info( "repository is OFFline: " + r.getDescription() );
                    r.setOffline( true );
                }

            }
        }
    }

    public static String formatBytes( Long n ) {
        if( n == null ) {
            return "Unknown";
        } else if( n < 1000 ) {
            return n + " bytes";
        } else if( n < 1000000 ) {
            return n / 1000 + "KB";
        } else if( n < 1000000000 ) {
            return n / 1000000 + "MB";
        } else {
            return n / 1000000000 + "GB";
        }
    }
}
