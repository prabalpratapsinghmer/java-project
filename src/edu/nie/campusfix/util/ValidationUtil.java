package edu.nie.campusfix.util;

import edu.nie.campusfix.exception.ValidationException;
import java.util.regex.Pattern;

/**
 * Validates domain inputs, lengths, identifiers, and security boundaries before persistence.
 */
public final class ValidationUtil {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{7,20}$");
    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{3,64}$");

    private ValidationUtil() {
        // Utility
    }

    public static void validateId(String id, String fieldName) {
        if (id == null || id.isBlank() || !SAFE_ID_PATTERN.matcher(id.trim()).matches()) {
            throw new ValidationException("Invalid " + fieldName + " format.");
        }
    }

    public static void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new ValidationException("Username must be 3-30 characters and contain only letters, numbers, or underscores.");
        }
    }

    public static void validatePassword(char[] password) {
        if (password == null || password.length < 6 || password.length > 128) {
            throw new ValidationException("Password must be between 6 and 128 characters long.");
        }
    }

    public static void validateFullName(String fullName) {
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            throw new ValidationException("Full name must be between 2 and 100 characters.");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || email.trim().length() > 150 || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Invalid email format (e.g. user@campus.edu).");
        }
    }

    public static void validateOptionalPhone(String phone) {
        if (phone != null && !phone.isBlank()) {
            if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
                throw new ValidationException("Phone number must be 7-20 characters (digits, +, -, spaces, parentheses).");
            }
        }
    }

    public static void validateOptionalText(String value, String fieldName, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            throw new ValidationException(fieldName + " cannot exceed " + maxLength + " characters.");
        }
    }

    public static void validateComplaintFields(String title, String location, String description) {
        if (title == null || title.trim().length() < 3 || title.trim().length() > 100) {
            throw new ValidationException("Complaint title must be between 3 and 100 characters.");
        }
        if (location == null || location.trim().length() < 2 || location.trim().length() > 100) {
            throw new ValidationException("Location must be between 2 and 100 characters.");
        }
        if (description == null || description.trim().length() < 10 || description.trim().length() > 1000) {
            throw new ValidationException("Description must be between 10 and 1000 characters.");
        }
    }

    public static void validateResolutionNotes(String notes) {
        if (notes == null || notes.trim().length() < 5 || notes.trim().length() > 1000) {
            throw new ValidationException("Resolution notes must be between 5 and 1000 characters.");
        }
    }
}
