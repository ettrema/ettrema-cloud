package com.ettrema.client;

import javax.swing.JPanel;

/**
 * Defines an event handler for a BuildDialog events.
 * @author hollomg
 */
public interface EventHandler {
 
  /**   Return the panel with the UI for this dialog. Usually, just return this
   *
   *    Should register any drop target components with the transfer handler
   */
  JPanel getPanel();
  
  /**   return the text of the title for a dialog box which will show these details
   */
  public String getTitle();
}
