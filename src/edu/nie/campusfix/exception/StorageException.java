package edu.nie.campusfix.exception;

/**
 * Thrown when I/O or persistent file operations fail.
 */
public class StorageException extends CampusFixException {
    private static final long serialVersionUID = 1L;
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
