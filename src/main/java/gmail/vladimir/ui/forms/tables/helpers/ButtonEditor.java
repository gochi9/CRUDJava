package gmail.vladimir.ui.forms.tables.helpers;

import gmail.vladimir.ui.forms.tables.MongoTablesView;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

//Classes in this package are used for mongodb's table. Rows are are both resizable and they can be moved around.
public class ButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
    private JButton button;
    private String label;
    private int row;
    private JTable table;
    private List<Map<String, Object>> tableData;
    private MongoTablesView parent;

    public ButtonEditor(JCheckBox checkBox, String label, List<Map<String, Object>> tableData, MongoTablesView parent) {
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(this);
        this.label = label;
        button.setPreferredSize(new Dimension(75, 25));
        this.tableData = tableData;
        this.parent = parent;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        this.row = row;
        this.table = table;
        button.setText((value == null) ? "" : value.toString());
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return label;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fireEditingStopped();
        Map<String, Object> rowData = tableData.get(row);
        if (label.equals("Edit")) {
            parent.openEntryForm(rowData);
        } else if (label.equals("Remove")) {
            int confirmation = JOptionPane.showConfirmDialog(parent, "Are you sure you want to delete this entry?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                try {
                    parent.getTableManager().deleteEntry(rowData.get("_id"));
                    parent.updateSortDropdown();
                    parent.refreshData();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(parent, "Error deleting entry: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    parent.logger.error("Error deleting entry: {}", ex.getMessage());
                }
            }
        }
    }
}
