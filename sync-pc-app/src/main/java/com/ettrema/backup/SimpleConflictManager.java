package com.ettrema.backup;

import com.ettrema.backup.engine.ConflictManager;
import java.io.File;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author brad
 */
public class SimpleConflictManager implements ConflictManager{

    @Override
    public void onConflict(File currentLocalFile, File remoteFile) {
        JOptionPane.showMessageDialog(null, "File conflict. current local: " + currentLocalFile.getAbsolutePath());
    }

	@Override
	public List<File> getConflicts() {
		return null;
	}

	@Override
	public void onTreeConflict(File l, com.ettrema.httpclient.File childFile) {
		throw new UnsupportedOperationException("Not supported yet.");
	}		    
}
