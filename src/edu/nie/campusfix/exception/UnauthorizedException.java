package edu.nie.campusfix.exception;

/**
 * Thrown when an authenticated user attempts an operation prohibited by their role.
 */
public class UnauthorizedException extends CampusFixException {
    private static final long serialVersionUID = 1L;
    public UnauthorizedException(String message) {
        super(message);
    }
}
