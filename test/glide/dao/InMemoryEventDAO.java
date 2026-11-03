package glide.dao;

import glide.model.Category;
import glide.model.Event;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Test stand-in for EventDAO: keeps events in memory instead of SQLite. */
public class InMemoryEventDAO extends EventDAO {
    public final Map<Integer, Event> rows = new TreeMap<>();
    private int nextId = 1;
    /** Set to true to simulate a database failure on the reminder query. */
    public boolean failReads = false;

    private static Event copy(Event e) {
        return new Event(e.getId(), e.getTitle(), e.getDescription(), e.getDateTime(),
                e.getCategory(), e.getAlertLeadTime(), e.isCompleted());
    }
    private static List<Event> sorted(List<Event> list) {
        list.sort(Comparator.comparing(Event::getDateTime));
        return list;
    }

    @Override public int insertEvent(Event event) {
        int id = nextId++;
        event.setId(id);
        rows.put(id, copy(event));
        return id;
    }
    @Override public List<Event> getEvents(Category filter) {
        List<Event> out = new ArrayList<>();
        for (Event e : rows.values()) if (filter == null || e.getCategory() == filter) out.add(copy(e));
        return sorted(out);
    }
    @Override public Event findEventById(int id) {
        Event e = rows.get(id);
        return e == null ? null : copy(e);
    }
    @Override public boolean updateEvent(Event event) {
        if (!rows.containsKey(event.getId())) return false;
        rows.put(event.getId(), copy(event));
        return true;
    }
    @Override public boolean deleteEvent(int id) { return rows.remove(id) != null; }
    @Override public List<Event> getActiveEvents() {
        if (failReads) throw new DatabaseException("simulated disk error");
        List<Event> out = new ArrayList<>();
        for (Event e : rows.values()) if (!e.isCompleted()) out.add(copy(e));
        return sorted(out);
    }
    @Override public void markCompleted(int id) {
        Event e = rows.get(id);
        if (e != null) e.setCompleted(true);
    }
}
