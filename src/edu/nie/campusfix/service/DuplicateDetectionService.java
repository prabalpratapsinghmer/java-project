package edu.nie.campusfix.service;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.repository.ComplaintRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic duplicate complaint detection service.
 * Identifies potential duplicate reports filed for the exact same location and category
 * within the last 7 days that are still unresolved.
 */
public class DuplicateDetectionService {
    private static final long DUPLICATE_WINDOW_DAYS = 7;
    private final ComplaintRepository complaintRepository;

    public DuplicateDetectionService(ComplaintRepository complaintRepository) {
        this.complaintRepository = Objects.requireNonNull(complaintRepository, "Complaint repository cannot be null");
    }

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

            long daysOld = Duration.between(complaint.getCreatedAt(), now).toDays();
            if (daysOld > DUPLICATE_WINDOW_DAYS) {
                continue;
            }

            String existingNormalized = normalizeLocation(complaint.getLocation());
            if (existingNormalized.equals(targetNormalized)) {
                duplicates.add(complaint);
            }
        }

        return duplicates;
    }
}
