package edu.nie.campusfix.enums;

/**
 * Calculated system priority derived deterministically from severity and category risk.
 */
public enum Priority {
    LOW("Low", 1, 168),          // 7 days (168 hours)
    MEDIUM("Medium", 2, 72),      // 72 hours
    HIGH("High", 3, 48),          // 48 hours
    CRITICAL("Critical", 4, 24);  // 24 hours

    private final String displayName;
    private final int score;
    private final long responseTargetHours;

    Priority(String displayName, int score, long responseTargetHours) {
        this.displayName = displayName;
        this.score = score;
        this.responseTargetHours = responseTargetHours;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getScore() {
        return score;
    }

    /**
     * Recommended response target duration in hours.
     */
    public long getResponseTargetHours() {
        return responseTargetHours;
    }

    /**
     * Map numerical score (clamped between 1 and 4) to Priority enum.
     */
    public static Priority fromScore(int rawScore) {
        int clamped = Math.max(1, Math.min(4, rawScore));
        return switch (clamped) {
            case 1 -> LOW;
            case 2 -> MEDIUM;
            case 3 -> HIGH;
            case 4 -> CRITICAL;
            default -> LOW;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
