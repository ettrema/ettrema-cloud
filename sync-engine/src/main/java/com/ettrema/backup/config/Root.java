package com.ettrema.backup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ettrema.backup.engine.Engine;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.ettrema.backup.engine.Services._;

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
    private void addNonScanBytes(long bytes) {
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
     * If the scancomplete value is used we will add to it any new files detected
     * by the file system watcher since the last scan
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

    /**
     * Check to see if the given file has been updated, by comparing it
     * with the repository version
     *
     * Will enqueue the job if required
     *
     *
     * @param child
     * @param job
     * @param root
     */
    public void scanFileUpdated(File child) {
        Engine engine = _(Engine.class);
        if (!engine.isBackupable(child, this)) {
            return;
        }
        addScanBytes(child.length());
        checkFileInRepos(engine, child, true);
    }

    public void scanAgainstLocalDb(File child) {
        Engine engine = _(Engine.class);
        if (!engine.isBackupable(child, this)) {
            return;
        }
        checkFileFast(engine, child);

    }

    /**
     * Called from the file system watcher on a modified event.
     *
     * Check to see if the given file has been updated, or requires an update
     *
     * Will enqueue the job if required
     *
     * @param child
     * @param job
     * @param root
     */
    public void onFileModified(File child) {
        Engine engine = _(Engine.class);
        if (!engine.isBackupable(child, this)) {
            return;
        }
        addNonScanBytes(child.length());
        checkFileInRepos(engine, child, false);
    }

    private void checkFileInRepos(Engine engine, File child, boolean isScan) {
        for (Repo r : job.getRepos()) {
            long tm = System.currentTimeMillis();
            if (!r.isOffline()) {
                try {
                    if (!r.isExcludedFile(child, this)) {
                        FileMeta meta = getMeta(r, child, getFullPath(), getRepoName(), isScan);
                        engine.checkFileUpdated(r, child, meta, this);
                    } else {
                        log.trace("file {} is excluded from {}", child.getAbsolutePath(), r.getDescription());
                    }
                } catch (RepoNotAvailableException ex) {
                    r.setOffline(true);
                }
            }
            if (log.isTraceEnabled()) {
                tm = System.currentTimeMillis() - tm;
                log.trace("scanned file in repo: " + r.getDescription() + " in " + tm + "ms");
            }
        }
    }

    private void checkFileFast(Engine engine, File child) {
        for (Repo r : job.getRepos()) {
            if (!r.isOffline()) {
                if (!r.isExcludedFile(child, this)) {
                    engine.checkFileUpdatedFast(r, child, this);
                } else {
                    log.trace("file {} is excluded from {}", child.getAbsolutePath(), r.getDescription());
                }
            }
        }
    }

    private FileMeta getMeta(Repo r, File child, String localRootPath, String repoName, boolean isScan) throws RepoNotAvailableException {
        FileMeta meta;
        try {
            meta = r.getFileMeta(child.getAbsolutePath(), localRootPath, repoName, isScan);
        } catch (RepoNotAvailableException ex) {
            log.info("repository not available: " + r.getClass().getCanonicalName(), ex);
            log.trace("retrying..");
            try {
                meta = r.getFileMeta(child.getAbsolutePath(), localRootPath, repoName, isScan);
            } catch (RepoNotAvailableException ex1) {
                log.info("repo still not available");
                throw ex1;
            }
        }


        return meta;
    }

    public void onFileDeleted(File f) {
        Engine engine = _(Engine.class);
        engine.onFileDeleted(f, job, this);
    }

    public void onMoved(String fullPathFrom, File dest) {
        Engine engine = _(Engine.class);
        engine.onFileMoved(fullPathFrom, dest, job, this);
    }

	public boolean isConfigured() {
		File f = new File(fullPath);
		return (repoName != null && repoName.length() > 0) &&
				(this.fullPath != null && this.fullPath.length() > 0) &&
				f.exists() && f.isDirectory();
	}
}
