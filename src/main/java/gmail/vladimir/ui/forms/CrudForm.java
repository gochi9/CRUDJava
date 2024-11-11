package gmail.vladimir.ui.forms;

import gmail.vladimir.Constants;
import gmail.vladimir.db.DatabaseType;
import gmail.vladimir.db.IDatabaseService;
import gmail.vladimir.managers.SchemaBuilder;
import gmail.vladimir.managers.TableManager;
import gmail.vladimir.ui.forms.tables.AbstractTablesView;
import gmail.vladimir.ui.forms.tables.MongoTablesView;
import gmail.vladimir.ui.forms.tables.RelationalTablesView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class CrudForm extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(CrudForm.class);

    private final IDatabaseService dbService;
    private final DatabaseType dbType;
    private final JPanel tablesPanel;

    private final TableManager tableManager;
    private final SchemaBuilder schemaBuilder = new SchemaBuilder();

    private DefaultTableModel schemaTableModel;

    public CrudForm(IDatabaseService dbService, DatabaseType dbType) {
        this.dbService = dbService;
        this.dbType = dbType;
        this.tableManager = new TableManager(dbService);

        setTitle("Database Name - " + dbService.getDatabaseName());
        setSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top panel with database info and buttons
        JPanel topPanel = new JPanel(new BorderLayout());

        // Database info panel
        JPanel dbInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbInfoPanel.add(new JLabel("Database Type: " + dbType));
        topPanel.add(dbInfoPanel, BorderLayout.NORTH);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addTableButton = new JButton("Add Table");
        JButton disconnectButton = new JButton("Disconnect");
        buttonsPanel.add(addTableButton);
        buttonsPanel.add(disconnectButton);
        topPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // Panel to display the list of tables
        tablesPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(tablesPanel);
        add(scrollPane, BorderLayout.CENTER);

        // Event listeners for buttons
        addTableButton.addActionListener(e -> openTableForm());
        disconnectButton.addActionListener(e -> disconnect());

        refreshTables();
    }

    private void openTableForm() {
        JDialog dialog = new JDialog(this, "Add Table", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new BorderLayout());

        if (requiresSchema(dbType)) {
            JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JRadioButton rawSchemaOption = new JRadioButton("Enter Raw Schema");
            JRadioButton guiSchemaOption = new JRadioButton("Use Schema Builder");
            ButtonGroup schemaOptionsGroup = new ButtonGroup();
            schemaOptionsGroup.add(rawSchemaOption);
            schemaOptionsGroup.add(guiSchemaOption);
            guiSchemaOption.setSelected(true);
            optionsPanel.add(rawSchemaOption);
            optionsPanel.add(guiSchemaOption);
            formPanel.add(optionsPanel, BorderLayout.NORTH);

            JPanel schemaPanel = new JPanel(new CardLayout());
            JPanel rawSchemaPanel = new JPanel(new BorderLayout());
            JTextArea tableSchemaArea = new JTextArea();
            JScrollPane schemaScrollPane = new JScrollPane(tableSchemaArea);
            rawSchemaPanel.add(schemaScrollPane, BorderLayout.CENTER);

            JPanel guiSchemaPanel = createSchemaBuilderPanel();

            schemaPanel.add(guiSchemaPanel, "GUI");
            schemaPanel.add(rawSchemaPanel, "Raw");

            formPanel.add(schemaPanel, BorderLayout.CENTER);

            CardLayout cl = (CardLayout) (schemaPanel.getLayout());
            rawSchemaOption.addActionListener(e -> cl.show(schemaPanel, "Raw"));
            guiSchemaOption.addActionListener(e -> cl.show(schemaPanel, "GUI"));

            JButton saveButton = new JButton("Save");
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(saveButton);
            formPanel.add(buttonPanel, BorderLayout.SOUTH);

            saveButton.addActionListener(e -> {
                if (guiSchemaOption.isSelected()) {
                    String tableSchema = schemaBuilder.buildSchemaFromGUI(schemaTableModel);
                    if (tableSchema == null || tableSchema.isEmpty()) {
                        return;
                    }
                    try {
                        tableManager.createTable(null, tableSchema);
                        refreshTables();
                        dialog.dispose();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error creating table: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        logger.error("Error creating table: {}", ex.getMessage());
                    }
                } else if (rawSchemaOption.isSelected()) {
                    String tableSchema = tableSchemaArea.getText().trim();
                    if (tableSchema.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "Table schema cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        tableManager.createTable(null, tableSchema);
                        refreshTables();
                        dialog.dispose();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error creating table: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        logger.error("Error creating table: {}", ex.getMessage());
                    }
                }
            });
        } else {
            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            infoPanel.add(new JLabel("Collection Name:"));
            JTextField collectionNameField = new JTextField(20);
            infoPanel.add(collectionNameField);
            formPanel.add(infoPanel, BorderLayout.NORTH);

            JButton saveButton = new JButton("Save");
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(saveButton);
            formPanel.add(buttonPanel, BorderLayout.SOUTH);

            saveButton.addActionListener(e -> {
                String collectionName = collectionNameField.getText().trim();
                if (collectionName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Collection name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    tableManager.createTable(collectionName, null);
                    refreshTables();
                    dialog.dispose();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Error creating collection: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    logger.error("Error creating collection {}: {}", collectionName, ex.getMessage());
                }
            });
        }

        dialog.add(formPanel);
        dialog.setVisible(true);
    }

    private String buildSchemaFromGUI() {
        // Deprecated, now using SchemaBuilder class
        return null;
    }

    private JPanel createSchemaBuilderPanel() {
        JPanel schemaBuilderPanel = new JPanel(new BorderLayout());
        String[] columnNames = {"Column Name", "Data Type", "Primary Key"};
        schemaTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2) {
                    return Boolean.class;
                }
                return String.class;
            }
        };
        JTable schemaTable = new JTable(schemaTableModel);
        schemaTable.setFillsViewportHeight(true);

        schemaTable.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 2) {
                schemaBuilder.enforceSinglePrimaryKey(schemaTableModel, e.getFirstRow());
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(schemaTable);
        tableScrollPane.setPreferredSize(new Dimension(550, 250));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addColumnButton = new JButton("Add Column");
        JButton removeColumnButton = new JButton("Remove Column");
        buttonPanel.add(addColumnButton);
        buttonPanel.add(removeColumnButton);

        addColumnButton.addActionListener(e -> {
            schemaTableModel.addRow(new Object[]{"", "VARCHAR(255)", Boolean.FALSE});
        });

        removeColumnButton.addActionListener(e -> {
            int selectedRow = schemaTable.getSelectedRow();
            if (selectedRow != -1) {
                schemaTableModel.removeRow(selectedRow);
            }
        });

        schemaBuilderPanel.add(tableScrollPane, BorderLayout.CENTER);
        schemaBuilderPanel.add(buttonPanel, BorderLayout.SOUTH);

        return schemaBuilderPanel;
    }

    private void refreshTables() {
        tablesPanel.removeAll();
        try {
            List<String> tables = tableManager.listTables();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            for (String table : tables) {
                JPanel tableRow = createTableRow(table);
                tablesPanel.add(tableRow, gbc);
                gbc.gridy++;
            }

            tablesPanel.revalidate();
            tablesPanel.repaint();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching tables: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Error refreshing tables: {}", ex.getMessage());
        }
    }

    private JPanel createTableRow(String tableName) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        JLabel tableLabel = new JLabel(tableName);
        rowPanel.add(tableLabel, BorderLayout.WEST);

        JButton editButton = new JButton("Edit");
        JButton removeButton = new JButton("Remove");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);

        if (dbType != DatabaseType.MONGODB) {
            JButton modifySchemaButton = new JButton("Modify Schema");
            buttonPanel.add(modifySchemaButton);
            modifySchemaButton.addActionListener(e -> {
                JOptionPane.showMessageDialog(this, "Work in progress.", "Info", JOptionPane.INFORMATION_MESSAGE);
            });
        }

        rowPanel.add(buttonPanel, BorderLayout.EAST);

        editButton.addActionListener(e -> openTableView(tableName));

        removeButton.addActionListener(e -> {
            int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete table '" + tableName + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirmation != JOptionPane.YES_OPTION)
                return;

            try {
                tableManager.deleteTable(tableName);
                refreshTables();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting table: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error deleting table {}: {}", tableName, ex.getMessage());
            }
        });

        return rowPanel;
    }

    private boolean requiresSchema(DatabaseType dbType) {
        return dbType != DatabaseType.MONGODB;
    }

    private void openTableView(String tableName) {
        try {
            AbstractTablesView tableView;
            if (dbType == DatabaseType.MONGODB)
                tableView = new MongoTablesView(dbService, tableName);
            else
                tableView = new RelationalTablesView(dbService, tableName);

            tableView.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error opening table view: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Error opening table view for {}: {}", tableName, ex.getMessage());
        }
    }

    private void disconnect() {
        try {
            dbService.disconnect();
        } catch (SQLException ex) {
            logger.error("Error disconnecting: {}", ex.getMessage());
        }
        dispose();
        new LoginView().setVisible(true);
    }
}