/*
 * RestoreView.java
 *
 * Created on 5/11/2010, 3:39:16 PM
 */
package com.ettrema.backup;

import com.ettrema.backup.utils.TimeUtils;
import com.ettrema.backup.engine.FileChangeChecker;
import com.ettrema.backup.engine.FileChangeChecker.SyncStatus;
import com.ettrema.httpclient.Utils.CancelledException;
import javax.swing.table.DefaultTableModel;
import com.ettrema.backup.config.FileMeta;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.PermanentUploadException;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.config.RepoNotAvailableException;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.config.UploadException;
import com.ettrema.backup.engine.FileWatcher;
import com.ettrema.httpclient.ProgressListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.jdesktop.application.Action;

import static com.ettrema.backup.BackupApplication._;

/**
 *
 * @author brad
 */
public class RestoreView extends javax.swing.JFrame implements ProgressListener {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RestoreView.class);
	private final Job job;
	private final Repo repo;
	private final Iterable<Root> roots;
	private boolean running;
	private Thread thread;

	/** Creates new form RestoreView */
	public RestoreView(Job job, Repo repo) {
		initComponents();
		this.job = job;
		this.repo = repo;
		this.roots = job.getRoots();
		this.setTitle("Download missing files from: " + repo.getDescription());
	}

	public RestoreView(Job job, Repo repo, Root root) {
		initComponents();
		this.job = job;
		this.repo = repo;
		this.roots = Arrays.asList(root);
		this.setTitle("Download missing files from: " + repo.getDescription() + "/" + root.getRepoName());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pblOuter = new javax.swing.JPanel();
        pnlCurrentTask = new javax.swing.JPanel();
        lblCurrentScan = new javax.swing.JLabel();
        lblCurrentDirVal = new javax.swing.JLabel();
        lblCurrentProgress = new javax.swing.JLabel();
        lblCurrentFile = new javax.swing.JLabel();
        lblProgress = new javax.swing.JLabel();
        progressCurrent = new javax.swing.JProgressBar();
        btnCancel = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblProcessed = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.ettrema.backup.BackupApplication.class).getContext().getResourceMap(RestoreView.class);
        pblOuter.setBackground(resourceMap.getColor("pblOuter.background")); // NOI18N
        pblOuter.setName("pblOuter"); // NOI18N

        pnlCurrentTask.setBackground(resourceMap.getColor("pnlCurrentTask.background")); // NOI18N
        pnlCurrentTask.setName("pnlCurrentTask"); // NOI18N

        lblCurrentScan.setFont(resourceMap.getFont("lblCurrentScan.font")); // NOI18N
        lblCurrentScan.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurrentScan.setText(resourceMap.getString("lblCurrentScan.text")); // NOI18N
        lblCurrentScan.setName("lblCurrentScan"); // NOI18N

        lblCurrentDirVal.setBackground(resourceMap.getColor("lblCurrentDirVal.background")); // NOI18N
        lblCurrentDirVal.setFont(resourceMap.getFont("lblCurrentDirVal.font")); // NOI18N
        lblCurrentDirVal.setText(resourceMap.getString("lblCurrentDirVal.text")); // NOI18N
        lblCurrentDirVal.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("lblCurrentDirVal.border.lineColor"))); // NOI18N
        lblCurrentDirVal.setName("lblCurrentDirVal"); // NOI18N

        lblCurrentProgress.setFont(resourceMap.getFont("lblCurrentProgress.font")); // NOI18N
        lblCurrentProgress.setText(resourceMap.getString("lblCurrentProgress.text")); // NOI18N
        lblCurrentProgress.setName("lblCurrentProgress"); // NOI18N

        lblCurrentFile.setFont(resourceMap.getFont("lblCurrentFile.font")); // NOI18N
        lblCurrentFile.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("lblCurrentFile.border.lineColor"))); // NOI18N
        lblCurrentFile.setName("lblCurrentFile"); // NOI18N

        lblProgress.setFont(resourceMap.getFont("lblProgress.font")); // NOI18N
        lblProgress.setText(resourceMap.getString("lblProgress.text")); // NOI18N
        lblProgress.setName("lblProgress"); // NOI18N

        progressCurrent.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("progressCurrent.border.lineColor"))); // NOI18N
        progressCurrent.setName("progressCurrent"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.ettrema.backup.BackupApplication.class).getContext().getActionMap(RestoreView.class, this);
        btnCancel.setAction(actionMap.get("toggleRun")); // NOI18N
        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N

        javax.swing.GroupLayout pnlCurrentTaskLayout = new javax.swing.GroupLayout(pnlCurrentTask);
        pnlCurrentTask.setLayout(pnlCurrentTaskLayout);
        pnlCurrentTaskLayout.setHorizontalGroup(
            pnlCurrentTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCurrentTaskLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCurrentTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblCurrentProgress)
                    .addComponent(lblCurrentScan, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblProgress))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlCurrentTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblCurrentDirVal, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                    .addComponent(lblCurrentFile, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                    .addComponent(progressCurrent, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE))
                .addGap(10, 10, 10)
                .addComponent(btnCancel))
        );
        pnlCurrentTaskLayout.setVerticalGroup(
            pnlCurrentTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlCurrentTaskLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCurrentTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblCurrentScan)
                    .addComponent(lblCurrentDirVal, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlCurrentTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnlCurrentTaskLayout.createSequentialGroup()
                        .addComponent(lblCurrentProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblProgress))
                    .addGroup(pnlCurrentTaskLayout.createSequentialGroup()
                        .addComponent(lblCurrentFile, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(progressCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnCancel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(23, 23, 23))
        );

        jScrollPane1.setBackground(resourceMap.getColor("jScrollPane1.background")); // NOI18N
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        tblProcessed.setFont(resourceMap.getFont("tblProcessed.font")); // NOI18N
        tblProcessed.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Size", "Time (secs)", "Restored to"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblProcessed.setName("tblProcessed"); // NOI18N
        tblProcessed.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblProcessed);

        javax.swing.GroupLayout pblOuterLayout = new javax.swing.GroupLayout(pblOuter);
        pblOuter.setLayout(pblOuterLayout);
        pblOuterLayout.setHorizontalGroup(
            pblOuterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pblOuterLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(pnlCurrentTask, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 585, Short.MAX_VALUE)
        );
        pblOuterLayout.setVerticalGroup(
            pblOuterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pblOuterLayout.createSequentialGroup()
                .addComponent(pnlCurrentTask, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE))
        );

        getContentPane().add(pblOuter, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	@Action
	public void toggleRun() {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				if (running) {
					running = false;
					btnCancel.setText("Start");
					thread.interrupt();
				} else {
					running = true;
					startRun();
					btnCancel.setText("Cancel");
				}

			}
		});
	}

	private void startRun() {
		thread = new Thread(new Runnable() {

			public void run() {
				log.trace("startRun");
				_(FileWatcher.class).setDisabled(true);
				try {
					for (Root root : roots) {
						if (running) {
							File localRoot = new File(root.getFullPath());
							int num = downloadRoot(root, "", localRoot);
							JOptionPane.showMessageDialog(RestoreView.this, "Finished! Downloaded " + num + " files");
						}
					}
				} catch (RepoNotAvailableException e) {
					log.error("offline", e);
					JOptionPane.showMessageDialog(RestoreView.this, "The repository is offline", "Error downloading", JOptionPane.ERROR_MESSAGE);
				} finally {
					_(FileWatcher.class).setDisabled(false);
				}

			}
		});
		thread.setDaemon(true);
		thread.setName("Restore thread");
		thread.start();
	}

	private int downloadRoot(Root root, String path, File localFolder) throws RepoNotAvailableException {
		lblCurrentDirVal.setText(localFolder.getAbsolutePath());
		String repoPath = root.getRepoName() + "/" + path;
		log.trace("download: " + repoPath);
		List<FileMeta> list = repo.listFileMeta(repoPath);
		if (list == null) {
			log.trace("downloadRoot: no resources");
			return 0;
		} else {
			int numFilesDownloaded = 0;
			for (FileMeta fm : list) {
				if (!running) {
					return numFilesDownloaded;
				}
				File localChild = new File(localFolder, fm.getName());
				if (fm.isDirectory()) {
					if (fm.getName().startsWith("_sys_")) {
						log.trace("ignore system folder: " + fm.getName());
					} else {
						log.trace("isdir: " + fm.getName());
						int n = downloadRoot(root, path + fm.getName() + "/", localChild);
						numFilesDownloaded += n;
					}
				} else {
					log.trace("is file: " + fm.getName());
					try {
						if (checkDownloadFile(localChild, fm)) {
							numFilesDownloaded++;
						}
					} catch (CancelledException e) {
						log.info("cancelled");
					} catch (UploadException ex) {
						log.error("Couldnt download: " + localChild.getAbsolutePath(), ex);
					} catch (PermanentUploadException ex) {
						log.error("Couldnt download: " + localChild.getAbsolutePath(), ex);
					}
				}
			}
			return numFilesDownloaded;
		}
	}

	/**
	 *
	 * @param localChild
	 * @param fm
	 * @return - true if downloaded a file
	 * @throws UploadException
	 * @throws RepoNotAvailableException
	 * @throws PermanentUploadException
	 */
	private boolean checkDownloadFile(final File localChild, FileMeta fm) throws UploadException, RepoNotAvailableException, PermanentUploadException, CancelledException {
		SyncStatus status = _(FileChangeChecker.class).checkFile(repo, fm, localChild);
		if (status == SyncStatus.REMOTE_NEWER) {
			log.trace("is newer so download");
			long tm = System.currentTimeMillis();
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					lblCurrentFile.setText(localChild.getAbsolutePath());
				}
			});
			if (!running) {
				return false;
			}
			repo.download(localChild, localChild, job, this);
			tm = System.currentTimeMillis() - tm;
			addCompletedRow(localChild, fm, tm);
			return true;
		} else {
			log.trace("not newer, so ignore");
			return false;
		}
	}

	public void onComplete(final String fileName) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				progressCurrent.setValue(100);
			}
		});
	}

	public void onRead(int bytes) {
	
	}

	public void onProgress(final long bytesRead, final Long totalBytes, String fileName) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				if (totalBytes != null) {
					int percent = (int) (bytesRead * 100 / totalBytes);
					progressCurrent.setValue(percent);
				} else {
					progressCurrent.setValue(0);
				}
			}
		});
	}

	public boolean isCancelled() {
		return !running;
	}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblCurrentDirVal;
    private javax.swing.JLabel lblCurrentFile;
    private javax.swing.JLabel lblCurrentProgress;
    private javax.swing.JLabel lblCurrentScan;
    private javax.swing.JLabel lblProgress;
    private javax.swing.JPanel pblOuter;
    private javax.swing.JPanel pnlCurrentTask;
    private javax.swing.JProgressBar progressCurrent;
    private javax.swing.JTable tblProcessed;
    // End of variables declaration//GEN-END:variables

	private void addCompletedRow(File localChild, FileMeta fm, long timeMs) {
		DefaultTableModel model = (DefaultTableModel) tblProcessed.getModel();
		Object[] rowData = new Object[4];
		//"Name", "Size", "Time (secs)", "Restored to"
		rowData[0] = fm.getName();
		rowData[1] = TimeUtils.formatBytes(fm.getLength());
		rowData[2] = TimeUtils.formatSecsAsTime(timeMs / 1000);
		rowData[3] = localChild.getAbsolutePath();
		model.insertRow(0, rowData);
	}
}
