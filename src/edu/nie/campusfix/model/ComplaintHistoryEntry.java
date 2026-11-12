package edu.nie.campusfix.model;

import edu.nie.campusfix.enums.AuditActionType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit trail record capturing state and lifecycle events on a complaint.
 */
public class ComplaintHistoryEntry {
    private final String entryId;
    private final Instant timestamp;
    private final String actorId;
    private final String actorName;
    private final AuditActionType actionType;
    private final String notes;

    public ComplaintHistoryEntry(String entryId, Instant timestamp, String actorId,
                                 String actorName, AuditActionType actionType, String notes) {
        this.entryId = entryId == null ? UUID.randomUUID().toString() : entryId;
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
        this.actorId = Objects.requireNonNull(actorId, "Actor ID cannot be null");
        this.actorName = actorName == null ? "System" : actorName;
        this.actionType = Objects.requireNonNull(actionType, "Action type cannot be null");
        this.notes = notes == null ? "" : notes;
    }

    public static ComplaintHistoryEntry create(String actorId, String actorName,
                                               AuditActionType actionType, String notes) {
        return new ComplaintHistoryEntry(UUID.randomUUID().toString(), Instant.now(),
                actorId, actorName, actionType, notes);
    }

    public String getEntryId() {
        return entryId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + actionType.getDisplayName() + " by " + actorName +
                (notes.isEmpty() ? "" : ": " + notes);
    }
}
