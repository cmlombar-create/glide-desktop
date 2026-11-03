package glide.service;

import glide.controller.EventController;
import glide.model.Event;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.Timer;

public class AlertService {

    public static final int POLL_INTERVAL_MS = 30_000;
    public static final int MISSED_THRESHOLD_MINUTES = 5;
    public static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a", Locale.US);

    private final EventController controller;
    private final Runnable onReminderProcessed;
    private final Timer timer;
    private boolean running;
    private boolean checking;

    public AlertService(EventController controller) {
        this(controller, null);
    }

    public AlertService(EventController controller, Runnable onReminderProcessed) {
        this.controller = controller;
        this.onReminderProcessed = onReminderProcessed;
        this.timer = new Timer(POLL_INTERVAL_MS, e -> checkPendingReminders());
        this.timer.setInitialDelay(0);
    }

    public void start() { running = true; timer.start(); }
    public void stop() { running = false; timer.stop(); }
    public boolean isRunning() { return running; }

    public void checkPendingReminders() {
        if (running) checkAt(LocalDateTime.now());
    }

    void checkAt(LocalDateTime now) {
        if (checking) return;
        checking = true;
        boolean updatedAny = false;
        try {
            List<Event> due = controller.getDueReminders(now);
            for (Event event : due) {
                controller.markCompleted(event.getId());
                updatedAny = true;
                if (shouldShowPopup(event, now)) {
                    showReminderPopup(event, now);
                } else {
                    System.err.println("Missed reminder skipped (app was closed): event_id="
                            + event.getId());
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("Reminder check failed, will retry: " + ex.getMessage());
        } finally {
            checking = false;
            if (updatedAny && onReminderProcessed != null) {
                onReminderProcessed.run();
            }
        }
    }

    public static boolean shouldShowPopup(Event event, LocalDateTime now) {
        return event.getDateTime().isAfter(now.minusMinutes(MISSED_THRESHOLD_MINUTES));
    }

    public static String buildReminderMessage(Event event, LocalDateTime now) {
        long minutesUntil = Math.round(Duration.between(now, event.getDateTime()).getSeconds() / 60.0);
        String timing;
        if (minutesUntil > 0) {
            timing = "Starts in " + formatDuration(minutesUntil);
        } else if (minutesUntil == 0) {
            timing = "Starting now";
        } else {
            timing = "Started " + formatDuration(-minutesUntil) + " ago";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(event.getTitle()).append('\n')
          .append(timing).append('\n')
          .append("When: ").append(event.getDateTime().format(DISPLAY_FORMAT)).append('\n')
          .append("Category: ").append(event.getCategory().getDisplayName());
        String details = event.getDescription();
        if (details != null && !details.isBlank()) {
            sb.append('\n').append("Details: ").append(details);
        }
        return sb.toString();
    }

    public static String buildReminderTitle(Event event) {
        return "Glide Reminder - " + event.getCategory().getDisplayName();
    }

    static String formatDuration(long minutes) {
        long days = minutes / 1440;
        long hours = (minutes % 1440) / 60;
        long mins = minutes % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day" : " days");
        if (hours > 0) sb.append(sb.length() > 0 ? " " : "").append(hours).append(hours == 1 ? " hour" : " hours");
        if (mins > 0 || sb.length() == 0) {
            sb.append(sb.length() > 0 ? " " : "").append(mins).append(mins == 1 ? " minute" : " minutes");
        }
        return sb.toString();
    }

    public void showReminderPopup(Event event, LocalDateTime now) {
        JOptionPane pane = new JOptionPane(buildReminderMessage(event, now), JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(null, buildReminderTitle(event));
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        dialog.dispose();
    }
}