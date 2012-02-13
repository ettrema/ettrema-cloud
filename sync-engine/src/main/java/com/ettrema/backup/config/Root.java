package com.ettrema.backup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a local root directory
 *
 * @author brad
 */
public class Root {

    private static final Logger log = LoggerFactory.getLogger(Root.class);
    private String fullPath;
    private List<Dir> exclusions;
    private Long scanCompleteTotalBytes;
    private Long scanInProgressBytes;
    private long addedSinceLastScanBytes;
    private transient Job job;
    /**
     * The root name to map to in repositories
     */
    private String repoName;

    public Root() {
    }

    public Root(String fullPath, String repoName) {
        this.fullPath = fullPath;
        this.exclusions = new ArrayList<Dir>();
        this.repoName = repoName;
    }

    public Long getScanCompleteTotalBytes() {
        return scanCompleteTotalBytes;
    }

    public Long getScanInProgressBytes() {
        return scanInProgressBytes;
    }

    public Long getAddedSinceLastScanBytes() {
        return addedSinceLastScanBytes;
    }

    /**
     * Add bytes detected outside of a filesystem scan. Ie detected by the
     * filesystem watcher
     *
     * @param bytes
     */
    public void addNonScanBytes(long bytes) {
        addedSinceLastScanBytes += bytes;
    }

    /**
     * Add bytes detected during a filesystem scan
     *
     * @param bytes
     */
    public void addScanBytes(long bytes) {
        if (scanInProgressBytes == null) {
            scanInProgressBytes = bytes;
        } else {
            scanInProgressBytes += bytes;
        }
    }

    /**
     * If there is no scan completed size available, use the the in progress
     * amount if available. If not available return null to indicate unknown
     *
     * If the scancomplete value is used we will add to it any new files
     * detected by the file system watcher since the last scan
     *
     * @return
     */
    public Long getTotalLocalBytes() {
        if (scanCompleteTotalBytes != null) {
//            log.trace( "scan complete bytes: " + scanCompleteTotalBytes + ", " + addedSinceLastScanBytes );
            return scanCompleteTotalBytes + addedSinceLastScanBytes;
        } else if (scanInProgressBytes != null) {
//            log.trace( "scan in progress bytes: " + scanInProgressBytes );
            return scanInProgressBytes;
        } else {
            return null;
        }
    }

    public void onScan() {
        scanInProgressBytes = 0l;
    }

    public void onScanCompletedOk() {
        scanCompleteTotalBytes = scanInProgressBytes;
        addedSinceLastScanBytes = 0l;
        job.getConfig().saveState();
    }

    /**
     * @return the fullPath
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * @param fullPath the fullPath to set
     */
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
        job.config.onChildChanged();
    }

    /**
     * @return the dirs
     */
    public List<Dir> getExclusions() {
        return exclusions;
    }

    /**
     * @param dirs the dirs to set
     */
    public void setExclusions(List<Dir> dirs) {
        this.exclusions = dirs;
        if (job != null) {
            job.config.onChildChanged();
        }
    }

    /**
     * The name of this root in the remote repository
     *
     * @return the repoName
     */
    public String getRepoName() {
        return repoName;
    }

    /**
     * @param repoName the repoName to set
     */
    public void setRepoName(String repoName) {
        this.repoName = repoName;
        job.config.onChildChanged();
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job j) {
        this.job = j;
    }

    public boolean isConfigured() {
        File f = new File(fullPath);
        return (repoName != null && repoName.length() > 0)
                && (this.fullPath != null && this.fullPath.length() > 0)
                && f.exists() && f.isDirectory();
    }

    public boolean contains(File parent) {
        return parent.getAbsolutePath().startsWith(this.getFullPath());
    }
}
