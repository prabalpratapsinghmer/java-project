package edu.nie.campusfix.repository;

import edu.nie.campusfix.enums.AuditActionType;
import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.exception.StorageException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.ComplaintHistoryEntry;
import edu.nie.campusfix.util.FileStorageUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * File-based implementation of ComplaintRepository storing complaints and their complete
 * audit histories in individual properties files.
 * Provides atomic persistence, overwrite protection, and corrupted file isolation.
 */
public class FileComplaintRepository implements ComplaintRepository {
    private final Path storageDir;
    private final Object lock = new Object();
    private final Set<String> corruptedFileWarnings = new LinkedHashSet<>();

    public FileComplaintRepository() {
        this(FileStorageUtil.resolveDefaultDataDir("complaints"));
    }

    public FileComplaintRepository(Path storageDir) {
        this.storageDir = Objects.requireNonNull(storageDir, "Storage directory cannot be null");
        FileStorageUtil.ensureDirectory(this.storageDir);
    }

    @Override
    public Optional<Complaint> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        synchronized (lock) {
            Path file;
            try {
                file = FileStorageUtil.resolveSafePath(storageDir, id, ".properties");
            } catch (StorageException e) {
                return Optional.empty();
            }
            if (!Files.exists(file)) {
                return Optional.empty();
            }
            try {
                Properties props = FileStorageUtil.loadProperties(file);
                return Optional.ofNullable(mapPropertiesToComplaint(props));
            } catch (Exception e) {
                String msg = "Corrupted complaint record for id=" + id + ": " + e.getMessage();
                System.err.println("[CampusFix] Warning: " + msg);
                corruptedFileWarnings.add(msg);
                FileStorageUtil.quarantineCorruptFile(file);
                return Optional.empty();
            }
        }
    }

    @Override
    public List<Complaint> findAll() {
        synchronized (lock) {
            List<Complaint> complaints = new ArrayList<>();
            List<Path> files = FileStorageUtil.listPropertiesFiles(storageDir);
            for (Path file : files) {
                try {
                    Properties props = FileStorageUtil.loadProperties(file);
                    Complaint complaint = mapPropertiesToComplaint(props);
                    if (complaint != null) {
                        complaints.add(complaint);
                    }
                } catch (Exception e) {
                    String msg = "Skipping malformed complaint file " + file.getFileName() + ": " + e.getMessage();
                    System.err.println("[CampusFix] Warning: " + msg);
                    corruptedFileWarnings.add(msg);
                    FileStorageUtil.quarantineCorruptFile(file);
                }
            }
            return complaints;
        }
    }

    public List<String> getCorruptedFileWarnings() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(corruptedFileWarnings));
        }
    }

    @Override
    public List<Complaint> findByReporterId(String reporterId) {
        if (reporterId == null || reporterId.isBlank()) {
            return List.of();
        }
        synchronized (lock) {
            List<Complaint> result = new ArrayList<>();
            for (Complaint c : findAll()) {
                if (reporterId.equals(c.getReporterId())) {
                    result.add(c);
                }
            }
            return result;
        }
    }

    @Override
    public List<Complaint> findByTechnicianId(String technicianId) {
        if (technicianId == null || technicianId.isBlank()) {
            return List.of();
        }
        synchronized (lock) {
            List<Complaint> result = new ArrayList<>();
            for (Complaint c : findAll()) {
                if (technicianId.equals(c.getAssignedTechnicianId())) {
                    result.add(c);
                }
            }
            return result;
        }
    }

    @Override
    public List<Complaint> findByStatus(ComplaintStatus status) {
        if (status == null) {
            return List.of();
        }
        synchronized (lock) {
            List<Complaint> result = new ArrayList<>();
            for (Complaint c : findAll()) {
                if (c.getStatus() == status) {
                    result.add(c);
                }
            }
            return result;
        }
    }

    @Override
    public Complaint create(Complaint complaint) {
        Objects.requireNonNull(complaint, "Complaint cannot be null");
        synchronized (lock) {
            Path file = FileStorageUtil.resolveSafePath(storageDir, complaint.getId(), ".properties");
            if (Files.exists(file)) {
                throw new StorageException("Complaint record already exists with ID: " + complaint.getId() + ". Refusing to overwrite.");
            }
            Properties props = mapComplaintToProperties(complaint);
            FileStorageUtil.savePropertiesAtomically(file, props, "CampusFix Complaint: " + complaint.getId());
            return complaint;
        }
    }

    @Override
    public Complaint save(Complaint complaint) {
        Objects.requireNonNull(complaint, "Complaint cannot be null");
        synchronized (lock) {
            Path file = FileStorageUtil.resolveSafePath(storageDir, complaint.getId(), ".properties");
            Properties props = mapComplaintToProperties(complaint);
            FileStorageUtil.savePropertiesAtomically(file, props, "CampusFix Complaint: " + complaint.getId());
            return complaint;
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) return false;
        synchronized (lock) {
            Path file;
            try {
                file = FileStorageUtil.resolveSafePath(storageDir, id, ".properties");
            } catch (StorageException e) {
                return false;
            }
            try {
                return Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new StorageException("Failed to delete complaint record: " + file, e);
            }
        }
    }

    @Override
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) return false;
        synchronized (lock) {
            try {
                return Files.exists(FileStorageUtil.resolveSafePath(storageDir, id, ".properties"));
            } catch (StorageException e) {
                return false;
            }
        }
    }
    private Properties mapComplaintToProperties(Complaint c) {
        Properties p = new Properties();
        p.setProperty("id", c.getId());
        p.setProperty("reporterId", c.getReporterId());
        p.setProperty("reporterName", c.getReporterName());
        p.setProperty("title", c.getTitle());
        p.setProperty("category", c.getCategory().name());
        p.setProperty("location", c.getLocation());
        p.setProperty("description", c.getDescription());
        p.setProperty("severity", c.getSeverity().name());
        p.setProperty("priority", c.getPriority().name());
        p.setProperty("status", c.getStatus().name());
        p.setProperty("assignedTechnicianId", c.getAssignedTechnicianId() == null ? "" : c.getAssignedTechnicianId());
        p.setProperty("assignedTechnicianName", c.getAssignedTechnicianName() == null ? "" : c.getAssignedTechnicianName());
        p.setProperty("createdAt", c.getCreatedAt().toString());
        p.setProperty("updatedAt", c.getUpdatedAt().toString());
        if (c.getResolvedAt() != null) {
            p.setProperty("resolvedAt", c.getResolvedAt().toString());
        }
        p.setProperty("resolutionNotes", c.getResolutionNotes() == null ? "" : c.getResolutionNotes());

        List<ComplaintHistoryEntry> history = c.getAuditHistory();
        p.setProperty("history.count", String.valueOf(history.size()));
        for (int i = 0; i < history.size(); i++) {
            ComplaintHistoryEntry entry = history.get(i);
            String prefix = "history." + i + ".";
            p.setProperty(prefix + "id", entry.getEntryId());
            p.setProperty(prefix + "timestamp", entry.getTimestamp().toString());
            p.setProperty(prefix + "actorId", entry.getActorId());
            p.setProperty(prefix + "actorName", entry.getActorName());
            p.setProperty(prefix + "action", entry.getActionType().name());
            p.setProperty(prefix + "notes", entry.getNotes() == null ? "" : entry.getNotes());
        }
        return p;
    }

    private Complaint mapPropertiesToComplaint(Properties p) {
        String id = p.getProperty("id");
        String reporterId = p.getProperty("reporterId");
        String reporterName = p.getProperty("reporterName", "Unknown");
        String title = p.getProperty("title");
        String categoryStr = p.getProperty("category");
        String location = p.getProperty("location");
        String description = p.getProperty("description");
        String severityStr = p.getProperty("severity");
        String priorityStr = p.getProperty("priority");
        String statusStr = p.getProperty("status");
        String assignedTechId = p.getProperty("assignedTechnicianId", "");
        String assignedTechName = p.getProperty("assignedTechnicianName", "");
        String createdAtStr = p.getProperty("createdAt");
        String updatedAtStr = p.getProperty("updatedAt");
        String resolvedAtStr = p.getProperty("resolvedAt");
        String resolutionNotes = p.getProperty("resolutionNotes", "");

        if (id == null || reporterId == null || title == null || categoryStr == null
                || location == null || description == null
                || severityStr == null || priorityStr == null || statusStr == null) {
            throw new IllegalArgumentException("Missing mandatory complaint fields in properties");
        }

        Category category = Category.valueOf(categoryStr);
        Severity severity = Severity.valueOf(severityStr);
        Priority priority = Priority.valueOf(priorityStr);
        ComplaintStatus status = ComplaintStatus.valueOf(statusStr);
        Instant createdAt = createdAtStr != null ? Instant.parse(createdAtStr) : Instant.now();
        Instant updatedAt = updatedAtStr != null ? Instant.parse(updatedAtStr) : createdAt;
        Instant resolvedAt = resolvedAtStr != null && !resolvedAtStr.isBlank() ? Instant.parse(resolvedAtStr) : null;

        List<ComplaintHistoryEntry> historyList = new ArrayList<>();
        String countStr = p.getProperty("history.count", "0");
        int count = Integer.parseInt(countStr);
        for (int i = 0; i < count; i++) {
            String prefix = "history." + i + ".";
            String hId = p.getProperty(prefix + "id");
            String hTimestampStr = p.getProperty(prefix + "timestamp");
            String hActorId = p.getProperty(prefix + "actorId");
            String hActorName = p.getProperty(prefix + "actorName");
            String hActionStr = p.getProperty(prefix + "action");
            String hNotes = p.getProperty(prefix + "notes", "");

            if (hId != null && hActionStr != null) {
                Instant hTimestamp = hTimestampStr != null ? Instant.parse(hTimestampStr) : Instant.now();
                AuditActionType hAction = AuditActionType.valueOf(hActionStr);
                historyList.add(new ComplaintHistoryEntry(hId, hTimestamp, hActorId, hActorName, hAction, hNotes));
            }
        }

        return new Complaint(id, reporterId, reporterName, title, category, location,
                description, severity, priority, status, assignedTechId, assignedTechName,
                createdAt, updatedAt, resolvedAt, resolutionNotes, historyList);
    }
}
