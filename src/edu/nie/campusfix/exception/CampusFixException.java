package edu.nie.campusfix.exception;

/**
 * Base unchecked exception for all CampusFix application domain errors.
 */
public class CampusFixException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public CampusFixException(String message) {
        super(message);
    }

    public CampusFixException(String message, Throwable cause) {
        super(message, cause);
    }
}
