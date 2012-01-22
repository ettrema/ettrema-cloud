package com.ettrema.backup.engine;

import java.io.File;

/**
 *
 * @author brad
 */
public interface FileSyncer {

    /**
     * Initiate a scan. This is called infrequently so is suitable for long
     * running tasks such as scanning the whole file system for changed files
     */
    void scan(ScanStatus scanStatus);

    void onFileModified(File child);

    void onFileDeleted(File child);

    void onFileMoved(String fullPathFrom, File dest);
}
