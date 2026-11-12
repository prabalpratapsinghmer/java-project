package edu.nie.campusfix.model;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.Role;
import java.time.Instant;
import java.util.Objects;

/**
 * Concrete model for maintenance personnel specializing in specific infrastructure domains.
 */
public class TechnicianUser extends User {
    private final String technicianId;
    private Category specialization;
    private boolean active;

    public TechnicianUser(String id, String username, String passwordHash, String fullName,
                          String email, String phone, String technicianId,
                          Category specialization, boolean active, Instant createdAt) {
        super(id, username, passwordHash, fullName, email, phone, Role.TECHNICIAN, createdAt);
        this.technicianId = technicianId == null ? "" : technicianId;
        this.specialization = Objects.requireNonNull(specialization, "Specialization cannot be null");
        this.active = active;
    }

    public String getTechnicianId() {
        return technicianId;
    }

    public Category getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Category specialization) {
        this.specialization = Objects.requireNonNull(specialization, "Specialization cannot be null");
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getRoleDescription() {
        return "Maintenance Specialist (" + specialization.getDisplayName() + ")";
    }

    @Override
    public boolean canManageTechnicians() {
        return false;
    }

    @Override
    public boolean canAssignComplaints() {
        return false;
    }

    @Override
    public boolean canUpdateStatus() {
        return true;
    }
}
