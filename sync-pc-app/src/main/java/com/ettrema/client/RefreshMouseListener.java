package com.ettrema.client;

import com.ettrema.backup.App;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author mcevoyb
 */
public class RefreshMouseListener extends AbstractMouseListener {

    public static void add(JPopupMenu popup, AbstractTreeNode node) {
        JMenuItem item = new JMenuItem("Refresh");
        item.addMouseListener( new RefreshMouseListener(node) );
        popup.add(item);
    }
        
    public RefreshMouseListener(AbstractTreeNode node) {
        super(node);
    }
    
    public void onClick() {
        node.flushChildren();
        DefaultTreeModel model = (DefaultTreeModel) App.current().getBrowser().tree().getModel();
        model.nodeStructureChanged(node);
    }    
}
