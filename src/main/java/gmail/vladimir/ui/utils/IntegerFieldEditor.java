package gmail.vladimir.ui.utils;

import javax.swing.*;

public class IntegerFieldEditor implements FieldEditor {

    @Override
    public JComponent createComponent(boolean isEditable) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        spinner.setEnabled(isEditable);
        return spinner;
    }

    @Override
    public void setValue(JComponent component, Object value) {
        ((JSpinner) component).setValue(value != null ? value : 0);
    }

    @Override
    public Object getValue(JComponent component) {
        return ((JSpinner) component).getValue();
    }

}
