package com.ettrema.client;

import com.ettrema.httpclient.ConnectionListener;
import com.ettrema.httpclient.Host;
import com.ettrema.httpclient.HttpException;
import java.awt.Cursor;
import java.io.IOException;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * @author mcevoyb
 */
public class HostNode extends FolderNode {

    final Host host;
    final BrowserView frame;

    public HostNode( final BrowserView frame, Host host ) throws IOException, HttpException {
        super( null, host.getFolder( "" ) );
        this.frame = frame;
        this.host = host;
        host.addConnectionListener( new ConnectionListener() {

            public void onStartRequest() {
                Cursor hourglassCursor = new Cursor( Cursor.WAIT_CURSOR );
                frame.getComponent().setCursor( hourglassCursor );
            }

            public void onFinishRequest() {
                Cursor normalCursor = new Cursor( Cursor.DEFAULT_CURSOR );
                frame.getComponent().setCursor( normalCursor );
            }
        } );
    }

    @Override
    protected HostNode root() {
        return this;
    }

    @Override
    protected String getIconName() {
        return "home.png";
    }

    void select( String path ) {
        if( path.startsWith( "/" ) ) {
            path = path.substring( 1 );
        }
        if( path.endsWith( "/" ) ) {
            path = path.substring( 0, path.length() - 1 );
        }
        path = path.trim();
        if( path.length() == 0 ) {
            select();
        } else {
            String[] arr = path.split( "[/]" );
            for( String s : arr ) {
            }
            select( arr, 0 );
        }
    }

    @Override
    void updatePopupMenu( JPopupMenu popupMenu ) {
        super.updatePopupMenu( popupMenu );

        JMenuItem item = new JMenuItem( "Remove Host" );
        item.addMouseListener( new AbstractMouseListener( this ) {

            @Override
            public void onClick() {
                parent.remove( HostNode.this );
            }
        } );
        popupMenu.add( item );
    }

    @Override
    public String toString() {
        return host.server;
    }
}
