package edu.nie.campusfix.repository;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.Role;
import edu.nie.campusfix.exception.StorageException;
import edu.nie.campusfix.model.AdminUser;
import edu.nie.campusfix.model.StudentUser;
import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.util.FileStorageUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * File-based implementation of UserRepository storing user records as Properties files.
 * Provides atomic writes, thread-safe synchronization, and automatic corrupted record isolation.
 */
public class FileUserRepository implements UserRepository {
    private final Path storageDir;
    private final Object lock = new Object();

    public FileUserRepository() {
        this(Path.of("data/users"));
    }

    public FileUserRepository(Path storageDir) {
        this.storageDir = Objects.requireNonNull(storageDir, "Storage directory cannot be null");
        FileStorageUtil.ensureDirectory(this.storageDir);
    }

    @Override
    public Optional<User> findById(String id) {
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
                return Optional.ofNullable(mapPropertiesToUser(props));
            } catch (Exception e) {
                System.err.println("[CampusFix] Warning: Corrupted user record for id=" + id + ": " + e.getMessage());
                FileStorageUtil.quarantineCorruptFile(file);
                return Optional.empty();
            }
        }
    }

    @Override
    public List<User> findAll() {
        synchronized (lock) {
            List<User> users = new ArrayList<>();
            List<Path> files = FileStorageUtil.listPropertiesFiles(storageDir);
            for (Path file : files) {
                try {
                    Properties props = FileStorageUtil.loadProperties(file);
                    User user = mapPropertiesToUser(props);
                    if (user != null) {
                        users.add(user);
                    }
                } catch (Exception e) {
                    System.err.println("[CampusFix] Warning: Skipping malformed user file " + file.getFileName() + ": " + e.getMessage());
                    FileStorageUtil.quarantineCorruptFile(file);
                }
            }
            return users;
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        synchronized (lock) {
            String target = username.trim();
            for (User user : findAll()) {
                if (user.getUsername().equalsIgnoreCase(target)) {
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public List<TechnicianUser> findAllTechnicians() {
        synchronized (lock) {
            List<TechnicianUser> technicians = new ArrayList<>();
            for (User user : findAll()) {
                if (user instanceof TechnicianUser tech) {
                    technicians.add(tech);
                }
            }
            return technicians;
        }
    }

    @Override
    public boolean hasAdminUser() {
        synchronized (lock) {
            for (User user : findAll()) {
                if (user.getRole() == Role.ADMIN) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        synchronized (lock) {
            Path file = FileStorageUtil.resolveSafePath(storageDir, user.getId(), ".properties");
            Properties props = mapUserToProperties(user);
            FileStorageUtil.savePropertiesAtomically(file, props, "CampusFix User: " + user.getUsername());
            return user;
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
                throw new StorageException("Failed to delete user record: " + file, e);
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
    private Properties mapUserToProperties(User user) {
        Properties p = new Properties();
        p.setProperty("id", user.getId());
        p.setProperty("username", user.getUsername());
        p.setProperty("passwordHash", user.getPasswordHash());
        p.setProperty("fullName", user.getFullName());
        p.setProperty("email", user.getEmail());
        p.setProperty("phone", user.getPhone() == null ? "" : user.getPhone());
        p.setProperty("role", user.getRole().name());
        p.setProperty("createdAt", user.getCreatedAt().toString());

        if (user instanceof StudentUser s) {
            p.setProperty("studentIdNumber", s.getStudentIdNumber());
            p.setProperty("department", s.getDepartment());
        } else if (user instanceof AdminUser a) {
            p.setProperty("employeeId", a.getEmployeeId());
            p.setProperty("department", a.getDepartment());
        } else if (user instanceof TechnicianUser t) {
            p.setProperty("technicianId", t.getTechnicianId());
            p.setProperty("specialization", t.getSpecialization().name());
            p.setProperty("active", String.valueOf(t.isActive()));
        }
        return p;
    }

    private User mapPropertiesToUser(Properties p) {
        String id = p.getProperty("id");
        String username = p.getProperty("username");
        String passwordHash = p.getProperty("passwordHash");
        String fullName = p.getProperty("fullName");
        String email = p.getProperty("email");
        String phone = p.getProperty("phone", "");
        String roleStr = p.getProperty("role");
        String createdAtStr = p.getProperty("createdAt");

        if (id == null || username == null || passwordHash == null || roleStr == null) {
            throw new IllegalArgumentException("Missing mandatory user fields in properties");
        }

        Role role = Role.valueOf(roleStr);
        Instant createdAt = createdAtStr != null ? Instant.parse(createdAtStr) : Instant.now();

        return switch (role) {
            case STUDENT -> new StudentUser(id, username, passwordHash, fullName, email, phone,
                    p.getProperty("studentIdNumber", ""), p.getProperty("department", ""), createdAt);
            case ADMIN -> new AdminUser(id, username, passwordHash, fullName, email, phone,
                    p.getProperty("employeeId", ""), p.getProperty("department", ""), createdAt);
            case TECHNICIAN -> {
                String specStr = p.getProperty("specialization", Category.OTHER.name());
                Category spec = Category.valueOf(specStr);
                boolean active = Boolean.parseBoolean(p.getProperty("active", "true"));
                yield new TechnicianUser(id, username, passwordHash, fullName, email, phone,
                        p.getProperty("technicianId", ""), spec, active, createdAt);
            }
        };
    }
}
