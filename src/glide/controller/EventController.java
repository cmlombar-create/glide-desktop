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
 * Validates event input, applies the event rules, and passes storage work to EventDAO.
 */
public class EventController {

    /** Allowed reminder lead times in minutes. 0 = remind at the event time. */
    public static final Set<Integer> ALERT_LEAD_TIMES = Set.of(0, 5, 10, 15, 30, 60, 120, 1440);
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    static final String NOT_FOUND_MESSAGE = "This event no longer exists.";

    private final EventDAO dao;

    public EventController() { this(new EventDAO()); }
    public EventController(EventDAO dao) { this.dao = Objects.requireNonNull(dao); }

    /**
     * Checks every rule and returns all problems found.
     * An empty list means the input is valid.
     */
    public List<String> validateInputs(String title, String description, LocalDateTime dateTime,
                                       Category category, int alertLeadTime) {
        List<String> errors = new ArrayList<>();

        String trimmedTitle = title == null ? "" : title.trim();
        if (trimmedTitle.isEmpty()) {
            errors.add("Title is required.");
        } else if (trimmedTitle.length() > MAX_TITLE_LENGTH) {
            errors.add("Title must be " + MAX_TITLE_LENGTH + " characters or fewer.");
        }

        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("Description must be " + MAX_DESCRIPTION_LENGTH + " characters or fewer.");
        }

        if (dateTime == null) {
            errors.add("Date and time are required.");
        } else if (!dateTime.isAfter(LocalDateTime.now())) {
            errors.add("Event date and time must be in the future.");
        }

        if (category == null) {
            errors.add("Please choose a category.");
        }

        if (!ALERT_LEAD_TIMES.contains(alertLeadTime)) {
            errors.add("Please choose a reminder time from the list.");
        }

        return errors;
    }

    private void validate(Event e) throws ValidationException {
        List<String> errors = validateInputs(e.getTitle(), e.getDescription(), e.getDateTime(),
                e.getCategory(), e.getAlertLeadTime());
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    /** Validates, saves, and returns the new event's ID. Nothing is saved if validation fails. */
    public int addEvent(String title, String description, LocalDateTime dateTime,
                        Category category, int alertLeadTime) throws ValidationException {
        return addEvent(new Event(title, description, dateTime, category, alertLeadTime));
    }

    public int addEvent(Event event) throws ValidationException {
        validate(event);
        event.setCompleted(false);
        return dao.insertEvent(event);
    }

    /**
     * Saves changes to an existing event.
     * Changing the date/time or the alert lead time sets isCompleted back to false so the
     * reminder fires again. Changing only the title, description, or category keeps the
     * reminder status stored in the database.
     *
     * @throws DatabaseException "This event no longer exists." if the event was deleted
     */
    public boolean updateEvent(Event event) throws ValidationException {
        validate(event);
        Event original = dao.findEventById(event.getId());
        if (original == null) throw new DatabaseException(NOT_FOUND_MESSAGE);

        boolean timingChanged = !Objects.equals(original.getDateTime(), event.getDateTime())
                || original.getAlertLeadTime() != event.getAlertLeadTime();
        event.setCompleted(timingChanged ? false : original.isCompleted());

        if (!dao.updateEvent(event)) throw new DatabaseException(NOT_FOUND_MESSAGE);
        return true;
    }

    /**
     * Permanently deletes an event.
     *
     * @throws DatabaseException "This event no longer exists." if there was nothing to delete
     */
    public boolean deleteEvent(int id) {
        if (!dao.deleteEvent(id)) throw new DatabaseException(NOT_FOUND_MESSAGE);
        return true;
    }

    /** Returns events in date order. A null filter returns every category. */
    public List<Event> getEvents(Category filter) { return dao.getEvents(filter); }

    public Event findEventById(int id) { return dao.findEventById(id); }

    /** Events whose reminder has not fired and whose reminder time is at or before {@code now}. */
    public List<Event> getDueReminders(LocalDateTime now) {
        List<Event> results = new ArrayList<>();
        for (Event e : dao.getActiveEvents()) if (e.isReminderDue(now)) results.add(e);
        return results;
    }

    /** Records that a reminder has fired so it is never shown twice (is_completed = 1). */
    public void markCompleted(int id) { dao.markCompleted(id); }
}
