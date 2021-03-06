package com.ettrema.cloudsync.view;

import com.ettrema.backup.config.Dir;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import com.ettrema.cloudsync.account.AccountUtils;
import com.ettrema.cloudsync.account.AccountUtils.DirType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.tree.TreePath;
import org.jdesktop.application.Action;

/**
 *
 * @author brad
 */
public class BackupLocationPanel extends javax.swing.JPanel {

    private static final long serialVersionUID = 1L;
    private final JFrame parent;
    private ExlusionsTreeModal modal;
    private List<File> excludedFolders;
    private AccountUtils.DirType dirType;

    public BackupLocationPanel() {
        this.parent = null;
        initComponents();
    }

    public String getLocationText() {
        return lblName.getText();
    }

    public void setLocationText(String s) {
        lblName.setText(s);
    }

    public DirType getDirType() {
        return dirType;
    }

    public void setDirType(DirType dirType) {
        this.dirType = dirType;
        File f = AccountUtils.getDefaultLocation(dirType);
        if (f != null) {
            txtFolder.setText(f.getAbsolutePath());
        }
    }

    public void setDirectory(String s) {
        this.txtFolder.setText(s);
    }

    public void setExcludedFolders(List<File> excludedFolders) {
        this.excludedFolders = excludedFolders;
    }

    @Action
    public void browse() {
        JFileChooser jc = new JFileChooser(root());
        jc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = jc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            txtFolder.setText(jc.getSelectedFile().getAbsolutePath());
        }
    }

    public File root() {
        String s = txtFolder.getText();
        File f = null;
        if (s != null && s.length() > 0) {
            f = new File(s);
            if (!f.exists()) {
                f = null;
            }
        }
        return f;
    }

    public boolean isValidDirectory() {
        String s = txtFolder.getText();
        if (s == null || s.length() == 0) {
            // empty is valid
            return true;
        }
        File f = null;
        if (s != null && s.length() > 0) {
            f = new File(s);
            if (!f.exists()) {
                return false;
            }
        }
        return true;
    }

    public String getDirectory() {
        return txtFolder.getText();
    }

    @Action
    public void showExlusions() {
        modal = new ExlusionsTreeModal(parent, root(), new Runnable() {

            public void run() {
                updateExclusions(modal.getCheckedPaths());
            }
        });
        modal.setChecked(excludedFolders);
        modal.setVisible(true);
    }

    private void updateExclusions(TreePath[] checkedPaths) {
        excludedFolders = new ArrayList<File>();
        for (TreePath tp : checkedPaths) {
            File f = toFile(tp);
            excludedFolders.add(f);
        }
    }

    private File toFile(TreePath tp) {
        return (File) tp.getLastPathComponent();
    }

    @Action
    public void toggleEnabled() {
        txtFolder.setText("");
    }

    public Root toRoot(String accountPath, Job job) {
        boolean en = txtFolder.getText().length() > 0;
        if (en) {
            Root root = new Root(txtFolder.getText(), accountPath + "/" + lblName.getText());
            root.setJob(job);
            List<Dir> excludedDirs = new ArrayList<Dir>();
            if (excludedFolders != null) {
                for (File f : excludedFolders) {
                    excludedDirs.add(new Dir(f.getAbsolutePath()));
                }
            }
            root.setExclusions(excludedDirs);
            return root;
        } else {
            return null;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblName = new javax.swing.JLabel();
        txtFolder = new javax.swing.JTextField();
        btnBrowse = new javax.swing.JButton();
        btnExclude = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();

        setOpaque(false);

        lblName.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblName.setForeground(new java.awt.Color(127, 176, 50));
        lblName.setText(org.openide.util.NbBundle.getMessage(BackupLocationPanel.class, "BackupLocationPanel.lblName.text")); // NOI18N

        txtFolder.setText(org.openide.util.NbBundle.getMessage(BackupLocationPanel.class, "BackupLocationPanel.txtFolder.text")); // NOI18N
        txtFolder.setMaximumSize(new java.awt.Dimension(2147483647, 22));

        btnBrowse.setText(org.openide.util.NbBundle.getMessage(BackupLocationPanel.class, "BackupLocationPanel.btnBrowse.text")); // NOI18N
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });

        btnExclude.setText(org.openide.util.NbBundle.getMessage(BackupLocationPanel.class, "BackupLocationPanel.btnExclude.text")); // NOI18N

        btnClear.setText(org.openide.util.NbBundle.getMessage(BackupLocationPanel.class, "BackupLocationPanel.btnClear.text")); // NOI18N
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lblName, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtFolder, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnBrowse)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnExclude)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnClear))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(txtFolder, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(lblName))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btnClear)
                .addComponent(btnExclude)
                .addComponent(btnBrowse))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        this.txtFolder.setText("");
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseActionPerformed
        JFileChooser fc;
        if (isValidDirectory()) {
            fc = new JFileChooser(this.getDirectory());
        } else {
            fc = new JFileChooser();
        }
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            txtFolder.setText(file.getAbsolutePath());
        }
    }//GEN-LAST:event_btnBrowseActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowse;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnExclude;
    private javax.swing.JLabel lblName;
    private javax.swing.JTextField txtFolder;
    // End of variables declaration//GEN-END:variables
}
