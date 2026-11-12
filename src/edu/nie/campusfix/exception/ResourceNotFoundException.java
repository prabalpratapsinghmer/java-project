package edu.nie.campusfix.exception;

/**
 * Thrown when an expected user, complaint, or file cannot be located.
 */
public class ResourceNotFoundException extends CampusFixException {
    private static final long serialVersionUID = 1L;
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
