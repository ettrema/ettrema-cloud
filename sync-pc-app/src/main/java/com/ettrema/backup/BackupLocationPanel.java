
/*
 * BackupLocationPanel.java
 *
 * Created on 16/10/2010, 7:49:37 PM
 */
package com.ettrema.backup;

import com.ettrema.backup.config.Dir;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    /** Creates new form BackupLocationPanel */
    public BackupLocationPanel(JFrame parent, String text, File defaultDir) {
        this.parent = parent;
        initComponents();
        lblName.setText(text);
        if (defaultDir != null) {
            txtFolder.setText(defaultDir.getAbsolutePath());
        }
    }

    public void setDirectory(String s) {
        this.txtFolder.setText(s);
    }

    public void setExcludedFolders(List<File> excludedFolders) {
        this.excludedFolders = excludedFolders;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblName = new javax.swing.JLabel();
        txtFolder = new javax.swing.JTextField();
        btnBrowse = new javax.swing.JButton();
        btnExclude = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.ettrema.backup.BackupApplication.class).getContext().getResourceMap(BackupLocationPanel.class);
        setBackground(resourceMap.getColor("Form.background")); // NOI18N
        setMaximumSize(new java.awt.Dimension(570, 30));
        setMinimumSize(new java.awt.Dimension(570, 30));
        setName("Form"); // NOI18N
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(570, 30));
        setLayout(null);

        lblName.setFont(resourceMap.getFont("lblName.font")); // NOI18N
        lblName.setForeground(resourceMap.getColor("lblName.foreground")); // NOI18N
        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblName.setName("lblName"); // NOI18N
        lblName.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        add(lblName);
        lblName.setBounds(0, 3, 60, 16);

        txtFolder.setFont(resourceMap.getFont("txtFolder.font")); // NOI18N
        txtFolder.setForeground(resourceMap.getColor("txtFolder.foreground")); // NOI18N
        txtFolder.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtFolder.setMargin(new java.awt.Insets(2, 7, 2, 7));
        txtFolder.setName("txtFolder"); // NOI18N
        txtFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFolderActionPerformed(evt);
            }
        });
        add(txtFolder);
        txtFolder.setBounds(60, 0, 225, 25);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.ettrema.backup.BackupApplication.class).getContext().getActionMap(BackupLocationPanel.class, this);
        btnBrowse.setAction(actionMap.get("browse")); // NOI18N
        btnBrowse.setFont(resourceMap.getFont("btnBrowse.font")); // NOI18N
        btnBrowse.setText(resourceMap.getString("btnBrowse.text")); // NOI18N
        btnBrowse.setToolTipText(resourceMap.getString("btnBrowse.toolTipText")); // NOI18N
        btnBrowse.setName("btnBrowse"); // NOI18N
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });
        add(btnBrowse);
        btnBrowse.setBounds(290, 0, 120, 25);

        btnExclude.setAction(actionMap.get("showExlusions")); // NOI18N
        btnExclude.setFont(resourceMap.getFont("btnExclude.font")); // NOI18N
        btnExclude.setText(resourceMap.getString("btnExclude.text")); // NOI18N
        btnExclude.setToolTipText(resourceMap.getString("btnExclude.toolTipText")); // NOI18N
        btnExclude.setName("btnExclude"); // NOI18N
        btnExclude.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExcludeActionPerformed(evt);
            }
        });
        add(btnExclude);
        btnExclude.setBounds(410, 0, 90, 25);

        btnClear.setAction(actionMap.get("toggleEnabled")); // NOI18N
        btnClear.setFont(resourceMap.getFont("btnClear.font")); // NOI18N
        btnClear.setText(resourceMap.getString("btnClear.text")); // NOI18N
        btnClear.setToolTipText(resourceMap.getString("btnClear.toolTipText")); // NOI18N
        btnClear.setName("btnClear"); // NOI18N
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });
        add(btnClear);
        btnClear.setBounds(500, 0, 70, 25);
    }// </editor-fold>//GEN-END:initComponents

    private void txtFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFolderActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_txtFolderActionPerformed

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_btnBrowseActionPerformed

    private void btnExcludeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExcludeActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_btnExcludeActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_btnClearActionPerformed

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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowse;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnExclude;
    private javax.swing.JLabel lblName;
    private javax.swing.JTextField txtFolder;
    // End of variables declaration//GEN-END:variables

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
}
