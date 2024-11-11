package gmail.vladimir.ui.utils;

import javax.swing.*;

public interface FieldEditor {

    JComponent createComponent(boolean isEditable);
    void setValue(JComponent component, Object value);
    Object getValue(JComponent component);

}
