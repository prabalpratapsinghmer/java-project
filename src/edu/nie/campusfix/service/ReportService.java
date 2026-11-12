package edu.nie.campusfix.service;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.repository.ComplaintRepository;
import edu.nie.campusfix.repository.UserRepository;
import edu.nie.campusfix.util.CsvExportUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generates analytics, workload summaries, and CSV reports based strictly on stored data.
 */
public class ReportService {
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public ReportService(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = Objects.requireNonNull(complaintRepository, "Complaint repository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "User repository cannot be null");
    }

    /**
     * Immutable value object holding consolidated campus maintenance metrics.
     */
    public record ComplaintSummaryReport(
            long totalComplaints,
            long pendingCount,
            long assignedCount,
            long inProgressCount,
            long resolvedCount,
            long overdueCount,
            Map<Category, Long> categoryBreakdown,
            Map<Priority, Long> priorityBreakdown,
            Map<ComplaintStatus, Long> statusBreakdown,
            Map<String, Long> technicianWorkload
    ) {}

    /**
     * Aggregates real-time metrics across all persistent complaint records.
     */
    public ComplaintSummaryReport generateSummary() {
        List<Complaint> all = complaintRepository.findAll();

        long total = all.size();
        long pending = 0;
        long assigned = 0;
        long inProgress = 0;
        long resolved = 0;
        long overdue = 0;

        Map<Category, Long> categoryCounts = new EnumMap<>(Category.class);
        for (Category cat : Category.values()) {
            categoryCounts.put(cat, 0L);
        }

        Map<Priority, Long> priorityCounts = new EnumMap<>(Priority.class);
        for (Priority prio : Priority.values()) {
            priorityCounts.put(prio, 0L);
        }

        Map<ComplaintStatus, Long> statusCounts = new EnumMap<>(ComplaintStatus.class);
        for (ComplaintStatus st : ComplaintStatus.values()) {
            statusCounts.put(st, 0L);
        }

        Map<String, Long> techWorkload = new LinkedHashMap<>();
        for (TechnicianUser tech : userRepository.findAllTechnicians()) {
            techWorkload.put(tech.getFullName() + " (" + tech.getSpecialization().getDisplayName() + ")", 0L);
        }

        for (Complaint c : all) {
            // Status counting
            switch (c.getStatus()) {
                case PENDING -> pending++;
                case ASSIGNED -> assigned++;
                case IN_PROGRESS -> inProgress++;
                case RESOLVED -> resolved++;
            }
            statusCounts.put(c.getStatus(), statusCounts.get(c.getStatus()) + 1);

            // Category & Priority
            categoryCounts.put(c.getCategory(), categoryCounts.get(c.getCategory()) + 1);
            priorityCounts.put(c.getPriority(), priorityCounts.get(c.getPriority()) + 1);

            // Overdue
            if (c.isOverdue()) {
                overdue++;
            }

            // Technician active workload
            if (c.getStatus() != ComplaintStatus.RESOLVED && !c.getAssignedTechnicianName().isBlank()) {
                String key = c.getAssignedTechnicianName();
                // Match to full tech display if available
                String matchedKey = key;
                for (String registeredKey : techWorkload.keySet()) {
                    if (registeredKey.startsWith(key)) {
                        matchedKey = registeredKey;
                        break;
                    }
                }
                techWorkload.put(matchedKey, techWorkload.getOrDefault(matchedKey, 0L) + 1);
            }
        }

        return new ComplaintSummaryReport(
                total, pending, assigned, inProgress, resolved, overdue,
                categoryCounts, priorityCounts, statusCounts, techWorkload
        );
    }

    /**
     * Exports full detailed complaint registry to an RFC-4180 CSV file with formula injection mitigation.
     */
    public void exportComplaintsToCsv(Path destinationFile) {
        List<String> headers = List.of(
                "Complaint ID",
                "Title",
                "Category",
                "Location",
                "Severity",
                "Priority",
                "Status",
                "Reporter Name",
                "Reporter ID",
                "Assigned Technician",
                "Created At",
                "Updated At",
                "Resolved At",
                "Resolution Notes"
        );

        List<List<String>> rows = new ArrayList<>();
        for (Complaint c : complaintRepository.findAll()) {
            List<String> row = List.of(
                    c.getId(),
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getSeverity().getDisplayName(),
                    c.getPriority().getDisplayName(),
                    c.getStatus().getDisplayName(),
                    c.getReporterName(),
                    c.getReporterId(),
                    c.getAssignedTechnicianName(),
                    c.getCreatedAt().toString(),
                    c.getUpdatedAt().toString(),
                    c.getResolvedAt() != null ? c.getResolvedAt().toString() : "",
                    c.getResolutionNotes()
            );
            rows.add(row);
        }

        CsvExportUtil.exportCsv(destinationFile, headers, rows);
    }
}
