package com.ettrema.client;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 *
 * @author mcevoyb
 */
public abstract class AbstractMouseListener implements MouseListener{
    
    protected final AbstractTreeNode node;
    
    public abstract void onClick();
    
    public AbstractMouseListener() {
        this.node = null;
    }

    public AbstractMouseListener(AbstractTreeNode node) {
        if( node == null ) throw new NullPointerException();
        this.node = node;
    }

    protected void doEvent() {
      onClick();
    }
    
    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        doEvent();        
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
