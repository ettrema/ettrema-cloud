package com.ettrema.client;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;

public class DragImage {

  public static void main(String args[]) {

    JFrame frame = new JFrame("Drag Image");
    frame.setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE);
    Container contentPane = frame.getContentPane();

    final Clipboard clipboard =
            frame.getToolkit().getSystemClipboard();

    final JLabel label = new JLabel();
//    Icon icon = new ImageIcon("/home/j2ee/Desktop/war.103.gif");
//    label.setIcon(icon);
    label.setTransferHandler(new ImageSelection());

    
    MouseListener mouseListener =
            new MouseAdapter() {

              public void mousePressed(MouseEvent e) {
                JComponent comp = (JComponent) e.getSource();
                TransferHandler handler =
                        comp.getTransferHandler();
                handler.exportAsDrag(
                        comp, e, TransferHandler.COPY);
              }
            };
    label.addMouseListener(mouseListener);

    JScrollPane pane = new JScrollPane(label);
    contentPane.add(pane, BorderLayout.CENTER);

    JButton copy = new JButton("Copy");
    copy.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        // fire TransferHandler's built-in copy 
        // action with a new actionEvent having 
        // "label" as the source
        Action copyAction =
                TransferHandler.getCopyAction();
        copyAction.actionPerformed(
                new ActionEvent(
                label, ActionEvent.ACTION_PERFORMED,
                (String) copyAction.getValue(Action.NAME),
                EventQueue.getMostRecentEventTime(),
                0));
      }
    });

    JButton clear = new JButton("Clear");
    clear.addActionListener(new ActionListener() {

      public void actionPerformed(
              ActionEvent actionEvent) {
        label.setIcon(null);
      }
    });

    JButton paste = new JButton("Paste");
    paste.addActionListener(new ActionListener() {

      public void actionPerformed(
              ActionEvent actionEvent) {
        // use TransferHandler's built-in 
        // paste action
        Action pasteAction =
                TransferHandler.getPasteAction();
        pasteAction.actionPerformed(
                new ActionEvent(label,
                ActionEvent.ACTION_PERFORMED,
                (String) pasteAction.getValue(Action.NAME),
                EventQueue.getMostRecentEventTime(),
                0));
      }
    });

    JPanel p = new JPanel();
    p.add(copy);
    p.add(clear);
    p.add(paste);
    contentPane.add(p, BorderLayout.SOUTH);

    frame.setSize(300, 300);
    frame.show();
  }
}
