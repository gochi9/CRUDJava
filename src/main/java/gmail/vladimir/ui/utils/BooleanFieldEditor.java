package gmail.vladimir.ui.utils;

import javax.swing.*;

public class BooleanFieldEditor implements FieldEditor {

    @Override
    public JComponent createComponent(boolean isEditable) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setEnabled(isEditable);
        return checkBox;
    }

    @Override
    public void setValue(JComponent component, Object value) {
        ((JCheckBox) component).setSelected(Boolean.TRUE.equals(value));
    }

    @Override
    public Object getValue(JComponent component) {
        return ((JCheckBox) component).isSelected();
    }
}

