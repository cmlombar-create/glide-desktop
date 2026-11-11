package glide.controller;

import glide.dao.DatabaseException;
import glide.dao.EventDAO;
import glide.model.Category;
import glide.model.Event;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Application/Business Logic Controller (SDD Section 4.2 & Table 4-3).
 * Enforces multi-rule input validation, smart reminder reset on edit, and DAO coordination.
 */
public class EventController {

    /** Whitelist of allowed reminder lead times in minutes (0 = remind at event start time). */
    public static final Set<Integer> ALERT_LEAD_TIMES = Set.of(0, 5, 10, 15, 30, 60, 120, 1440);
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    static final String NOT_FOUND_MESSAGE = "This event no longer exists.";

    private final EventDAO dao;

    public EventController() {
        this(new EventDAO());
    }

    public EventController(EventDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    /**
     * Evaluates all 5 domain validation rules without short-circuiting so the user sees
     * every form error in a single pass (SDD Section 4.2 & TC-07 / TC-08).
     */
    public List<String> validateInputs(String title, String description, LocalDateTime dateTime,
                                       Category category, int alertLeadTime) {
        List<String> errors = new ArrayList<>();

        // Rule 1: Title must be non-blank and <= 100 characters
        String trimmedTitle = title == null ? "" : title.trim();
        if (trimmedTitle.isEmpty()) {
            errors.add("Title is required.");
        } else if (trimmedTitle.length() > MAX_TITLE_LENGTH) {
            errors.add("Title must be " + MAX_TITLE_LENGTH + " characters or fewer.");
        }

        // Rule 2: Optional description must not exceed 500 characters
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("Description must be " + MAX_DESCRIPTION_LENGTH + " characters or fewer.");
        }

        // Rule 3: Date/time is required and must be strictly in the future
        if (dateTime == null) {
            errors.add("Date and time are required.");
        } else if (!dateTime.isAfter(LocalDateTime.now())) {
            errors.add("Event date and time must be in the future.");
        }

        // Rule 4: Category must be selected from the 8 student-athlete categories
        if (category == null) {
            errors.add("Please choose a category.");
        }

        // Rule 5: Reminder lead time must match one of the discrete offset choices
        if (!ALERT_LEAD_TIMES.contains(alertLeadTime)) {
            errors.add("Please choose a reminder time from the list.");
        }

        return errors;
    }

    /** Internal validation gate that throws ValidationException if any rule fails. */
    private void validate(Event e) throws ValidationException {
        List<String> errors = validateInputs(e.getTitle(), e.getDescription(), e.getDateTime(),
                e.getCategory(), e.getAlertLeadTime());
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /** Validates inputs, creates a new Event, persists it to SQLite, and returns the new event_id. */
    public int addEvent(String title, String description, LocalDateTime dateTime,
                        Category category, int alertLeadTime) throws ValidationException {
        return addEvent(new Event(title, description, dateTime, category, alertLeadTime));
    }

    public int addEvent(Event event) throws ValidationException {
        validate(event);
        event.setCompleted(false); // New events always start in Scheduled (incomplete) status
        return dao.insertEvent(event);
    }

    /**
     * Updates an existing event (US-03 & TC-05).
     * Smart Reminder Reset: If the user changes dateTime or alertLeadTime, isCompleted resets
     * to false so the reminder fires again at the newly scheduled time.
     */
    public boolean updateEvent(Event event) throws ValidationException {
        validate(event);
        Event original = dao.findEventById(event.getId());
        if (original == null) {
            throw new DatabaseException(NOT_FOUND_MESSAGE);
        }

        // Check whether the event's timing or reminder offset was modified
        boolean timingChanged = !Objects.equals(original.getDateTime(), event.getDateTime())
                || original.getAlertLeadTime() != event.getAlertLeadTime();
        event.setCompleted(timingChanged ? false : original.isCompleted());

        if (!dao.updateEvent(event)) {
            throw new DatabaseException(NOT_FOUND_MESSAGE);
        }
        return true;
    }

    /** Permanently deletes an event by ID, throwing DatabaseException if already removed (US-04). */
    public boolean deleteEvent(int id) {
        if (!dao.deleteEvent(id)) {
            throw new DatabaseException(NOT_FOUND_MESSAGE);
        }
        return true;
    }

    /** Returns events sorted chronologically, optionally filtered by Category (US-01). */
    public List<Event> getEvents(Category filter) {
        return dao.getEvents(filter);
    }

    public Event findEventById(int id) {
        return dao.findEventById(id);
    }

    /** Returns all active (incomplete) events whose reminder trigger time has arrived. */
    public List<Event> getDueReminders(LocalDateTime now) {
        List<Event> results = new ArrayList<>();
        for (Event e : dao.getActiveEvents()) {
            if (e.isReminderDue(now)) {
                results.add(e);
            }
        }
        return results;
    }

    /** Marks an event's reminder as completed (is_completed = 1) in SQLite. */
    public void markCompleted(int id) {
        dao.markCompleted(id);
    }
}