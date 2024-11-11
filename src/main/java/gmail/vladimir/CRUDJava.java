package gmail.vladimir;

import com.formdev.flatlaf.FlatDarkLaf;
import gmail.vladimir.ui.forms.LoginView;

import javax.swing.*;

public class CRUDJava {

    public static void main(String[] args) {
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();
            loginView.setVisible(true);
        });
    }
}