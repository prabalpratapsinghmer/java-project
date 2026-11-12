package edu.nie.campusfix.model;

import edu.nie.campusfix.enums.Role;
import java.time.Instant;
import java.util.Objects;

/**
 * Abstract base class demonstrating OOP Abstraction and Inheritance.
 * Represents an authenticated user in the CampusFix system with role-based capabilities.
 */
public abstract class User {
    protected final String id;
    protected final String username;
    protected String passwordHash;
    protected String fullName;
    protected String email;
    protected String phone;
    protected final Role role;
    protected final Instant createdAt;

    protected User(String id, String username, String passwordHash, String fullName,
                   String email, String phone, Role role, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.phone = phone == null ? "" : phone;
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "Email cannot be null");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Abstract methods demonstrating Polymorphism across user roles
    public abstract String getRoleDescription();
    public abstract boolean canManageTechnicians();
    public abstract boolean canAssignComplaints();
    public abstract boolean canUpdateStatus();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return fullName + " (" + role.getDisplayName() + ")";
    }
}
