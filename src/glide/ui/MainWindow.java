package glide.ui;

import glide.controller.EventController;
import glide.model.Category;
import glide.model.Event;
import glide.service.AlertService;
import glide.dao.EventDAO;
import glide.dao.DatabaseManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import javax.swing.*;
import java.util.List;

public class MainWindow extends JFrame {
    private final EventController controller = new EventController();
    private final AlertService alerts = new AlertService(controller);
    private final DefaultListModel<Event> model = new DefaultListModel<>();
    private final JList<Event> eventList = new JList<>(model);
    private final JComboBox<String> filter = new JComboBox<>();

    public MainWindow() {
        super("Glide — Student Reminder");
        filter.addItem("All");
        for (Category c : Category.values()) filter.addItem(c.getDisplayName());
        setSize(760, 480); setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add"), edit = new JButton("Edit"),
                delete = new JButton("Delete"), refresh = new JButton("Refresh");
        top.add(new JLabel("Category:")); top.add(filter);
        top.add(add); top.add(edit); top.add(delete); top.add(refresh);
        add(top, BorderLayout.NORTH); add(new JScrollPane(eventList), BorderLayout.CENTER);
        add.addActionListener(e -> form(null));
        edit.addActionListener(e -> form(eventList.getSelectedValue()));
        delete.addActionListener(e -> removeSelected());
        refresh.addActionListener(e -> refresh());
        filter.addActionListener(e -> refresh());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                alerts.stop(); DatabaseManager.close();
            }
        });
        refresh(); alerts.start();
    }
    private void refresh() {
        try {
            Category c = filter.getSelectedIndex() == 0 ? null
                    : Category.fromDisplayName((String)filter.getSelectedItem());
            List<Event> events = controller.getEvents(c);
            model.clear(); events.forEach(model::addElement);
        } catch (Exception e) { error(e); }
    }
    private void form(Event event) {
        JComboBox<Category> category = new JComboBox<>(Category.values());
        JTextField title = new JTextField(event == null ? "" : event.getTitle());
        JTextField time = new JTextField(event == null
                ? LocalDateTime.now().plusHours(1).format(EventDAO.FORMAT)
                : event.getDateTime().format(EventDAO.FORMAT));
        JComboBox<Integer> lead = new JComboBox<>(new Integer[]{0,5,10,15,30,60,120,1440});
        lead.setSelectedItem(event == null ? 15 : event.getAlertLeadTime());
        JTextArea desc = new JTextArea(event == null ? "" : event.getDescription(),3,20);
        if (event != null) category.setSelectedItem(event.getCategory());
        JPanel panel = new JPanel(new GridLayout(0,1,3,3));
        panel.add(new JLabel("Title:")); panel.add(title);
        panel.add(new JLabel("Category:")); panel.add(category);
        panel.add(new JLabel("Date/time (yyyy-MM-dd HH:mm:ss):")); panel.add(time);
        panel.add(new JLabel("Reminder minutes before:")); panel.add(lead);
        panel.add(new JLabel("Description:")); panel.add(new JScrollPane(desc));
        if (JOptionPane.showConfirmDialog(this,panel,event == null ? "Add event" : "Edit event",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                LocalDateTime date = LocalDateTime.parse(time.getText().trim(), EventDAO.FORMAT);
                if (event == null) controller.addEvent(title.getText(),desc.getText(),date,
                        (Category)category.getSelectedItem(),(Integer)lead.getSelectedItem());
                else {
                    Event updated = new Event(event.getId(),title.getText(),desc.getText(),date,
                        (Category)category.getSelectedItem(),(Integer)lead.getSelectedItem(),event.isCompleted());
                    controller.updateEvent(updated);
                }
                refresh();
            } catch (Exception e) { error(e); }
        }
    }
    private void removeSelected() {
        Event e=eventList.getSelectedValue(); if(e==null) return;
        if (JOptionPane.showConfirmDialog(this,"Delete " + e.getTitle() + "?", "Confirm delete",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try { controller.deleteEvent(e.getId());refresh(); } catch(Exception ex) { error(ex); }
    }
    private void error(Exception e) {
        JOptionPane.showMessageDialog(this,e.getMessage(),"Glide error",JOptionPane.ERROR_MESSAGE);
    }
}
