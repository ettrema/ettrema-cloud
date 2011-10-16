package com.ettrema.client;

import com.ettrema.httpclient.Resource;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;

/**
 *
 * @author mcevoyb
 */
public class TransferableRows implements Transferable {

    public static final DataFlavor NODE_FLAVOR = new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType, "Row" );
    private static DataFlavor[] flavors = new DataFlavor[]{NODE_FLAVOR};
    final List<Resource> rows;

    public TransferableRows( List<Resource> rows ) {
        this.rows = rows;
    }

    public synchronized Object getTransferData( DataFlavor flavor ) throws UnsupportedFlavorException {
        if( flavor == NODE_FLAVOR ) {
            return rows;
        } else {
            throw new UnsupportedFlavorException( flavor );
        }
    }

    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    public boolean isDataFlavorSupported( DataFlavor flavor ) {
        for( int i = 0; i < flavors.length; i++ ) {
            if( flavor.equals( flavors[i] ) ) {
                return true;
            }
        }
        return false;
    }
}
