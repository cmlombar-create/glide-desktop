package glide.ui;

import glide.dao.EventDAO;
import glide.model.Event;
import glide.service.ReminderService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import javax.swing.*;
import java.util.List;

public class MainWindow extends JFrame {
    private static final String[] CATEGORIES={"All", "Exam", "Lecture", "Homework", "Practice", "Game", "Study", "Work", "Personal"};
    private final EventDAO dao=new EventDAO();
    private final ReminderService reminders=new ReminderService(dao);
    private final DefaultListModel<Event> model=new DefaultListModel<>();
    private final JList<Event> eventList=new JList<>(model);
    private final JComboBox<String> filter=new JComboBox<>(CATEGORIES);

    public MainWindow() {
        super("Glide — Student Reminder");
        setSize(750,450); setLocationRelativeTo(null); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add=new JButton("Add"),edit=new JButton("Edit"),delete=new JButton("Delete"),refresh=new JButton("Refresh");
        top.add(new JLabel("Category:")); top.add(filter);
        top.add(add);top.add(edit);top.add(delete);top.add(refresh);
        add(top,BorderLayout.NORTH); add(new JScrollPane(eventList),BorderLayout.CENTER);
        add.addActionListener(e->form(null)); edit.addActionListener(e->form(eventList.getSelectedValue()));
        delete.addActionListener(e->removeSelected()); refresh.addActionListener(e->refresh());
        filter.addActionListener(e->refresh());
        addWindowListener(new java.awt.event.WindowAdapter(){ @Override public void windowClosing(java.awt.event.WindowEvent e){ reminders.stop(); }});
        refresh(); reminders.start();
    }
    private void refresh() {
        int index=filter.getSelectedIndex();
        try {
            List<Event> events=dao.list(index==0?null:index);
            model.clear(); events.forEach(model::addElement);
        } catch(Exception e) { error(e); }
    }
    private void form(Event event) {
        JComboBox<String> category=new JComboBox<>(java.util.Arrays.copyOfRange(CATEGORIES,1,CATEGORIES.length));
        JTextField title=new JTextField(event==null?"":event.title());
        JTextField time=new JTextField(event==null?LocalDateTime.now().plusHours(1).format(EventDAO.FORMAT):event.eventDateTime());
        JTextField lead=new JTextField(event==null?"15":Integer.toString(event.reminderLeadMinutes()));
        JTextArea desc=new JTextArea(event==null?"":event.description(),3,20);
        if(event!=null) category.setSelectedIndex(event.categoryId()-1);
        JPanel panel=new JPanel(new GridLayout(0,1,3,3));
        panel.add(new JLabel("Title:"));panel.add(title);
        panel.add(new JLabel("Category:"));panel.add(category);
        panel.add(new JLabel("Date/time (yyyy-MM-dd HH:mm:ss):"));panel.add(time);
        panel.add(new JLabel("Reminder minutes before:"));panel.add(lead);
        panel.add(new JLabel("Description:"));panel.add(new JScrollPane(desc));
        if(JOptionPane.showConfirmDialog(this,panel,event==null?"Add event":"Edit event",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
            try {
                dao.save(event==null?null:event.id(),title.getText(),desc.getText(),category.getSelectedIndex()+1,
                    LocalDateTime.parse(time.getText().trim(),EventDAO.FORMAT),Integer.parseInt(lead.getText().trim()));
                refresh();
            } catch(Exception e) { error(e); }
        }
    }
    private void removeSelected() {
        Event e=eventList.getSelectedValue(); if(e==null) return;
        if(JOptionPane.showConfirmDialog(this,"Delete "+e.title()+"?","Confirm delete",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try { dao.delete(e.id());refresh(); } catch(Exception ex){error(ex);}
    }
    private void error(Exception e) { JOptionPane.showMessageDialog(this,e.getMessage(),"Glide error",JOptionPane.ERROR_MESSAGE); }
}
