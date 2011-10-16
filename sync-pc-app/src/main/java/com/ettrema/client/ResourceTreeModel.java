package com.ettrema.client;

import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.httpclient.HttpException;
import java.io.IOException;
import javax.swing.tree.DefaultTreeModel;

public class ResourceTreeModel extends DefaultTreeModel {

    private static final long serialVersionUID = 1L;

    public static ResourceTreeModel create( DavRepo davRepo, BrowserView frame ) throws RepoNotAvailableException, HttpException {
        HostNode root;
        try {
            root = new HostNode( frame, davRepo.host(true) );
        } catch( IOException ex ) {
            throw new RepoNotAvailableException( ex );
        }
        return new ResourceTreeModel( root );
    }
    HostNode hostNode;

    public ResourceTreeModel( HostNode root ) {
        super( root );
        this.hostNode = root;
    }

    void select( String s ) {
        hostNode.select( s );
    }
}
