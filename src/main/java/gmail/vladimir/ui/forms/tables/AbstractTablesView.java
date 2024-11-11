package gmail.vladimir.ui.forms.tables;

import gmail.vladimir.Constants;
import gmail.vladimir.db.IDatabaseService;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractTablesView extends JFrame {

    protected final IDatabaseService dbService;
    protected final String tableName;

    protected final JPanel dataPanel;

    public AbstractTablesView(IDatabaseService dbService, String tableName) {
        this.dbService = dbService;
        this.tableName = tableName;

        setTitle("CRUD Operations for Table: " + tableName);
        setSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("Table: " + tableName));
        topPanel.add(infoPanel, BorderLayout.NORTH);

        JButton addButton = new JButton("Add Entry");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addButton);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        dataPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(dataPanel);
        add(scrollPane, BorderLayout.CENTER);

        addButton.addActionListener(e -> openEntryForm(null));
    }

    protected abstract void refreshData();

    protected abstract void openEntryForm(Object existingData);
}