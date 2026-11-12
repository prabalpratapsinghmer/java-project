package edu.nie.campusfix.exception;

/**
 * Thrown when data validation constraints fail.
 */
public class ValidationException extends CampusFixException {
    private static final long serialVersionUID = 1L;
    public ValidationException(String message) {
        super(message);
    }
}
