package glide.controller;

import glide.dao.EventDAO;
import glide.model.Category;
import glide.model.Event;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Objects;

public class EventController {
    private static final Set<Integer> LEADS = Set.of(0, 5, 10, 15, 30, 60, 120, 1440);
    private final EventDAO dao;
    public EventController() { this(new EventDAO()); }
    public EventController(EventDAO dao) { this.dao = Objects.requireNonNull(dao); }

    public List<String> validateInputs(String title, String description, LocalDateTime dateTime,
                                       Category category, int alertLeadTime) {
        List<String> errors = new ArrayList<>();
        if (title == null || title.trim().isEmpty() || title.trim().length() > 100)
            errors.add("Title must contain 1 to 100 characters.");
        if (description != null && description.length() > 500)
            errors.add("Description cannot exceed 500 characters.");
        if (dateTime == null || !dateTime.isAfter(LocalDateTime.now()))
            errors.add("Date/time must be strictly in the future.");
        if (category == null) errors.add("Category is required.");
        if (!LEADS.contains(alertLeadTime)) errors.add("Invalid alert lead time.");
        return errors;
    }
    private void validate(Event e) throws ValidationException {
        List<String> errors = validateInputs(e.getTitle(), e.getDescription(), e.getDateTime(),
                                             e.getCategory(), e.getAlertLeadTime());
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
    public int addEvent(String title, String description, LocalDateTime dateTime,
                        Category category, int alertLeadTime) throws ValidationException {
        Event event = new Event(title, description, dateTime, category, alertLeadTime);
        validate(event);
        return dao.insertEvent(event);
    }
    public int addEvent(Event event) throws ValidationException {
        validate(event);
        event.setCompleted(false);
        return dao.insertEvent(event);
    }
    public boolean updateEvent(Event event) throws ValidationException {
        validate(event);
        Event original = dao.findEventById(event.getId());
        if (original == null) return false;
        if (!Objects.equals(original.getDateTime(), event.getDateTime())
                || original.getAlertLeadTime() != event.getAlertLeadTime()) event.setCompleted(false);
        return dao.updateEvent(event);
    }
    public boolean deleteEvent(int id) { return dao.deleteEvent(id); }
    public List<Event> getEvents(Category filter) { return dao.getEvents(filter); }
    public Event findEventById(int id) { return dao.findEventById(id); }
    public List<Event> getDueReminders(LocalDateTime now) {
        List<Event> results = new ArrayList<>();
        for (Event e : dao.getActiveEvents()) if (e.isReminderDue(now)) results.add(e);
        return results;
    }
    public void markCompleted(int id) { dao.markCompleted(id); }
}
