package db;

/** Thrown when an INSERT breaks a UNIQUE rule (same email, same slot, same review twice...). */
public class DuplicateEntryException extends DataAccessException {
    public DuplicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}