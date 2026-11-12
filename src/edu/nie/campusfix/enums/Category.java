package edu.nie.campusfix.enums;

/**
 * Campus complaint infrastructure categories.
 */
public enum Category {
    ELECTRICAL("Electrical"),
    PLUMBING("Plumbing"),
    CLASSROOM_EQUIPMENT("Classroom Equipment"),
    LABORATORY_EQUIPMENT("Laboratory Equipment"),
    HOSTEL_MAINTENANCE("Hostel Maintenance"),
    FURNITURE("Furniture"),
    SANITATION("Sanitation"),
    SAFETY("Safety"),
    OTHER("Other");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * High-risk categories that receive a priority bonus in the deterministic priority engine.
     */
    public boolean isHighRisk() {
        return this == ELECTRICAL || this == SAFETY;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
