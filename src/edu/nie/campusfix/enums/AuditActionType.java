package edu.nie.campusfix.enums;

/**
 * Types of audited lifecycle actions recorded for complaints.
 */
public enum AuditActionType {
    CREATED("Complaint Created"),
    ASSIGNED("Technician Assigned"),
    REASSIGNED("Technician Reassigned"),
    WORK_STARTED("Work Started"),
    RESOLVED("Complaint Resolved");

    private final String displayName;

    AuditActionType(String displayName) {
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
