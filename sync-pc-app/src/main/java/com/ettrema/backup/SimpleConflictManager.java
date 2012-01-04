package com.ettrema.backup;

import com.ettrema.backup.engine.ConflictManager;
import com.ettrema.httpclient.Resource;
import java.io.File;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author brad
 */
public class SimpleConflictManager implements ConflictManager {

    @Override
    public void onConflict(File currentLocalFile, File remoteFile) {
        JOptionPane.showMessageDialog(null, "File conflict. current local: " + currentLocalFile.getAbsolutePath());
    }

    @Override
    public List<File> getConflicts() {
        return null;
    }


    public void onTreeConflict(File l, Resource childFile) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
