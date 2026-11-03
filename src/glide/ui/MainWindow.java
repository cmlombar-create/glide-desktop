package glide.ui;

import glide.controller.EventController;
import glide.dao.DatabaseException;
import glide.dao.DatabaseManager;
import glide.dao.EventDAO;
import glide.model.Category;
import glide.model.Event;
import glide.service.AlertService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {

    private static final String[] COLUMNS = {
        "Title", "Date & Time", "Category", "Reminder Lead", "Status"
    };

    private final EventController controller = new EventController();
    private final AlertService alerts = new AlertService(controller, this::refreshTable);
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable eventTable = new JTable(tableModel);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private List<Event> currentEvents = new ArrayList<>();

    public MainWindow() {
        super("Glide - Schedule Manager");
        setSize(820, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        categoryFilter.addItem("All");
        for (Category c : Category.values()) {
            categoryFilter.addItem(c.getDisplayName());
        }

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topPanel.add(new JLabel("Category:"));
        topPanel.add(categoryFilter);

        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventTable.setRowHeight(24);
        eventTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(eventTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        JButton addButton = new JButton("+ Add Event");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");

        bottomPanel.add(addButton);
        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> onAddClicked());
        editButton.addActionListener(e -> onEditClicked());
        deleteButton.addActionListener(e -> onDeleteClicked());
        refreshButton.addActionListener(e -> refreshTable());
        categoryFilter.addActionListener(e -> refreshTable());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                alerts.stop();
                DatabaseManager.close();
            }
        });

        refreshTable();
        alerts.start();
    }

    public final void refreshTable() {
        try {
            Category selected = categoryFilter.getSelectedIndex() <= 0
                    ? null
                    : Category.fromDisplayName((String) categoryFilter.getSelectedItem());
            currentEvents = controller.getEvents(selected);
            tableModel.setRowCount(0);
            for (Event ev : currentEvents) {
                tableModel.addRow(new Object[]{
                    ev.getTitle(),
                    ev.getDateTime().format(EventDAO.FORMAT),
                    ev.getCategory().getDisplayName(),
                    EventDialog.formatLeadTime(ev.getAlertLeadTime()),
                    ev.isCompleted() ? "Completed" : "Scheduled"
                });
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void onAddClicked() {
        EventDialog dialog = new EventDialog(this, controller, null);
        if (dialog.showDialog()) {
            refreshTable();
        }
    }

    private void onEditClicked() {
        int row = eventTable.getSelectedRow();
        if (row < 0 || row >= currentEvents.size()) {
            JOptionPane.showMessageDialog(this, "Please select an event to edit.",
                    "Glide", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Event selected = currentEvents.get(row);
        EventDialog dialog = new EventDialog(this, controller, selected);
        if (dialog.showDialog()) {
            refreshTable();
        }
    }

    private void onDeleteClicked() {
        int row = eventTable.getSelectedRow();
        if (row < 0 || row >= currentEvents.size()) {
            JOptionPane.showMessageDialog(this, "Please select an event to delete.",
                    "Glide", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Event selected = currentEvents.get(row);
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this event?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                controller.deleteEvent(selected.getId());
                refreshTable();
            } catch (DatabaseException ex) {
                showError(ex);
                refreshTable();
            }
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Glide Error", JOptionPane.ERROR_MESSAGE);
    }
}