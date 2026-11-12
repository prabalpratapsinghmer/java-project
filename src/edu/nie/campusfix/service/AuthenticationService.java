package edu.nie.campusfix.service;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.exception.AuthenticationException;
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
import java.util.Objects;
import java.util.UUID;

/**
 * Handles authentication, secure registration, and administrator initialization.
 * Prevents unauthorized privilege escalation and guarantees secure salted hashing.
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
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            throw new AuthenticationException("Username and password are required.");
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password."));

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }

        return user;
    }

    /**
     * Registers a new Student/Staff user. Public registration creates ONLY student/staff accounts.
     */
    public StudentUser registerStudent(String username, char[] password, String fullName,
                                       String email, String phone, String studentId, String dept) {
        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);
        ValidationUtil.validateFullName(fullName);
        ValidationUtil.validateEmail(email);

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

        userRepository.save(student);
        return student;
    }

    /**
     * Initializes the first administrator account on first system launch.
     * Subsequent calls are rejected with an UnauthorizedException.
     */
    public AdminUser registerInitialAdmin(String username, char[] password, String fullName,
                                          String email, String phone, String employeeId, String dept) {
        if (userRepository.hasAdminUser()) {
            throw new UnauthorizedException("Administrator account is already initialized. Public admin registration is disabled.");
        }

        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);
        ValidationUtil.validateFullName(fullName);
        ValidationUtil.validateEmail(email);

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
                employeeId == null ? "ADM-001" : employeeId.trim(),
                dept == null ? "Administration" : dept.trim(),
                Instant.now()
        );

        userRepository.save(admin);
        return admin;
    }

    /**
     * Creates a new maintenance technician account.
     * Enforces role verification: only administrators can create technician accounts.
     */
    public TechnicianUser createTechnician(User creator, String username, char[] password,
                                           String fullName, String email, String phone,
                                           String techId, Category specialization) {
        Objects.requireNonNull(creator, "Creator cannot be null");
        Objects.requireNonNull(specialization, "Specialization cannot be null");

        if (!creator.canManageTechnicians()) {
            throw new UnauthorizedException("Only administrators can create technician accounts.");
        }

        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);
        ValidationUtil.validateFullName(fullName);
        ValidationUtil.validateEmail(email);

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
                techId == null ? "TECH-" + System.currentTimeMillis() % 10000 : techId.trim(),
                specialization,
                true,
                Instant.now()
        );

        userRepository.save(tech);
        return tech;
    }
}
