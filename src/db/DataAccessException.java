package db;

import java.sql.SQLException;

/**
 * Unchecked wrapper for SQLException, so UI code does not need try/catch
 * around every database call. The original error is kept as the cause.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /** MySQL error 1062 = duplicate key (e.g. email already registered). */
    public static DataAccessException of(String message, SQLException e) {
        if (e.getErrorCode() == 1062) return new DuplicateEntryException(message, e);
        return new DataAccessException(message, e);
    }
}
