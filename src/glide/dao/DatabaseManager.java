package glide.dao;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseManager extends EventDAO {
    private Connection conn;

    public DatabaseManager() {
        super();
        initialize();
    }

    public final void initialize() {
        try {
            conn = DatabaseConnection.getConnection();
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA user_version = 1");
                st.execute("CREATE INDEX IF NOT EXISTS idx_active_events ON events (is_completed, date_time)");
            }
        } catch (Exception ignored) {
        }
    }

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception ignored) {
        }
    }
}