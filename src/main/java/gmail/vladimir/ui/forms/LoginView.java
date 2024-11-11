package gmail.vladimir.ui.forms;

import gmail.vladimir.db.DatabaseServiceFactory;
import gmail.vladimir.db.DatabaseType;
import gmail.vladimir.db.IDatabaseService;
import gmail.vladimir.managers.ConnectionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class LoginView extends JFrame {
    private JComboBox<DatabaseType> dbTypeComboBox;
    private JPanel inputPanel;
    private CardLayout inputCardLayout;

    private JTextField usernameField;
    private JPasswordField passwordField;

    private JTextField hostField;
    private JTextField portField;
    private JTextField dbNameField;
    private JTextField extraParamsField;

    private JTextField filePathField;

    private JTextField mongoUriField;
    private JTextField mongoDbNameField;

    private ConnectionManager connectionManager = new ConnectionManager();

    public LoginView() {
        setTitle("Database Login");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel dbTypePanel = new JPanel(new FlowLayout());
        dbTypePanel.add(new JLabel("Database Type:"));
        dbTypeComboBox = new JComboBox<>(DatabaseType.values());
        dbTypePanel.add(dbTypeComboBox);
        mainPanel.add(dbTypePanel, BorderLayout.NORTH);

        inputCardLayout = new CardLayout();
        inputPanel = new JPanel(inputCardLayout);

        JPanel relationalInputPanel = createRelationalInputPanel();
        JPanel sqliteInputPanel = createSQLiteInputPanel();
        JPanel mongoInputPanel = createMongoInputPanel();

        inputPanel.add(relationalInputPanel, "Relational");
        inputPanel.add(sqliteInputPanel, "SQLite");
        inputPanel.add(mongoInputPanel, "MongoDB");

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        JButton connectButton = new JButton("Connect");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        dbTypeComboBox.addActionListener(e -> updateInputFields());

        connectButton.addActionListener(e -> {
            DatabaseType dbType = (DatabaseType) dbTypeComboBox.getSelectedItem();
            Map<String, String> inputValues = new HashMap<>();

            try {
                switch (dbType) {
                    case MYSQL:
                    case POSTGRESQL:
                    case MARIADB:
                    case HYPERSQL:
                        inputValues.put("host", hostField.getText());
                        inputValues.put("port", portField.getText());
                        inputValues.put("dbName", dbNameField.getText());
                        inputValues.put("username", usernameField.getText());
                        inputValues.put("password", new String(passwordField.getPassword()));
                        inputValues.put("extraParams", extraParamsField.getText());
                        break;
                    case SQLITE:
                        inputValues.put("dbFilePath", filePathField.getText());
                        break;
                    case MONGODB:
                        inputValues.put("uri", mongoUriField.getText());
                        inputValues.put("dbName", mongoDbNameField.getText());
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported database type: " + dbType);
                }

                Map<String, String> connectionParams = connectionManager.getConnectionParams(dbType, inputValues);

                connect(dbType, connectionParams);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        cancelButton.addActionListener(e -> System.exit(0));

        updateInputFields();
    }

    private JPanel createRelationalInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createGbc();

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Host:"), gbc);

        gbc.gridx = 1;
        hostField = new JTextField("localhost");
        panel.add(hostField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        portField = new JTextField("3306");
        panel.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Database Name:"), gbc);

        gbc.gridx = 1;
        dbNameField = new JTextField();
        panel.add(dbNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField();
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField();
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Extra Parameters:"), gbc);

        gbc.gridx = 1;
        extraParamsField = new JTextField();
        panel.add(extraParamsField, gbc);

        gbc.gridx = 2;
        JButton helpButton = new JButton("?");
        helpButton.setMargin(new Insets(2, 2, 2, 2));
        panel.add(helpButton, gbc);

        helpButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "This field is for additional JDBC URL parameters.\nExample: ?ssl-mode=REQUIRED",
                    "Extra Parameters Help",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        return panel;
    }

    private JPanel createSQLiteInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createGbc();

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Database File Path:"), gbc);

        gbc.gridx = 1;
        filePathField = new JTextField();
        panel.add(filePathField, gbc);

        gbc.gridx = 2;
        JButton browseButton = new JButton("Browse");
        panel.add(browseButton, gbc);

        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION)
                filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        });

        return panel;
    }

    private JPanel createMongoInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createGbc();

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("MongoDB URI:"), gbc);

        gbc.gridx = 1;
        mongoUriField = new JTextField("mongodb://localhost:27017");
        panel.add(mongoUriField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Database Name:"), gbc);

        gbc.gridx = 1;
        mongoDbNameField = new JTextField();
        panel.add(mongoDbNameField, gbc);

        return panel;
    }

    private void updateInputFields() {
        DatabaseType dbType = (DatabaseType) dbTypeComboBox.getSelectedItem();
        if (dbType == DatabaseType.SQLITE) {
            inputCardLayout.show(inputPanel, "SQLite");
        } else if (dbType == DatabaseType.MONGODB) {
            inputCardLayout.show(inputPanel, "MongoDB");
        } else {
            inputCardLayout.show(inputPanel, "Relational");
            switch (dbType) {
                case MYSQL:
                case MARIADB:
                    portField.setText("3306");
                    break;
                case POSTGRESQL:
                    portField.setText("5432");
                    break;
                case HYPERSQL:
                    portField.setText("9001");
                    break;
                default:
                    portField.setText("");
            }
        }
    }

    private void connect(DatabaseType dbType, Map<String, String> connectionParams) throws SQLException {
        IDatabaseService dbService = DatabaseServiceFactory.createDatabaseService(dbType);
        dbService.connect(connectionParams);

        CrudForm crudForm = new CrudForm(dbService, dbType);
        crudForm.setVisible(true);
        dispose();
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }
}