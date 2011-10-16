package com.ettrema.client;

import java.awt.Component;
import java.awt.datatransfer.Transferable;

/**
 *
 * @author j2ee
 */
public interface DragComponent {
  Component getTargetComponent();
  Transferable getSelectedForDrag();
}
