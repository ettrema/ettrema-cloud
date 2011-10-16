package com.ettrema.backup;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JOptionPane;

/**
 *
 * @author brad
 */
public class BrowserController {

    private final Desktop desktop;

    public BrowserController() {
        // Before more Desktop API is used, first check
        // whether the API is supported by this particular
        // virtual machine (VM) on this particular host.
        if( Desktop.isDesktopSupported() ) {
            desktop = Desktop.getDesktop();
        } else {
            desktop = null;
        }
    }

    public void openUrl( String url ) {
        if( desktop.isSupported( Desktop.Action.BROWSE ) ) {
            URI uri = null;
            try {
                uri = new URI( url );
            } catch( URISyntaxException use ) {
                showError( "Sorry, I can't open this web address: " + url );
                return;
            }
            try {
                desktop.browse( uri );
            } catch( IOException ex ) {
                showError( "Can't open: " + url );
            }
        } else {
            showError( "Can't open: " + url );
        }
    }

    private void showError( String err ) {
        JOptionPane.showMessageDialog( App.current().getMainFrame(), err, "Error opening browser", JOptionPane.ERROR_MESSAGE );
    }
}
