package com.ettrema.backup;

import com.ettrema.backup.utils.TimeUtils;
import com.ettrema.backup.view.QueueTableModel2;
import java.awt.Component;
import java.util.Date;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author brad
 */
public class TableUtils {
    public static void setTimeColumn( JTable table, String columnName ) {
        table.getColumn(columnName).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Date dt = (Date)value;
                setText(TimeUtils.formatTime( dt ));
                return this;
            }
        });
    }

    
    public static void setDateTimeColumn( JTable table, String columnName ) {
        table.getColumn(columnName).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Date dt = (Date)value;
                setText(TimeUtils.formatDateTime( dt ));
                return this;
            }
        });
    }
    
    public static  void setByteSizeColumn( JTable table, String columnName ) {
        table.getColumn(columnName).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Long val;
                if( value instanceof Long ) {
                    val = (Long)value;
                } else if( value instanceof Integer ) {
                    int i = ((Integer)value);
                    val = (long)i;
                } else {
                    val = null;
                }
                setText(TimeUtils.formatBytes( val ));
                return this;
            }
        });
    }
}
