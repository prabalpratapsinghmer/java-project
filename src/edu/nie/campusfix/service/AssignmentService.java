package edu.nie.campusfix.service;

import edu.nie.campusfix.exception.ResourceNotFoundException;
import edu.nie.campusfix.exception.UnauthorizedException;
import edu.nie.campusfix.exception.ValidationException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.ComplaintRepository;
import edu.nie.campusfix.repository.UserRepository;

import java.util.Objects;

/**
 * Service managing technician assignments and reassignments on maintenance complaints.
 * Strictly enforces administrator authorization in the service layer.
 */
public class AssignmentService {
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public AssignmentService(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = Objects.requireNonNull(complaintRepository, "Complaint repository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "User repository cannot be null");
    }

    /**
     * Assigns or reassigns a complaint to an active technician.
     * Enforces role verification: only administrators can assign complaints.
     */
    public Complaint assignTechnician(User assigner, String complaintId, String technicianId, String note) {
        Objects.requireNonNull(assigner, "Assigner cannot be null");
        Objects.requireNonNull(complaintId, "Complaint ID cannot be null");
        Objects.requireNonNull(technicianId, "Technician ID cannot be null");

        if (!assigner.canAssignComplaints()) {
            throw new UnauthorizedException("Only administrators are authorized to assign or reassign complaints.");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        User targetUser = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with ID: " + technicianId));

        if (!(targetUser instanceof TechnicianUser technician)) {
            throw new ValidationException("User " + targetUser.getUsername() + " is not a registered technician.");
        }

        if (!technician.isActive()) {
            throw new ValidationException("Technician " + technician.getFullName() + " is currently inactive.");
        }

        complaint.assignTechnician(technician, assigner, note);
        return complaintRepository.save(complaint);
    }
}
