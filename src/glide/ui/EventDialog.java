package glide.ui;

import glide.controller.EventController;
import glide.controller.ValidationException;
import glide.dao.DatabaseException;
import glide.dao.EventDAO;
import glide.model.Category;
import glide.model.Event;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class EventDialog extends JDialog {

    private static final int[] LEAD_MINUTES = {0, 5, 10, 15, 30, 60, 120, 1440};
    private static final String[] LEAD_LABELS = {
        "At Event Time", "5 Mins Before", "10 Mins Before", "15 Mins Before",
        "30 Mins Before", "1 Hour Before", "2 Hours Before", "1 Day Before"
    };
    private static final DateTimeFormatter SHORT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EventController controller;
    private final Event existingEvent;
    private boolean saved = false;

    private final JTextField titleField = new JTextField(24);
    private final JComboBox<Category> categoryBox = new JComboBox<>(Category.values());
    private final JTextField dateTimeField = new JTextField(24);
    private final JComboBox<String> leadTimeBox = new JComboBox<>(LEAD_LABELS);
    private final JTextArea descriptionArea = new JTextArea(4, 24);

    public EventDialog(Frame owner, EventController controller, Event existingEvent) {
        super(owner, existingEvent == null ? "Create Event" : "Edit Event", true);
        this.controller = controller;
        this.existingEvent = existingEvent;
        buildLayout();
        if (existingEvent != null) {
            populateFields(existingEvent);
        } else {
            dateTimeField.setText(LocalDateTime.now().plusHours(1).withSecond(0).withNano(0).format(EventDAO.FORMAT));
            leadTimeBox.setSelectedItem("15 Mins Before");
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildLayout() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(formPanel, gbc, 0, "Event Title:", titleField);
        addRow(formPanel, gbc, 1, "Category:", categoryBox);
        addRow(formPanel, gbc, 2, "Date & Time (yyyy-MM-dd HH:mm:ss):", dateTimeField);
        addRow(formPanel, gbc, 3, "Reminder Offset:", leadTimeBox);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Description (Optional):"), gbc);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        formPanel.add(new JScrollPane(descriptionArea), gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save Event");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(formPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    public final void populateFields(Event e) {
        titleField.setText(e.getTitle());
        categoryBox.setSelectedItem(e.getCategory());
        dateTimeField.setText(e.getDateTime().format(EventDAO.FORMAT));
        leadTimeBox.setSelectedItem(formatLeadTime(e.getAlertLeadTime()));
        descriptionArea.setText(e.getDescription() == null ? "" : e.getDescription());
    }

    public boolean showDialog() {
        setVisible(true);
        return saved;
    }

    private void onSave() {
        String rawDate = dateTimeField.getText().trim();
        LocalDateTime parsedDate;
        try {
            parsedDate = parseDateTime(rawDate);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please correct the following:\n- Date/time must use format yyyy-MM-dd HH:mm:ss",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int leadMinutes = LEAD_MINUTES[Math.max(0, leadTimeBox.getSelectedIndex())];
        Category selectedCategory = (Category) categoryBox.getSelectedItem();

        try {
            if (existingEvent == null) {
                controller.addEvent(titleField.getText(), descriptionArea.getText(),
                        parsedDate, selectedCategory, leadMinutes);
            } else {
                Event updated = new Event(existingEvent.getId(), titleField.getText(),
                        descriptionArea.getText(), parsedDate, selectedCategory,
                        leadMinutes, existingEvent.isCompleted());
                controller.updateEvent(updated);
            }
            saved = true;
            dispose();
        } catch (ValidationException ex) {
            StringBuilder sb = new StringBuilder("Please correct the following:\n");
            for (String err : ex.getErrors()) {
                sb.append("- ").append(err).append('\n');
            }
            JOptionPane.showMessageDialog(this, sb.toString().trim(),
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text, EventDAO.FORMAT);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(text, SHORT_FORMAT);
        }
    }

    public static String formatLeadTime(int minutes) {
        for (int i = 0; i < LEAD_MINUTES.length; i++) {
            if (LEAD_MINUTES[i] == minutes) return LEAD_LABELS[i];
        }
        return minutes + " Mins Before";
    }
}