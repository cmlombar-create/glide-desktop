package glide.dao;

/** Converts low-level JDBC failures into an application-level exception. */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) { super(message, cause); }
    public DatabaseException(String message) { super(message); }
}
