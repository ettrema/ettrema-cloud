package com.ettrema.backup.queue;

import com.ettrema.backup.config.*;
import com.ettrema.backup.engine.CrcCalculator;
import com.ettrema.backup.engine.StateTokenFileSyncer;
import com.ettrema.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author brad
 */
public class NewFileHandler implements QueueItemHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(NewFileHandler.class);
    private final CrcCalculator crcCalculator;
    private final StateTokenFileSyncer fileSyncer;

    public NewFileHandler(CrcCalculator crcCalculator, StateTokenFileSyncer fileSyncer) {
        this.crcCalculator = crcCalculator;
        this.fileSyncer = fileSyncer;
    }

    @Override
    public boolean supports(QueueItem item) {
        return item instanceof NewFileQueueItem;
    }

    @Override
    public boolean requiresWait(QueueItem item) {
        NewFileQueueItem nf = (NewFileQueueItem) item;

        // Wait until stable
        if (isModifiedRecently(nf)) {
            return true;
        }

        if (isUploadedRecently(nf)) {
            return true;
        }

        if (isFileOpen(nf.getFile())) {
            return true;
        }
        return false;
    }

    @Override
    public void process(Repo r, QueueItem item) throws RepoNotAvailableException, PermanentUploadException {
        log.debug("uploading: " + item.getFile().getAbsolutePath());
        item.setStarted(new Date());

        File f = item.getFile();
        try {
            if (item.getFile().exists()) {
                if (item.getFile().isFile()) {

                    uploadFile(f, r, item);

                } else {
                    // Upload directory
                    uploadDir(f, r, item);
                    log.trace("finished Dir upload: " + f.getAbsolutePath());
                }
            } else {
                log.info("file no longer exists: " + item.getFile().getAbsolutePath());
                item.setNotes("file no longer exists");
            }
        } catch (RepoNotAvailableException e) {
            System.out.println("Failed to upload: " + f.getAbsolutePath());
            throw e;
        } catch (PermanentUploadException e) {
            System.out.println("Failed to upload: " + f.getAbsolutePath());
            throw e;
        } catch (Exception e) {
            log.error("Exception uploading file: " + f.getAbsolutePath(), e);
            item.setNotes("error uploading file: " + e.getMessage());
        } finally {
            item.setCompleted(new Date());
            log.debug("completed upload task");
        }
    }

    private void uploadDir(File dir, Repo r, QueueItem item) throws RepoNotAvailableException, UploadException, PermanentUploadException {
        LogUtils.trace(log, "uploadDir: dir:" , dir.getAbsolutePath());
        File[] children = dir.listFiles();
        for(File child : children ) {
            if( child.isDirectory() ) {
                uploadDir(child, r, item);
            } else {
                uploadFile(child, r, item);
            }
        }
    }
    
    private void uploadFile(File f, Repo r, QueueItem item) throws RepoNotAvailableException, UploadException, PermanentUploadException {
        if (isFileOpen(f)) {
            log.warn("UploadFile: File is locked so will skip: " + f.getAbsolutePath());
            return;
        }        
        long tm = System.currentTimeMillis();
        log.trace("Uploading file: " + f.getAbsolutePath() + " ----------------------------- ");
        r.upload(f, item);

        long crc = crcCalculator.getLocalCrc(f);

        fileSyncer.setLocalBackedupCrc(f, r, crc);

        //tm = System.currentTimeMillis() - tm;
        // String duration = TimeUtils.formatSecsAsTime(tm / 1000);
        //long bytes = item.getFile().length();

        //double bw = bytes * 1000 / tm;
    }

    private boolean isFileOpen(File file) {
        log.info("is file open? " + file.getAbsolutePath());
        FileOutputStream fout = null;
        try {
            try {
                fout = new FileOutputStream(file, true);
                log.trace("not lockled");
                return false;
            } catch (FileNotFoundException ex) {
                log.info("file doesnt exist: " + file.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            log.info("exception occured, so presume file is locked: " + file.getAbsolutePath() + " - " + e.getMessage());
            return true;
        } finally {
            IOUtils.closeQuietly(fout);
        }
    }

    private boolean isUploadedRecently(NewFileQueueItem nf) {
        // TODO: check when last uploaded. Larger files will require longer
        // intervals between being uploaded
        return false;
    }

    private boolean isModifiedRecently(NewFileQueueItem nf) {
        long lastModTime = nf.getFile().lastModified();
        long timeSinceMod = System.currentTimeMillis() - lastModTime;
        // modified within the last 5 seconds is recent
        return (timeSinceMod < 5000);
    }
}
