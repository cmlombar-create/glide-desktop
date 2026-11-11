package glide.ui;

import glide.controller.EventController;
import glide.controller.ValidationException;
import glide.dao.DatabaseException;
import glide.dao.EventDAO;
import glide.model.Category;
import glide.model.Event;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

public class EventDialog extends JDialog {
    private static final int[] LEAD_MINUTES = {0, 5, 10, 15, 30, 60, 120, 1440};
    private static final String[] LEAD_LABELS = {
        "At Event Time", "5 Mins Before", "10 Mins Before", "15 Mins Before",
        "30 Mins Before", "1 Hour Before", "2 Hours Before", "1 Day Before"
    };
    public static final DateTimeFormatter DISPLAY_FORMAT =
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM/dd/yyyy hh:mm a").toFormatter(Locale.US);
    private static final DateTimeFormatter FLEX_12H =
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy h:mm a").toFormatter(Locale.US);
    private static final DateTimeFormatter SHORT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static final Color BG_DARK = new Color(18, 20, 24);
    static final Color BG_PANEL = new Color(28, 31, 38);
    static final Color BG_INPUT = new Color(36, 40, 48);
    static final Color GOLD = new Color(212, 175, 55);
    static final Color GOLD_DIM = new Color(160, 130, 40);
    static final Color TEXT_LIGHT = new Color(240, 240, 245);

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
        if (owner != null && owner.getIconImage() != null) {
            setIconImage(owner.getIconImage());
        }
        buildLayout();
        if (existingEvent != null) {
            populateFields(existingEvent);
        } else {
            LocalDateTime defaultTime = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
            dateTimeField.setText(defaultTime.format(DISPLAY_FORMAT));
            leadTimeBox.setSelectedItem("15 Mins Before");
        }
        pack();
        setLocationRelativeTo(owner);
    }

    static void styleGoldComboBox(JComboBox<?> box) {
        box.setBackground(BG_INPUT);
        box.setForeground(GOLD);
        box.setFont(new Font("SansSerif", Font.BOLD, 12));
        box.setBorder(BorderFactory.createLineBorder(GOLD_DIM, 1));
        box.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                BasicArrowButton btn = new BasicArrowButton(
                    BasicArrowButton.SOUTH,
                    BG_PANEL,
                    GOLD_DIM,
                    GOLD,
                    BG_PANEL
                );
                btn.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, GOLD_DIM));
                return btn;
            }
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(BG_INPUT);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                if (isSelected) {
                    lbl.setBackground(GOLD);
                    lbl.setForeground(BG_DARK);
                } else {
                    lbl.setBackground(BG_INPUT);
                    lbl.setForeground(GOLD);
                }
                return lbl;
            }
        });
    }

    private void styleInput(JComponent comp) {
        comp.setBackground(BG_INPUT);
        comp.setForeground(TEXT_LIGHT);
        comp.setFont(new Font("SansSerif", Font.PLAIN, 13));
        Border line = BorderFactory.createLineBorder(GOLD_DIM, 1);
        Border pad = BorderFactory.createEmptyBorder(4, 6, 4, 6);
        comp.setBorder(BorderFactory.createCompoundBorder(line, pad));
        if (comp instanceof JTextField) {
            ((JTextField) comp).setCaretColor(GOLD);
        } else if (comp instanceof JTextArea) {
            ((JTextArea) comp).setCaretColor(GOLD);
        }
    }

    private void buildLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        styleInput(titleField);
        styleInput(dateTimeField);
        styleInput(descriptionArea);
        styleGoldComboBox(categoryBox);
        styleGoldComboBox(leadTimeBox);

        addRow(panel, gbc, 0, "Event Title:", titleField);
        addRow(panel, gbc, 1, "Category:", categoryBox);
        addRow(panel, gbc, 2, "Date & Time (MM/dd/yyyy hh:mm AM/PM):", dateTimeField);
        addRow(panel, gbc, 3, "Reminder Offset:", leadTimeBox);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel descLabel = new JLabel("Description (Optional):");
        descLabel.setForeground(GOLD);
        descLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(descLabel, gbc);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        JScrollPane scroll = new JScrollPane(descriptionArea);
        scroll.setBorder(BorderFactory.createLineBorder(GOLD_DIM, 1));
        panel.add(scroll, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(BG_PANEL);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GOLD_DIM));

        JButton saveBtn = new JButton("Save Event");
        saveBtn.setBackground(GOLD);
        saveBtn.setForeground(BG_DARK);
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        saveBtn.setFocusPainted(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(BG_INPUT);
        cancelBtn.setForeground(TEXT_LIGHT);
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        cancelBtn.setFocusPainted(false);

        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(btnPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveBtn);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        JLabel lbl = new JLabel(label);
        lbl.setForeground(GOLD);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        panel.add(comp, gbc);
    }

    public final void populateFields(Event event) {
        titleField.setText(event.getTitle());
        categoryBox.setSelectedItem(event.getCategory());
        dateTimeField.setText(event.getDateTime().format(DISPLAY_FORMAT));
        leadTimeBox.setSelectedItem(formatLeadTime(event.getAlertLeadTime()));
        descriptionArea.setText(event.getDescription() == null ? "" : event.getDescription());
    }

    public boolean showDialog() {
        setVisible(true);
        return saved;
    }

    private void onSave() {
        String rawDate = dateTimeField.getText().trim();
        LocalDateTime dt;
        try {
            dt = parseDateTime(rawDate);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                "Please correct the following:\n- Date/time must use format MM/dd/yyyy hh:mm AM/PM (e.g., 11/04/2026 02:30 PM)",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int leadMinutes = LEAD_MINUTES[Math.max(0, leadTimeBox.getSelectedIndex())];
        Category cat = (Category) categoryBox.getSelectedItem();
        try {
            if (existingEvent == null) {
                controller.addEvent(titleField.getText(), descriptionArea.getText(), dt, cat, leadMinutes);
            } else {
                Event updated = new Event(existingEvent.getId(), titleField.getText(), descriptionArea.getText(),
                    dt, cat, leadMinutes, existingEvent.isCompleted());
                controller.updateEvent(updated);
            }
            saved = true;
            dispose();
        } catch (ValidationException ex) {
            StringBuilder sb = new StringBuilder("Please correct the following:\n");
            for (String err : ex.getErrors()) {
                sb.append("- ").append(err).append('\n');
            }
            JOptionPane.showMessageDialog(this, sb.toString().trim(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static LocalDateTime parseDateTime(String text) {
        String cleaned = text.trim().replaceAll("\\s+", " ");
        try { return LocalDateTime.parse(cleaned, DISPLAY_FORMAT); } catch (DateTimeParseException ignored) {}
        try { return LocalDateTime.parse(cleaned, FLEX_12H); } catch (DateTimeParseException ignored) {}
        try { return LocalDateTime.parse(cleaned, EventDAO.FORMAT); } catch (DateTimeParseException ignored) {}
        return LocalDateTime.parse(cleaned, SHORT_FORMAT);
    }

    public static String formatLeadTime(int minutes) {
        for (int i = 0; i < LEAD_MINUTES.length; i++) {
            if (LEAD_MINUTES[i] == minutes) return LEAD_LABELS[i];
        }
        return minutes + " mins";
    }
}