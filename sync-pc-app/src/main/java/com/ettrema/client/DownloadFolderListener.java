package com.ettrema.client;

import com.ettrema.backup.App;
import com.ettrema.httpclient.Folder;
import com.ettrema.httpclient.ProgressListener;
import java.io.File;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 *
 * @author mcevoyb
 */
public class DownloadFolderListener extends AbstractMouseListener {

	static void add(JPopupMenu popupMenu, FolderNode aThis) {
		JMenuItem item = new JMenuItem("Download to desktop");
		item.addMouseListener(new DownloadFolderListener(aThis.folder));
		popupMenu.add(item);
	}
	final Folder folder;

	public DownloadFolderListener(Folder folder) {
		this.folder = folder;
	}

	@Override
	public void onClick() {
		Application app = App.current();
		Task task = new DownloadFolderTask(app);
		app.getContext().getTaskService().execute(task);
	}

	class DownloadFolderTask extends Task implements ProgressListener {

		DownloadFolderTask(Application app) {
			super(app);
		}

		@Override
		protected Object doInBackground() throws Exception {
			String dest = System.getProperty("user.home");
			File fDest = new File(dest);
			if (fDest.exists()) {
				File f2 = new File(fDest, "Desktop");
				if (f2.exists()) {
					fDest = f2;
				}
			} else {
				throw new RuntimeException("Couldnt find user's home directory: " + dest);
			}
			folder.downloadTo(fDest, this);
			return null;
		}

		public void onProgress(final long bytesRead, final Long totalBytes, String fileName) {

			App.current().getBrowser().status("Downloading: " + fileName);
			if (totalBytes != null) {
				int percent = (int) (bytesRead * 100 / totalBytes);
				this.setProgress(percent);
			} else {
				this.setProgress(0);
			}
			
		}

		public void onComplete(String fileName) {
			App.current().getBrowser().status("Finished: " + fileName);
			this.setProgress(100);
		}

		public void onRead(int bytes) {
			
		}
	}
}
