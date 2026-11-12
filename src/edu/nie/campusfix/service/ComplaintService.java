package edu.nie.campusfix.service;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Role;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.exception.ResourceNotFoundException;
import edu.nie.campusfix.exception.UnauthorizedException;
import edu.nie.campusfix.exception.ValidationException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.ComplaintRepository;
import edu.nie.campusfix.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
/**
 * Core business service orchestrating complaint lifecycle operations,
 * authorization validation, searching, and filtering.
 */
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final PriorityService priorityService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final AssignmentService assignmentService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            PriorityService priorityService,
                            DuplicateDetectionService duplicateDetectionService,
                            AssignmentService assignmentService) {
        this.complaintRepository = Objects.requireNonNull(complaintRepository, "Complaint repository cannot be null");
        this.priorityService = Objects.requireNonNull(priorityService, "Priority service cannot be null");
        this.duplicateDetectionService = Objects.requireNonNull(duplicateDetectionService, "Duplicate service cannot be null");
        this.assignmentService = Objects.requireNonNull(assignmentService, "Assignment service cannot be null");
    }

    /**
     * Submits a new maintenance complaint with automatic deterministic priority calculation.
     */
    public Complaint submitComplaint(User reporter, String title, Category category,
                                     String location, String description, Severity severity) {
        Objects.requireNonNull(reporter, "Reporter cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");

        ValidationUtil.validateComplaintFields(title, location, description);

        Priority priority = priorityService.calculatePriority(severity, category);
        Complaint complaint = Complaint.createNew(reporter.getId(), reporter.getFullName(),
                title.trim(), category, location.trim(), description.trim(), severity, priority);

        return complaintRepository.save(complaint);
    }

    /**
     * Submits a public maintenance complaint without requiring user account credentials.
     * Open to children, campus visitors, parents, and community members.
     */
    public Complaint submitPublicComplaint(String reporterName, String contact, String title,
                                          Category category, String location, String description,
                                          Severity severity) {
        if (reporterName == null || reporterName.trim().length() < 2 || reporterName.trim().length() > 100) {
            throw new ValidationException("Reporter name must be between 2 and 100 characters.");
        }
        ValidationUtil.validateComplaintFields(title, location, description);
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");

        String contactSuffix = (contact == null || contact.trim().isEmpty()) ? "" : " (" + contact.trim() + ")";
        String formattedReporterName = reporterName.trim() + contactSuffix;
        String reporterId = "public_" + UUID.randomUUID().toString().substring(0, 8);

        Priority priority = priorityService.calculatePriority(severity, category);
        Complaint complaint = Complaint.createNew(reporterId, formattedReporterName,
                title.trim(), category, location.trim(), description.trim(), severity, priority);

        return complaintRepository.save(complaint);
    }
    /**
     * Identifies potential duplicate complaints for a location and category.
     */
    public List<Complaint> checkPotentialDuplicates(String location, Category category) {
        return duplicateDetectionService.findPotentialDuplicates(location, category);
    }

    /**
     * Assigns a technician to a complaint. Delegates to AssignmentService.
     */
    public Complaint assignTechnician(User assigner, String complaintId, String technicianId, String note) {
        return assignmentService.assignTechnician(assigner, complaintId, technicianId, note);
    }

    /**
     * Starts maintenance work on an assigned complaint.
     * Enforces that only the assigned technician (or an admin) may begin work.
     */
    public Complaint startWork(User actor, String complaintId, String note) {
        Objects.requireNonNull(actor, "Actor cannot be null");
        Objects.requireNonNull(complaintId, "Complaint ID cannot be null");

        Complaint complaint = getComplaintById(complaintId);

        boolean isAssignedTech = actor.getId().equals(complaint.getAssignedTechnicianId());
        boolean isAdmin = actor.canAssignComplaints();

        if (!isAssignedTech && !isAdmin) {
            throw new UnauthorizedException("Only the assigned technician or an administrator can start work on this complaint.");
        }

        complaint.startWork(actor, note);
        return complaintRepository.save(complaint);
    }

    /**
     * Marks a complaint as resolved with mandatory completion notes.
     * Enforces that only the assigned technician (or an admin) may resolve work.
     */
    public Complaint resolveComplaint(User actor, String complaintId, String resolutionNotes) {
        Objects.requireNonNull(actor, "Actor cannot be null");
        Objects.requireNonNull(complaintId, "Complaint ID cannot be null");

        ValidationUtil.validateResolutionNotes(resolutionNotes);

        Complaint complaint = getComplaintById(complaintId);

        boolean isAssignedTech = actor.getId().equals(complaint.getAssignedTechnicianId());
        boolean isAdmin = actor.canAssignComplaints();

        if (!isAssignedTech && !isAdmin) {
            throw new UnauthorizedException("Only the assigned technician or an administrator can resolve this complaint.");
        }

        complaint.resolve(actor, resolutionNotes);
        return complaintRepository.save(complaint);
    }

    /**
     * Retrieves a single complaint by ID.
     */
    public Complaint getComplaintById(String id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));
    }

    /**
     * Returns all complaints stored in the repository.
     */
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    /**
     * Retrieves complaints visible to a specific user according to their role.
     */
    public List<Complaint> getComplaintsForUser(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        if (user.getRole() == Role.STUDENT) {
            return complaintRepository.findByReporterId(user.getId());
        } else if (user.getRole() == Role.TECHNICIAN) {
            return complaintRepository.findByTechnicianId(user.getId());
        }
        return complaintRepository.findAll();
    }

    /**
     * Returns all unresolved complaints that have exceeded their priority response target.
     */
    public List<Complaint> getOverdueComplaints() {
        List<Complaint> overdue = new ArrayList<>();
        for (Complaint c : complaintRepository.findAll()) {
            if (c.isOverdue()) {
                overdue.add(c);
            }
        }
        return overdue;
    }

    /**
     * Multi-criteria search and filter supporting role scoping, keywords, category, priority, status, and overdue flags.
     */
    public List<Complaint> searchComplaints(User viewer, String query, Category category,
                                            Priority priority, ComplaintStatus status, Boolean overdueOnly) {
        List<Complaint> scope = getComplaintsForUser(viewer);
        List<Complaint> filtered = new ArrayList<>();

        String q = query != null ? query.trim().toLowerCase() : "";

        for (Complaint c : scope) {
            if (category != null && c.getCategory() != category) {
                continue;
            }
            if (priority != null && c.getPriority() != priority) {
                continue;
            }
            if (status != null && c.getStatus() != status) {
                continue;
            }
            if (Boolean.TRUE.equals(overdueOnly) && !c.isOverdue()) {
                continue;
            }
            if (!q.isEmpty()) {
                boolean matches = c.getTitle().toLowerCase().contains(q)
                        || c.getLocation().toLowerCase().contains(q)
                        || c.getId().toLowerCase().contains(q)
                        || c.getDescription().toLowerCase().contains(q)
                        || c.getReporterName().toLowerCase().contains(q)
                        || c.getAssignedTechnicianName().toLowerCase().contains(q);
                if (!matches) {
                    continue;
                }
            }
            filtered.add(c);
        }
        return filtered;
    }
}
