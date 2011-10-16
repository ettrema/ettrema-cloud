package com.ettrema.backup;

/**
 *
 * @author j2ee
 */
public class App {


    public static BackupApplication current() {
        return BackupApplication.getApplication();
    }
    

}
