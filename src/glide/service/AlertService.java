package glide.service;

import glide.controller.EventController;
import glide.model.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/** Swing EDT timer; worker thread performs DB reads and queues dialogs on the EDT. */
public class AlertService {
    private final EventController controller;
    private final Timer timer;
    private final AtomicBoolean checking = new AtomicBoolean(false);
    private volatile boolean running;

    public AlertService(EventController controller) {
        this.controller = controller;
        this.timer = new Timer(30_000, e -> checkPendingReminders());
    }
    public void start() { running = true; timer.start(); checkPendingReminders(); }
    public void stop() { running = false; timer.stop(); }

    public void checkPendingReminders() {
        if (!running || !checking.compareAndSet(false, true)) return;
        Thread worker = new Thread(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                List<Event> due = controller.getDueReminders(now);
                for (Event event : due) {
                    if (!running) break;
                    // Skip stale appointments, and mark so they do not reappear.
                    if (now.isAfter(event.getDateTime().plusMinutes(5))) {
                        controller.markCompleted(event.getId());
                    } else {
                        controller.markCompleted(event.getId());
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            if (running) showReminderPopup(event, LocalDateTime.now());
                        });
                    }
                }
            } catch (RuntimeException ex) {
                System.err.println("Reminder query error: " + ex.getMessage());
            } finally { checking.set(false); }
        }, "Glide-Alert-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    public void showReminderPopup(Event event, LocalDateTime now) {
        JOptionPane pane = new JOptionPane(event.getTitle() + "\n"
            + event.getDateTime() + "\n"
            + (event.getDescription() == null ? "" : event.getDescription()),
            JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(null, "Glide Reminder");
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        dialog.dispose();
    }
}
