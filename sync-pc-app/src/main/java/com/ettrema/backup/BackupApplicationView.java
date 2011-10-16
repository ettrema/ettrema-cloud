package com.ettrema.backup;

import javax.swing.JMenu;
import com.ettrema.backup.config.Root;
import com.ettrema.backup.config.Job;
import javax.swing.JMenuItem;
import com.ettrema.backup.config.Queue;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.account.AccountCreator;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.engine.Engine;
import com.ettrema.backup.engine.FileWatcher;
import com.ettrema.backup.history.HistoryDao;
import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.queue.QueueManager;
import com.ettrema.backup.view.SummaryDetails;
import com.ettrema.event.EventManager;
import java.awt.Image;
import javax.swing.ImageIcon;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ettrema.backup.BackupApplication._;

/**
 * The application's main frame.
 */
public class BackupApplicationView extends FrameView implements Observer<Config, Object> {

	private static final Logger log = LoggerFactory.getLogger(BackupApplicationView.class);
	private final Engine engine;
	private final Config config;
	private final EventManager eventManager;
	private final QueueManager queueProcessor;
	private final AccountCreator accountCreator;
	private final BrowserController browserController;
	private final HistoryDao historyDao;
	private boolean isProblem;
	private String problemDescription;
	private boolean throttleChanged;

	public BackupApplicationView(SingleFrameApplication app, Engine engine, AccountCreator accountCreator, EventManager eventManager, QueueManager queueProcessor, BrowserController browserController, HistoryDao historyDao) throws Exception {
		super(app);
		this.engine = engine;
		this.eventManager = eventManager;
		this.queueProcessor = queueProcessor;
		this.accountCreator = accountCreator;
		this.browserController = browserController;
		this.historyDao = historyDao;

		this.config = engine.getConfig();

		initComponents();

		getFrame().setResizable(false);

		sldThrottle.setModel(new DefaultBoundedRangeModel(config.getThrottlePerc(), 1, 25, 100));

		// status bar initialization - message timeout, idle icon and busy animation, etc

		ResourceMap resourceMap = getResourceMap();


		int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
		messageTimer = new Timer(messageTimeout, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				statusMessageLabel.setText("");
			}
		});
		messageTimer.setRepeats(false);
		int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
		for (int i = 0; i < busyIcons.length; i++) {
			busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
		}
		busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
				statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
			}
		});
		idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
		statusAnimationLabel.setIcon(idleIcon);
		progressBar.setVisible(false);

		// connecting action tasks to status bar via TaskMonitor
		TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
		taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

			public void propertyChange(java.beans.PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if ("started".equals(propertyName)) {
					if (!busyIconTimer.isRunning()) {
						statusAnimationLabel.setIcon(busyIcons[0]);
						busyIconIndex = 0;
						busyIconTimer.start();
					}
					progressBar.setVisible(true);
					progressBar.setIndeterminate(true);
				} else if ("done".equals(propertyName)) {
					busyIconTimer.stop();
					statusAnimationLabel.setIcon(idleIcon);
					progressBar.setVisible(false);
					progressBar.setValue(0);
				} else if ("message".equals(propertyName)) {
					String text = (String) (evt.getNewValue());
					statusMessageLabel.setText((text == null) ? "" : text);
					messageTimer.restart();
				} else if ("progress".equals(propertyName)) {
					int value = (Integer) (evt.getNewValue());
					progressBar.setVisible(true);
					progressBar.setIndeterminate(false);
					progressBar.setValue(value);
				}
			}
		});

		ImageIcon imageIcon = createImageIcon("/logo16x16.png", "logo");
		Image logo = imageIcon.getImage();
		getFrame().setIconImage(logo);

		initQueueMenu();
		initModifyAccountsMenu();
		initDownloadMenu();
		config.addObserver(this);

//        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay( new Runnable() {
//
//            public void run() {
//                doUpdateScreen();
//            }
//        }, 3000, 1000, TimeUnit.MILLISECONDS );

	}

	public JPanel getMainPanel() {
		return mainPanel;
	}

	final protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	@Action
	public void showAboutBox() {
		if (aboutBox == null) {
			JFrame mainFrame = BackupApplication.getApplication().getMainFrame();
			aboutBox = new BackupApplicationAboutBox(mainFrame);
			aboutBox.setLocationRelativeTo(mainFrame);
		}
		BackupApplication.getApplication().show(aboutBox);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        sldThrottle = new javax.swing.JSlider();
        lblUsageVal = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        progCurrent = new javax.swing.JProgressBar();
        lblCurrentVal = new javax.swing.JLabel();
        lblOverallProgressVal = new javax.swing.JLabel();
        lblTimeRemainingVal = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        menuitemNewAccount = new javax.swing.JMenuItem();
        menuModifyAccounts = new javax.swing.JMenu();
        menuViewFilesOnServer = new javax.swing.JMenuItem();
        menuBrowseMedia = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        toolsMenu = new javax.swing.JMenu();
        menuScanNow = new javax.swing.JMenuItem();
        menuQueues = new javax.swing.JMenu();
        menuRestore = new javax.swing.JMenu();
        menuConflicts = new javax.swing.JMenuItem();
        menuHistory = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.ettrema.backup.BackupApplication.class).getContext().getResourceMap(BackupApplicationView.class);
        mainPanel.setBackground(resourceMap.getColor("mainPanel.background")); // NOI18N
        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setMaximumSize(new java.awt.Dimension(680, 462));
        mainPanel.setMinimumSize(new java.awt.Dimension(680, 462));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(680, 462));
        mainPanel.setLayout(null);

        sldThrottle.setBackground(resourceMap.getColor("sldThrottle.background")); // NOI18N
        sldThrottle.setFont(resourceMap.getFont("sldThrottle.font")); // NOI18N
        sldThrottle.setAlignmentX(1.0F);
        sldThrottle.setMaximumSize(new java.awt.Dimension(400, 24));
        sldThrottle.setName("sldThrottle"); // NOI18N
        sldThrottle.setPreferredSize(new java.awt.Dimension(100, 30));
        sldThrottle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sldThrottleMouseReleased(evt);
            }
        });
        sldThrottle.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldThrottleStateChanged(evt);
            }
        });
        sldThrottle.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                sldThrottleFocusLost(evt);
            }
        });
        sldThrottle.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                sldThrottlePropertyChange(evt);
            }
        });
        mainPanel.add(sldThrottle);
        sldThrottle.setBounds(180, 361, 384, 50);

        lblUsageVal.setFont(resourceMap.getFont("lblTimeRemainingVal.font")); // NOI18N
        lblUsageVal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblUsageVal.setText(resourceMap.getString("lblUsageVal.text")); // NOI18N
        lblUsageVal.setName("lblUsageVal"); // NOI18N
        mainPanel.add(lblUsageVal);
        lblUsageVal.setBounds(545, 217, 80, 18);

        jLabel7.setBackground(resourceMap.getColor("jLabel7.background")); // NOI18N
        jLabel7.setFont(resourceMap.getFont("lblTimeRemainingVal.font")); // NOI18N
        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N
        mainPanel.add(jLabel7);
        jLabel7.setBounds(95, 169, 151, 18);

        jLabel13.setFont(resourceMap.getFont("lblTimeRemainingVal.font")); // NOI18N
        jLabel13.setForeground(resourceMap.getColor("jLabel13.foreground")); // NOI18N
        jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N
        mainPanel.add(jLabel13);
        jLabel13.setBounds(405, 217, 115, 18);

        jLabel12.setBackground(resourceMap.getColor("jLabel12.background")); // NOI18N
        jLabel12.setFont(resourceMap.getFont("jLabel12.font")); // NOI18N
        jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N
        mainPanel.add(jLabel12);
        jLabel12.setBounds(100, 210, 30, 18);

        jLabel4.setFont(resourceMap.getFont("jLabel4.font")); // NOI18N
        jLabel4.setForeground(resourceMap.getColor("jLabel4.foreground")); // NOI18N
        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel4.setName("jLabel4"); // NOI18N
        mainPanel.add(jLabel4);
        jLabel4.setBounds(43, 92, 590, 26);

        jLabel5.setFont(resourceMap.getFont("jLabel15.font")); // NOI18N
        jLabel5.setForeground(resourceMap.getColor("jLabel5.foreground")); // NOI18N
        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel5.setName("jLabel5"); // NOI18N
        jLabel5.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        mainPanel.add(jLabel5);
        jLabel5.setBounds(43, 122, 590, 16);

        jLabel14.setFont(resourceMap.getFont("jLabel14.font")); // NOI18N
        jLabel14.setForeground(resourceMap.getColor("jLabel14.foreground")); // NOI18N
        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel14.setName("jLabel14"); // NOI18N
        mainPanel.add(jLabel14);
        jLabel14.setBounds(43, 274, 590, 26);

        jLabel15.setFont(resourceMap.getFont("jLabel15.font")); // NOI18N
        jLabel15.setForeground(resourceMap.getColor("jLabel15.foreground")); // NOI18N
        jLabel15.setText(resourceMap.getString("jLabel15.text")); // NOI18N
        jLabel15.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel15.setName("jLabel15"); // NOI18N
        mainPanel.add(jLabel15);
        jLabel15.setBounds(43, 306, 501, 16);

        jLabel16.setFont(resourceMap.getFont("jLabel15.font")); // NOI18N
        jLabel16.setForeground(resourceMap.getColor("jLabel16.foreground")); // NOI18N
        jLabel16.setText(resourceMap.getString("jLabel16.text")); // NOI18N
        jLabel16.setName("jLabel16"); // NOI18N
        mainPanel.add(jLabel16);
        jLabel16.setBounds(43, 322, 189, 16);

        progCurrent.setName("progCurrent"); // NOI18N
        mainPanel.add(progCurrent);
        progCurrent.setBounds(130, 230, 190, 13);

        lblCurrentVal.setFont(resourceMap.getFont("lblTimeRemainingVal.font")); // NOI18N
        lblCurrentVal.setText(resourceMap.getString("lblCurrentVal.text")); // NOI18N
        lblCurrentVal.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblCurrentVal.setName("lblCurrentVal"); // NOI18N
        mainPanel.add(lblCurrentVal);
        lblCurrentVal.setBounds(130, 207, 220, 20);

        lblOverallProgressVal.setFont(resourceMap.getFont("lblTimeRemainingVal.font")); // NOI18N
        lblOverallProgressVal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblOverallProgressVal.setText(resourceMap.getString("lblOverallProgressVal.text")); // NOI18N
        lblOverallProgressVal.setName("lblOverallProgressVal"); // NOI18N
        mainPanel.add(lblOverallProgressVal);
        lblOverallProgressVal.setBounds(395, 169, 230, 18);

        lblTimeRemainingVal.setFont(resourceMap.getFont("lblTimeRemainingVal.font")); // NOI18N
        lblTimeRemainingVal.setText(resourceMap.getString("lblTimeRemainingVal.text")); // NOI18N
        lblTimeRemainingVal.setName("lblTimeRemainingVal"); // NOI18N
        mainPanel.add(lblTimeRemainingVal);
        lblTimeRemainingVal.setBounds(272, 169, 70, 18);

        lblStatus.setFont(resourceMap.getFont("lblStatus.font")); // NOI18N
        lblStatus.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblStatus.setIcon(resourceMap.getIcon("lblStatusOk.icon")); // NOI18N
        lblStatus.setText(resourceMap.getString("lblStatus.text")); // NOI18N
        lblStatus.setName("lblStatus"); // NOI18N
        lblStatus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblStatusMouseClicked(evt);
            }
        });
        mainPanel.add(lblStatus);
        lblStatus.setBounds(210, 34, 420, 39);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/appinterface/Assets/AppInterface.png"))); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        mainPanel.add(jLabel1);
        jLabel1.setBounds(0, 0, 680, 470);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.ettrema.backup.BackupApplication.class).getContext().getActionMap(BackupApplicationView.class, this);
        menuitemNewAccount.setAction(actionMap.get("showNewAccount")); // NOI18N
        menuitemNewAccount.setText(resourceMap.getString("menuitemNewAccount.text")); // NOI18N
        menuitemNewAccount.setName("menuitemNewAccount"); // NOI18N
        fileMenu.add(menuitemNewAccount);

        menuModifyAccounts.setText(resourceMap.getString("menuModifyAccounts.text")); // NOI18N
        menuModifyAccounts.setName("menuModifyAccounts"); // NOI18N
        fileMenu.add(menuModifyAccounts);

        menuViewFilesOnServer.setAction(actionMap.get("showRemoteBrowser")); // NOI18N
        menuViewFilesOnServer.setText(resourceMap.getString("menuViewFilesOnServer.text")); // NOI18N
        menuViewFilesOnServer.setName("menuViewFilesOnServer"); // NOI18N
        menuViewFilesOnServer.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuViewFilesOnServerMouseClicked(evt);
            }
        });
        fileMenu.add(menuViewFilesOnServer);

        menuBrowseMedia.setAction(actionMap.get("openMediaLounge")); // NOI18N
        menuBrowseMedia.setText(resourceMap.getString("menuBrowseMedia.text")); // NOI18N
        menuBrowseMedia.setName("menuBrowseMedia"); // NOI18N
        fileMenu.add(menuBrowseMedia);

        jSeparator1.setName("jSeparator1"); // NOI18N
        fileMenu.add(jSeparator1);

        exitMenuItem.setAction(actionMap.get("doQuit")); // NOI18N
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        toolsMenu.setText(resourceMap.getString("toolsMenu.text")); // NOI18N
        toolsMenu.setName("toolsMenu"); // NOI18N

        menuScanNow.setAction(actionMap.get("scanNow")); // NOI18N
        menuScanNow.setText(resourceMap.getString("menuScanNow.text")); // NOI18N
        menuScanNow.setName("menuScanNow"); // NOI18N
        toolsMenu.add(menuScanNow);

        menuQueues.setText(resourceMap.getString("menuQueues.text")); // NOI18N
        menuQueues.setName("menuQueues"); // NOI18N
        toolsMenu.add(menuQueues);

        menuRestore.setText(resourceMap.getString("menuRestore.text")); // NOI18N
        menuRestore.setName("menuRestore"); // NOI18N
        toolsMenu.add(menuRestore);

        menuConflicts.setAction(actionMap.get("showConflicts")); // NOI18N
        menuConflicts.setText(resourceMap.getString("menuConflicts.text")); // NOI18N
        menuConflicts.setName("menuConflicts"); // NOI18N
        toolsMenu.add(menuConflicts);

        menuHistory.setAction(actionMap.get("showHistory")); // NOI18N
        menuHistory.setText(resourceMap.getString("menuHistory.text")); // NOI18N
        menuHistory.setName("menuHistory"); // NOI18N
        toolsMenu.add(menuHistory);

        menuBar.add(toolsMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setPreferredSize(new java.awt.Dimension(680, 35));

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 660, Short.MAX_VALUE)
                .add(statusAnimationLabel)
                .addContainerGap())
            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusPanelLayout.createSequentialGroup()
                .addContainerGap(524, Short.MAX_VALUE)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 10, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusMessageLabel)
                    .add(statusAnimationLabel))
                .add(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void menuViewFilesOnServerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_menuViewFilesOnServerMouseClicked
    }//GEN-LAST:event_menuViewFilesOnServerMouseClicked

    private void sldThrottlePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_sldThrottlePropertyChange
    }//GEN-LAST:event_sldThrottlePropertyChange

    private void sldThrottleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldThrottleStateChanged
    }//GEN-LAST:event_sldThrottleStateChanged

    private void sldThrottleFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_sldThrottleFocusLost
		if (throttleChanged) {
			System.out.println("sldThrottleFocusLost");
			config.saveData();
			throttleChanged = false;
		}

    }//GEN-LAST:event_sldThrottleFocusLost

    private void lblStatusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblStatusMouseClicked
		if (isProblem) {
			JOptionPane.showMessageDialog(this.getComponent(), problemDescription);
		} else {
			JOptionPane.showMessageDialog(this.getComponent(), "Real time file backup protection is active and functioning");
		}
    }//GEN-LAST:event_lblStatusMouseClicked

    private void sldThrottleMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sldThrottleMouseReleased
		throttleChanged = true;
		config.setThrottlePerc(sldThrottle.getValue());
		_(SummaryDetails.class).refresh();

    }//GEN-LAST:event_sldThrottleMouseReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JLabel lblCurrentVal;
    private javax.swing.JLabel lblOverallProgressVal;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblTimeRemainingVal;
    private javax.swing.JLabel lblUsageVal;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem menuBrowseMedia;
    private javax.swing.JMenuItem menuConflicts;
    private javax.swing.JMenuItem menuHistory;
    private javax.swing.JMenu menuModifyAccounts;
    private javax.swing.JMenu menuQueues;
    private javax.swing.JMenu menuRestore;
    private javax.swing.JMenuItem menuScanNow;
    private javax.swing.JMenuItem menuViewFilesOnServer;
    private javax.swing.JMenuItem menuitemNewAccount;
    private javax.swing.JProgressBar progCurrent;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JSlider sldThrottle;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JMenu toolsMenu;
    // End of variables declaration//GEN-END:variables
	private final Timer messageTimer;
	private final Timer busyIconTimer;
	private final Icon idleIcon;
	private final Icon[] busyIcons = new Icon[15];
	private int busyIconIndex = 0;
	private JDialog aboutBox;

	void init(Engine engine) {
	}

	@Action
	public void showNewAccount() {
		AccountView queueView = new AccountView(engine, eventManager, config, accountCreator, null);
		queueView.setVisible(true);

	}

	@Action
	public void showRemoteBrowser() {
		try {
			JFrame browserFrame = BackupApplication.getApplication().getBrowser().getFrame();
			browserFrame.setSize(500, 400);
			browserFrame.setVisible(true);
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public void doUpdateScreen() {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				try {
					SummaryDetails dets = _(SummaryDetails.class);
					lblTimeRemainingVal.setText(dets.getTimeRemaining());
					if (dets.getCurrentFileName() == null || dets.getCurrentFileName().length() == 0) {
						progCurrent.setVisible(false);
						lblCurrentVal.setText("No files are being processed");
					} else {
						progCurrent.setVisible(true);
						lblCurrentVal.setText(dets.getCurrentFileName());
					}
					//System.out.println("doUpdateScreen: " + dets.getCurrentFilePerc());
					progCurrent.setValue(dets.getCurrentFilePerc());
					lblOverallProgressVal.setText(dets.getProgress());
					lblUsageVal.setText(dets.getUsage());

					org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.ettrema.backup.BackupApplication.class).getContext().getResourceMap(BackupApplicationView.class);
					if (dets.isAllOk()) {
						lblStatus.setIcon(resourceMap.getIcon("lblStatusOk.icon")); // NOI18N
						lblStatus.setText("ShmeGO is protecting your files");
						isProblem = false;
					} else {
						lblStatus.setIcon(resourceMap.getIcon("lblStatusErr.icon"));
						lblStatus.setText("ShmeGO is not currently protecting your files");
						problemDescription = dets.getProblemDescription();
						isProblem = true;
					}
				} catch (Throwable e) {
					log.error("exception in doUpdateScreen", e);
				}
			}
		});

	}

	@Action
	public void scanNow() {
		_(FileWatcher.class).scanNow();
	}

	int getThrottlePerc() {
		return sldThrottle.getValue();
	}

	public void onAdded(Config t, Object parent) {
	}

	public void onRemoved(Config t, Object parent, Integer indexOf) {
	}

	public void onUpdated(final Config t, Object parent) {
		System.out.println("Config updated!!!");
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				System.out.println("BackupAppView - on updated");
				initQueueMenu();
				initDownloadMenu();
				initModifyAccountsMenu();
			}
		});
	}

	private void initQueueMenu() {
		log.debug("initQueueMenu");
		menuQueues.removeAll();
		for (final Repo r : config.getAllRepos()) {
			final Queue q = r.getQueue();
			JMenuItem menuItem = new JMenuItem(r.getDescription());
			menuItem.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					QueueView queueView = new QueueView(engine, eventManager, queueProcessor, q, r);
					queueView.setVisible(true);
				}
			});
			menuQueues.add(menuItem);

		}
	}

	private void initModifyAccountsMenu() {
		log.debug("initModifyAccountsMenu");
		System.out.println("init mod acc ---------------------------");
		menuModifyAccounts.removeAll();
		for (final Job j : config.getJobs()) {
			JMenuItem menuItem = new JMenuItem(j.getFirstDavRepo());
			menuItem.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					AccountView queueView = new AccountView(engine, eventManager, j, accountCreator, null);
					queueView.setVisible(true);
				}
			});
			menuModifyAccounts.add(menuItem);

		}
	}

	private void initDownloadMenu() {
		log.debug("initDownloadMenu");
		menuRestore.removeAll();
		for (final Job job : config.getJobs()) {
			for (final Repo r : job.getRepos()) {
				JMenu menuItem = new JMenu(r.getDescription());
				menuRestore.add(menuItem);

				//

				JMenuItem subMenuItem = new JMenuItem("Everything");
				subMenuItem.setToolTipText("Restore all files from " + r.getDescription() + " which aren't already on your computer");
				subMenuItem.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						RestoreView restoreView = new RestoreView(job, r);
						restoreView.setVisible(true);
					}
				});
				menuItem.add(subMenuItem);

				for (final Root root : job.getRoots()) {
					JMenuItem rootMenuItem = new JMenuItem(root.getRepoName());
					rootMenuItem.setToolTipText("Restore files from " + r.getDescription() + "/" + root.getRepoName() + " which aren't already on your computer");
					rootMenuItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							RestoreView restoreView = new RestoreView(job, r, root);
							restoreView.setVisible(true);
						}
					});
					menuItem.add(rootMenuItem);

				}
			}
		}
	}

	@Action
	public void showSettings() {
		// todo
	}

	@Action
	public void openMediaLounge() {
		String url = config.getMediaLoungeUrl();
		if( url != null ) {
			browserController.openUrl(url);
		} else {
			JOptionPane.showMessageDialog(progCurrent, "Can't open because you havent set up any web repositories");
		}
	}

	@Action
	public void doQuit() {
		System.exit(0);
	}

	@Action
	public void showConflicts() {
		ConflictView view = new ConflictView(engine);
		view.setVisible(true);
	}

	@Action
	public void showHistory() {
		HistoryView view = new HistoryView(historyDao, eventManager);
		view.setVisible(true);
	}
}
