package com.ettrema.client;

import com.ettrema.httpclient.Resource;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

public class TransferableResourceList extends ArrayList<Resource> implements Transferable {

//  static DataFlavor uriListFlavor;
//
//  static {
//    try {
//      uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
//    } catch (ClassNotFoundException e) { // can't happen
//      e.printStackTrace();
//    }
//  }  
    public static final DataFlavor RESOURCE_LIST_FLAVOR = new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType, "ResourceList" );
    private static DataFlavor[] flavors = new DataFlavor[]{RESOURCE_LIST_FLAVOR};//, DataFlavor.javaFileListFlavor, uriListFlavor};
    private static final long serialVersionUID = 1L;

    public TransferableResourceList() {
    }

    public TransferableResourceList( Resource resource ) {
        super();
        this.add( resource );
    }

    public TransferableResourceList( List<Resource> resources ) {
        super( resources );
    }

    public synchronized Object getTransferData( DataFlavor flavor ) throws UnsupportedFlavorException {
        if( flavor == RESOURCE_LIST_FLAVOR ) {
            return this;
//    } else if (flavor.equals(DataFlavor.javaFileListFlavor)) {
//      java.util.List data = new java.util.ArrayList();
//      java.io.File file = new java.io.File("/home/j2ee/Desktop/beergroup");
//      data.add(file);
//      return data;
//    } else if (flavor.equals(uriListFlavor)) {
//      java.io.File file = new java.io.File("/home/j2ee/Desktop/beergroup");
//      // refer to RFC 2483 for the text/uri-list format
//      String data = file.toURI() + "\r\n";
//      System.out.println("returning files list: " + data);
//      return data;
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
