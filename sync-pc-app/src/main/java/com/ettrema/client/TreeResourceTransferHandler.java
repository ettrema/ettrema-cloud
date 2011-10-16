package com.ettrema.client;

import com.ettrema.httpclient.Resource;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 *
 * @author mcevoyb
 */
public class TreeResourceTransferHandler extends ResourceTransferHandler {
    private static final long serialVersionUID = 1L;

    public TreeResourceTransferHandler( JTree tree ) {
        super( tree );
    }

    protected JTree tree() {
        return (JTree) this.component;
    }

    @Override
    protected TransferableResourceList selectedResources() {
        Resource r = null;
        TreePath path = tree().getSelectionPath();
        if( path != null ) {
            AbstractTreeNode selNode = (AbstractTreeNode) path.getLastPathComponent();
            if( selNode instanceof FolderNode ) {
                FolderNode fn = (FolderNode) selNode;
                return new TransferableResourceList( fn.folder );
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected boolean canPerformAction( JComponent target, Transferable transferable, int action, Point location ) {
        TreePath pathTarget = tree().getPathForLocation( location.x, location.y );
        if( pathTarget == null ) {
            tree().setSelectionPath( null );
            return ( false );
        }
        tree().setSelectionPath( pathTarget );
        if( action == DnDConstants.ACTION_COPY ) {
            AbstractTreeNode parentNode = (AbstractTreeNode) pathTarget.getLastPathComponent();
            if( parentNode instanceof Droppable ) {
                Droppable d = (Droppable) parentNode;
                return d.canPerformCopy( transferable );
            } else {
                return false;
            }
        } else if( action == DnDConstants.ACTION_MOVE ) {
            AbstractTreeNode parentNode = (AbstractTreeNode) pathTarget.getLastPathComponent();
            if( parentNode instanceof Droppable ) {
                Droppable d = (Droppable) parentNode;
                return d.canPerformMove( transferable );
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    protected boolean executeDrop( Transferable transferable, Point pt, int action ) {
        TreePath pathTarget = tree().getPathForLocation( pt.x, pt.y );
        AbstractTreeNode newParentNode = (AbstractTreeNode) pathTarget.getLastPathComponent();
        if( action == DnDConstants.ACTION_COPY ) {
            final Droppable d = (Droppable) newParentNode;
            if( d.acceptCopyDrop( transferable ) ) {
                return true;
            } else {
                return false;
            }
        } else if( action == DnDConstants.ACTION_MOVE ) {
            final Droppable d = (Droppable) newParentNode;
            if( d.acceptMoveDrop( transferable ) ) {
//        draggedNode.removeFromParent();
                return true;
            } else {
                return false;
            }
        } else {
            throw new RuntimeException( "unknown action: " + action );
        }
    }
}
