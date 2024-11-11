package gmail.vladimir.ui.utils;

import org.jdatepicker.impl.DateComponentFormatter;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class DateFieldEditor implements FieldEditor {

    @Override
    public JComponent createComponent(boolean isEditable) {
        UtilDateModel model = new UtilDateModel();
        model.setSelected(true);
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateComponentFormatter());
        datePicker.getComponent(0).setEnabled(isEditable); //date field
        datePicker.getComponent(1).setEnabled(isEditable); //buton
        return datePicker;
    }

    @Override
    public void setValue(JComponent component, Object value) {
        UtilDateModel model = (UtilDateModel) ((JDatePickerImpl) component).getModel();
        if (value instanceof java.sql.Date)
            model.setValue(new Date(((java.sql.Date) value).getTime()));

        else if (value instanceof Timestamp)
            model.setValue(new Date(((Timestamp) value).getTime()));

        else if (value instanceof Date)
            model.setValue((Date) value);

        else if (value == null)
            model.setValue(null);

        else
            try {
                Date dateValue = new SimpleDateFormat("yyyy-MM-dd").parse(value.toString());
                model.setValue(dateValue);
            }
            catch (Exception e) {model.setValue(null);}
    }

    @Override
    public Object getValue(JComponent component) {
        Date date = (Date) ((JDatePickerImpl) component).getModel().getValue();
        if (date != null)
            return new java.sql.Date(date.getTime());

        return null;
    }
}

