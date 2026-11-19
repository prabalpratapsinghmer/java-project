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
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generates real-time analytics, ID-accurate technician workload summaries,
 * overdue SLA reports, and RFC-4180 CSV exports based strictly on stored data.
 */
public class ReportService {
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public ReportService(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = Objects.requireNonNull(complaintRepository, "Complaint repository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "User repository cannot be null");
    }

    /**
     * Exact per-technician workload entry keyed reliably by technician UUID.
     */
    public record TechnicianWorkloadEntry(
            String id,
            String technicianCode,
            String fullName,
            String username,
            Category specialization,
            boolean active,
            long activeTasks,
            long resolvedTasks
    ) {}

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
            Map<String, Long> technicianWorkload,
            Map<String, Long> activeWorkloadByTechId,
            List<TechnicianWorkloadEntry> technicianWorkloads,
            List<Complaint> overdueComplaints
    ) {}

    /**
     * Aggregates real-time metrics across all persistent complaint records.
     */
    public ComplaintSummaryReport generateSummary() {
        List<Complaint> all = complaintRepository.findAll();
        List<TechnicianUser> technicians = userRepository.findAllTechnicians();

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

        Map<String, Long> activeByTechId = new HashMap<>();
        Map<String, Long> resolvedByTechId = new HashMap<>();
        for (TechnicianUser tech : technicians) {
            activeByTechId.put(tech.getId(), 0L);
            resolvedByTechId.put(tech.getId(), 0L);
        }

        List<Complaint> overdueList = new ArrayList<>();

        for (Complaint c : all) {
            switch (c.getStatus()) {
                case PENDING -> pending++;
                case ASSIGNED -> assigned++;
                case IN_PROGRESS -> inProgress++;
                case RESOLVED -> resolved++;
            }
            statusCounts.put(c.getStatus(), statusCounts.get(c.getStatus()) + 1);
            categoryCounts.put(c.getCategory(), categoryCounts.get(c.getCategory()) + 1);
            priorityCounts.put(c.getPriority(), priorityCounts.get(c.getPriority()) + 1);

            if (c.isOverdue()) {
                overdue++;
                overdueList.add(c);
            }

            String techId = c.getAssignedTechnicianId();
            if (techId != null && !techId.isBlank()) {
                if (c.getStatus() == ComplaintStatus.RESOLVED) {
                    resolvedByTechId.put(techId, resolvedByTechId.getOrDefault(techId, 0L) + 1);
                } else {
                    activeByTechId.put(techId, activeByTechId.getOrDefault(techId, 0L) + 1);
                }
            }
        }

        overdueList.sort(Comparator.comparing(Complaint::getCreatedAt));

        List<TechnicianWorkloadEntry> workloadEntries = new ArrayList<>();
        Map<String, Long> displayWorkload = new LinkedHashMap<>();
        for (TechnicianUser tech : technicians) {
            long activeCount = activeByTechId.getOrDefault(tech.getId(), 0L);
            long resolvedCount = resolvedByTechId.getOrDefault(tech.getId(), 0L);
            workloadEntries.add(new TechnicianWorkloadEntry(
                    tech.getId(),
                    tech.getTechnicianId(),
                    tech.getFullName(),
                    tech.getUsername(),
                    tech.getSpecialization(),
                    tech.isActive(),
                    activeCount,
                    resolvedCount
            ));
            String label = tech.getFullName() + " [" + tech.getTechnicianId() + "] (" + tech.getSpecialization().getDisplayName() + ")";
            displayWorkload.put(label, activeCount);
        }

        return new ComplaintSummaryReport(
                total, pending, assigned, inProgress, resolved, overdue,
                Collections.unmodifiableMap(categoryCounts),
                Collections.unmodifiableMap(priorityCounts),
                Collections.unmodifiableMap(statusCounts),
                Collections.unmodifiableMap(displayWorkload),
                Collections.unmodifiableMap(activeByTechId),
                Collections.unmodifiableList(workloadEntries),
                Collections.unmodifiableList(overdueList)
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
                "Overdue",
                "Elapsed Time",
                "Response Target (Hours)",
                "Reporter Name",
                "Reporter ID",
                "Assigned Technician",
                "Created At",
                "Updated At",
                "Resolved At",
                "Resolution Notes"
        );

        List<Complaint> all = new ArrayList<>(complaintRepository.findAll());
        all.sort(Comparator.comparing(Complaint::getCreatedAt).reversed());

        List<List<String>> rows = new ArrayList<>();
        for (Complaint c : all) {
            rows.add(List.of(
                    c.getId(),
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getSeverity().getDisplayName(),
                    c.getPriority().getDisplayName(),
                    c.getStatus().getDisplayName(),
                    c.isOverdue() ? "YES" : "NO",
                    c.getFormattedElapsedTime(),
                    String.valueOf(c.getPriority().getResponseTargetHours()),
                    c.getReporterName(),
                    c.getReporterId(),
                    c.getAssignedTechnicianName().isBlank() ? "Unassigned" : c.getAssignedTechnicianName(),
                    c.getCreatedAt().toString(),
                    c.getUpdatedAt().toString(),
                    c.getResolvedAt() != null ? c.getResolvedAt().toString() : "",
                    c.getResolutionNotes()
            ));
        }

        CsvExportUtil.exportCsv(destinationFile, headers, rows);
    }

    /**
     * Exports overdue unresolved complaints report to CSV.
     */
    public void exportOverdueReportToCsv(Path destinationFile) {
        ComplaintSummaryReport summary = generateSummary();
        List<String> headers = List.of(
                "Complaint ID",
                "Title",
                "Category",
                "Location",
                "Priority",
                "Status",
                "Assigned Technician",
                "Submitted At",
                "Elapsed Time",
                "Response Target (Hours)",
                "Overdue Status"
        );

        List<List<String>> rows = new ArrayList<>();
        for (Complaint c : summary.overdueComplaints()) {
            rows.add(List.of(
                    c.getId(),
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority().getDisplayName(),
                    c.getStatus().getDisplayName(),
                    c.getAssignedTechnicianName().isBlank() ? "Unassigned" : c.getAssignedTechnicianName(),
                    c.getCreatedAt().toString(),
                    c.getFormattedElapsedTime(),
                    String.valueOf(c.getPriority().getResponseTargetHours()),
                    c.getFormattedOverdueStatus()
            ));
        }

        CsvExportUtil.exportCsv(destinationFile, headers, rows);
    }

    /**
     * Exports consolidated summary breakdown (totals, category, priority, status, technician workload) to CSV.
     */
    public void exportSummaryReportToCsv(Path destinationFile) {
        ComplaintSummaryReport summary = generateSummary();
        List<String> headers = List.of("Section", "Metric", "Count", "Details");
        List<List<String>> rows = new ArrayList<>();

        rows.add(List.of("Overview", "Total Complaints", String.valueOf(summary.totalComplaints()), ""));
        rows.add(List.of("Overview", "Pending", String.valueOf(summary.pendingCount()), ""));
        rows.add(List.of("Overview", "Assigned", String.valueOf(summary.assignedCount()), ""));
        rows.add(List.of("Overview", "In Progress", String.valueOf(summary.inProgressCount()), ""));
        rows.add(List.of("Overview", "Resolved", String.valueOf(summary.resolvedCount()), ""));
        rows.add(List.of("Overview", "Overdue (Unresolved)", String.valueOf(summary.overdueCount()), ""));

        for (Map.Entry<Category, Long> e : summary.categoryBreakdown().entrySet()) {
            rows.add(List.of("Category", e.getKey().getDisplayName(), String.valueOf(e.getValue()), ""));
        }
        for (Map.Entry<Priority, Long> e : summary.priorityBreakdown().entrySet()) {
            rows.add(List.of("Priority", e.getKey().getDisplayName(), String.valueOf(e.getValue()),
                    "Target: " + e.getKey().getResponseTargetHours() + "h"));
        }
        for (Map.Entry<ComplaintStatus, Long> e : summary.statusBreakdown().entrySet()) {
            rows.add(List.of("Status", e.getKey().getDisplayName(), String.valueOf(e.getValue()), ""));
        }
        for (TechnicianWorkloadEntry t : summary.technicianWorkloads()) {
            rows.add(List.of("Technician Workload", t.fullName() + " (" + t.technicianCode() + ")",
                    String.valueOf(t.activeTasks()),
                    "Specialization: " + t.specialization().getDisplayName()
                            + ", Resolved: " + t.resolvedTasks()
                            + ", Status: " + (t.active() ? "Active" : "Inactive")));
        }

        CsvExportUtil.exportCsv(destinationFile, headers, rows);
    }
}
