package edu.nie.campusfix.service;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.exception.AuthenticationException;
import edu.nie.campusfix.exception.ResourceNotFoundException;
import edu.nie.campusfix.exception.UnauthorizedException;
import edu.nie.campusfix.exception.ValidationException;
import edu.nie.campusfix.model.AdminUser;
import edu.nie.campusfix.model.StudentUser;
import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.UserRepository;
import edu.nie.campusfix.util.PasswordUtil;
import edu.nie.campusfix.util.ValidationUtil;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles authentication, secure registration, technician account management,
 * and first-launch administrator initialization.
 * Prevents unauthorized privilege escalation and guarantees secure salted PBKDF2 hashing.
 */
public class AuthenticationService {
    private final UserRepository userRepository;

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository cannot be null");
    }

    /**
     * Checks whether an initial administrator account has already been registered.
     */
    public boolean isInitialAdminSetupRequired() {
        return !userRepository.hasAdminUser();
    }

    /**
     * Authenticates a user using constant-time PBKDF2 comparison.
     */
    public User login(String username, char[] password) {
        try {
            if (username == null || username.isBlank() || password == null || password.length == 0) {
                throw new AuthenticationException("Username and password are required.");
            }

            User user = userRepository.findByUsername(username.trim())
                    .orElseThrow(() -> new AuthenticationException("Invalid username or password."));

            if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                throw new AuthenticationException("Invalid username or password.");
            }

            if (user instanceof TechnicianUser tech && !tech.isActive()) {
                throw new AuthenticationException("This technician account is currently inactive. Please contact an administrator.");
            }

            return user;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Registers a new Student/Staff user. Public registration creates ONLY student/staff accounts.
     */
    public StudentUser registerStudent(String username, char[] password, String fullName,
                                       String email, String phone, String studentId, String dept) {
        try {
            ValidationUtil.validateUsername(username);
            ValidationUtil.validatePassword(password);
            ValidationUtil.validateFullName(fullName);
            ValidationUtil.validateEmail(email);
            ValidationUtil.validateOptionalPhone(phone);
            ValidationUtil.validateOptionalText(studentId, "Student / Staff ID", 60);
            ValidationUtil.validateOptionalText(dept, "Department", 80);

            if (userRepository.findByUsername(username.trim()).isPresent()) {
                throw new ValidationException("Username '" + username.trim() + "' is already taken.");
            }

            String passwordHash = PasswordUtil.hashPassword(password);
            StudentUser student = new StudentUser(
                    UUID.randomUUID().toString(),
                    username.trim(),
                    passwordHash,
                    fullName.trim(),
                    email.trim(),
                    phone == null ? "" : phone.trim(),
                    studentId == null ? "" : studentId.trim(),
                    dept == null ? "" : dept.trim(),
                    Instant.now()
            );

            userRepository.create(student);
            return student;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Initializes the first administrator account on first system launch.
     * Subsequent calls are rejected with an UnauthorizedException.
     */
    public AdminUser registerInitialAdmin(String username, char[] password, String fullName,
                                          String email, String phone, String employeeId, String dept) {
        try {
            if (userRepository.hasAdminUser()) {
                throw new UnauthorizedException("Administrator account is already initialized. Public admin registration is disabled.");
            }

            ValidationUtil.validateUsername(username);
            ValidationUtil.validatePassword(password);
            ValidationUtil.validateFullName(fullName);
            ValidationUtil.validateEmail(email);
            ValidationUtil.validateOptionalPhone(phone);
            ValidationUtil.validateOptionalText(employeeId, "Employee ID", 60);
            ValidationUtil.validateOptionalText(dept, "Department", 80);

            if (userRepository.findByUsername(username.trim()).isPresent()) {
                throw new ValidationException("Username '" + username.trim() + "' is already taken.");
            }

            String passwordHash = PasswordUtil.hashPassword(password);
            AdminUser admin = new AdminUser(
                    UUID.randomUUID().toString(),
                    username.trim(),
                    passwordHash,
                    fullName.trim(),
                    email.trim(),
                    phone == null ? "" : phone.trim(),
                    (employeeId == null || employeeId.isBlank()) ? "ADM-001" : employeeId.trim(),
                    (dept == null || dept.isBlank()) ? "Campus Infrastructure" : dept.trim(),
                    Instant.now()
            );

            userRepository.create(admin);
            return admin;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Creates a new maintenance technician account.
     * Enforces role verification: only administrators can create technician accounts.
     */
    public TechnicianUser createTechnician(User creator, String username, char[] password,
                                           String fullName, String email, String phone,
                                           String techId, Category specialization) {
        try {
            Objects.requireNonNull(creator, "Creator cannot be null");
            Objects.requireNonNull(specialization, "Specialization cannot be null");

            if (!creator.canManageTechnicians()) {
                throw new UnauthorizedException("Only administrators can create technician accounts.");
            }

            ValidationUtil.validateUsername(username);
            ValidationUtil.validatePassword(password);
            ValidationUtil.validateFullName(fullName);
            ValidationUtil.validateEmail(email);
            ValidationUtil.validateOptionalPhone(phone);
            ValidationUtil.validateOptionalText(techId, "Technician ID", 60);

            if (userRepository.findByUsername(username.trim()).isPresent()) {
                throw new ValidationException("Username '" + username.trim() + "' is already taken.");
            }

            String passwordHash = PasswordUtil.hashPassword(password);
            TechnicianUser tech = new TechnicianUser(
                    UUID.randomUUID().toString(),
                    username.trim(),
                    passwordHash,
                    fullName.trim(),
                    email.trim(),
                    phone == null ? "" : phone.trim(),
                    (techId == null || techId.isBlank()) ? "TECH-" + (System.currentTimeMillis() % 10000) : techId.trim(),
                    specialization,
                    true,
                    Instant.now()
            );

            userRepository.create(tech);
            return tech;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Activates or deactivates a technician account. Administrator-only operation.
     */
    public TechnicianUser setTechnicianActive(User actor, String technicianId, boolean active) {
        Objects.requireNonNull(actor, "Actor cannot be null");
        ValidationUtil.validateId(technicianId, "Technician ID");

        if (!actor.canManageTechnicians()) {
            throw new UnauthorizedException("Only administrators can manage technician accounts.");
        }

        User user = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with ID: " + technicianId));

        if (!(user instanceof TechnicianUser tech)) {
            throw new ValidationException("User " + user.getUsername() + " is not a technician account.");
        }

        tech.setActive(active);
        userRepository.save(tech);
        return tech;
    }
}
