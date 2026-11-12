package edu.nie.campusfix.enums;

/**
 * Subjective severity reported by user upon complaint submission.
 */
public enum Severity {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3);

    private final String displayName;
    private final int baseScore;

    Severity(String displayName, int baseScore) {
        this.displayName = displayName;
        this.baseScore = baseScore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseScore() {
        return baseScore;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
