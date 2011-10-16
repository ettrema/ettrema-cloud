package com.ettrema.backup.event;

import com.ettrema.event.Event;
import java.io.File;

/**
 *
 * @author brad
 */
public class ScanDirEvent implements Event {
    private final File scanDir;

    public ScanDirEvent( File scanDir ) {
        this.scanDir = scanDir;
    }

    public File getScanDir() {
        return scanDir;
    }


}
