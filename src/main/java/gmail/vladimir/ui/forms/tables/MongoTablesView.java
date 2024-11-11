package gmail.vladimir.ui.forms.tables;

import gmail.vladimir.db.IDatabaseService;
import gmail.vladimir.managers.MongoTableManager;
import gmail.vladimir.ui.forms.tables.helpers.ButtonEditor;
import gmail.vladimir.ui.forms.tables.helpers.ButtonRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MongoTablesView extends AbstractTablesView {
    public static final Logger logger = LoggerFactory.getLogger(MongoTablesView.class);
    private final MongoTableManager tableManager;
    private JComboBox<String> sortDropdown;

    public MongoTablesView(IDatabaseService dbService, String tableName) {
        super(dbService, tableName);
        this.tableManager = new MongoTableManager(dbService, tableName);
        setupSortDropdown();
        setupButtonPanel();
        updateSortDropdown();
        refreshData();
    }

    public MongoTableManager getTableManager() {
        return tableManager;
    }

    private void setupButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addEntryButton = new JButton("Add Entry");
        addEntryButton.addActionListener(e -> openEntryForm(null));

        JButton massActionButton = new JButton("Mass Action");
        massActionButton.addActionListener(e -> openMassActionDialog());

        buttonPanel.add(addEntryButton);
        buttonPanel.add(massActionButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupSortDropdown() {
        sortDropdown = new JComboBox<>();
        sortDropdown.addActionListener(e -> refreshData());
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sortPanel.add(new JLabel("Sort/Filter By: "));
        sortPanel.add(sortDropdown);
        add(sortPanel, BorderLayout.NORTH);
    }

    public void updateSortDropdown() {
        sortDropdown.removeAllItems();
        try {
            Set<String> individualFields = new TreeSet<>();
            Set<String> fieldCombinations = new TreeSet<>();
            fieldCombinations.add("ALL");

            List<Map<String, Object>> tableData = tableManager.getTableData();

            for (Map<String, Object> entry : tableData) {
            for (String field : entry.keySet())
                if (!field.equals("_id"))
                    individualFields.add(field);

                String combination = entry.keySet().stream()
                        .filter(field -> !field.equals("_id"))
                        .sorted()
                        .collect(Collectors.joining(", "));
                fieldCombinations.add(combination);
            }

            sortDropdown.addItem("ALL");
            individualFields.forEach(field -> sortDropdown.addItem("Field: " + field));
            fieldCombinations.stream()
                    .filter(combination -> !combination.equals("ALL"))
                    .forEach(combination -> sortDropdown.addItem("Fields: " + combination));
        }
        catch (SQLException ex) {
            logger.error("Error fetching entries for sort dropdown: {}", ex.getMessage());
        }
    }

    @Override
    public void refreshData() {
        String selectedSort = (String) sortDropdown.getSelectedItem();
        dataPanel.removeAll();
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            List<Map<String, Object>> tableData = tableManager.getSortedData(selectedSort);
            LinkedHashSet<String> allFields = new LinkedHashSet<>();
            for (Map<String, Object> row : tableData)
                allFields.addAll(row.keySet());

            if (selectedSort != null && !selectedSort.equals("ALL")) {
                List<String> selectedFields = parseSelectedSortFields(selectedSort);
                allFields.retainAll(selectedFields);
            }

            List<String> fieldList = new ArrayList<>(allFields);

            DefaultTableModel tableModel = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column >= fieldList.size();
                }
            };

            fieldList.forEach(tableModel::addColumn);
            tableModel.addColumn("Edit");
            tableModel.addColumn("Remove");

            for (Map<String, Object> rowData : tableData) {
                Object[] rowValues = new Object[fieldList.size() + 2];
                int i = 0;
                for (String field : fieldList) {
                    Object value = rowData.get(field);
                    rowValues[i++] = value != null ? value.toString() : "-";
                }
                rowValues[i++] = "Edit";
                rowValues[i++] = "Remove";
                tableModel.addRow(rowValues);
            }

            JTable table = new JTable(tableModel);
            table.setFillsViewportHeight(true);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            for (int i = 0; i < fieldList.size(); i++) {
                TableColumn column = table.getColumnModel().getColumn(i);
                column.setPreferredWidth(100);
            }

            int buttonColumnIndex = fieldList.size();
            int removeColumnIndex = buttonColumnIndex + 1;

            TableColumn editColumn = table.getColumnModel().getColumn(buttonColumnIndex);
            TableColumn removeColumn = table.getColumnModel().getColumn(removeColumnIndex);

            editColumn.setPreferredWidth(75);
            removeColumn.setPreferredWidth(75);

            table.getColumnModel().getColumn(buttonColumnIndex).setCellRenderer(new ButtonRenderer());
            table.getColumnModel().getColumn(buttonColumnIndex).setCellEditor(new ButtonEditor(new JCheckBox(), "Edit", tableData, this));

            table.getColumnModel().getColumn(removeColumnIndex).setCellRenderer(new ButtonRenderer());
            table.getColumnModel().getColumn(removeColumnIndex).setCellEditor(new ButtonEditor(new JCheckBox(), "Remove", tableData, this));

            table.setRowHeight(30);

            JScrollPane scrollPane = new JScrollPane(table);
            dataPanel.setLayout(new BorderLayout());
            dataPanel.add(scrollPane, BorderLayout.CENTER);

            for (int i = 0; i < table.getColumnCount(); i++) {
                String columnName = table.getColumnName(i);
                if (!columnName.equals("Edit") && !columnName.equals("Remove") && !columnName.isEmpty())
                    table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }

        }
        catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Error fetching data: {}", ex.getMessage());
        }

        dataPanel.revalidate();
        dataPanel.repaint();
    }

    private List<String> parseSelectedSortFields(String selectedSort) {
        if (selectedSort.startsWith("Field: "))
            return Collections.singletonList(selectedSort.substring(7));
        else if (selectedSort.startsWith("Fields: "))
            return Arrays.asList(selectedSort.substring(8).split(",\\s*"));
        return new ArrayList<>();
    }

    @Override
    public void openEntryForm(Object existingData) {
        Map<String, Object> entryData = existingData != null ? (Map<String, Object>) existingData : null;
        JDialog dialog = new JDialog(this, entryData == null ? "Add Entry" : "Edit Entry", true);
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addFieldButton = new JButton("Add Field");
        JButton loadFromEntryButton = new JButton("Load from Existing Entry");
        JButton saveButton = new JButton("Save");
        buttonPanel.add(addFieldButton);
        buttonPanel.add(loadFromEntryButton);
        buttonPanel.add(saveButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        Map<String, JTextField> fieldMap = new LinkedHashMap<>();
        Map<String, JButton> removeButtonMap = new HashMap<>();
        int[] row = {0};

        if (entryData != null)
            populateFormWithData(formPanel, fieldMap, removeButtonMap, entryData, row);

        addFieldButton.addActionListener(e -> {
            addFieldToForm(formPanel, fieldMap, removeButtonMap, null, null, row);
            formPanel.revalidate();
            formPanel.repaint();
        });

        loadFromEntryButton.setEnabled(false);
        if (entryData == null) {
            try {
                List<Map<String, Object>> dataForLoadButton = tableManager.getTableData();
                if (!dataForLoadButton.isEmpty())
                    loadFromEntryButton.setEnabled(true);
            }
            catch (SQLException ex) {
                logger.error("Error fetching entries for 'Load from Existing Entry' button: {}", ex.getMessage());
            }
        }

        loadFromEntryButton.addActionListener(e -> {
            Map<String, Object> selectedEntry = selectExistingEntry();
            if (selectedEntry != null) {
                fieldMap.clear();
                removeButtonMap.clear();
                formPanel.removeAll();
                row[0] = 0;
                populateFormWithData(formPanel, fieldMap, removeButtonMap, selectedEntry, row);
                formPanel.revalidate();
                formPanel.repaint();
            }
        });

        saveButton.addActionListener(e -> {
            Map<String, Object> newData = new HashMap<>();
            for (Map.Entry<String, JTextField> entry : fieldMap.entrySet()) {
                String fieldName = entry.getKey();
                String value = entry.getValue().getText();
                newData.put(fieldName, value);
            }

            try {
                tableManager.saveEntry(newData, entryData);
                updateSortDropdown();
                refreshData();
                dialog.dispose();
            }
            catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error saving data: {}", ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void populateFormWithData(JPanel formPanel, Map<String, JTextField> fieldMap,
                                      Map<String, JButton> removeButtonMap, Map<String, Object> entryData, int[] row) {
        for (Map.Entry<String, Object> entry : entryData.entrySet()) {
            String fieldName = entry.getKey();

            if (fieldName.equals("_id"))
                continue;

            Object value = entry.getValue();
            addFieldToForm(formPanel, fieldMap, removeButtonMap, fieldName, value != null ? value.toString() : "", row);
        }
    }

    private Map<String, Object> selectExistingEntry() {
        try {
            List<Map<String, Object>> tableData = tableManager.getTableData();
            if (tableData.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No existing entries to load.", "Information", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }

            Map<String, Map<String, Object>> entryMap = new HashMap<>();
            List<String> entryIdentifiers = new ArrayList<>();
            for (Map<String, Object> entry : tableData) {
                String identifier = entry.entrySet().stream()
                        .filter(e -> !e.getKey().equals("_id"))
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", "));
                entryIdentifiers.add(identifier);
                entryMap.put(identifier, entry);
            }

            String selectedIdentifier = (String) JOptionPane.showInputDialog(this, "Select an entry to load as template:", "Load from Existing Entry",
                    JOptionPane.PLAIN_MESSAGE, null, entryIdentifiers.toArray(new String[0]), entryIdentifiers.get(0));

            if (selectedIdentifier != null)
                return entryMap.get(selectedIdentifier);
        }
        catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching entries: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Error fetching entries: {}", ex.getMessage());
        }
        return null;
    }

    private void addFieldToForm(JPanel formPanel, Map<String, JTextField> fieldMap, Map<String, JButton> removeButtonMap, String fieldName, String value, int[] row) {
        if (fieldName == null) {
            fieldName = JOptionPane.showInputDialog(formPanel, "Enter new field name:");
            if (fieldName == null || fieldName.trim().isEmpty())
                return;

            fieldName = fieldName.trim();
            if (fieldMap.containsKey(fieldName)) {
                JOptionPane.showMessageDialog(formPanel, "Field already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = row[0];
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel(fieldName + ":");
        formPanel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.8;
        JTextField textField = new JTextField(value != null ? value : "", 20);
        formPanel.add(textField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.1;
        JButton removeButton = new JButton("Remove");
        formPanel.add(removeButton, gbc);

        String finalFieldName = fieldName;
        removeButton.addActionListener(e -> {
            formPanel.remove(label);
            formPanel.remove(textField);
            formPanel.remove(removeButton);
            fieldMap.remove(finalFieldName);
            removeButtonMap.remove(finalFieldName);
            formPanel.revalidate();
            formPanel.repaint();
        });

        fieldMap.put(fieldName, textField);
        removeButtonMap.put(fieldName, removeButton);
        row[0]++;
    }

    private void openMassActionDialog() {
        JDialog dialog = new JDialog(this, "Mass Action", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel fieldLabel = new JLabel("Field:");
        JComboBox<String> fieldDropdown = new JComboBox<>();

        Set<String> fieldNames = new LinkedHashSet<>();
        fieldNames.add("ALL");
        try {
            List<Map<String, Object>> tableData = tableManager.getTableData();
            for (Map<String, Object> entry : tableData)
            for (String field : entry.keySet())
                if (!field.equals("_id"))
                    fieldNames.add(field);
        }
        catch (SQLException ex) {
            logger.error("Error fetching fields for mass action: {}", ex.getMessage());
        }
        fieldNames.forEach(fieldDropdown::addItem);

        topPanel.add(fieldLabel);
        topPanel.add(fieldDropdown);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());

        JPanel actionSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JRadioButton removeEntriesRadio = new JRadioButton("Remove entries");
        JRadioButton addFieldRadio = new JRadioButton("Add field");
        JRadioButton modifyFieldRadio = new JRadioButton("Modify field values");

        ButtonGroup actionGroup = new ButtonGroup();
        actionGroup.add(removeEntriesRadio);
        actionGroup.add(addFieldRadio);
        actionGroup.add(modifyFieldRadio);

        actionSelectionPanel.add(removeEntriesRadio);
        actionSelectionPanel.add(addFieldRadio);
        actionSelectionPanel.add(modifyFieldRadio);

        centerPanel.add(actionSelectionPanel, BorderLayout.NORTH);

        JPanel removeEntriesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel conditionLabel = new JLabel("Condition (value to match):");
        JTextField conditionField = new JTextField(20);
        removeEntriesPanel.add(conditionLabel);
        removeEntriesPanel.add(conditionField);

        JPanel addFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel newFieldLabel = new JLabel("Field Name:");
        JTextField newFieldNameField = new JTextField(15);
        JLabel prefillLabel = new JLabel("Prefill Value:");
        JTextField prefillValueField = new JTextField(15);
        addFieldPanel.add(newFieldLabel);
        addFieldPanel.add(newFieldNameField);
        addFieldPanel.add(prefillLabel);
        addFieldPanel.add(prefillValueField);

        JPanel modifyFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel newValueLabel = new JLabel("New Value:");
        JTextField newValueField = new JTextField(20);
        JCheckBox onlyIfEmptyCheckbox = new JCheckBox("Only modify if field is empty");
        modifyFieldPanel.add(newValueLabel);
        modifyFieldPanel.add(newValueField);
        modifyFieldPanel.add(onlyIfEmptyCheckbox);

        JPanel actionDetailsPanel = new JPanel(new CardLayout());
        actionDetailsPanel.add(new JPanel(), "NONE");
        actionDetailsPanel.add(removeEntriesPanel, "REMOVE");
        actionDetailsPanel.add(addFieldPanel, "ADD");
        actionDetailsPanel.add(modifyFieldPanel, "MODIFY");

        centerPanel.add(actionDetailsPanel, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton executeButton = new JButton("Execute");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(executeButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);

        removeEntriesRadio.addActionListener(e -> {
            CardLayout cl = (CardLayout) (actionDetailsPanel.getLayout());
            cl.show(actionDetailsPanel, "REMOVE");
        });

        addFieldRadio.addActionListener(e -> {
            CardLayout cl = (CardLayout) (actionDetailsPanel.getLayout());
            cl.show(actionDetailsPanel, "ADD");
        });

        modifyFieldRadio.addActionListener(e -> {
            CardLayout cl = (CardLayout) (actionDetailsPanel.getLayout());
            cl.show(actionDetailsPanel, "MODIFY");
        });

        removeEntriesRadio.setSelected(true);
        removeEntriesRadio.doClick();

        cancelButton.addActionListener(e -> dialog.dispose());

        executeButton.addActionListener(e -> {
            String selectedField = (String) fieldDropdown.getSelectedItem();
            try {
                if (removeEntriesRadio.isSelected()) {
                    String condition = conditionField.getText().trim();
                    tableManager.massDeleteEntries(selectedField, condition);
                }
                else if (addFieldRadio.isSelected()) {
                    String newFieldName = newFieldNameField.getText().trim();
                    String prefillValue = prefillValueField.getText();

                    if (newFieldName.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "Field name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    tableManager.massAddField(newFieldName, prefillValue);
                }
                else if (modifyFieldRadio.isSelected()) {
                    String newValue = newValueField.getText();
                    boolean onlyIfEmpty = onlyIfEmptyCheckbox.isSelected();

                    tableManager.massModifyField(selectedField, newValue, onlyIfEmpty);
                }

                updateSortDropdown();
                refreshData();
                dialog.dispose();
            }
            catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error executing mass action: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error executing mass action: {}", ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }
}