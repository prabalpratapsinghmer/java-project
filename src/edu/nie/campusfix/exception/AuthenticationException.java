package edu.nie.campusfix.exception;

/**
 * Thrown when credentials fail authentication.
 */
public class AuthenticationException extends CampusFixException {
    private static final long serialVersionUID = 1L;
    public AuthenticationException(String message) {
        super(message);
    }
}
