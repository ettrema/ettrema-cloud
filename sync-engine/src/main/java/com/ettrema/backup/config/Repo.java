package com.ettrema.backup.config;

import com.ettrema.common.Withee;
import com.ettrema.httpclient.ProgressListener;
import com.ettrema.httpclient.Utils.CancelledException;
import java.io.File;
import java.util.List;

/**
 * Backup Repository
 *
 * @author brad
 */
public interface Repo {

    /**
     * Total bytes which have been backed up to this repository which correspond
     * to local files
     *
     * @return
     */
    Long getBackedUpBytes();


    /**
     * Will be set by the engine
     * @param b
     */
    void setOffline( boolean b );

    boolean isOffline();


    /**
     * Get the current queue of items for this repository
     *
     * @return
     */
    Queue getQueue();


    /**
     * Called just before a new scan
     */
    void onScan();

    void onScanComplete();

    /**
     * Get meta information about a file.
     *
     * @param filePath - the full local path of the resource in question
     * @param localRootPath - the full local path of the root currently being processed
     * @param repoName - the remote name of the root
     * @param isScan - if this is being called from a scan. This means that a given file will only
     * be requested once, allowed the repo to accumulate the file size. A repository might also
	 * choose to cache information loaded during a scan, but to re-validate cached data if not in a scan
     * @return - null if the file doesnt exist in this repository
     */
    FileMeta getFileMeta( String filePath, String localRootPath, String repoName, boolean isScan ) throws RepoNotAvailableException;


	/**
	 * Use the file meta in a transaction like block. The repository will, if possible,
	 * lock the resource so it cannot be changed during this block.
	 * 
	 * @param filePath
	 * @param localRootPath
	 * @param repoName
	 * @param withee
	 * @throws RepoNotAvailableException 
	 */
	void withFileMeta( String filePath, String localRootPath, String repoName, Withee<FileMeta> withee ) throws RepoNotAvailableException;
	
    /**
     * Get a list of resources in the given repository folder path.
     * 
     * The folder path includes the repoName
     * 
     * @param folderPath
     * @param repoName
     * @return - a list of the child resources, if any, in the folderPath in this repository
     */
    List<FileMeta> listFileMeta(String folderPath) throws RepoNotAvailableException;

    /**
     * Upload the given file
     *
     * @param file
     * @param status
     * @throws UploadException - for a transient error processing the file. Should be retried
     * @throws RepoNotAvailableException - the repository is temporarily unavailable
     * @throws PermanentUploadException - an exception has occurred which cannot be explicitly handler. Do not retry
     *
     */
    void upload(File file, Job job, QueueItem item ) throws UploadException, RepoNotAvailableException, PermanentUploadException;

    /**
     *
     * @param dest - the physical file to download to. Will be a temp file
     * @param localTarget - the local copy of the file to download. This identifies the repository file to download
     * @param job
     * @param listener
     * @throws UploadException
     * @throws RepoNotAvailableException
     * @throws PermanentUploadException
     * @throws CancelledException - if the user cancels the operation
     */
    void download(File dest, File localTarget, Job job, ProgressListener listener) throws UploadException, RepoNotAvailableException, PermanentUploadException, CancelledException;

    /**
     * Move the item identified by fullPathFrom to a location which corresponds
     * to dest.
     *
     * @param fullPathFrom - the full local path to the resource which should be moved
     * @param dest - the destination location of the file/folder which has been moved
     * @param job
     */
    void move( String fullPathFrom, File dest, Job job, QueueItem item ) throws RepoNotAvailableException, PermanentUploadException, UploadException;

    /**
     * Delete the file from the repository
     *
     * @param r
     * @param file
     * @param job
     * @param status
     * @throws DeleteException - for a transient error processing the file. Should be retried
     * @throws RepoNotAvailableException - the repository is temporarily unavailable
     * @throws PermanentUploadException - an exception has occurred which cannot be explicitly handler. Do not retry
     * @return true that the file was deleted, false that it was not (ie did not exist)
     */
    boolean delete( File file, Job job ) throws DeleteException, RepoNotAvailableException, PermanentUploadException;

    void setCurrent(QueueItem item);

    /**
     * transient information, the currently processing queue item.
     *
     * Only allow one at a time per repository
     *
     * @return
     */
    QueueItem getCurrent();

    /**
     * called by configurator on startup
     *
     * @param j
     */
    public void setJob( Job j );

    /**
     * called by configurator on startup
     * 
     * @param queue
     */
    public void setQueue( Queue queue );

    /**
     * A human readable description of this repository
     * 
     * @return
     */
    public String getDescription();

    /**
     * Check to see if the repository is accessible
     *
     * @return - true if the repository is accessible
     */
    public boolean ping();

    /**
     * Return true if the given file is in an excluded folder for this repository
     *
     * @param child
     * @return
     */
    boolean isExcludedFile( File child, Root root );

    /**
     * Called after the scanner has finished scannign a folder
     *
     */
    void onScanDirComplete(String filePath, String localRootPath, String repoName ) throws RepoNotAvailableException;



    /**
     * Whether or not to synchronise files with the repository. In effect this
     * will mean local files may be modified if a later version is detected
     * int the repository
     * 
     * @return
     */
    boolean isSync();
 
    
    Long getMaxBytes();

    Long getAccountUsedBytes();

	boolean isConfigured();


    
}
