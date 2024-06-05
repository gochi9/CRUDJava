package gmail.vladimir.ui;

import gmail.vladimir.UserService;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TablesView extends JFrame {

    private final UserService userService;
    private final String tableName;
    private final Map<String, String> tableSchema;
    private final String primaryKeyColumn;

    private final JPanel dataPanel;

    public TablesView(UserService userService, String tableName, String databaseName, String url, String port, String username) throws SQLException {
        this.userService = userService;
        this.tableName = tableName;

        setTitle("CRUD Operations for Table: " + tableName);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        JPanel dbInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbInfoPanel.add(new JLabel("Database: " + databaseName));
        dbInfoPanel.add(new JLabel(" | URL: " + url));
        dbInfoPanel.add(new JLabel(" | Port: " + port));
        dbInfoPanel.add(new JLabel(" | User: " + username));
        dbInfoPanel.add(new JLabel(" | Table: " + tableName));
        infoPanel.add(dbInfoPanel);

        add(infoPanel, BorderLayout.NORTH);

        dataPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(dataPanel);
        scrollPane.setPreferredSize(new Dimension(780, 500));
        add(scrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton("Add Entry");
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(addButton);
        infoPanel.add(topPanel);

        addButton.addActionListener(e -> openEntryForm(null));

        tableSchema = userService.getTableSchema(tableName);
        primaryKeyColumn = tableSchema.keySet().iterator().next();
        refreshData();
    }

    private void refreshData() {
        dataPanel.removeAll();
        try {
            List<Map<String, Object>> data = userService.getTableData(tableName);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.NORTH; // Align to top

            int columnCount = tableSchema.keySet().size();
            int baseCellWidth = 100;
            int buttonWidth = 75;
            int totalRowWidth = columnCount * baseCellWidth + 2 * buttonWidth + 30; // 30 for padding

            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            headerPanel.setPreferredSize(new Dimension(totalRowWidth, 35));
            headerPanel.setBackground(Color.DARK_GRAY);

            for (String column : tableSchema.keySet()) {
                JLabel headerLabel = new JLabel(column);
                headerLabel.setForeground(Color.WHITE);
                headerLabel.setPreferredSize(new Dimension(baseCellWidth, 25));
                headerPanel.add(headerLabel);
            }

            dataPanel.add(headerPanel, gbc);
            gbc.gridy++;

            JSeparator darkSeparator = new JSeparator(SwingConstants.HORIZONTAL);
            darkSeparator.setBackground(Color.BLACK);
            darkSeparator.setPreferredSize(new Dimension(totalRowWidth, 5));
            dataPanel.add(darkSeparator, gbc);
            gbc.gridy++;

            for (Map<String, Object> row : data) {
                JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2)); // Reduced spacing
                rowPanel.setPreferredSize(new Dimension(totalRowWidth, 35));

                for (String column : tableSchema.keySet()) {
                    JLabel cell = new JLabel(String.valueOf(row.get(column)));
                    cell.setPreferredSize(new Dimension(baseCellWidth, 25));
                    rowPanel.add(cell);
                }

                JButton editButton = new JButton("Edit");
                editButton.setPreferredSize(new Dimension(buttonWidth, 25));
                JButton removeButton = new JButton("Remove");
                removeButton.setPreferredSize(new Dimension(buttonWidth, 25));

                editButton.addActionListener(e -> openEntryForm(row));

                removeButton.addActionListener(e -> {
                    int confirmation = JOptionPane.showConfirmDialog(TablesView.this, "Are you sure you want to delete this entry?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirmation == JOptionPane.YES_OPTION) {
                        try {
                            userService.deleteData(tableName, primaryKeyColumn, row.get(primaryKeyColumn));
                            refreshData();
                        } catch (SQLException ex) {
                            JOptionPane.showMessageDialog(TablesView.this, "Error deleting entry: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                        }
                    }
                });

                rowPanel.add(editButton);
                rowPanel.add(Box.createRigidArea(new Dimension(10, 0)));
                rowPanel.add(removeButton);

                dataPanel.add(rowPanel, gbc);
                gbc.gridy++;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                dataPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
                gbc.gridy++;
            }

            gbc.weighty = 1;
            dataPanel.add(new JPanel(), gbc);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        dataPanel.revalidate();
        dataPanel.repaint();
    }

    private void openEntryForm(Map<String, Object> existingData) {
        JDialog dialog = new JDialog(this, "Entry Form", true);
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Map<String, JComponent> fieldMap = new HashMap<>();

        int row = 0;
        for (String column : tableSchema.keySet()) {
            gbc.gridx = 0;
            gbc.gridy = row;
            formPanel.add(new JLabel(column), gbc);

            gbc.gridx = 1;
            String type = tableSchema.get(column);
            JComponent inputField = getInputField(column, type, existingData != null);

            if(existingData != null) {
                if (inputField instanceof JTextField)
                    ((JTextField) inputField).setText(String.valueOf(existingData.get(column)));
                else if (inputField instanceof JCheckBox)
                    ((JCheckBox) inputField).setSelected((Boolean) existingData.get(column));
                else if (inputField instanceof JSpinner)
                    ((JSpinner) inputField).setValue(existingData.get(column));
                else if (inputField instanceof JDatePickerImpl) {
                    java.util.Date selectedDate = (java.util.Date) existingData.get(column);
                    ((UtilDateModel) ((JDatePickerImpl) inputField).getModel()).setValue(selectedDate);
                }
            }
            formPanel.add(inputField, gbc);
            fieldMap.put(column, inputField);
            row++;
        }

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton saveButton = new JButton("Save");
        formPanel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            Map<String, Object> newData = new HashMap<>();
            for (String column : fieldMap.keySet()) {
                JComponent inputField = fieldMap.get(column);
                if (inputField instanceof JTextField)
                    newData.put(column, ((JTextField) inputField).getText());
                else if (inputField instanceof JCheckBox)
                    newData.put(column, ((JCheckBox) inputField).isSelected());
                else if (inputField instanceof JSpinner)
                    newData.put(column, ((JSpinner) inputField).getValue());
                else if (inputField instanceof JDatePickerImpl) {
                    java.util.Date selectedDate = (java.util.Date) ((JDatePickerImpl) inputField).getModel().getValue();
                    newData.put(column, new SimpleDateFormat("yyyy-MM-dd").format(selectedDate));
                }
            }

            try {
                if (existingData == null)
                    userService.insertData(tableName, newData);
                else
                    userService.updateData(tableName, newData, primaryKeyColumn, existingData.get(primaryKeyColumn));

                refreshData();
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setPreferredSize(new Dimension(380, 360));
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private JComponent getInputField(String column, String type, boolean isEdit) {
        switch (type) {
            case "INT":
                if (column.equalsIgnoreCase(primaryKeyColumn)) {
                    JTextField field = new JTextField();
                    field.setEnabled(false); // Disable primary key field
                    return field;
                }
                else
                    return new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
            case "BOOLEAN":
                return new JCheckBox();
            case "DATE":
                UtilDateModel model = new UtilDateModel();
                if (!isEdit) {
                    Calendar calendar = Calendar.getInstance();
                    model.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                    model.setSelected(true);
                }
                Properties p = new Properties();
                p.put("text.today", "Today");
                p.put("text.month", "Month");
                p.put("text.year", "Year");
                JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
                return new JDatePickerImpl(datePanel, new org.jdatepicker.impl.DateComponentFormatter());
            case "DECIMAL":
                return new JSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.01));
            default:
                return new JTextField();
        }
    }
}