package glide.dao;

/**
 * Custom unchecked exception that wraps low-level java.sql.SQLException errors (SDD Section 4.6).
 * Prevents raw SQL stack traces from leaking to the end user UI.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    public DatabaseException(String message) {
        super(message);
    }
}