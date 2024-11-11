package gmail.vladimir.managers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class SchemaBuilder {

    public String buildSchemaFromGUI(DefaultTableModel schemaTableModel) {
        StringBuilder sb = new StringBuilder();
        String primaryKeyColumn = null;

        if (schemaTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "Please add at least one column.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String tableName = JOptionPane.showInputDialog(null, "Enter Table Name:", "Table Name", JOptionPane.PLAIN_MESSAGE);
        if (tableName == null || tableName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Table name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        tableName = tableName.trim();

        for (int i = 0; i < schemaTableModel.getRowCount(); i++) {
            String columnName = schemaTableModel.getValueAt(i, 0).toString().trim();
            String dataType = schemaTableModel.getValueAt(i, 1).toString().trim();
            Boolean isPrimaryKey = (Boolean) schemaTableModel.getValueAt(i, 2);

            if (columnName.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Column name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            if (dataType.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Data type cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            sb.append(columnName).append(" ").append(dataType);
            if (isPrimaryKey != null && isPrimaryKey) {
                primaryKeyColumn = columnName;
                sb.append(" PRIMARY KEY");
            }
            sb.append(", ");
        }

        if (sb.length() >= 2)
            sb.setLength(sb.length() - 2);


        String fullSchema = String.format("CREATE TABLE %s (%s);", tableName, sb.toString());
        return fullSchema;
    }

    public void enforceSinglePrimaryKey(DefaultTableModel model, int rowIndex) {
        Boolean isPrimaryKey = (Boolean) model.getValueAt(rowIndex, 2);
        if (!(isPrimaryKey != null && isPrimaryKey))
            return;

        for (int i = 0; i < model.getRowCount(); i++)
            if (i != rowIndex)
                model.setValueAt(Boolean.FALSE, i, 2);
    }
}
