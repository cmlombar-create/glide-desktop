package glide.model;

import java.time.LocalDateTime;

public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDateTime dateTime;
    private Category category;
    private int alertLeadTime;
    private boolean completed;

    public Event() { }
    public Event(String title, String description, LocalDateTime dateTime,
                 Category category, int alertLeadTime) {
        this(0, title, description, dateTime, category, alertLeadTime, false);
    }
    public Event(int id, String title, String description, LocalDateTime dateTime,
                 Category category, int alertLeadTime, boolean isCompleted) {
        this.id = id;
        setTitle(title); setDescription(description); setDateTime(dateTime);
        setCategory(category); setAlertLeadTime(alertLeadTime); setCompleted(isCompleted);
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? null : title.trim(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? null : description.trim(); }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public int getAlertLeadTime() { return alertLeadTime; }
    public void setAlertLeadTime(int alertLeadTime) { this.alertLeadTime = alertLeadTime; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getReminderTime() {
        return dateTime == null ? null : dateTime.minusMinutes(alertLeadTime);
    }
    public boolean isReminderDue(LocalDateTime now) {
        return !completed && now != null && getReminderTime() != null
                && !now.isBefore(getReminderTime());
    }
    @Override public String toString() {
        return dateTime + " | " + category + " | " + title + (completed ? " (Completed)" : "");
    }
}
