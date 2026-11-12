package edu.nie.campusfix.model;

import edu.nie.campusfix.enums.Role;
import java.time.Instant;

/**
 * Concrete model for student / staff reporting maintenance complaints.
 */
public class StudentUser extends User {
    private final String studentIdNumber;
    private String department;

    public StudentUser(String id, String username, String passwordHash, String fullName,
                       String email, String phone, String studentIdNumber,
                       String department, Instant createdAt) {
        super(id, username, passwordHash, fullName, email, phone, Role.STUDENT, createdAt);
        this.studentIdNumber = studentIdNumber == null ? "" : studentIdNumber;
        this.department = department == null ? "" : department;
    }

    public String getStudentIdNumber() {
        return studentIdNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department == null ? "" : department;
    }

    @Override
    public String getRoleDescription() {
        return "Student / Staff Member (" + (department.isEmpty() ? "General" : department) + ")";
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
        return false;
    }
}
