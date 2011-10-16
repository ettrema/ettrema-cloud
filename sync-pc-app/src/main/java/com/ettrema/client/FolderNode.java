package com.ettrema.client;

import com.ettrema.backup.App;
import com.ettrema.httpclient.Folder;
import com.ettrema.httpclient.FolderListener;
import com.ettrema.httpclient.HttpException;
import com.ettrema.httpclient.Resource;
import com.ettrema.httpclient.ResourceListener;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.tree.MutableTreeNode;

/**
 *
 * @author mcevoyb
 */
public class FolderNode extends AbstractTreeNode implements Droppable, DeletableNode {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( FolderNode.class );
    public static final String ICON_FOLDER = "/s_folder.png";
    final Folder folder;
    FolderListener folderListener;

    public FolderNode( final AbstractTreeNode parent, Folder folder ) {
        super( parent, folder.name, false );
        if( folder == null ) throw new NullPointerException( "folder is null" );
        this.folder = folder;

        folder.addListener( new ResourceListener() {

            public void onChanged( Resource r ) {
                model().nodeChanged( FolderNode.this );
            }

            public void onDeleted( Resource r ) {
                FolderNode fn = (FolderNode) FolderNode.this.getParent().child( FolderNode.this.folder.name );
                if( fn != null ) {
                    model().removeNodeFromParent( FolderNode.this );
                }
            }

            @Override
            public String toString() {
                return "FolderNode:ResourceListener:" + FolderNode.this.toString();
            }
        } );

        children = new ArrayList<AbstractTreeNode>();
    }

    @Override
    public void delete() throws IOException {
        ResourceUtils.doDelete( App.current().getMainFrame(), folder );
    }

    private MutableTreeNode findNode( Resource r ) {
        if( children == null ) return null;
        for( AbstractTreeNode childNode : children ) {
            if( childNode instanceof FolderNode ) {
                FolderNode fn = (FolderNode) childNode;
                if( fn.folder == r ) return fn;
            }
        }
        return null;
    }

    public void select( String[] arr, int i ) {
        if( i >= arr.length ) {
            select();
        } else {
            String s = arr[i];
            FolderNode child = (FolderNode) child( s );
            if( child != null ) {
                child.select( arr, i + 1 );
            }
        }
    }

    public void select() {
        log.trace("select");
        selectThis();
        App.current().getBrowser().showDetails( createDetails() );
    }

    @Override
    public JPanel createDetails() {
        return new FolderPanel( folder );
    }

    @Override
    protected String getIconName() {
        return ICON_FOLDER;
    }

    @Override
    protected void beforeFlush() {
        try {
            this.folder.flush();
        } catch( IOException ex ) {
            Logger.getLogger( FolderNode.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    @Override
    protected void flushChildren() {
        try {
            this.folder.flush();
        } catch( IOException ex ) {
            log.error( "", ex );
        }
    }

    @Override
    protected final List<AbstractTreeNode> getChildren() {
        if( folderListener == null ) {
            try {
                folder.children();
                folderListener = new FolderNodeListener();
                folder.addListener( folderListener ); // activate folder population
            } catch( Exception ex ) {
                log.error( "", ex );
            }
        }
        return children;
    }

    @Override
    protected List<AbstractTreeNode> listChildren() {
        throw new RuntimeException( "should never get called" );
    }

    @Override
    public Enumeration children() {
        return Collections.enumeration( getChildren() );
    }

    void createNewFolder( String name ) {
        try {
            Folder newFolder = this.folder.createFolder( name );
        } catch( Exception ex ) {
            log.error( "", ex );
        }
    }

    @Override
    void updatePopupMenu( JPopupMenu popupMenu ) {
        super.updatePopupMenu( popupMenu );

        JMenuItem item = new JMenuItem( "New Folder" );
        item.addMouseListener( new NewFolderListener() );
        popupMenu.add( item );

        DownloadFolderListener.add( popupMenu, this );

    }

    class NewFolderListener extends AbstractMouseListener {

        @Override
        public void onClick() {
            String name = JOptionPane.showInputDialog( "New Folder Name" );
            if( name == null ) return;
            createNewFolder( name );
        }
    }

    public DataFlavor[] getTransferDataFlavors() {
        return null;
    }

    public boolean isDataFlavorSupported( DataFlavor flavor ) {
        return true;
    }

    public Object getTransferData( DataFlavor flavor ) throws UnsupportedFlavorException, IOException {
        return "heelo";
    }

    public boolean acceptCopyDrop( Transferable transferable ) {
        TransferableResourceList list = (TransferableResourceList) transferable;
        for( Resource r : list ) {
            try {
                r.copyTo( this.folder );
            } catch( Exception ex ) {
                log.error( "", ex );
                JOptionPane.showMessageDialog( null, "Failed to copy file", "Copy failed", JOptionPane.ERROR_MESSAGE);
                break;
            }
        }
        return true;
    }

    public boolean canPerformMove( Transferable transferable ) {
        boolean b = true; //(transferable instanceof TransferableResourceList);
        return b;
    }

    public boolean canPerformCopy( Transferable transferable ) {
        return true;
//    return (transferable instanceof TransferableResourceList);
    }

    public boolean acceptMoveDrop( Transferable transferable ) {
        try {
            TransferableResourceList list = (TransferableResourceList) transferable.getTransferData( TransferableResourceList.RESOURCE_LIST_FLAVOR );
            for( Resource r : list ) {
                try {
                    r.moveTo( this.folder );
                } catch( HttpException ex ) {
                    JOptionPane.showMessageDialog( null, "Failed to move file " + r.name, "move failed", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            return true;
        } catch( UnsupportedFlavorException ex ) {
            JOptionPane.showMessageDialog( null, "Failed to move file", "move failed", JOptionPane.ERROR_MESSAGE);
        } catch( IOException ex ) {
            JOptionPane.showMessageDialog( null, "Failed to move file", "move failed", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    class FolderNodeListener implements FolderListener {

        public void onChildAdded( Folder parent, Resource child ) {
            if( parent == FolderNode.this.folder ) {
                if( child instanceof Folder ) {
                    int num = numChildren();
                    FolderNode f = new FolderNode( FolderNode.this, (Folder) child );
                    model().insertNodeInto( f, FolderNode.this, num );
                }
            }
        }

        public void onChildRemoved( Folder parent, Resource child ) {
            if( parent == FolderNode.this.folder ) {
                MutableTreeNode node = findNode( child );
                if( node == null ) {
                    return;
                } else {
                    model().removeNodeFromParent( node );
                }
            }
        }

        @Override
        public String toString() {
            return "FolderNode:FolderListener:" + FolderNode.this.toString();
        }
    }
}
