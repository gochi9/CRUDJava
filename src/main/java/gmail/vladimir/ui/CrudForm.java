package gmail.vladimir.ui;

import gmail.vladimir.UserService;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

public class CrudForm extends JFrame {

    private final UserService userService;
    private final JPanel tablesPanel;
    private final String databaseName, url, port, username;

    public CrudForm(UserService userService, String databaseName, String url, String port, String username) {
        this.userService = userService;

        setTitle("Database Tables");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        JPanel dbInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbInfoPanel.add(new JLabel("Database: " + (this.databaseName = databaseName)));
        dbInfoPanel.add(new JLabel(" | URL: " + (this.url = url)));
        dbInfoPanel.add(new JLabel(" | Port: " + (this.port = port)));
        dbInfoPanel.add(new JLabel(" | User: " + (this.username = username)));
        infoPanel.add(dbInfoPanel);

        add(infoPanel, BorderLayout.NORTH);

        tablesPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(tablesPanel);
        scrollPane.setPreferredSize(new Dimension(780, 500));
        add(scrollPane, BorderLayout.CENTER);

        JButton addTableButton = new JButton("Add Table");
        JButton disconnectButton = new JButton("Disconnect");

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(addTableButton);
        topPanel.add(disconnectButton);
        infoPanel.add(topPanel);

        addTableButton.addActionListener(e -> openTableForm(null));

        disconnectButton.addActionListener(e -> disconnect());

        try {
            refreshTables();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(CrudForm.this, "Error fetching tables: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void refreshTables() throws SQLException {
        tablesPanel.removeAll();
        List<String> tables = userService.listTables();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.NORTH;

        for (String table : tables) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            rowPanel.setPreferredSize(new Dimension(750, 35));

            JLabel tableNameLabel = new JLabel(table);
            tableNameLabel.setPreferredSize(new Dimension(500, 30));
            rowPanel.add(tableNameLabel);

            JButton editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension(75, 25));
            JButton removeButton = new JButton("Remove");
            removeButton.setPreferredSize(new Dimension(75, 25));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            buttonPanel.add(editButton);
            buttonPanel.add(removeButton);

            editButton.addActionListener(e -> openTableCrudForm(table));

            removeButton.addActionListener(e -> {
                int confirmation = JOptionPane.showConfirmDialog(CrudForm.this, "Are you sure you want to delete this table?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirmation != JOptionPane.YES_OPTION)
                    return;

                try {
                    userService.deleteTable(table);
                    refreshTables();
                }
                catch (SQLException ex) {
                    JOptionPane.showMessageDialog(CrudForm.this, "Error deleting table: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            rowPanel.add(buttonPanel);

            tablesPanel.add(rowPanel, gbc);
            gbc.gridy++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            tablesPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
            gbc.gridy++;
        }

        gbc.weighty = 1;
        tablesPanel.add(new JPanel(), gbc);

        tablesPanel.revalidate();
        tablesPanel.repaint();
    }

    private void openTableForm(String tableName) {
        JDialog dialog = new JDialog(this, tableName == null ? "Add Table" : "Edit Table: " + tableName, true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Table Name:"), gbc);

        gbc.gridx = 1;
        JTextField tableNameField = new JTextField();

        if (tableName != null) {
            tableNameField.setText(tableName);
            tableNameField.setEnabled(false);
        }

        formPanel.add(tableNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Table Schema:"), gbc);

        gbc.gridx = 1;
        JTextArea tableSchemaArea = new JTextArea(5, 20);
        JScrollPane schemaScrollPane = new JScrollPane(tableSchemaArea);
        formPanel.add(schemaScrollPane, gbc);

        JButton saveButton = new JButton("Save");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            String newTableName = tableNameField.getText();
            String tableSchema = tableSchemaArea.getText();

            try {
                userService.createTable(newTableName, tableSchema);
                refreshTables();
                dialog.dispose();
            }
            catch (SQLException ex) {
                if (ex.getMessage().contains("already exists"))
                    JOptionPane.showMessageDialog(dialog, "Table '" + newTableName + "' already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                else
                    JOptionPane.showMessageDialog(dialog, "Error saving table: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        dialog.add(formPanel);
        dialog.setVisible(true);
    }

    private void openTableCrudForm(String tableName) {
        TablesView TablesView = null;
        try {
            TablesView = new TablesView(userService, tableName, databaseName, url, port, username);
            TablesView.setVisible(true);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void disconnect() {
        this.dispose();
        try {
            userService.closeConnection();
            LoginView loginView = new LoginView(databaseName, url, port, username);
            loginView.setVisible(true);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}