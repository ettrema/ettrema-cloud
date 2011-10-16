package com.ettrema.client;

import javax.swing.JPanel;


/**
 *
 * @author mcevoyb
 */
public abstract class BaseDetailsPanel extends javax.swing.JPanel implements EventHandler{
    
    
        
    /** Textual representation of the type of object
     */
    protected String entityName;
            
    public abstract void populateScreen();
    
    
    public BaseDetailsPanel(String entityName) {
        this.entityName = entityName;
    }
        
    /** Must be called immediately after construction
     */     
    protected final void init() {        
        populateScreen();
    }

        
    public final JPanel getPanel() {
        return this;
    }    

    public String getTitle() {
        return entityName;
    }
    
    public final boolean onCancel() {
        populateScreen();
        return true;
    }        
}
