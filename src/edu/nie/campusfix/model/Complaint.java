package edu.nie.campusfix.model;

import edu.nie.campusfix.enums.AuditActionType;
import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.exception.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Core domain aggregate representing a campus maintenance complaint.
 * Encapsulates lifecycle transitions, technician assignment, and audit history.
 */
public class Complaint {
    private final String id;
    private final String reporterId;
    private final String reporterName;
    private String title;
    private Category category;
    private String location;
    private String description;
    private Severity severity;
    private Priority priority;
    private ComplaintStatus status;
    private String assignedTechnicianId;
    private String assignedTechnicianName;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    private String resolutionNotes;
    private final List<ComplaintHistoryEntry> auditHistory;

    public Complaint(String id, String reporterId, String reporterName, String title,
                     Category category, String location, String description,
                     Severity severity, Priority priority, ComplaintStatus status,
                     String assignedTechnicianId, String assignedTechnicianName,
                     Instant createdAt, Instant updatedAt, Instant resolvedAt,
                     String resolutionNotes, List<ComplaintHistoryEntry> auditHistory) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.reporterId = Objects.requireNonNull(reporterId, "Reporter ID cannot be null");
        this.reporterName = reporterName == null ? "Unknown" : reporterName;
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.location = Objects.requireNonNull(location, "Location cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.status = status == null ? ComplaintStatus.PENDING : status;
        this.assignedTechnicianId = assignedTechnicianId == null ? "" : assignedTechnicianId;
        this.assignedTechnicianName = assignedTechnicianName == null ? "" : assignedTechnicianName;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.resolvedAt = resolvedAt;
        this.resolutionNotes = resolutionNotes == null ? "" : resolutionNotes;
        this.auditHistory = new ArrayList<>();
        if (auditHistory != null) {
            this.auditHistory.addAll(auditHistory);
        }
    }

    public static Complaint createNew(String reporterId, String reporterName, String title,
                                      Category category, String location, String description,
                                      Severity severity, Priority priority) {
        String newId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        List<ComplaintHistoryEntry> initialHistory = new ArrayList<>();
        initialHistory.add(new ComplaintHistoryEntry(UUID.randomUUID().toString(), now,
                reporterId, reporterName, AuditActionType.CREATED, "Complaint submitted"));
        return new Complaint(newId, reporterId, reporterName, title, category, location,
                description, severity, priority, ComplaintStatus.PENDING, "", "",
                now, now, null, "", initialHistory);
    }

    public String getId() {
        return id;
    }

    public String getReporterId() {
        return reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.updatedAt = Instant.now();
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.updatedAt = Instant.now();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = Objects.requireNonNull(location, "Location cannot be null");
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.updatedAt = Instant.now();
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
        this.updatedAt = Instant.now();
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.updatedAt = Instant.now();
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public String getAssignedTechnicianName() {
        return assignedTechnicianName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    /**
     * Defensive copy returning unmodifiable view of audit history.
     */
    public List<ComplaintHistoryEntry> getAuditHistory() {
        return Collections.unmodifiableList(auditHistory);
    }

    public void addHistoryEntry(ComplaintHistoryEntry entry) {
        if (entry != null) {
            this.auditHistory.add(entry);
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Assigns complaint to a technician. Can be initial assignment or reassignment.
     */
    public void assignTechnician(TechnicianUser technician, User assigner, String note) {
        Objects.requireNonNull(technician, "Technician cannot be null");
        Objects.requireNonNull(assigner, "Assigner cannot be null");

        if (!status.canTransitionTo(ComplaintStatus.ASSIGNED)) {
            throw new ValidationException("Cannot assign complaint in status: " + status.getDisplayName());
        }

        AuditActionType action = (status == ComplaintStatus.PENDING)
                ? AuditActionType.ASSIGNED
                : AuditActionType.REASSIGNED;

        this.assignedTechnicianId = technician.getId();
        this.assignedTechnicianName = technician.getFullName();
        this.status = ComplaintStatus.ASSIGNED;
        this.updatedAt = Instant.now();

        String comment = (note == null || note.isBlank())
                ? "Assigned to " + technician.getFullName()
                : note;
        addHistoryEntry(ComplaintHistoryEntry.create(assigner.getId(), assigner.getFullName(), action, comment));
    }

    /**
     * Transitions complaint to IN_PROGRESS when work begins.
     */
    public void startWork(User actor, String note) {
        Objects.requireNonNull(actor, "Actor cannot be null");
        if (!status.canTransitionTo(ComplaintStatus.IN_PROGRESS)) {
            throw new ValidationException("Cannot start work from status: " + status.getDisplayName());
        }
        this.status = ComplaintStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
        String comment = (note == null || note.isBlank()) ? "Maintenance work commenced" : note;
        addHistoryEntry(ComplaintHistoryEntry.create(actor.getId(), actor.getFullName(),
                AuditActionType.WORK_STARTED, comment));
    }

    /**
     * Resolves complaint with mandatory completion notes.
     */
    public void resolve(User actor, String resolutionNotes) {
        Objects.requireNonNull(actor, "Actor cannot be null");
        if (resolutionNotes == null || resolutionNotes.trim().length() < 5) {
            throw new ValidationException("Resolution notes must be at least 5 characters.");
        }
        if (!status.canTransitionTo(ComplaintStatus.RESOLVED)) {
            throw new ValidationException("Cannot resolve complaint from status: " + status.getDisplayName());
        }
        this.status = ComplaintStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolutionNotes = resolutionNotes.trim();
        this.updatedAt = this.resolvedAt;
        addHistoryEntry(ComplaintHistoryEntry.create(actor.getId(), actor.getFullName(),
                AuditActionType.RESOLVED, this.resolutionNotes));
    }

    /**
     * Determines whether complaint has exceeded priority response duration.
     * Resolved complaints are never overdue.
     */
    public boolean isOverdue() {
        if (status == ComplaintStatus.RESOLVED) {
            return false;
        }
        long hoursElapsed = Duration.between(createdAt, Instant.now()).toHours();
        return hoursElapsed > priority.getResponseTargetHours();
    }

    /**
     * Returns elapsed hours since complaint submission.
     */
    public long getElapsedHours() {
        Instant endPoint = (status == ComplaintStatus.RESOLVED && resolvedAt != null) ? resolvedAt : Instant.now();
        return Duration.between(createdAt, endPoint).toHours();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Complaint complaint)) return false;
        return id.equals(complaint.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Complaint{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category=" + category +
                ", priority=" + priority +
                ", status=" + status +
                '}';
    }
}
