package edu.nie.campusfix.repository;

import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.model.Complaint;
import java.util.List;

/**
 * Repository contract for managing persistence and query operations for Complaints.
 */
public interface ComplaintRepository extends Repository<Complaint, String> {
    List<Complaint> findByReporterId(String reporterId);
    List<Complaint> findByTechnicianId(String technicianId);
    List<Complaint> findByStatus(ComplaintStatus status);
}
