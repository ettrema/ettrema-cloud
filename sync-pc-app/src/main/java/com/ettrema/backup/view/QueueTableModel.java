package com.ettrema.backup.view;

import com.ettrema.backup.config.Queue;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.observer.Observer;
import com.ettrema.backup.utils.TimeUtils;
import java.util.Date;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author brad
 */
public class QueueTableModel extends AbstractTableModel implements Observer<QueueItem, Queue> {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( QueueTableModel.class );
    private static final long serialVersionUID = 1L;
    private final Queue q;

    public QueueTableModel(Queue q) {
        this.q = q;
        q.addObserver( this );
    }

    public int getRowCount() {
        return q.size();
    }

    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName( int column ) {
        switch( column ) {
            case 0:
                return "Path";
            case 1:
                return "Size";
            case 2:
                return "Modified Date";
            case 3:
                return "Action";
        }
        throw new RuntimeException( "unknown column: " + column );
    }

    public Object getValueAt( int row, int column ) {
        QueueItem item = getRowItem( row );
        switch( column ) {
            case 0:
                return item.getFile().getAbsolutePath();
            case 1:
                return TimeUtils.formatBytes( item.getBytesToUpload());

            case 2:
                return getModDate( item );

            case 3:
                return getAction( item );

        }
        throw new RuntimeException( "unknown column: " + column );
    }

    private QueueItem getRowItem( int row ) {
        QueueItem qi = q.item( row );
        return qi;
    }



    private Object getModDate( QueueItem item ) {
        return new Date( item.getLastModified() );
    }

    public void onAdded( QueueItem t, Queue parent ) {
        int row = q.indexOf( t );
        fireTableRowsInserted( row, row );
    }

    public void onRemoved( QueueItem t, Queue parent, Integer row ) {
        log.trace("onRemoved: " + row);
        this.fireTableRowsDeleted( row, row );
    }

    public void onUpdated( QueueItem t, Queue parent ) {
        int row = q.indexOf( t );
        log.trace("onUpdated: " + row);
        fireTableRowsUpdated( row, row );
    }

    private String getAction( QueueItem item ) {
        return item.getActionDescription();
    }
}
