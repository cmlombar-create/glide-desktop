package glide.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the single shared SQLite JDBC connection, automatic schema bootstrap,
 * composite B-tree indexing, and schema version header (SDD Sections 4.3, 5.5, 5.6, & 8.3).
 */
public final class DatabaseManager {
    // Local serverless SQLite file stored in the working directory
    public static final String URL = "jdbc:sqlite:glide.db";
    // Reused connection instance to eliminate connection-opening overhead on polling ticks
    private static Connection shared;

    private DatabaseManager() { }

    /**
     * Returns the active JDBC connection to glide.db, opening a new one if necessary.
     * Configures a 5000ms busy_timeout so concurrent reads/writes never throw immediate lock errors.
     */
    public static synchronized Connection getConnection() {
        try {
            if (shared == null || shared.isClosed()) {
                shared = DriverManager.getConnection(URL);
                try (Statement s = shared.createStatement()) {
                    s.execute("PRAGMA busy_timeout = 5000");
                }
            }
            return shared;
        } catch (SQLException ex) {
            throw new DatabaseException("Cannot connect to Glide SQLite database", ex);
        }
    }

    /**
     * Self-healing startup routine (SDD Section 5.6 & TC-10):
     * Creates glide.db if missing, verifies column compatibility, creates the 7-column events table,
     * builds the idx_active_events composite index, and stamps PRAGMA user_version = 1.
     */
    public static synchronized void initialize() {
        try {
            Connection conn = getConnection();
            // Inspect existing table columns to protect against incompatible legacy schemas
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("PRAGMA table_info(events)")) {
                Set<String> columns = new HashSet<>();
                while (rs.next()) {
                    columns.add(rs.getString("name"));
                }
                if (!columns.isEmpty() && !columns.equals(Set.of(
                        "event_id", "title", "description", "date_time", "category",
                        "alert_lead_time", "is_completed"))) {
                    throw new DatabaseException("Existing glide.db has an incompatible events schema. "
                            + "Back it up and migrate it before running this version.");
                }
            }
            try (Statement s = conn.createStatement()) {
                // Create the normalized 7-column events table (SDD Table 5-1)
                s.executeUpdate("CREATE TABLE IF NOT EXISTS events ("
                    + "event_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title TEXT NOT NULL, description TEXT NULL, "
                    + "date_time TEXT NOT NULL, category TEXT NOT NULL, "
                    + "alert_lead_time INTEGER NOT NULL DEFAULT 15, "
                    + "is_completed INTEGER NOT NULL DEFAULT 0)");
                // Create composite B-tree index on (is_completed, date_time) for O(log N) polling & sorting
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_active_events "
                    + "ON events (is_completed, date_time)");
                // Stamp SQLite header version to 1 for forward-compatible schema tracking (SDD Table 5-4)
                s.execute("PRAGMA user_version = 1");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Unable to initialize Glide database", ex);
        }
    }

    /** Cleanly closes the shared JDBC connection when Glide exits or resets tests. */
    public static synchronized void close() {
        if (shared != null) {
            try {
                shared.close();
            } catch (SQLException ex) {
                throw new DatabaseException("Unable to close database", ex);
            } finally {
                shared = null;
            }
        }
    }
}