/*
 * ExistingUserPanel.java
 *
 * Created on 16/10/2010, 11:44:09 PM
 */

package com.ettrema.backup;

import com.ettrema.backup.account.AccountCreator;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Repo;
import com.ettrema.httpclient.ProxyDetails;
import java.net.ConnectException;
import javax.swing.JOptionPane;

/**
 *
 * @author brad
 */
public class ExistingUserPanel extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;

    private AccountCreator accountCreator;
    private ProxyDetails proxyDetails;


    /** Creates new form ExistingUserPanel */
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
        
    
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        txtAccountName = new javax.swing.JTextField();
        txtHost = new javax.swing.JTextField();
        lblHost = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        jPanel1.setName("jPanel1"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.ettrema.backup.BackupApplication.class).getContext().getResourceMap(ExistingUserPanel.class);
        setBackground(resourceMap.getColor("Form.background")); // NOI18N
        setMaximumSize(new java.awt.Dimension(590, 100));
        setMinimumSize(new java.awt.Dimension(590, 100));
        setName("Form"); // NOI18N
        setLayout(null);

        jLabel10.setFont(resourceMap.getFont("jLabel10.font")); // NOI18N
        jLabel10.setForeground(resourceMap.getColor("jLabel10.foreground")); // NOI18N
        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel10.setName("jLabel10"); // NOI18N
        jLabel10.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        add(jLabel10);
        jLabel10.setBounds(80, 50, 90, 15);

        jLabel14.setFont(resourceMap.getFont("jLabel14.font")); // NOI18N
        jLabel14.setForeground(resourceMap.getColor("jLabel14.foreground")); // NOI18N
        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel14.setName("jLabel14"); // NOI18N
        jLabel14.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        add(jLabel14);
        jLabel14.setBounds(80, 90, 90, 15);

        txtPassword.setFont(resourceMap.getFont("txtPassword.font")); // NOI18N
        txtPassword.setText(resourceMap.getString("txtPassword.text")); // NOI18N
        txtPassword.setName("txtPassword"); // NOI18N
        add(txtPassword);
        txtPassword.setBounds(260, 80, 202, 25);

        txtAccountName.setFont(resourceMap.getFont("txtAccountName.font")); // NOI18N
        txtAccountName.setText(resourceMap.getString("txtAccountName.text")); // NOI18N
        txtAccountName.setName("txtAccountName"); // NOI18N
        add(txtAccountName);
        txtAccountName.setBounds(260, 50, 202, 25);

        txtHost.setFont(resourceMap.getFont("txtHost.font")); // NOI18N
        txtHost.setName("txtHost"); // NOI18N
        add(txtHost);
        txtHost.setBounds(260, 20, 202, 25);

        lblHost.setFont(resourceMap.getFont("lblHost.font")); // NOI18N
        lblHost.setForeground(resourceMap.getColor("lblHost.foreground")); // NOI18N
        lblHost.setText(resourceMap.getString("lblHost.text")); // NOI18N
        lblHost.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblHost.setName("lblHost"); // NOI18N
        lblHost.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        add(lblHost);
        lblHost.setBounds(80, 20, 110, 15);

        jLabel1.setIcon(resourceMap.getIcon("jLabel1.icon")); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel1.setMaximumSize(new java.awt.Dimension(591, 120));
        jLabel1.setMinimumSize(new java.awt.Dimension(591, 120));
        jLabel1.setName("jLabel1"); // NOI18N
        jLabel1.setPreferredSize(new java.awt.Dimension(591, 120));
        add(jLabel1);
        jLabel1.setBounds(0, 0, 592, 121);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblHost;
    private javax.swing.JTextField txtAccountName;
    private javax.swing.JTextField txtHost;
    private javax.swing.JPasswordField txtPassword;
    // End of variables declaration//GEN-END:variables

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
            String err = accountCreator.validateHost( getHostName() );
            if( err != null ) {
                showValidation( err );
                return false;
            }			
            err = accountCreator.validateName( getAccountName() );
            if( err != null ) {
                showValidation( err );
                return false;
            }
            if( !accountCreator.checkExists( getHostName(), getAccountName(), proxyDetails ) ) {
                // TODO: check password
                showValidation( "That account was not found, please check your account name" );
                return false;
            }
            err = accountCreator.validatePassword( getPassword() );
            if( err != null ) {
                showValidation( err );
                return false;
            }
            return true;
        } catch( ConnectException ex ) {
            showValidation( "Couldnt connect to the website, please check that you are online" );
            return false;
        }
    }

    private void showValidation(String err) {
        JOptionPane.showMessageDialog( this, err, "Validation", JOptionPane.ERROR_MESSAGE);
    }

    public void load(Job job) {
        if( job == null ) {
            return ;
        }
        for( Repo r : job.getRepos() ) {
            if( r instanceof DavRepo ) {
                initRepo((DavRepo)r);
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
        for( Repo r : job.getRepos() ) {
            if( r instanceof DavRepo ) {
                saveRepo(accPath, (DavRepo)r);
            }
        }        
    }

    private void saveRepo(String accPath, DavRepo davRepo) {
		String[] arr = txtHost.getText().split(":");
		davRepo.setHostName(arr[0]);
		if( arr.length > 1) {
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
		if( davRepo.getPort() == 80) {
			return davRepo.getHostName();
		} else {
			return davRepo.getHostName() + ":" + davRepo.getPort();
		}
	}

}
