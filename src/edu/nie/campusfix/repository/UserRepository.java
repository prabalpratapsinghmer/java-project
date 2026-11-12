package edu.nie.campusfix.repository;

import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Repository contract for managing persistence and query operations for Users.
 */
public interface UserRepository extends Repository<User, String> {
    Optional<User> findByUsername(String username);
    List<TechnicianUser> findAllTechnicians();
    boolean hasAdminUser();
}
