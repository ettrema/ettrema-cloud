package com.ettrema.backup.config;

import com.ettrema.backup.observer.Observer;
import java.io.File;
import java.util.Date;

/**
 *
 * @author brad
 */
public interface QueueItem {
    

    void addObserver( Observer<QueueItem,Queue> ob );

    /**
     * The most recent time that this item has been changed. Used to determine
     * whether the underlying event is stable enough to process.
     *
     * @return
     */
    long getLastModified();

    /**
     * The number of bytes of content to upload. May be zero for deletes etc
     * 
     * @return
     */
    long getBytesToUpload();

    /**
     * The name of the resource being operated on. Eg if the task is to upload
     * a file, it is just the name of the file.
     *
     * @return
     */
    String getFileName();

    File getFile();

    /**
     * Human readble description of what type of queue item this is
     * 
     * ie upload, delete
     * 
     * @return
     */
    String getActionDescription();

    /**
     * The target repository
     * 
     * @return
     */
    Repo getRepo();

    Date getStarted();

    void setStarted( Date started );

    Date getCompleted();

    void setCompleted( Date finished );

    void setNotes( String string );

    String getNotes();

}
