/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettrema.cloudsync.view;

import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.cloudsync.account.AccountCreator;
import com.ettrema.httpclient.ProxyDetails;
import java.net.ConnectException;
import javax.swing.JOptionPane;

/**
 *
 * @author brad
 */
public class ExistingUserPanel extends javax.swing.JPanel {

    private AccountCreator accountCreator;
    private ProxyDetails proxyDetails;

    /**
     * Creates new form ExistingUserPanel
     */
    public ExistingUserPanel(AccountCreator accountCreator, ProxyDetails proxyDetails) {
        this.proxyDetails = proxyDetails;
        initComponents();
        this.accountCreator = accountCreator;
    }

    public ExistingUserPanel() {
        initComponents();
    }

    public void setAccountCreator(AccountCreator accountCreator) {
        this.accountCreator = accountCreator;
    }

    public AccountCreator getAccountCreator() {
        return accountCreator;
    }

    public void setProxyDetails(ProxyDetails proxyDetails) {
        this.proxyDetails = proxyDetails;
    }

    public ProxyDetails getProxyDetails() {
        return proxyDetails;
    }

    String getHostName() {
        return txtHost.getText();
    }

    String getAccountName() {
        return txtAccountName.getText();
    }

    String getPassword() {
        return txtPassword.getText();
    }

    public boolean validateStep() {
        try {
            String err = accountCreator.validateHost(getHostName());
            if (err != null) {
                showValidation(err);
                return false;
            }
            err = accountCreator.validateName(getAccountName());
            if (err != null) {
                showValidation(err);
                return false;
            }
            if (!accountCreator.checkExists(getHostName(), getAccountName(), proxyDetails)) {
                // TODO: check password
                showValidation("That account was not found, please check your account name");
                return false;
            }
            err = accountCreator.validatePassword(getPassword());
            if (err != null) {
                showValidation(err);
                return false;
            }
            return true;
        } catch (ConnectException ex) {
            showValidation("Couldnt connect to the website, please check that you are online");
            return false;
        }
    }

    private void showValidation(String err) {
        JOptionPane.showMessageDialog(this, err, "Validation", JOptionPane.ERROR_MESSAGE);
    }

    public void load(Job job) {
        if (job == null) {
            return;
        }
        for (Repo r : job.getRepos()) {
            if (r instanceof DavRepo) {
                initRepo((DavRepo) r);
            }
        }
    }

    private void initRepo(DavRepo davRepo) {
        String hostName = formatHostForEdit(davRepo);
        this.txtHost.setText(hostName);
        this.txtAccountName.setText(davRepo.getUser());
        this.txtPassword.setText(davRepo.getPwd());
    }

    public void save(String accPath, Job job) {
        for (Repo r : job.getRepos()) {
            if (r instanceof DavRepo) {
                saveRepo(accPath, (DavRepo) r);
            }
        }
    }

    private void saveRepo(String accPath, DavRepo davRepo) {
        String[] arr = txtHost.getText().split(":");
        davRepo.setHostName(arr[0]);
        if (arr.length > 1) {
            int port = Integer.parseInt(arr[1]);
            davRepo.setPort(port);
        } else {
            davRepo.setPort(80);
        }
        davRepo.setUser(txtAccountName.getText());
        davRepo.setPwd(txtPassword.getPassword());
        davRepo.setRootPath(accPath);

    }

    private String formatHostForEdit(DavRepo davRepo) {
        if (davRepo.getPort() == 80) {
            return davRepo.getHostName();
        } else {
            return davRepo.getHostName() + ":" + davRepo.getPort();
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

        lblServer = new javax.swing.JLabel();
        lblServer1 = new javax.swing.JLabel();
        lblServer2 = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        txtHost = new javax.swing.JTextField();
        txtAccountName = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();

        setBackground(new java.awt.Color(255, 255, 255));

        lblServer.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        lblServer.setForeground(new java.awt.Color(127, 176, 50));
        lblServer.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblServer.setText(org.openide.util.NbBundle.getMessage(ExistingUserPanel.class, "ExistingUserPanel.lblServer.text")); // NOI18N

        lblServer1.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        lblServer1.setForeground(new java.awt.Color(127, 176, 50));
        lblServer1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblServer1.setText(org.openide.util.NbBundle.getMessage(ExistingUserPanel.class, "ExistingUserPanel.lblServer1.text")); // NOI18N

        lblServer2.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        lblServer2.setForeground(new java.awt.Color(127, 176, 50));
        lblServer2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblServer2.setText(org.openide.util.NbBundle.getMessage(ExistingUserPanel.class, "ExistingUserPanel.lblServer2.text")); // NOI18N

        txtHost.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtHost.setText(org.openide.util.NbBundle.getMessage(ExistingUserPanel.class, "ExistingUserPanel.txtHost.text")); // NOI18N
        txtHost.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtHostActionPerformed(evt);
            }
        });

        txtAccountName.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtAccountName.setText(org.openide.util.NbBundle.getMessage(ExistingUserPanel.class, "ExistingUserPanel.txtAccountName.text")); // NOI18N
        txtAccountName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAccountNameActionPerformed(evt);
            }
        });

        txtPassword.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtPassword.setText(org.openide.util.NbBundle.getMessage(ExistingUserPanel.class, "ExistingUserPanel.txtPassword.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblServer2, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblServer, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblServer1, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, 9, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtHost, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                    .addComponent(txtAccountName)
                    .addComponent(txtPassword))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblServer))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtAccountName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblServer1, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblServer2)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(filler1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtHostActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtHostActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtHostActionPerformed

    private void txtAccountNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtAccountNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAccountNameActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.JLabel lblServer;
    private javax.swing.JLabel lblServer1;
    private javax.swing.JLabel lblServer2;
    private javax.swing.JTextField txtAccountName;
    private javax.swing.JTextField txtHost;
    private javax.swing.JPasswordField txtPassword;
    // End of variables declaration//GEN-END:variables
}
