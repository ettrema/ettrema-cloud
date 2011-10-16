package com.ettrema.backup.engine;

import java.io.File;
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
    
}
