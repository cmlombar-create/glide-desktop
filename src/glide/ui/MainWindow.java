package glide.ui;

import glide.controller.EventController;
import glide.dao.DatabaseException;
import glide.model.Category;
import glide.model.Event;
import glide.service.AlertService;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class MainWindow extends JFrame {
    private static final String[] COLUMNS = {"Title", "Date & Time", "Category", "Reminder Lead", "Status"};

    private static final Color BG_DARK = new Color(18, 20, 24);
    private static final Color BG_PANEL = new Color(26, 29, 36);
    private static final Color BG_ROW_ALT = new Color(32, 36, 44);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color GOLD_DIM = new Color(140, 115, 35);
    private static final Color TEXT_LIGHT = new Color(240, 240, 245);

    private final EventController controller;
    private final AlertService alerts;
    private final DefaultTableModel tableModel;
    private final JTable eventTable;
    private final JComboBox<String> categoryFilter;
    private List<Event> currentEvents;

    public MainWindow() {
        super("Glide - Schedule Manager");
        setIconImage(createGlideIcon());
        getContentPane().setBackground(BG_DARK);
        this.controller = new EventController();
        this.alerts = new AlertService(this.controller, this::refreshTable);
        this.tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.eventTable = new JTable(this.tableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                if (row >= 0 && row < currentEvents.size()) {
                    Event ev = currentEvents.get(row);
                    String desc = ev.getDescription();
                    if (desc != null && !desc.trim().isEmpty()) {
                        return ev.getTitle() + " — Notes: " + desc;
                    }
                    return ev.getTitle() + " (Double-click to edit)";
                }
                return null;
            }
        };
        this.categoryFilter = new JComboBox<>();
        this.currentEvents = new ArrayList<>();

        setSize(860, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        categoryFilter.addItem("All Categories");
        for (Category c : Category.values()) {
            categoryFilter.addItem(c.getDisplayName());
        }
        EventDialog.styleGoldComboBox(categoryFilter);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topPanel.setBackground(BG_PANEL);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, GOLD));

        JLabel brandLabel = new JLabel("GLIDE");
        brandLabel.setForeground(GOLD);
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        topPanel.add(brandLabel);

        JLabel sep = new JLabel("  |   Category Filter:");
        sep.setForeground(TEXT_LIGHT);
        sep.setFont(new Font("SansSerif", Font.BOLD, 12));
        topPanel.add(sep);
        topPanel.add(categoryFilter);

        eventTable.setBackground(BG_DARK);
        eventTable.setForeground(TEXT_LIGHT);
        eventTable.setGridColor(new Color(55, 60, 72));
        eventTable.setSelectionBackground(GOLD);
        eventTable.setSelectionForeground(BG_DARK);
        eventTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventTable.setRowHeight(28);

        JTableHeader header = eventTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(BG_PANEL);
        header.setForeground(GOLD);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GOLD));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(GOLD);
                    c.setForeground(BG_DARK);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                    if (column == 2) {
                        c.setForeground(GOLD);
                    } else {
                        c.setForeground(TEXT_LIGHT);
                    }
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        for (int i = 0; i < COLUMNS.length; i++) {
            eventTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        eventTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && eventTable.getSelectedRow() >= 0) {
                    onEditClicked();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(eventTable);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBackground(BG_DARK);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 12, 10, 12),
            BorderFactory.createLineBorder(GOLD_DIM, 1)
        ));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 12));
        bottomPanel.setBackground(BG_PANEL);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GOLD_DIM));

        JButton addBtn = createStyledButton("+ Add Event", true);
        JButton editBtn = createStyledButton("Edit Selected", false);
        JButton deleteBtn = createStyledButton("Delete", false);
        JButton refreshBtn = createStyledButton("Refresh", false);

        bottomPanel.add(addBtn);
        bottomPanel.add(editBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(refreshBtn);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> onAddClicked());
        editBtn.addActionListener(e -> onEditClicked());
        deleteBtn.addActionListener(e -> onDeleteClicked());
        refreshBtn.addActionListener(e -> refreshTable());
        categoryFilter.addActionListener(e -> refreshTable());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                alerts.stop();
            }
        });

        seedDemoEventsIfEmpty();
        refreshTable();
        alerts.start();
    }

    private static JButton createStyledButton(String text, boolean primaryGold) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        if (primaryGold) {
            btn.setBackground(GOLD);
            btn.setForeground(BG_DARK);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
            ));
        } else {
            btn.setBackground(BG_ROW_ALT);
            btn.setForeground(GOLD);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_DIM, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
        }
        return btn;
    }

    private void seedDemoEventsIfEmpty() {
        try {
            if (controller.getEvents(null).isEmpty()) {
                LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
                controller.addEvent("CSI 2999 Final Presentation",
                    "Phase 2 Live Demo & QA Validation Report for Prof. Kalwani",
                    base.plusHours(2), Category.LECTURE, 15);
                controller.addEvent("Team Film & Conditioning Practice",
                    "Athletics Complex - Bring playbook and hydration gear",
                    base.plusHours(5), Category.PRACTICE, 30);
                controller.addEvent("Calculus 2 Midterm Exam",
                    "Mathematics Building Room 204 - Integration techniques",
                    base.plusDays(1).withHour(14), Category.EXAM, 60);
                controller.addEvent("Campus IT Helpdesk Shift",
                    "Evening student-worker shift at Kresge Library",
                    base.plusDays(2).withHour(17), Category.WORK, 15);
            }
        } catch (Exception ignored) {
        }
    }

    private static Image createGlideIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(18, 20, 24));
        g2.fillRoundRect(0, 0, 64, 64, 16, 16);
        g2.setColor(new Color(212, 175, 55));
        g2.setStroke(new BasicStroke(3.5f));
        g2.drawRoundRect(3, 3, 58, 58, 14, 14);
        g2.setFont(new Font("SansSerif", Font.BOLD, 40));
        FontMetrics fm = g2.getFontMetrics();
        int x = (64 - fm.stringWidth("G")) / 2;
        int y = ((64 - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString("G", x, y);
        g2.dispose();
        return img;
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
                    ev.getDateTime().format(EventDialog.DISPLAY_FORMAT),
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
        EventDialog dlg = new EventDialog(this, controller, null);
        if (dlg.showDialog()) {
            refreshTable();
        }
    }

    private void onEditClicked() {
        int row = eventTable.getSelectedRow();
        if (row < 0 || row >= currentEvents.size()) {
            JOptionPane.showMessageDialog(this, "Please select an event to edit.", "Glide", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Event ev = currentEvents.get(row);
        EventDialog dlg = new EventDialog(this, controller, ev);
        if (dlg.showDialog()) {
            refreshTable();
        }
    }

    private void onDeleteClicked() {
        int row = eventTable.getSelectedRow();
        if (row < 0 || row >= currentEvents.size()) {
            JOptionPane.showMessageDialog(this, "Please select an event to delete.", "Glide", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Event ev = currentEvents.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this event?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                controller.deleteEvent(ev.getId());
                refreshTable();
            } catch (DatabaseException ex) {
                showError(ex);
                refreshTable();
            }
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Glide Error", JOptionPane.ERROR_MESSAGE);
    }
}