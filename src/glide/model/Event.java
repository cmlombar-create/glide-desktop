package glide.model;

import java.time.LocalDateTime;

/**
 * Core domain model representing a single scheduled student-athlete event (SDD Section 4.1 & Table 4-1).
 * Encapsulates the 7 attributes mapped to the SQLite events table and calculates reminder trigger times.
 */
public class Event {
    private int id;                     // Maps to event_id (INTEGER PRIMARY KEY AUTOINCREMENT)
    private String title;               // Maps to title (TEXT NOT NULL, 1..100 chars)
    private String description;         // Maps to description (TEXT NULL, 0..500 chars)
    private LocalDateTime dateTime;     // Maps to date_time (TEXT NOT NULL)
    private Category category;          // Maps to category (TEXT NOT NULL)
    private int alertLeadTime;          // Maps to alert_lead_time (Minutes before event to alert)
    private boolean completed;          // Maps to is_completed (0 = Scheduled/Active, 1 = Completed)

    /** Default no-arg constructor for flexible instantiation. */
    public Event() { }

    /** Constructor for new events prior to database insertion (defaults id to 0 and completed to false). */
    public Event(String title, String description, LocalDateTime dateTime,
                 Category category, int alertLeadTime) {
        this(0, title, description, dateTime, category, alertLeadTime, false);
    }

    /** Full constructor used when hydrating existing Event records from SQLite ResultSet rows. */
    public Event(int id, String title, String description, LocalDateTime dateTime,
                 Category category, int alertLeadTime, boolean isCompleted) {
        this.id = id;
        setTitle(title);
        setDescription(description);
        setDateTime(dateTime);
        setCategory(category);
        setAlertLeadTime(alertLeadTime);
        setCompleted(isCompleted);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    /** Defensively trims leading/trailing whitespace from the event title (SDD Table 4-2). */
    public void setTitle(String title) { this.title = title == null ? null : title.trim(); }

    public String getDescription() { return description; }
    /** Defensively trims optional notes while preserving null if omitted. */
    public void setDescription(String description) { this.description = description == null ? null : description.trim(); }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public int getAlertLeadTime() { return alertLeadTime; }
    public void setAlertLeadTime(int alertLeadTime) { this.alertLeadTime = alertLeadTime; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    /**
     * Computes the exact timestamp when the visual reminder popup should trigger
     * by subtracting alertLeadTime minutes from the scheduled dateTime (SDD Section 4.1).
     */
    public LocalDateTime getReminderTime() {
        return dateTime == null ? null : dateTime.minusMinutes(alertLeadTime);
    }

    /**
     * Evaluates whether this event's reminder is currently due to fire.
     * Returns true if the alert has not yet fired (!completed) and current time is >= reminderTime.
     */
    public boolean isReminderDue(LocalDateTime now) {
        return !completed && now != null && getReminderTime() != null
                && !now.isBefore(getReminderTime());
    }

    @Override
    public String toString() {
        return dateTime + " | " + category + " | " + title + (completed ? " (Completed)" : "");
    }
}