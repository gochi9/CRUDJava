package gmail.vladimir.ui.forms.tables;

import gmail.vladimir.db.IDatabaseService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import gmail.vladimir.db.ISqlResultHandler;
import gmail.vladimir.db.QueryResult;
import gmail.vladimir.managers.RelationalTableManager;
import gmail.vladimir.ui.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationalTablesView extends AbstractTablesView {

    private static final Logger logger = LoggerFactory.getLogger(RelationalTablesView.class);

    private final RelationalTableManager tableManager;
    private Map<String, String> displaySchema;
    private List<Map<String, Object>> displayData;

    private JTextArea consoleOutputArea;
    private JTextArea sqlInputArea;

    private final List<String> commandHistory;
    private int historyIndex;

    private int entriesPerPage;
    private int currentPage;
    private int totalEntries;

    private final JTextField entriesPerPageField;
    private final JButton prevPageButton;
    private final JButton nextPageButton;
    private final JTextField pageNumberField;

    public RelationalTablesView(IDatabaseService dbService, String tableName) throws SQLException {
        super(dbService, tableName);
        this.tableManager = new RelationalTableManager(dbService, tableName);

        this.displaySchema = tableManager.getTableSchema();
        if (displaySchema == null || displaySchema.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The table has no columns.", "Information", JOptionPane.INFORMATION_MESSAGE);
            displaySchema = Collections.emptyMap();
        }

        this.setLayout(new BorderLayout());

        commandHistory = new ArrayList<>();
        historyIndex = -1;

        entriesPerPage = 10;
        currentPage = 1;

        JPanel mainDataPanel = new JPanel(new BorderLayout());

        JButton addEntryButton = new JButton("Add Entry");
        addEntryButton.addActionListener(e -> openEntryForm(null));

        JButton resetSortButton = new JButton("Reset Sort");
        resetSortButton.addActionListener(e -> resetData());

        entriesPerPageField = new JTextField(String.valueOf(entriesPerPage), 5);
        entriesPerPageField.addActionListener(e -> {
            try {
                entriesPerPage = Integer.parseInt(entriesPerPageField.getText());
                currentPage = 1;
                resetData();
            }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                entriesPerPageField.setText(String.valueOf(entriesPerPage));
            }
        });

        prevPageButton = new JButton("<");
        nextPageButton = new JButton(">");
        pageNumberField = new JTextField(String.valueOf(currentPage), 3);

        prevPageButton.addActionListener(e -> {
            if (currentPage <= 1)
                return;

            currentPage--;
            resetData();
        });

        nextPageButton.addActionListener(e -> {
            if (currentPage >= getTotalPages())
                return;

            currentPage++;
            resetData();
        });

        pageNumberField.addActionListener(e -> {
            try {
                int page = Integer.parseInt(pageNumberField.getText());

                page = Math.max(page, 1);
                page = Math.min(page, getTotalPages());

                if (page >= 1 && page <= getTotalPages()) {
                    currentPage = page;
                    resetData();
                }
            }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid page number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                pageNumberField.setText(String.valueOf(currentPage));
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addEntryButton);
        buttonPanel.add(resetSortButton);
        buttonPanel.add(new JLabel("Entries per page:"));
        buttonPanel.add(entriesPerPageField);
        buttonPanel.add(prevPageButton);
        buttonPanel.add(pageNumberField);
        buttonPanel.add(nextPageButton);

        mainDataPanel.add(buttonPanel, BorderLayout.NORTH);

        JScrollPane dataScrollPane = new JScrollPane(dataPanel);

        mainDataPanel.add(dataScrollPane, BorderLayout.CENTER);

        consoleOutputArea = new JTextArea();
        consoleOutputArea.setEditable(false);
        JScrollPane consoleOutputScrollPane = new JScrollPane(consoleOutputArea);

        sqlInputArea = new JTextArea(3, 70);
        sqlInputArea.setLineWrap(true);
        sqlInputArea.setWrapStyleWord(true);
        sqlInputArea.setFocusTraversalKeysEnabled(false);

        sqlInputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0) {
                        sqlInputArea.insert("\n", sqlInputArea.getCaretPosition());
                        e.consume();
                    }
                    else {
                        executeSqlCommand();
                        e.consume();
                    }
                }
                else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    sqlInputArea.insert("    ", sqlInputArea.getCaretPosition());
                    e.consume();
                }
                else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    navigateCommandHistory(-1);
                    e.consume();
                }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    navigateCommandHistory(1);
                    e.consume();
                }
            }
        });

        JPanel sqlInputPanel = new JPanel(new BorderLayout());
        JScrollPane sqlInputScrollPane = new JScrollPane(sqlInputArea);
        sqlInputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        sqlInputPanel.add(sqlInputScrollPane, BorderLayout.CENTER);

        JSplitPane consoleInputSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sqlInputPanel, consoleOutputScrollPane);
        consoleInputSplitPane.setResizeWeight(0.2);

        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(consoleInputSplitPane, BorderLayout.CENTER);

        consoleOutputArea.append("You can use '{}' as a placeholder for the table name.\n");
        consoleOutputArea.append("Press ENTER to execute command, ALT+ENTER to insert a new line.\n\n");

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainDataPanel, consolePanel);
        splitPane.setResizeWeight(0.7);

        this.add(splitPane, BorderLayout.CENTER);

        resetData();
    }

    private void navigateCommandHistory(int direction) {
        if (commandHistory.isEmpty()) return;

        historyIndex += direction;
        if (historyIndex < 0) {
            historyIndex = -1;
            sqlInputArea.setText("");
        }
        else if (historyIndex >= commandHistory.size())
            historyIndex = commandHistory.size() - 1;

        else
            sqlInputArea.setText(commandHistory.get(historyIndex));
    }

    private void resetData() {
        try {
            totalEntries = tableManager.getTotalEntries();
            int offset = (currentPage - 1) * entriesPerPage;
            displayData = tableManager.getTableData(entriesPerPage, offset);
            displaySchema = tableManager.getTableSchema();

            updatePaginationControls();
            refreshData();
        }
        catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Error fetching data: {}", ex.getMessage());
            consoleOutputArea.append("Error fetching data: " + ex.getMessage() + "\n");
            consoleOutputArea.setCaretPosition(consoleOutputArea.getDocument().getLength());
        }
    }

    private void updatePaginationControls() {
        int totalPages = getTotalPages();
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
        pageNumberField.setText(String.valueOf(currentPage));
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) totalEntries / entriesPerPage);
    }

    @Override
    protected void refreshData() {
        dataPanel.removeAll();

        try {
            if (displaySchema == null || displaySchema.isEmpty()) {
                dataPanel.add(new JLabel("No data to display"));
                return;
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.NORTH;

            int columnCount = displaySchema.size();
            int baseCellWidth = 100;
            int buttonWidth = 75;
            int totalRowWidth = columnCount * baseCellWidth + 2 * buttonWidth + 30;

            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            headerPanel.setPreferredSize(new Dimension(totalRowWidth, 35));
            headerPanel.setBackground(Color.DARK_GRAY);

            for (String column : displaySchema.keySet()) {
                JLabel headerLabel = new JLabel(column);
                headerLabel.setForeground(Color.WHITE);
                headerLabel.setPreferredSize(new Dimension(baseCellWidth, 25));
                headerPanel.add(headerLabel);
            }

            dataPanel.add(headerPanel, gbc);
            gbc.gridy++;

            JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            separator.setPreferredSize(new Dimension(totalRowWidth, 5));
            dataPanel.add(separator, gbc);
            gbc.gridy++;

            if (displayData != null && !displayData.isEmpty()) {
                for (Map<String, Object> row : displayData) {
                    JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
                    rowPanel.setPreferredSize(new Dimension(totalRowWidth, 35));

                    for (String column : displaySchema.keySet()) {
                        Object value = row.get(column);
                        JLabel cell = new JLabel(value != null ? value.toString() : "");
                        cell.setPreferredSize(new Dimension(baseCellWidth, 25));
                        rowPanel.add(cell);
                    }

                    JButton editButton = new JButton("Edit");
                    editButton.setPreferredSize(new Dimension(buttonWidth, 25));
                    JButton removeButton = new JButton("Remove");
                    removeButton.setPreferredSize(new Dimension(buttonWidth, 25));

                    boolean pkAvailable = displaySchema.containsKey(tableManager.getPrimaryKeyColumn());

                    if (pkAvailable) {
                        editButton.addActionListener(e -> openEntryForm(row));
                        removeButton.addActionListener(e -> {
                            int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this entry?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                            if (confirmation != JOptionPane.YES_OPTION)
                                return;

                            try {
                                Object primaryKeyValue = row.get(tableManager.getPrimaryKeyColumn());
                                tableManager.deleteEntry(primaryKeyValue);
                                resetData();
                            } catch (SQLException ex) {
                                JOptionPane.showMessageDialog(this, "Error deleting entry: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                                logger.error("Error deleting entry: {}", ex.getMessage());
                                consoleOutputArea.append("Error deleting entry: " + ex.getMessage() + "\n");
                                consoleOutputArea.setCaretPosition(consoleOutputArea.getDocument().getLength());
                            }
                        });
                    } else {
                        editButton.setEnabled(false);
                        removeButton.setEnabled(false);
                    }

                    rowPanel.add(editButton);
                    rowPanel.add(Box.createRigidArea(new Dimension(10, 0)));
                    rowPanel.add(removeButton);

                    dataPanel.add(rowPanel, gbc);
                    gbc.gridy++;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    dataPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
                    gbc.gridy++;
                }
            }
            else {
                JPanel noDataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
                noDataPanel.setPreferredSize(new Dimension(totalRowWidth, 35));
                JLabel noDataLabel = new JLabel("No data available");
                noDataLabel.setPreferredSize(new Dimension(totalRowWidth - 20, 25));
                noDataPanel.add(noDataLabel);
                dataPanel.add(noDataPanel, gbc);
                gbc.gridy++;
            }

            gbc.weighty = 1;
            dataPanel.add(new JPanel(), gbc);
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error displaying data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Error displaying data: {}", ex.getMessage());
            consoleOutputArea.append("Error displaying data: " + ex.getMessage() + "\n");
            consoleOutputArea.setCaretPosition(consoleOutputArea.getDocument().getLength());
        }
        dataPanel.revalidate();
        dataPanel.repaint();
    }

    @Override
    protected void openEntryForm(Object existingData) {
        Map<String, Object> data = (Map<String, Object>) existingData;
        JDialog dialog = new JDialog(this, data == null ? "Add Entry" : "Edit Entry", true);
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 0, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Map<String, JComponent> fieldMap = new LinkedHashMap<>();
        int row = 0;

        Map<String, String> tableSchema = tableManager.getTableSchema();
        String primaryKeyColumn = tableManager.getPrimaryKeyColumn();
        Set<String> numericDataTypes = tableManager.getNumericDataTypes();

        for (String column : tableSchema.keySet()) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;
            formPanel.add(new JLabel(column + ":"), gbc);

            gbc.gridx = 1;
            String rawType = tableSchema.get(column);
            String type = getBaseType(rawType).toUpperCase();
            FieldEditor editor = fieldEditorMap.getOrDefault(type, new StringFieldEditor());

            boolean isEditable = true;
            if (column.equalsIgnoreCase(primaryKeyColumn))
                isEditable = data == null;


            JComponent inputField = editor.createComponent(isEditable);

            if (data != null && data.containsKey(column)) {
                Object value = data.get(column);
                editor.setValue(inputField, value);
            }
            else if (data == null && column.equalsIgnoreCase(primaryKeyColumn) && numericDataTypes.contains(type)) {
                try {
                    int nextId = tableManager.getNextPrimaryKeyValue();
                    editor.setValue(inputField, nextId);
                }
                catch (SQLException ex) {
                    logger.error("Error getting next ID for primary key column: {}", ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error getting next ID: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    consoleOutputArea.append("Error getting next ID: " + ex.getMessage() + "\n");
                    consoleOutputArea.setCaretPosition(consoleOutputArea.getDocument().getLength());
                }
            }

            formPanel.add(inputField, gbc);
            fieldMap.put(column, inputField);
            row++;
        }

        JButton saveButton = new JButton("Save");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            Map<String, Object> newData = new HashMap<>();
            for (String column : fieldMap.keySet()) {
                JComponent inputField = fieldMap.get(column);
                String rawType = tableSchema.get(column);
                String type = getBaseType(rawType).toUpperCase();
                FieldEditor editor = fieldEditorMap.getOrDefault(type, new StringFieldEditor());
                Object value = editor.getValue(inputField);
                newData.put(column, value);
            }

            try {
                tableManager.saveEntry(newData, data);
                resetData();
                dialog.dispose();
            }
            catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error saving data: {}", ex.getMessage());
                consoleOutputArea.append("Error saving data: " + ex.getMessage() + "\n");
                consoleOutputArea.setCaretPosition(consoleOutputArea.getDocument().getLength());
            }
        });

        JScrollPane scrollPane = new JScrollPane(formPanel);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private String getBaseType(String dataType) {
        int index = dataType.indexOf('(');
        if (index != -1)
            return dataType.substring(0, index).trim();
        else
            return dataType.trim();
    }

    private void executeSqlCommand() {
        String sqlCommand = sqlInputArea.getText();
        if (sqlCommand.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an SQL command.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        commandHistory.add(sqlCommand);
        historyIndex = commandHistory.size();

        sqlCommand = sqlCommand.replace("{}", tableName);

        String singleLineCommand = sqlCommand.replace("\n", " ").replace("\r", " ");
        consoleOutputArea.append("-> " + singleLineCommand + "\n");

        try {
            String finalSqlCommand = sqlCommand;
            boolean hasResultSet = tableManager.executeSqlCommand(sqlCommand, new ISqlResultHandler() {
                @Override
                public void handleResultSet(ResultSet rs) throws SQLException {
                    QueryResult queryResult = tableManager.getQueryResultFromResultSet(rs);
                    displayResultInConsole(queryResult);

                    String trimmedSql = finalSqlCommand.trim().toLowerCase();
                    if (trimmedSql.startsWith("select") && !trimmedSql.startsWith("explain") && !trimmedSql.startsWith("analyze")) {
                        displayData = queryResult.getData();
                        displaySchema = queryResult.getColumnTypes();

                        totalEntries = displayData.size();
                        currentPage = 1;
                        entriesPerPage = totalEntries > 0 ? totalEntries : 1;
                        entriesPerPageField.setText(String.valueOf(entriesPerPage));
                        pageNumberField.setText(String.valueOf(currentPage));
                        prevPageButton.setEnabled(false);
                        nextPageButton.setEnabled(false);

                        refreshData();
                    }
                }

                @Override
                public void handleUpdateCount(int updateCount) {
                    consoleOutputArea.append("Query executed successfully. Rows affected: " + updateCount + "\n");
                    if (finalSqlCommand.toLowerCase().contains(tableName.toLowerCase()) || finalSqlCommand.contains("{}"))
                        resetData();
                }

                @Override
                public void handleMessage(String message) {
                    consoleOutputArea.append(message + "\n");
                }
            });

            if (!hasResultSet)
                consoleOutputArea.append("Command executed successfully.\n");
        }
        catch (SQLException ex) {
            consoleOutputArea.append("Error executing SQL command: " + ex.getMessage() + "\n");
            logger.error("Error executing SQL command: {}", ex.getMessage());
        }

        consoleOutputArea.append("\n");
        consoleOutputArea.setCaretPosition(consoleOutputArea.getDocument().getLength());
        sqlInputArea.setText("");
    }

    private void displayResultInConsole(QueryResult queryResult) {
        List<Map<String, Object>> result = queryResult.getData();
        if (result == null || result.isEmpty()) {
            consoleOutputArea.append("Query executed successfully. No results returned.\n");
            return;
        }
        StringBuilder sb = new StringBuilder();
        Set<String> columns = queryResult.getColumnTypes().keySet();

        for (String column : columns)
            sb.append(column).append("\t");

        sb.append("\n");

        for (Map<String, Object> row : result) {
            for (String column : columns) {
                Object value = row.get(column);
                sb.append(value != null ? value.toString() : "NULL").append("\t");
            }
            sb.append("\n");
        }

        consoleOutputArea.append(sb.toString());
    }

    private static final Map<String, FieldEditor> fieldEditorMap = new HashMap<>();

    static {
        FieldEditor integerEditor = new IntegerFieldEditor();
        FieldEditor doubleEditor = new DoubleFieldEditor();
        FieldEditor booleanEditor = new BooleanFieldEditor();
        FieldEditor dateEditor = new DateFieldEditor();
        FieldEditor stringEditor = new StringFieldEditor();

        fieldEditorMap.put("INT", integerEditor);
        fieldEditorMap.put("INTEGER", integerEditor);
        fieldEditorMap.put("SMALLINT", integerEditor);
        fieldEditorMap.put("BIGINT", integerEditor);

        fieldEditorMap.put("DOUBLE", doubleEditor);
        fieldEditorMap.put("FLOAT", doubleEditor);
        fieldEditorMap.put("REAL", doubleEditor);
        fieldEditorMap.put("DECIMAL", doubleEditor);
        fieldEditorMap.put("NUMERIC", doubleEditor);

        fieldEditorMap.put("BOOLEAN", booleanEditor);
        fieldEditorMap.put("BIT", booleanEditor);

        fieldEditorMap.put("DATE", dateEditor);
        fieldEditorMap.put("TIMESTAMP", dateEditor);
        fieldEditorMap.put("DATETIME", dateEditor);

        fieldEditorMap.put("VARCHAR", stringEditor);
        fieldEditorMap.put("CHAR", stringEditor);
        fieldEditorMap.put("TEXT", stringEditor);
        fieldEditorMap.put("CLOB", stringEditor);
        fieldEditorMap.put("BLOB", stringEditor);
    }
}