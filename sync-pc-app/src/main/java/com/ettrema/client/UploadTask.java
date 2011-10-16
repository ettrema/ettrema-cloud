package com.ettrema.client;

import com.ettrema.backup.App;
import com.ettrema.httpclient.Folder;
import com.ettrema.httpclient.ProgressListener;
import java.awt.Component;
import javax.swing.JOptionPane;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 *
 * @author mcevoyb
 */
public class UploadTask extends Task implements ProgressListener {

    final Component parent;
    final java.io.File[] files;
    final Folder folder;

    public UploadTask( Component parent, Application app, java.io.File[] files, Folder folder ) {
        super( app );
        this.parent = parent;
        this.files = files;
        this.folder = folder;
        this.setUserCanCancel( true );
    }

    @Override
    protected Object doInBackground() throws Exception {
        int numFiles = files.length;
        int currentFile = 1;
        for( java.io.File f : files ) {
            if( f.getName().startsWith( "." ) || f.getParentFile().getName().startsWith( "." ) ) {
                System.out.println( "not uploading: " + f.getName() );
            } else {
                try {
                    folder.upload( f, this );
                    currentFile++;
                } catch( Throwable e ) {
                    e.printStackTrace();
                    String msg = "Failed to upload: " + f.getAbsolutePath() + ". Error: " + e.getMessage();
                    if( currentFile >= numFiles ) {
                        JOptionPane.showMessageDialog( parent, msg, "Upload Failed", JOptionPane.WARNING_MESSAGE );
                    } else {
                        int res = JOptionPane.showConfirmDialog( parent, msg + ". Would you like to continue?", "Upload failed", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
                        if( res != JOptionPane.YES_OPTION ) {
                            return null;
                        }
                    }
                }
            }
        }
        App.current().getBrowser().status( "Completed uploading file(s)" );
        return null;
    }

    public void onProgress( final long bytesRead, final Long totalBytes, String fileName ) {
        App.current().getBrowser().status( "Uploading: " + fileName );
			if (totalBytes != null) {
				int percent = (int) (bytesRead * 100 / totalBytes);
				this.setProgress(percent);
			} else {
				this.setProgress(0);
			}
    }

    public void onComplete( String fileName ) {
        App.current().getBrowser().status( "Finished: " + fileName );
        this.setProgress( 100 );
    }

	public void onRead(int bytes) {
	
	}
}
