package com.ettrema.backup.view;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelListener;
import javax.swing.event.EventListenerList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;

public class FileTreeModel implements TreeModel, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    protected EventListenerList listeners;
    private static final Object LEAF = new Serializable() {

        private static final long serialVersionUID = 1L;
    };
    private Map map = new HashMap();
    private File root;

    public FileTreeModel(File root) {
        this.root = root;

        if (!root.isDirectory()) {
            map.put(root, LEAF);
        }

        this.listeners = new EventListenerList();

        this.map = new HashMap();
    }

    public File getRoot() {
        return root;
    }

    public boolean isLeaf(Object node) {
        return map.get(node) == LEAF;
    }

    public int getChildCount(Object node) {
        List children = children(node);

        if (children == null) {
            return 0;
        }

        return children.size();
    }

    public File getChild(Object parent, int index) {
        return children(parent).get(index);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return children(parent).indexOf(child);
    }

    protected List<File> children(Object node) {
        File f = (File) node;

        Object value = map.get(f);

        if (value == LEAF) {
            return null;
        }

        List children = (List) value;

        if (children == null) {
            File[] c = f.listFiles();

            if (c != null) {
                children = new ArrayList(c.length);

                for (int len = c.length, i = 0; i < len; i++) {
                    File fChild = c[i];
                    if (fChild.isDirectory() && isBackupable(fChild) ) {
                        children.add(c[i]);
                    }
//                    if( !c[i].isDirectory() ) {
//                        map.put( c[i], LEAF );
//                    }
                }
            } else {
                children = new ArrayList(0);
            }

            map.put(f, children);
        }
        Collections.sort(children, new Comparator<File>() {

            public int compare(File o1, File o2) {
                return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
            }
        });
        return children;
    }

    public boolean isBackupable(File child) {
        File f = child;

        if (f.isHidden()) {
            return false;
        }
        if (f.getName().startsWith(".")) {
            return false;
        }
        if (f.getName().endsWith(".tmp")) {
            return false;
        }
        if (f.getName().startsWith("~")) {
            return false;
        }

        return true;
    }

    public void valueForPathChanged(TreePath path, Object value) {
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(TreeModelListener.class, l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(TreeModelListener.class, l);
    }

    @Override
    public Object clone() {
        try {
            FileTreeModel clone = (FileTreeModel) super.clone();

            clone.listeners = new EventListenerList();

            clone.map = new HashMap(map);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
