package com.ettrema.client;

import java.awt.*;
import java.awt.image.*;
import java.awt.datatransfer.*;
import java.io.*;
import javax.swing.*;

public class ImageSelection extends TransferHandler {

  private static final DataFlavor flavors[] =
          {DataFlavor.imageFlavor};

  public int getSourceActions(JComponent c) {
    return TransferHandler.COPY;
  }

  public boolean canImport(
          JComponent comp, DataFlavor flavor[]) {
    if (!(comp instanceof JLabel)) {
      return false;
    }
    for (int i = 0,  n = flavor.length; i < n; i++) {
      for (int j = 0,  m = flavors.length; j < m; j++) {
        if (flavor[i].equals(flavors[j])) {
          return true;
        }
      }
    }
    return false;
  }

  public Transferable createTransferable(
          JComponent comp) {

    if (comp instanceof JLabel) {
      JLabel label = (JLabel) comp;
      Icon icon = label.getIcon();
      if (icon instanceof ImageIcon) {
        final Image image = ((ImageIcon) icon).getImage();
        final JLabel source = label;
        Transferable transferable =
                new Transferable() {

                  public Object getTransferData(
                          DataFlavor flavor) {
                    if (isDataFlavorSupported(flavor)) {
                      return image;
                    }
                    return null;
                  }

                  public DataFlavor[] getTransferDataFlavors() {
                    return flavors;
                  }

                  public boolean isDataFlavorSupported(
                          DataFlavor flavor) {
                    return flavor.equals(
                            DataFlavor.imageFlavor);
                  }
                };
        return transferable;
      }
    }
    return null;
  }

  public boolean importData(
          JComponent comp, Transferable t) {
    if (comp instanceof JLabel) {
      JLabel label = (JLabel) comp;
      if (t.isDataFlavorSupported(flavors[0])) {
        try {
          Image image = (Image) t.getTransferData(flavors[0]);
          ImageIcon icon = new ImageIcon(image);
          label.setIcon(icon);
          return true;
        } catch (UnsupportedFlavorException ignored) {
        } catch (IOException ignored) {
        }
      }
    }
    return false;
  }
}
