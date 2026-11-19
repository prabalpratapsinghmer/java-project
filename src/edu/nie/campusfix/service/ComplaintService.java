package edu.nie.campusfix.service;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Role;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.exception.ResourceNotFoundException;
import edu.nie.campusfix.exception.UnauthorizedException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.ComplaintRepository;
import edu.nie.campusfix.util.ValidationUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Core business service orchestrating complaint lifecycle operations,
 * strict service-layer authorization, duplicate detection, searching, and filtering.
 */
public class ComplaintService {
    public static final String FILTER_UNASSIGNED = "__UNASSIGNED__";

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

        if (reporter.getRole() == Role.TECHNICIAN) {
            throw new UnauthorizedException("Maintenance personnel cannot submit new complaints from a technician account.");
        }

        ValidationUtil.validateComplaintFields(title, location, description);

        Priority priority = priorityService.calculatePriority(severity, category);
        Complaint complaint = Complaint.createNew(reporter.getId(), reporter.getFullName(),
                title.trim(), category, location.trim(), description.trim(), severity, priority);

        return complaintRepository.create(complaint);
    }

    /**
     * Identifies potential duplicate complaints for a location and category within the last 7 days.
     */
    public List<Complaint> checkPotentialDuplicates(String location, Category category) {
        return duplicateDetectionService.findPotentialDuplicates(location, category);
    }

    /**
     * Returns all active unresolved duplicate complaint clusters for administrator review.
     */
    public List<DuplicateDetectionService.DuplicateGroup> getPotentialDuplicateGroups(User viewer) {
        Objects.requireNonNull(viewer, "Viewer cannot be null");
        if (!viewer.canAssignComplaints()) {
            throw new UnauthorizedException("Only administrators can review campus-wide duplicate complaint groups.");
        }
        return duplicateDetectionService.findAllPotentialDuplicateGroups();
    }

    /**
     * Assigns or reassigns a technician to a complaint. Delegates to AssignmentService.
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
        ValidationUtil.validateId(complaintId, "Complaint ID");
        ValidationUtil.validateOptionalText(note, "Work note", 500);

        Complaint complaint = getComplaintById(complaintId);

        boolean isAssignedTech = actor.getRole() == Role.TECHNICIAN
                && actor.getId().equals(complaint.getAssignedTechnicianId());
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
        ValidationUtil.validateId(complaintId, "Complaint ID");
        ValidationUtil.validateResolutionNotes(resolutionNotes);

        Complaint complaint = getComplaintById(complaintId);

        boolean isAssignedTech = actor.getRole() == Role.TECHNICIAN
                && actor.getId().equals(complaint.getAssignedTechnicianId());
        boolean isAdmin = actor.canAssignComplaints();

        if (!isAssignedTech && !isAdmin) {
            throw new UnauthorizedException("Only the assigned technician or an administrator can resolve this complaint.");
        }

        complaint.resolve(actor, resolutionNotes);
        return complaintRepository.save(complaint);
    }

    /**
     * Retrieves a single complaint by ID (internal/service lookup).
     */
    public Complaint getComplaintById(String id) {
        ValidationUtil.validateId(id, "Complaint ID");
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));
    }

    /**
     * Retrieves a single complaint enforcing role-based visibility rules.
     */
    public Complaint getComplaintForUser(User viewer, String id) {
        Objects.requireNonNull(viewer, "Viewer cannot be null");
        Complaint complaint = getComplaintById(id);

        if (viewer.getRole() == Role.ADMIN) {
            return complaint;
        }
        if (viewer.getRole() == Role.STUDENT && viewer.getId().equals(complaint.getReporterId())) {
            return complaint;
        }
        if (viewer.getRole() == Role.TECHNICIAN && viewer.getId().equals(complaint.getAssignedTechnicianId())) {
            return complaint;
        }
        throw new UnauthorizedException("You are not authorized to view this complaint.");
    }

    /**
     * Returns all complaints stored in the repository sorted newest-first.
     */
    public List<Complaint> getAllComplaints() {
        List<Complaint> all = new ArrayList<>(complaintRepository.findAll());
        all.sort(Comparator.comparing(Complaint::getCreatedAt).reversed());
        return all;
    }

    /**
     * Retrieves complaints visible to a specific user according to their role, sorted newest-first.
     */
    public List<Complaint> getComplaintsForUser(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        List<Complaint> result;
        if (user.getRole() == Role.STUDENT) {
            result = new ArrayList<>(complaintRepository.findByReporterId(user.getId()));
        } else if (user.getRole() == Role.TECHNICIAN) {
            result = new ArrayList<>(complaintRepository.findByTechnicianId(user.getId()));
        } else {
            result = new ArrayList<>(complaintRepository.findAll());
        }
        result.sort(Comparator.comparing(Complaint::getCreatedAt).reversed());
        return result;
    }

    /**
     * Returns all unresolved complaints that have exceeded their priority response target, oldest-first.
     */
    public List<Complaint> getOverdueComplaints() {
        List<Complaint> overdue = new ArrayList<>();
        for (Complaint c : complaintRepository.findAll()) {
            if (c.isOverdue()) {
                overdue.add(c);
            }
        }
        overdue.sort(Comparator.comparing(Complaint::getCreatedAt));
        return overdue;
    }

    /**
     * Returns overdue complaints filtered optionally by priority (Admin-only).
     */
    public List<Complaint> getOverdueComplaints(User viewer, Priority priorityFilter) {
        Objects.requireNonNull(viewer, "Viewer cannot be null");
        if (!viewer.canAssignComplaints()) {
            throw new UnauthorizedException("Only administrators can view the campus-wide overdue queue.");
        }
        List<Complaint> overdue = new ArrayList<>();
        for (Complaint c : getOverdueComplaints()) {
            if (priorityFilter == null || c.getPriority() == priorityFilter) {
                overdue.add(c);
            }
        }
        return overdue;
    }

    /**
     * Multi-criteria search and filter supporting role scoping, keywords, category, priority,
     * status, and overdue flags.
     */
    public List<Complaint> searchComplaints(User viewer, String query, Category category,
                                            Priority priority, ComplaintStatus status, Boolean overdueOnly) {
        return searchComplaints(viewer, query, category, priority, status, null, overdueOnly);
    }

    /**
     * Full multi-criteria search and filter including administrator Assigned Technician filtering.
     */
    public List<Complaint> searchComplaints(User viewer, String query, Category category,
                                            Priority priority, ComplaintStatus status,
                                            String assignedTechnicianId, Boolean overdueOnly) {
        List<Complaint> scope = getComplaintsForUser(viewer);
        List<Complaint> filtered = new ArrayList<>();

        String q = query != null ? query.trim().toLowerCase() : "";
        String techFilter = assignedTechnicianId != null ? assignedTechnicianId.trim() : "";

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
            if (!techFilter.isEmpty()) {
                if (FILTER_UNASSIGNED.equals(techFilter)) {
                    if (!c.getAssignedTechnicianId().isBlank()) {
                        continue;
                    }
                } else if (!techFilter.equals(c.getAssignedTechnicianId())) {
                    continue;
                }
            }
            if (Boolean.TRUE.equals(overdueOnly) && !c.isOverdue()) {
                continue;
            }
            if (!q.isEmpty()) {
                boolean matches = c.getTitle().toLowerCase().contains(q)
                        || c.getLocation().toLowerCase().contains(q)
                        || c.getId().toLowerCase().contains(q)
                        || c.getCategory().getDisplayName().toLowerCase().contains(q)
                        || c.getStatus().getDisplayName().toLowerCase().contains(q)
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
