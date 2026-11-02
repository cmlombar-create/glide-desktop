package glide.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final Path DB = Path.of("database", "glide.db");
    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        try { Files.createDirectories(DB.toAbsolutePath().getParent()); }
        catch (IOException e) { throw new SQLException("Unable to create database directory", e); }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB.toAbsolutePath());
        try (Statement statement = conn.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) { conn.close(); throw e; }
        return conn;
    }

    public static void initialize() throws SQLException {
        String ddl;
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/glide_schema.sql")) {
            if (in == null) throw new IOException("Missing glide_schema.sql resource");
            ddl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new SQLException("Cannot read database schema", e); }
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            // Schema contains only fixed DDL and seed statements (no semicolons in values).
            for (String command : ddl.split(";")) {
                String sql = command.trim();
                if (!sql.isBlank()) st.execute(sql);
            }
        }
    }
}
