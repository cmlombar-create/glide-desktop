package glide.dao;

import glide.model.Event;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class EventDAO {
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static String text(LocalDateTime time) { return time.format(FORMAT); }

    public void save(Long id, String title, String description, int categoryId,
                     LocalDateTime when, int leadMinutes) throws SQLException {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required.");
        if (!when.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Date/time must be in the future.");
        if (leadMinutes < 0) throw new IllegalArgumentException("Lead time must be nonnegative.");
        String sql = id == null
            ? "INSERT INTO events(title,description,category_id,event_datetime,reminder_lead_minutes,reminder_datetime) VALUES(?,?,?,?,?,?)"
            : "UPDATE events SET title=?,description=?,category_id=?,event_datetime=?,reminder_lead_minutes=?,reminder_datetime=?,alert_shown=0,updated_at=CURRENT_TIMESTAMP WHERE event_id=?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title.trim()); ps.setString(2, description == null ? "" : description);
            ps.setInt(3, categoryId); ps.setString(4, text(when)); ps.setInt(5, leadMinutes);
            ps.setString(6, text(when.minusMinutes(leadMinutes)));
            if (id != null) ps.setLong(7, id);
            ps.executeUpdate();
        }
    }

    public List<Event> list(Integer categoryId) throws SQLException {
        String sql = "SELECT e.*,c.name category FROM events e JOIN categories c ON c.category_id=e.category_id "
                   + (categoryId == null ? "" : "WHERE e.category_id=? ") + "ORDER BY e.event_datetime ASC";
        try (Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)) {
            if (categoryId != null) ps.setInt(1,categoryId);
            try (ResultSet rs=ps.executeQuery()) { return read(rs); }
        }
    }

    public List<Event> due() throws SQLException {
        String sql="SELECT e.*,c.name category FROM events e JOIN categories c ON c.category_id=e.category_id "
                  + "WHERE e.alert_shown=0 AND e.reminder_datetime<=? AND e.event_datetime>? ORDER BY e.reminder_datetime";
        try (Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)) {
            String now=text(LocalDateTime.now()); ps.setString(1,now); ps.setString(2,now);
            try (ResultSet rs=ps.executeQuery()) { return read(rs); }
        }
    }

    private List<Event> read(ResultSet rs) throws SQLException {
        List<Event> events=new ArrayList<>();
        while(rs.next()) events.add(new Event(rs.getLong("event_id"),rs.getString("title"),
          rs.getString("description"),rs.getInt("category_id"),rs.getString("category"),
          rs.getString("event_datetime"),rs.getInt("reminder_lead_minutes"),
          rs.getString("reminder_datetime"),rs.getInt("alert_shown")==1));
        return events;
    }

    public void delete(long id) throws SQLException {
        try (Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM events WHERE event_id=?")) {
            ps.setLong(1,id); ps.executeUpdate();
        }
    }
    public boolean markShown(long id) throws SQLException {
        try (Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement("UPDATE events SET alert_shown=1 WHERE event_id=? AND alert_shown=0")) {
            ps.setLong(1,id); return ps.executeUpdate()>0;
        }
    }
}
