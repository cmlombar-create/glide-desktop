package glide.dao;

import glide.model.Category;
import glide.model.Event;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Parameterized SQLite access; no UI or business validation belongs here. */
public class EventDAO {
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static String format(LocalDateTime date) { return date.format(FORMAT); }
    private Connection connection() { return DatabaseManager.getConnection(); }

    public int insertEvent(Event event) {
        String sql = "INSERT INTO events(title,description,date_time,category,alert_lead_time,is_completed) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, event);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) { int id = keys.getInt(1); event.setId(id); return id; }
            }
            // SQLite driver compatibility fallback.
            try (Statement st = connection().createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) { int id = rs.getInt(1); event.setId(id); return id; }
            }
            throw new DatabaseException("INSERT succeeded but generated ID could not be read");
        } catch (SQLException ex) { throw new DatabaseException("Could not insert event", ex); }
    }
    public List<Event> getEvents(Category categoryFilter) {
        String sql = "SELECT * FROM events" + (categoryFilter == null ? "" : " WHERE category = ?")
                + " ORDER BY date_time ASC";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            if (categoryFilter != null) ps.setString(1, categoryFilter.getDisplayName());
            try (ResultSet rs = ps.executeQuery()) { return collect(rs); }
        } catch (SQLException ex) { throw new DatabaseException("Could not list events", ex); }
    }
    public Event findEventById(int id) {
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM events WHERE event_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? fromRow(rs) : null; }
        } catch (SQLException ex) { throw new DatabaseException("Could not find event", ex); }
    }
    public boolean updateEvent(Event event) {
        String sql = "UPDATE events SET title=?,description=?,date_time=?,category=?,alert_lead_time=?,is_completed=? WHERE event_id=?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            bind(ps, event); ps.setInt(7, event.getId()); return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw new DatabaseException("Could not update event", ex); }
    }
    public boolean deleteEvent(int id) {
        try (PreparedStatement ps = connection().prepareStatement("DELETE FROM events WHERE event_id=?")) {
            ps.setInt(1, id); return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw new DatabaseException("Could not delete event", ex); }
    }
    public List<Event> getActiveEvents() {
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT * FROM events WHERE is_completed=0 ORDER BY date_time ASC");
             ResultSet rs = ps.executeQuery()) { return collect(rs); }
        catch (SQLException ex) { throw new DatabaseException("Could not list active events", ex); }
    }
    public void markCompleted(int id) {
        try (PreparedStatement ps = connection().prepareStatement(
                "UPDATE events SET is_completed=1 WHERE event_id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException ex) { throw new DatabaseException("Could not complete event", ex); }
    }
    private static void bind(PreparedStatement ps, Event event) throws SQLException {
        ps.setString(1, event.getTitle());
        if (event.getDescription() == null) ps.setNull(2, Types.VARCHAR);
        else ps.setString(2, event.getDescription());
        ps.setString(3, format(event.getDateTime()));
        ps.setString(4, event.getCategory().getDisplayName());
        ps.setInt(5, event.getAlertLeadTime());
        ps.setInt(6, event.isCompleted() ? 1 : 0);
    }
    private static Event fromRow(ResultSet rs) throws SQLException {
        return new Event(rs.getInt("event_id"), rs.getString("title"), rs.getString("description"),
            LocalDateTime.parse(rs.getString("date_time"), FORMAT),
            Category.fromDisplayName(rs.getString("category")),
            rs.getInt("alert_lead_time"), rs.getInt("is_completed") == 1);
    }
    private static List<Event> collect(ResultSet rs) throws SQLException {
        List<Event> items = new ArrayList<>();
        while (rs.next()) items.add(fromRow(rs));
        return items;
    }
}
