package com.ettrema.backup.config;

import com.ettrema.httpclient.ProgressListener;

/**
 *
 * @author brad
 */
public class QueueItemProgressListener implements ProgressListener {

    private final QueueItem queueItem;
    private final ProgressListener wrapped;
    private transient int lastPercent;

    public QueueItemProgressListener(QueueItem queueItem, ProgressListener wrapped) {
        this.queueItem = queueItem;
        this.wrapped = wrapped;
        System.out.println("QueueItemProgressListener: wrapped: " + wrapped.getClass());
    }

    @Override
    public void onProgress(long bytesRead, Long totalBytes, String fileName) {
        System.out.println("onProgrsss: " + bytesRead + " - " + fileName);
        wrapped.onProgress(bytesRead, totalBytes, fileName);
    }

    @Override
    public void onComplete(String fileName) {
        wrapped.onComplete(fileName);
    }

    @Override
    public boolean isCancelled() {
        return wrapped.isCancelled();
    }

    private void setNotes(String notes) {
        queueItem.setNotes(notes);
    }

	@Override
	public void onRead(int bytes) {

	}
}
