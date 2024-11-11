package gmail.vladimir.ui.utils;

import javax.swing.*;

public class StringFieldEditor implements FieldEditor {

    @Override
    public JComponent createComponent(boolean isEditable) {
        JTextField textField = new JTextField();
        textField.setEditable(isEditable);
        return textField;
    }

    @Override
    public void setValue(JComponent component, Object value) {
        ((JTextField) component).setText(value != null ? value.toString() : "");
    }

    @Override
    public Object getValue(JComponent component) {
        return ((JTextField) component).getText();
    }
}

