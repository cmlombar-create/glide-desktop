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

/**
 * Checks for due reminders and shows a popup for each one.
 *
 * A javax.swing.Timer fires once when the timer starts and then every 30 seconds.
 * Every check runs on the Swing Event Dispatch Thread, the same thread as the UI, so the
 * popup can be shown directly and the single SQLite connection is never used by two
 * threads at once.
 */
public class AlertService {

    /** How often to check. A popup appears at most this late after its reminder time. */
    public static final int POLL_INTERVAL_MS = 30_000;
    /** If the event itself started more than this long ago (app was closed), skip the popup. */
    public static final int MISSED_THRESHOLD_MINUTES = 5;
    /** Date format shown in the popup, e.g. "Wed, Nov 4, 2026 at 5:00 PM". */
    public static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a", Locale.US);

    private final EventController controller;
    private final Timer timer;
    private boolean running;
    /** Stops a second check from starting while a popup from the current check is still open. */
    private boolean checking;

    public AlertService(EventController controller) {
        this.controller = controller;
        this.timer = new Timer(POLL_INTERVAL_MS, e -> checkPendingReminders());
        this.timer.setInitialDelay(0); // first check runs immediately when the timer starts
    }

    public void start() { running = true; timer.start(); }
    public void stop() { running = false; timer.stop(); }
    public boolean isRunning() { return running; }

    /** One polling cycle. Called by the Swing Timer on the EDT. */
    public void checkPendingReminders() {
        if (running) checkAt(LocalDateTime.now());
    }

    /** Runs one reminder check using the given time. */
    void checkAt(LocalDateTime now) {
        if (checking) return;
        checking = true;
        try {
            List<Event> due = controller.getDueReminders(now);
            for (Event event : due) {
                // Mark the event before showing the popup so the same reminder cannot fire twice.
                controller.markCompleted(event.getId());
                if (shouldShowPopup(event, now)) {
                    showReminderPopup(event, now);
                } else {
                    System.err.println("Missed reminder skipped (app was closed): event_id="
                            + event.getId());
                }
            }
        } catch (RuntimeException ex) {
            // If the database fails, skip this check; the next timer tick tries again.
            System.err.println("Reminder check failed, will retry: " + ex.getMessage());
        } finally {
            checking = false;
        }
    }

    /** Show the popup unless the event started more than MISSED_THRESHOLD_MINUTES ago. */
    public static boolean shouldShowPopup(Event event, LocalDateTime now) {
        return event.getDateTime().isAfter(now.minusMinutes(MISSED_THRESHOLD_MINUTES));
    }

    /** Builds the text shown in the reminder popup. */
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

    /** Window title, e.g. "Glide Reminder - Exam". */
    public static String buildReminderTitle(Event event) {
        return "Glide Reminder - " + event.getCategory().getDisplayName();
    }

    /** e.g. 15 -> "15 minutes", 90 -> "1 hour 30 minutes", 1440 -> "1 day". */
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

    /** Shows the always-on-top reminder dialog and waits for the user to click OK. */
    public void showReminderPopup(Event event, LocalDateTime now) {
        JOptionPane pane = new JOptionPane(buildReminderMessage(event, now), JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(null, buildReminderTitle(event));
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        dialog.dispose();
    }
}
