package com.ettrema.backup.view;

import com.ettrema.backup.config.Queue;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.utils.TimeUtils;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author brad
 */
public class QueueTableModel2 extends DefaultTableModel implements Observer<QueueItem, Queue> {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(QueueTableModel.class);
    private static final long serialVersionUID = 1L;
    private final Queue q;
    private final Map<QueueItem, Integer> mapOfRowIds = new HashMap<QueueItem, Integer>();
    private int rowCount;

    public QueueTableModel2(Queue q) {
        super(new String[]{"Path", "Size",  "Action", "Status", "Started", "Completed", "Notes"}, 0);
        this.q = q;
        for (QueueItem item : q) {
            addRow(item);
        }
        this.fireTableDataChanged();
        q.addObserver(this);
    }

    private int addRow(QueueItem item) {
        this.addRow(toRow(item));
        int newRowId = rowCount++;
        mapOfRowIds.put(item, newRowId);
        return newRowId;
    }

    private Object[] toRow(QueueItem item) {
        Object[] data = new Object[7];
        data[0] = item.getFile().getAbsolutePath();
        data[1] = TimeUtils.formatBytes(item.getBytesToUpload());
        data[2] = getAction(item);
        data[3] = getStatus(item);
        data[4] = item.getStarted();
        data[5] = item.getCompleted();
        data[6] = getNotes(item);
        return data;
    }

    private String getStatus(QueueItem item) {
        String s;
        if (item.getCompleted() != null) {
            return "Completed";
        } else {
            if (item.getStarted() != null) {
                return "Running...";
            } else {
                return "Waiting... ";
            }
        }
    }

    private Object getModDate(QueueItem item) {
        return new Date(item.getLastModified());
    }

    private String getAction(QueueItem item) {
        return item.getActionDescription();
    }

    public void onAdded(QueueItem t, Queue parent) {
        log.debug("onAdded");
        addRow(t);
    }

    public void onRemoved(QueueItem t, Queue parent, Integer row) {
        log.trace("onRemoved: " + t.getLastModified());
        update(t);
    }

    public void onUpdated(QueueItem t, Queue parent) {
        System.out.println("updated: " + t.getActionDescription());
        log.trace("onUpdated: " + t.getLastModified());
        update(t);
    }

    private void update(final QueueItem t) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Integer row = mapOfRowIds.get(t);
                log.trace("update: " + row);
                if (row == null) {
                    log.debug("row not found: " + t.getActionDescription());
                } else {
                    removeRow(row);
                    insertRow(row, toRow(t));
                }
            }
        });
    }

    private Object getNotes(QueueItem item) {
        return item.getNotes();
    }
}
