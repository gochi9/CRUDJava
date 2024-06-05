package gmail.vladimir;

import com.formdev.flatlaf.FlatDarkLaf;
import gmail.vladimir.ui.LoginView;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView("sql7711874", "sql7.freemysqlhosting.net", "3306", "sql7711874");
            loginView.setVisible(true);
        });
    }
}