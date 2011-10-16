package com.ettrema.client;

import java.awt.datatransfer.Transferable;

/**
 *
 * @author mcevoyb
 */
public interface Droppable {

    public boolean acceptCopyDrop(Transferable transferable);
    
    boolean canPerformMove(Transferable transferable);

    boolean canPerformCopy(Transferable transferable);
    
    boolean acceptMoveDrop(Transferable transferable);
}
