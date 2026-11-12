package edu.nie.campusfix.enums;

/**
 * System user roles enforcing role-based access control (RBAC).
 */
public enum Role {
    STUDENT("Student / Staff"),
    ADMIN("Administrator"),
    TECHNICIAN("Maintenance Technician");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
