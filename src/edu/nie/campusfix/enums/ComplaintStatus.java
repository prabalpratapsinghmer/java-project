package edu.nie.campusfix.enums;

/**
 * Status lifecycle of a maintenance complaint.
 */
public enum ComplaintStatus {
    PENDING("Pending"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved");

    private final String displayName;

    ComplaintStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Enforce strict status lifecycle transition rules.
     * Reassignment from ASSIGNED or IN_PROGRESS to ASSIGNED is permitted.
     * RESOLVED complaints cannot transition backwards.
     */
    public boolean canTransitionTo(ComplaintStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> next == ASSIGNED;
            case ASSIGNED -> next == IN_PROGRESS || next == ASSIGNED;
            case IN_PROGRESS -> next == RESOLVED || next == ASSIGNED;
            case RESOLVED -> false;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
