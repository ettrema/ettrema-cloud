package com.ettrema.client;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.tree.*;
import java.awt.dnd.*;
import java.awt.event.InputEvent;
import javax.swing.TransferHandler;

public abstract class ResourceTransferHandler extends TransferHandler implements Unloadable {

    protected final JComponent component;
    protected final DragSource dragSource;
    protected final DropTarget dt;

    protected abstract TransferableResourceList selectedResources();

    protected abstract boolean canPerformAction( JComponent target, Transferable transferable, int action, Point location );

    protected abstract boolean executeDrop( Transferable transferable, Point pt, int action );

    ResourceTransferHandler( JComponent component ) {
        this.component = component;
        component.setTransferHandler( this );
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer( component, DnDConstants.ACTION_COPY_OR_MOVE, new ResourceDragGestureListener() );
        dt = new DropTarget( component, new ResourceDropTargetListener() );
        component.setDropTarget( dt );
    }

    public void unload() {
        component.setTransferHandler( null );
        component.setDropTarget( null );
    }

    @Override
    public boolean canImport( JComponent comp, DataFlavor[] transferFlavors ) {
        boolean b = super.canImport( comp, transferFlavors );
        return b;
    }

    @Override
    public void exportAsDrag( JComponent comp, InputEvent e, int action ) {
        super.exportAsDrag( comp, e, action );
    }

    @Override
    public void exportToClipboard( JComponent comp, Clipboard clip, int action ) throws IllegalStateException {
        super.exportToClipboard( comp, clip, action );
    }

    @Override
    protected Transferable createTransferable( JComponent c ) {
        return selectedResources();
    }

    class ResourceDragSourceListener implements DragSourceListener {

        public void dragDropEnd( DragSourceDropEvent dsde ) {
        }

        public final void dragEnter( DragSourceDragEvent dsde ) {
            int action = dsde.getDropAction();
            if( action == DnDConstants.ACTION_COPY ) {
                dsde.getDragSourceContext().setCursor( DragSource.DefaultCopyDrop );
            } else {
                if( action == DnDConstants.ACTION_MOVE ) {
                    dsde.getDragSourceContext().setCursor( DragSource.DefaultMoveDrop );
                } else {
                    dsde.getDragSourceContext().setCursor( DragSource.DefaultMoveNoDrop );
                }
            }
        }

        public final void dragOver( DragSourceDragEvent dsde ) {
            int action = dsde.getDropAction();
            if( action == DnDConstants.ACTION_COPY ) {
                dsde.getDragSourceContext().setCursor( DragSource.DefaultCopyDrop );
            } else {
                if( action == DnDConstants.ACTION_MOVE ) {
                    dsde.getDragSourceContext().setCursor( DragSource.DefaultMoveDrop );
                } else {
                    dsde.getDragSourceContext().setCursor( DragSource.DefaultMoveNoDrop );
                }
            }
        }

        public final void dropActionChanged( DragSourceDragEvent dsde ) {
            int action = dsde.getDropAction();
            if( action == DnDConstants.ACTION_COPY ) {
                dsde.getDragSourceContext().setCursor( DragSource.DefaultCopyDrop );
            } else {
                if( action == DnDConstants.ACTION_MOVE ) {
                    dsde.getDragSourceContext().setCursor( DragSource.DefaultMoveDrop );
                } else {
                    dsde.getDragSourceContext().setCursor( DragSource.DefaultMoveNoDrop );
                }
            }
        }

        public final void dragExit( DragSourceEvent dse ) {
            dse.getDragSourceContext().setCursor( DragSource.DefaultMoveNoDrop );
        }

        public boolean executeDrop( final Transferable transferable, final Droppable droppable, final int action ) {
            if( action == DnDConstants.ACTION_COPY ) {
                return droppable.acceptCopyDrop( transferable );
            } else if( action == DnDConstants.ACTION_MOVE ) {
                return droppable.acceptMoveDrop( transferable );
            } else {
                return false;
            }
        }
    }

    class ResourceDropTargetListener implements DropTargetListener {

        public final void dragEnter( DropTargetDragEvent dtde ) {
            int action = dtde.getDropAction();
            dtde.acceptDrag( action );
        }

        public final void dragExit( DropTargetEvent dte ) {
        }

        public final void dragOver( DropTargetDragEvent dtde ) {
            Point pt = dtde.getLocation();
            int action = dtde.getDropAction();
            if( canPerformAction( component, dtde.getTransferable(), action, pt ) ) {
                dtde.acceptDrag( action );
            } else {
                dtde.rejectDrag();
            }
        }

        public final void dropActionChanged( DropTargetDragEvent dtde ) {
            Point pt = dtde.getLocation();
            int action = dtde.getDropAction();
            if( canPerformAction( component, dtde.getTransferable(), action, pt ) ) {
                dtde.acceptDrag( action );
            } else {
                dtde.rejectDrag();
            }
        }

        public final void drop( DropTargetDropEvent dtde ) {
            try {
                int action = dtde.getDropAction();
                Transferable transferable = dtde.getTransferable();
                Point pt = dtde.getLocation();
                if( executeDrop( transferable, pt, action ) ) {
                    dtde.acceptDrop( action );
                    dtde.dropComplete( true );
                    return;
                }
            } catch( Exception e ) {
                dtde.rejectDrop();
                dtde.dropComplete( false );
            }
        }
    }

    class ResourceDragGestureListener implements DragGestureListener {

        public final void dragGestureRecognized( DragGestureEvent dge ) {
            TransferableResourceList resources = selectedResources();
            if( resources != null && resources.size() > 0 ) {
                dragSource.startDrag( dge, DragSource.DefaultMoveNoDrop, resources, new ResourceDragSourceListener() );
            }
        }
    }
}
