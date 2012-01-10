package com.ettrema.backup.config;

import com.ettrema.common.Withee;
import com.ettrema.httpclient.ProgressListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author brad
 */
public class LocalRepo implements Repo {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LocalRepo.class);
    private String id;
    private File target;
    private transient Job job;
    private transient LocalRepoState state;

    public LocalRepo() {
        id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    public File getTarget() {
        return target;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    @Override
    public Long getBackedUpBytes() {
        return state.getBackedupBytes();
    }

    private void addBackedUpBytes(long n) {
        if (state.getBackedupBytes() == null) {
            state.setBackedupBytes((Long) n);
        } else {
            state.setBackedupBytes((Long) (state.getBackedupBytes() + n));
        }
    }

    @Override
    public void onScan() {
        state.setBackedupBytes(null);
    }

    @Override
    public void onScanComplete() {
    }

    private String mungePath(String filePath, String localRootPath, String repoName) {
//        log.debug( "mungePath: " + filePath + " - " + localRootPath + " - " + repoName );
        if (filePath.startsWith(localRootPath)) {
            String basePath = filePath.substring(localRootPath.length() + 1);
            if (repoName.length() > 0 && !repoName.endsWith("/")) {
                repoName = repoName + "/";
            }
            String repoPath = repoName + basePath;
            repoPath.replace("/", File.separator);
            //log.debug( "  --- > " + repoPath );
            return target.getAbsolutePath() + File.separator + repoPath;
        } else {
            throw new RuntimeException("The file path is not on this root: " + filePath + " - " + localRootPath);
        }
    }

    private File getDestFile(String filePath, String localRootPath, String repoName) {
        String repoPath = mungePath(filePath, localRootPath, repoName);
        File fDest = new File(repoPath);
        return fDest;
    }

    @Override
    public FileMeta getFileMeta(String filePath, String localRootPath, String repoName, boolean isScan) throws RepoNotAvailableException {
        log.debug("getFileMeta: " + filePath + " - " + localRootPath + " - " + repoName);
        File fDest = getDestFile(filePath, localRootPath, repoName);
        if (fDest.exists()) {
            FileMeta meta = new LocalFileMeta(fDest);            
            return meta;
        } else {
            return null;
        }
    }

    @Override
    public List<FileMeta> listFileMeta(String repoPath) {
        repoPath = repoPath.replace("/", File.separator);
        String fullRepoPath = target.getAbsolutePath() + File.separator + repoPath;
        log.trace("listFileMeta: " + repoPath + " -> " + fullRepoPath);
        File fRepoFolder = new File(fullRepoPath);
        if (fRepoFolder.exists()) {
            if (fRepoFolder.isDirectory()) {
                if (fRepoFolder.listFiles() != null) {
                    List<FileMeta> list = new ArrayList<FileMeta>();
                    for (File fChild : fRepoFolder.listFiles()) {
                        FileMeta fmChild = new LocalFileMeta(fChild);                        
                        list.add(fmChild);
                    }
                    return list;
                } else {
                    log.trace("no resources in path");
                    return null;
                }
            } else {
                log.trace("path is a file, not a folder");
                return null;
            }
        } else {
            log.trace("path doesnt exist");
            return null;
        }

    }

    @Override
    public void download(File dest, File localTarget, Job job, ProgressListener listener) throws UploadException, RepoNotAvailableException, PermanentUploadException {
        try {
            log.info("download: " + dest.getAbsolutePath());
            String s = munge(localTarget.getAbsolutePath(), job);
            File repoFile = new File(s);
            if (!repoFile.exists()) {
                throw new UploadException(dest, new Exception("The repository resource does not exist: " + s));
            }
            if (repoFile.isFile()) {
                // TODO: use NotifyingInputStream to callback to listener
                FileUtils.copyFile(repoFile, dest, true);

                if (listener != null) {
                    listener.onComplete(dest.getName());
                }
            } else {
                log.trace("remote file is a folder!");
            }
        } catch (IOException ex) {
            throw new RepoNotAvailableException(ex);
        }
    }

    @Override
    public void upload(File file, QueueItem item) throws UploadException, RepoNotAvailableException {
        // TODO check repo is available with file.exists on root dir
        String repoPath = munge(file.getAbsolutePath(), job);
        File dest = new File(repoPath);

        File parent = dest.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                throw new RepoNotAvailableException("Couldnt create parent dir: " + parent.getAbsolutePath());
            }
            if (!parent.exists()) {
                throw new RepoNotAvailableException("Didnt create: " + dest.getAbsolutePath());
            }
        }

        try {
            FileUtils.copyFile(file, dest);
        } catch (IOException ex) {
            throw new UploadException(file, ex);
        }
    }

    @Override
    public void move(String fullPathFrom, File localDest, QueueItem item) throws RepoNotAvailableException, PermanentUploadException, UploadException {

        log.info("move: " + fullPathFrom + " -> " + localDest.getAbsolutePath());
        String sSrcPath = munge(fullPathFrom, job);

        String sDestPath = munge(localDest.getAbsolutePath(), job);

        File fSrc = new File(sSrcPath); // this is the source file in the repository to be moved
        if (!fSrc.exists()) {
            // the resource doesnt yet exist in the repository, which just means it hasnt been backed up yet
            // If a file upload it, if a dir it will be uploaded on next scan
            if (localDest.exists()) {
                upload(localDest, item);
            } else {
                throw new PermanentUploadException("Attempted to move a file in the repository which doesnt exist, so tried to upload it, and found local resource doesnt exist either!");
            }
        } else {
            File fDest = new File(sDestPath); // this is the path in the repository to move to
            File fDestParent = fDest.getParentFile();
            if (!fDestParent.exists()) {
                if (!fDestParent.mkdirs()) {
                    throw new PermanentUploadException("Couldnt create destination folder: " + fDestParent.getAbsolutePath());
                }
            }

            // if destination exists remove it
            if (fDest.exists()) {
                if (!fDest.delete()) {
                    throw new PermanentUploadException("Couldnt move because a file or folder exists at the target location and it could not be deleted");
                }
            }

            if (!fSrc.renameTo(fDest)) {
                throw new PermanentUploadException("Couldnt rename to: " + fDest.getAbsolutePath());
            }
            log.trace("moved ok");
        }

    }

    @Override
    public boolean delete(File file) throws DeleteException, RepoNotAvailableException {
        // TODO check repo is available with file.exists on root dir

        String repoPath = munge(file.getAbsolutePath(), job);
        File dest = new File(repoPath);
        if (dest.exists()) {
            if (!dest.delete()) {
                throw new DeleteException(file, null);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public String munge(String fullSourcePath, Job job) {
        for (Root r : job.getRoots()) {
            if (fullSourcePath.startsWith(r.getFullPath())) {
                return mungePath(fullSourcePath, r.getFullPath(), r.getRepoName());
            }
        }
        throw new RuntimeException("File is not within a backed up directory: " + fullSourcePath);
    }

    @Override
    public Queue getQueue() {
        if (state.getQueue() == null) {
            state.setQueue(new Queue());
        }
        return state.getQueue();
    }

    @Override
    public boolean isOffline() {
        return state.isOffline();
    }

    @Override
    public void setOffline(boolean b) {
        state.setOffline(b);
    }

    @Override
    public void setCurrent(QueueItem item) {
        state.setCurrent(item);
    }

    @Override
    public QueueItem getCurrent() {
        return state.getCurrent();
    }

    @Override
    public void setJob(Job j) {
        this.job = j;
    }

    @Override
    public void setQueue(Queue queue) {
        state.setQueue(queue);
    }

    @Override
    public String getDescription() {
        return this.target.getAbsolutePath();
    }

    @Override
    public boolean isExcludedFile(File child, Root root) {
        return false;
    }

    @Override
    public void onScanDirComplete(String filePath, String localRootPath, String repoName) throws RepoNotAvailableException {
    }

    @Override
    public boolean isSync() {
        return false;
    }

    @Override
    public Long getMaxBytes() {
        return state.getMaxBytes();
    }

    @Override
    public Long getAccountUsedBytes() {
        return state.getUsedBytes();
    }

    @Override
    public boolean isConfigured() {
        return target != null && target.exists() && target.isDirectory();
    }

    @Override
    public void withFileMeta(String filePath, String localRootPath, String repoName, Withee<FileMeta> withee) throws RepoNotAvailableException {
        FileMeta meta = getFileMeta(filePath, localRootPath, repoName, false);
        try {
            withee.with(meta);
        } catch (RepoNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LocalRepoState getState() {
        return state;
    }

    @Override
    public void setState(Object state) {
        this.state = (LocalRepoState) state;
    }

    public static class LocalRepoState {

        private Long backedupBytes;
        private boolean offline;
        private QueueItem current;
        private Queue queue;
        private Long maxBytes;
        private Long usedBytes;

        /**
         * @return the backedupBytes
         */
        public Long getBackedupBytes() {
            return backedupBytes;
        }

        /**
         * @param backedupBytes the backedupBytes to set
         */
        public void setBackedupBytes(Long backedupBytes) {
            this.backedupBytes = backedupBytes;
        }

        /**
         * @return the offline
         */
        public boolean isOffline() {
            return offline;
        }

        /**
         * @param offline the offline to set
         */
        public void setOffline(boolean offline) {
            this.offline = offline;
        }

        /**
         * @return the current
         */
        public QueueItem getCurrent() {
            return current;
        }

        /**
         * @param current the current to set
         */
        public void setCurrent(QueueItem current) {
            this.current = current;
        }

        /**
         * @return the queue
         */
        public Queue getQueue() {
            return queue;
        }

        /**
         * @param queue the queue to set
         */
        public void setQueue(Queue queue) {
            this.queue = queue;
        }

        /**
         * @return the maxBytes
         */
        public Long getMaxBytes() {
            return maxBytes;
        }

        /**
         * @param maxBytes the maxBytes to set
         */
        public void setMaxBytes(Long maxBytes) {
            this.maxBytes = maxBytes;
        }

        /**
         * @return the usedBytes
         */
        public Long getUsedBytes() {
            return usedBytes;
        }

        /**
         * @param usedBytes the usedBytes to set
         */
        public void setUsedBytes(Long usedBytes) {
            this.usedBytes = usedBytes;
        }
    }
}
