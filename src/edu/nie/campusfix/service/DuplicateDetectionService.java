package edu.nie.campusfix.service;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.repository.ComplaintRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic duplicate complaint detection service.
 * Identifies potential duplicate reports filed for the same normalized location and category
 * within the preceding 7 days that are still unresolved.
 */
public class DuplicateDetectionService {
    private static final Duration DUPLICATE_WINDOW = Duration.ofDays(7);
    private final ComplaintRepository complaintRepository;

    public DuplicateDetectionService(ComplaintRepository complaintRepository) {
        this.complaintRepository = Objects.requireNonNull(complaintRepository, "Complaint repository cannot be null");
    }

    /**
     * Immutable group of unresolved complaints suspected to be duplicates of the same issue.
     */
    public record DuplicateGroup(
            Category category,
            String normalizedLocation,
            String displayLocation,
            List<Complaint> complaints
    ) {}

    /**
     * Normalizes a campus location string by lowercasing, stripping punctuation,
     * and collapsing repeated whitespace.
     */
    public String normalizeLocation(String location) {
        if (location == null) {
            return "";
        }
        return location.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Finds unresolved complaints matching the same category and normalized location
     * submitted within the preceding 7 days.
     */
    public List<Complaint> findPotentialDuplicates(String location, Category category) {
        if (location == null || location.isBlank() || category == null) {
            return List.of();
        }

        String targetNormalized = normalizeLocation(location);
        if (targetNormalized.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();
        List<Complaint> duplicates = new ArrayList<>();

        for (Complaint complaint : complaintRepository.findAll()) {
            if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
                continue;
            }
            if (complaint.getCategory() != category) {
                continue;
            }
            if (!isWithinDuplicateWindow(complaint.getCreatedAt(), now)) {
                continue;
            }

            String existingNormalized = normalizeLocation(complaint.getLocation());
            if (existingNormalized.equals(targetNormalized)) {
                duplicates.add(complaint);
            }
        }

        duplicates.sort(Comparator.comparing(Complaint::getCreatedAt).reversed());
        return duplicates;
    }

    /**
     * Finds all clusters of 2+ unresolved complaints sharing the same category and normalized
     * location within the preceding 7 days, for administrator review.
     */
    public List<DuplicateGroup> findAllPotentialDuplicateGroups() {
        Instant now = Instant.now();
        Map<String, List<Complaint>> grouped = new LinkedHashMap<>();

        for (Complaint complaint : complaintRepository.findAll()) {
            if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
                continue;
            }
            if (!isWithinDuplicateWindow(complaint.getCreatedAt(), now)) {
                continue;
            }
            String normLoc = normalizeLocation(complaint.getLocation());
            if (normLoc.isEmpty()) {
                continue;
            }
            String key = complaint.getCategory().name() + "::" + normLoc;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(complaint);
        }

        List<DuplicateGroup> groups = new ArrayList<>();
        for (List<Complaint> cluster : grouped.values()) {
            if (cluster.size() >= 2) {
                cluster.sort(Comparator.comparing(Complaint::getCreatedAt).reversed());
                Complaint newest = cluster.get(0);
                groups.add(new DuplicateGroup(
                        newest.getCategory(),
                        normalizeLocation(newest.getLocation()),
                        newest.getLocation(),
                        List.copyOf(cluster)
                ));
            }
        }

        groups.sort(Comparator.comparing((DuplicateGroup g) -> g.complaints().get(0).getCreatedAt()).reversed());
        return groups;
    }

    private boolean isWithinDuplicateWindow(Instant createdAt, Instant now) {
        if (createdAt == null) {
            return false;
        }
        Duration age = Duration.between(createdAt, now);
        if (age.isNegative()) {
            return false;
        }
        return age.compareTo(DUPLICATE_WINDOW) <= 0;
    }
}
