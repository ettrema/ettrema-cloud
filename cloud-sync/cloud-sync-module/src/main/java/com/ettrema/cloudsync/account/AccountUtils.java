package com.ettrema.cloudsync.account;

import java.io.File;

/**
 *
 * @author bradm
 */
public class AccountUtils {

    public enum DirType {
        pictures,
        videos,
        documents,
        music       
    }
    
    public static File getDefaultLocation(DirType dirType) {
        switch(dirType) {
            case pictures:
                return defaultPicsLocation();
            case videos:
                return defaultVideosLocation();
            case documents:
                return defaultDocumentsLocation();
            case music:
                return defaultMusicLocation();
            default:
                return null;
        }
    }
    
    public static File defaultPicsLocation() {

        File fPics;

        File userHome = userHome();
        if (userHome != null) {
            fPics = new File(userHome, "Pictures");
            if (fPics.exists()) {
                return fPics;
            }
            fPics = new File(userHome, "My Pictures");
            if (fPics.exists()) {
                return fPics;
            }
        }
        File fDocs = defaultDocumentsLocation();
        if (fDocs != null) {
            fPics = new File(fDocs, "Pictures");
            if (fPics.exists()) {
                return fPics;
            }
            fPics = new File(fDocs, "My Pictures");
            if (fPics.exists()) {
                return fPics;
            }
        }

        return null;
    }

    public static File defaultVideosLocation() {
        // First check for subfolder of docs
        File fVids;

        // The look for subfolder of user home
        File userHome = userHome();
        if (userHome != null) {
            fVids = new File(userHome, "Videos");
            if (fVids.exists()) {
                return fVids;
            }
            fVids = new File(userHome, "My Videos");
            if (fVids.exists()) {
                return fVids;
            }
            fVids = new File(userHome, "Movies");
            if (fVids.exists()) {
                return fVids;
            }
        }

        File fDocs = defaultDocumentsLocation();
        if (fDocs != null) {
            fVids = new File(fDocs, "Videos");
            if (fVids.exists()) {
                return fVids;
            }
            fVids = new File(fDocs, "My Videos");
            if (fVids.exists()) {
                return fVids;
            }
        }


        return null;
    }

    public static File defaultMusicLocation() {
        // First check for subfolder of docs
        // The look for subfolder of user home
        File fPics;

        File userHome = userHome();
        if (userHome != null) {
            fPics = new File(userHome, "Music");
            if (fPics.exists()) {
                return fPics;
            }
            fPics = new File(userHome, "My Music");
            if (fPics.exists()) {
                return fPics;
            }
        }


        File fDocs = defaultDocumentsLocation();
        if (fDocs != null) {
            fPics = new File(fDocs, "Music");
            if (fPics.exists()) {
                return fPics;
            }
            fPics = new File(fDocs, "My Music");
            if (fPics.exists()) {
                return fPics;
            }
        }

        return null;
    }

    public static File defaultDocumentsLocation() {
        File userHome = userHome();
        if (userHome != null) {
            // look for a documents sub folder
            File fDocs = new File(userHome, "Documents");
            if (fDocs.exists()) {
                return fDocs;
            }
            fDocs = new File(userHome, "My Documents");
            if (fDocs.exists()) {
                return fDocs;
            }
            return userHome; // if no subfolder, assume user home is the docs folder
        } else {
            return null;
        }
    }

    public static File userHome() {
        File f = new File(System.getProperty("user.home"));
        if (f.exists()) {
            // look for a documents sub folder
            return f;
        } else {
            return null;
        }
    }
}
