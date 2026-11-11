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

public class AlertService {
    public static final int POLL_INTERVAL_MS = 1000;
    public static final int MISSED_THRESHOLD_MINUTES = 5;
    public static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.US);

    private final EventController controller;
    private final Runnable onReminderProcessed;
    private final Timer timer;
    private boolean running = false;
    private boolean checking = false;

    public AlertService(EventController controller) {
        this(controller, null);
    }

    public AlertService(EventController controller, Runnable onReminderProcessed) {
        this.controller = controller;
        this.onReminderProcessed = onReminderProcessed;
        this.timer = new Timer(POLL_INTERVAL_MS, e -> checkPendingReminders());
    }

    public void start() {
        if (!running) {
            running = true;
            checkPendingReminders();
            timer.start();
        }
    }

    public void stop() {
        if (running) {
            running = false;
            timer.stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void checkPendingReminders() {
        checkAt(LocalDateTime.now());
    }

    void checkAt(LocalDateTime now) {
        if (checking) return;
        checking = true;
        try {
            List<Event> due = controller.getDueReminders(now);
            boolean anyProcessed = false;
            for (Event ev : due) {
                controller.markCompleted(ev.getId());
                anyProcessed = true;
                if (shouldShowPopup(ev, now)) {
                    showReminderPopup(ev, now);
                }
            }
            if (anyProcessed && onReminderProcessed != null) {
                onReminderProcessed.run();
            }
        } catch (DatabaseException ex) {
            System.err.println("Reminder check error: " + ex.getMessage());
        } finally {
            checking = false;
        }
    }

    public static boolean shouldShowPopup(Event event, LocalDateTime now) {
        LocalDateTime cutoff = event.getDateTime().plusMinutes(MISSED_THRESHOLD_MINUTES);
        return !now.isAfter(cutoff);
    }

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

    static String formatDuration(long minutes) {
        if (minutes <= 0) return "Now";
        if (minutes == 1) return "1 Minute";
        if (minutes < 60) return minutes + " Minutes";
        long hrs = minutes / 60;
        long rem = minutes % 60;
        if (rem == 0) return hrs == 1 ? "1 Hour" : hrs + " Hours";
        return hrs + " hr " + rem + " min";
    }

    private static void playReminderDing() {
        Thread audioThread = new Thread(() -> {
            try {
                float sampleRate = 44100f;
                int durationMs = 450;
                int numSamples = (int) (durationMs * sampleRate / 1000);
                byte[] buf = new byte[numSamples * 2];
                for (int i = 0; i < numSamples; i++) {
                    double t = i / sampleRate;
                    double env = Math.exp(-6.0 * t);
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

    public void showReminderPopup(Event event, LocalDateTime now) {
        playReminderDing();

        Color bgDark = new Color(18, 20, 24);
        Color gold = new Color(212, 175, 55);
        Color textLight = new Color(240, 240, 245);

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