package glide.service;

import glide.dao.EventDAO;
import glide.model.Event;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class ReminderService {
    private final EventDAO dao;
    private final Timer timer;
    public ReminderService(EventDAO dao) {
        this.dao=dao;
        timer=new Timer(10000, event -> check());
    }
    public void start() { timer.start(); check(); }
    public void stop() { timer.stop(); }
    private void check() {
        // Database operations on a worker thread, never block the Swing EDT.
        Thread worker=new Thread(() -> {
            try {
                List<Event> due=dao.due();
                for (Event e: due) {
                    if (dao.markShown(e.id())) SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null, e.title()+"\n"+e.eventDateTime()+"\n"+e.description(),
                        "Glide reminder",JOptionPane.INFORMATION_MESSAGE));
                }
            } catch (Exception e) { System.err.println("Reminder check failed: "+e.getMessage()); }
        }, "glide-reminder-check");
        worker.setDaemon(true); worker.start();
    }
}
