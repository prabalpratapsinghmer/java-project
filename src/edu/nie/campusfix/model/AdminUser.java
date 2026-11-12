package edu.nie.campusfix.model;

import edu.nie.campusfix.enums.Role;
import java.time.Instant;

/**
 * Concrete model for campus infrastructure administrators with full oversight and assignment rights.
 */
public class AdminUser extends User {
    private final String employeeId;
    private String department;

    public AdminUser(String id, String username, String passwordHash, String fullName,
                     String email, String phone, String employeeId,
                     String department, Instant createdAt) {
        super(id, username, passwordHash, fullName, email, phone, Role.ADMIN, createdAt);
        this.employeeId = employeeId == null ? "" : employeeId;
        this.department = department == null ? "Campus Infrastructure" : department;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department == null ? "" : department;
    }

    @Override
    public String getRoleDescription() {
        return "Campus Infrastructure Administrator (" + department + ")";
    }

    @Override
    public boolean canManageTechnicians() {
        return true;
    }

    @Override
    public boolean canAssignComplaints() {
        return true;
    }

    @Override
    public boolean canUpdateStatus() {
        return true;
    }
}
