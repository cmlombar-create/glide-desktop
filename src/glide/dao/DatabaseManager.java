package glide.dao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the single shared SQLite JDBC connection, automatic schema bootstrap,
 * composite B-tree indexing, and persistent per-user storage.
 */
public final class DatabaseManager {
    public static final String URL = buildDatabaseUrl();
    private static Connection shared;

    private DatabaseManager() { }

    private static String buildDatabaseUrl() {
        try {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path dir = (localAppData != null && !localAppData.isBlank())
                    ? Paths.get(localAppData, "Glide")
                    : Paths.get(System.getProperty("user.home", "."), ".glide");
            Files.createDirectories(dir);
            return "jdbc:sqlite:" + dir.resolve("glide.db").toAbsolutePath();
        } catch (Exception ex) {
            return "jdbc:sqlite:glide.db";
        }
    }

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

    public static synchronized void initialize() {
        try {
            Connection conn = getConnection();
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("PRAGMA table_info(events)")) {
                Set<String> columns = new HashSet<>();
                while (rs.next()) {
                    columns.add(rs.getString("name"));
                }
                if (!columns.isEmpty() && !columns.equals(Set.of(
                        "event_id", "title", "description", "date_time", "category",
                        "alert_lead_time", "is_completed"))) {
                    throw new DatabaseException("Existing glide.db has an incompatible events schema.");
                }
            }
            try (Statement s = conn.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS events ("
                    + "event_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title TEXT NOT NULL, description TEXT NULL, "
                    + "date_time TEXT NOT NULL, category TEXT NOT NULL, "
                    + "alert_lead_time INTEGER NOT NULL DEFAULT 15, "
                    + "is_completed INTEGER NOT NULL DEFAULT 0)");
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_active_events "
                    + "ON events (is_completed, date_time)");
                s.execute("PRAGMA user_version = 1");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Unable to initialize Glide database", ex);
        }
    }

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