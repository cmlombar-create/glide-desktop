package glide.service;

import glide.controller.EventController;
import glide.dao.DatabaseException;
import glide.model.Event;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;

/**
 * Automated background reminder engine (SDD Section 4.4, Figure 4-2, & Section 6.4).
 * Uses a javax.swing.Timer on the Swing EDT to poll due events, play an audio chime,
 * and display an always-on-top Black & Gold reminder dialog with a "Dismiss" button.
 */
public class AlertService {
    // 1-second polling interval so reminders pop up right as the clock changes
    public static final int POLL_INTERVAL_MS = 1000;
    // Stale threshold: skip visual popups for events that started > 5 minutes ago (TC-09)
    public static final int MISSED_THRESHOLD_MINUTES = 5;
    // 12-hour AM/PM timestamp format for student-friendly display
    public static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.US);

    private final EventController controller;
    private final Runnable onReminderProcessed;
    private final Timer timer;
    private boolean running = false;
    private boolean checking = false; // Re-entrancy guard preventing overlapping timer ticks

    public AlertService(EventController controller) {
        this(controller, null);
    }

    public AlertService(EventController controller, Runnable onReminderProcessed) {
        this.controller = controller;
        this.onReminderProcessed = onReminderProcessed;
        // javax.swing.Timer executes callbacks safely on the Swing Event Dispatch Thread (EDT)
        this.timer = new Timer(POLL_INTERVAL_MS, e -> checkPendingReminders());
    }

    /** Starts the background reminder timer and performs an immediate check. */
    public void start() {
        if (!running) {
            running = true;
            checkPendingReminders();
            timer.start();
        }
    }

    /** Cleanly stops the background reminder timer on window close. */
    public void stop() {
        if (running) {
            running = false;
            timer.stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    /** Evaluates pending reminders against the current system clock time. */
    public void checkPendingReminders() {
        checkAt(LocalDateTime.now());
    }

    /**
     * Core polling algorithm (SDD Figure 4-2):
     * 1. Guards against re-entrant checks if a modal popup is already open.
     * 2. Queries due events (is_completed = 0 and now >= reminderTime).
     * 3. Marks each due event completed in SQLite BEFORE showing the popup (prevents duplicates).
     * 4. Suppresses stale popups (> 5 mins past start time) and refreshes the JTable.
     */
    void checkAt(LocalDateTime now) {
        if (checking) return;
        checking = true;
        try {
            List<Event> due = controller.getDueReminders(now);
            boolean anyProcessed = false;
            for (Event ev : due) {
                // Commit completion state before rendering modal popup (SDD Section 4.4 Step 3)
                controller.markCompleted(ev.getId());
                anyProcessed = true;
                if (shouldShowPopup(ev, now)) {
                    showReminderPopup(ev, now);
                }
            }
            // Trigger MainWindow JTable refresh if any event transitioned to "Completed"
            if (anyProcessed && onReminderProcessed != null) {
                onReminderProcessed.run();
            }
        } catch (DatabaseException ex) {
            System.err.println("Reminder check error: " + ex.getMessage());
        } finally {
            checking = false;
        }
    }

    /** Returns true if the event is within the 5-minute stale cutoff window (TC-09). */
    public static boolean shouldShowPopup(Event event, LocalDateTime now) {
        LocalDateTime cutoff = event.getDateTime().plusMinutes(MISSED_THRESHOLD_MINUTES);
        return !now.isAfter(cutoff);
    }

    /** Builds the formatted reminder message text containing title, category, time, and countdown. */
    public static String buildReminderMessage(Event event, LocalDateTime now) {
        long mins = Duration.between(now, event.getDateTime()).toMinutes();
        String countdown = formatDuration(mins);
        StringBuilder sb = new StringBuilder();
        sb.append(event.getTitle()).append("\n")
          .append("Category: ").append(event.getCategory().getDisplayName()).append("\n")
          .append("Scheduled Time: ").append(event.getDateTime().format(DISPLAY_FORMAT)).append("\n")
          .append("Starting in: ").append(countdown);
        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            sb.append("\nNotes: ").append(event.getDescription().trim());
        }
        return sb.toString();
    }

    public static String buildReminderTitle(Event event) {
        return "Upcoming Event Reminder";
    }

    /** Formats remaining minutes into a clean countdown string (e.g., "Now", "15 Minutes", "1 Hour"). */
    static String formatDuration(long minutes) {
        if (minutes <= 0) return "Now";
        if (minutes == 1) return "1 Minute";
        if (minutes < 60) return minutes + " Minutes";
        long hrs = minutes / 60;
        long rem = minutes % 60;
        if (rem == 0) return hrs == 1 ? "1 Hour" : hrs + " Hours";
        return hrs + " hr " + rem + " min";
    }

    /**
     * Synthesizes a two-tone harmonic chime (A5 + E6) on a daemon background thread
     * when a reminder pops up, falling back to the OS system beep if audio lines are unavailable.
     */
    private static void playReminderDing() {
        Thread audioThread = new Thread(() -> {
            try {
                float sampleRate = 44100f;
                int durationMs = 450;
                int numSamples = (int) (durationMs * sampleRate / 1000);
                byte[] buf = new byte[numSamples * 2];
                for (int i = 0; i < numSamples; i++) {
                    double t = i / sampleRate;
                    double env = Math.exp(-6.0 * t); // Exponential decay envelope for bell chime
                    double wave = 0.65 * Math.sin(2.0 * Math.PI * 880.0 * t)
                                + 0.35 * Math.sin(2.0 * Math.PI * 1318.5 * t);
                    short val = (short) (wave * env * 22000);
                    buf[2 * i] = (byte) (val & 0xff);
                    buf[2 * i + 1] = (byte) ((val >> 8) & 0xff);
                }
                AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, false);
                try (SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
                    line.open(af);
                    line.start();
                    line.write(buf, 0, buf.length);
                    line.drain();
                }
            } catch (Exception ex) {
                Toolkit.getDefaultToolkit().beep();
            }
        }, "Glide-Ding");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    /**
     * Displays the always-on-top modal Black & Gold reminder dialog (SDD Section 6.4 & Figure 6-3).
     */
    public void showReminderPopup(Event event, LocalDateTime now) {
        playReminderDing();

        Color bgDark = new Color(18, 20, 24);
        Color gold = new Color(212, 175, 55);
        Color textLight = new Color(240, 240, 245);

        // Configure modal, always-on-top dialog so the alert interrupts background study/work (US-05)
        JDialog dialog = new JDialog((Frame) null, buildReminderTitle(event), true);
        dialog.setAlwaysOnTop(true);

        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBackground(bgDark);
        main.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(gold, 2),
            BorderFactory.createEmptyBorder(16, 22, 16, 22)
        ));

        JLabel header = new JLabel("Alert! Upcoming Event Reminder", SwingConstants.CENTER);
        header.setForeground(gold);
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        main.add(header, BorderLayout.NORTH);

        long mins = Math.max(0, Duration.between(now, event.getDateTime()).toMinutes());
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(bgDark);

        JLabel titleLbl = new JLabel(event.getTitle(), SwingConstants.CENTER);
        titleLbl.setForeground(textLight);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel catLbl = new JLabel("Category: " + event.getCategory().getDisplayName(), SwingConstants.CENTER);
        catLbl.setForeground(gold);
        catLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        catLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel timeLbl = new JLabel("Scheduled Time: " + event.getDateTime().format(DISPLAY_FORMAT), SwingConstants.CENTER);
        timeLbl.setForeground(textLight);
        timeLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        timeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel startLbl = new JLabel("Starting in: " + formatDuration(mins), SwingConstants.CENTER);
        startLbl.setForeground(gold);
        startLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        startLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(titleLbl);
        center.add(Box.createVerticalStrut(8));
        center.add(catLbl);
        center.add(Box.createVerticalStrut(4));
        center.add(timeLbl);
        center.add(Box.createVerticalStrut(4));
        center.add(startLbl);

        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            JLabel descLbl = new JLabel("Notes: " + event.getDescription().trim(), SwingConstants.CENTER);
            descLbl.setForeground(new Color(200, 200, 210));
            descLbl.setFont(new Font("SansSerif", Font.ITALIC, 12));
            descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(Box.createVerticalStrut(6));
            center.add(descLbl);
        }

        main.add(center, BorderLayout.CENTER);

        // Single "Dismiss" button per SDD Figure 6-3 wireframe
        JButton dismissBtn = new JButton("Dismiss");
        dismissBtn.setBackground(gold);
        dismissBtn.setForeground(bgDark);
        dismissBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        dismissBtn.setFocusPainted(false);
        dismissBtn.setBorder(BorderFactory.createEmptyBorder(6, 24, 6, 24));
        dismissBtn.addActionListener(e -> dialog.dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setBackground(bgDark);
        south.add(dismissBtn);
        main.add(south, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(360, 210));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}