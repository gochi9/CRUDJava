package gmail.vladimir.ui;

import gmail.vladimir.UserService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginView extends JFrame {

    private final JTextField urlField;
    private final JTextField portField;
    private final JTextField databaseField;
    private final JTextField userField;
    private final JPasswordField passwordField;

    public LoginView(String defaultDatabase, String defaultURL, String defaultPort, String defaultUser) {
        setTitle("Database Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("URL:"), gbc);

        gbc.gridx = 1;
        urlField = new JTextField(defaultURL);
        panel.add(urlField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        portField = new JTextField(defaultPort);
        panel.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Database:"), gbc);

        gbc.gridx = 1;
        databaseField = new JTextField(defaultDatabase);
        panel.add(databaseField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("User:"), gbc);

        gbc.gridx = 1;
        userField = new JTextField(defaultUser);
        panel.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField();
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);

        loginButton.addActionListener(e -> {
            String url = "jdbc:mysql://" + urlField.getText() + ":" + portField.getText() + "/" + databaseField.getText();
            String user = userField.getText();
            String password = new String(passwordField.getPassword());

            try {
                connect(url, user, password);
            }
            catch (SQLException ex) {
                JOptionPane.showMessageDialog(LoginView.this, "Failed to connect to database: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> System.exit(0));
        add(panel);
    }

    private void connect(String url, String user, String password) throws SQLException {
        UserService userService = new UserService(url, user, password);
        CrudForm crudForm = new CrudForm(userService, databaseField.getText(), urlField.getText(), portField.getText(), userField.getText());
        crudForm.setVisible(true);
        dispose();
    }
}
