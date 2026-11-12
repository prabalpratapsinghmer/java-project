# Java Object-Oriented Programming (OOP) Concepts in CampusFix

This document provides concrete code citations from the CampusFix source repository demonstrating the practical application of core Java OOP principles required for the Experiential Learning project evaluation.

---

## 1. Encapsulation
**Concept:** Bundling data (attributes) and methods that operate on the data into a single unit while restricting direct external access to prevent internal state corruption.

### Concrete Implementation:
- **File:** `src/edu/nie/campusfix/model/Complaint.java`
  - Instance variables (`id`, `title`, `status`, `assignedTechnicianId`, `auditHistory`, etc.) are declared `private`.
  - External code cannot mutate complaint status directly; it must invoke domain transition methods such as `assignTechnician(...)`, `startWork(...)`, or `resolve(...)`.
  - Invariants are verified prior to mutation:
    ```java
    if (!status.canTransitionTo(ComplaintStatus.IN_PROGRESS)) {
        throw new ValidationException("Cannot start work from status: " + status.getDisplayName());
    }
    ```
- **Defensive Copying:** In `Complaint.java`, `getAuditHistory()` wraps the internal list using `Collections.unmodifiableList(auditHistory)` so callers cannot clear or append entries outside domain methods.

---

## 2. Inheritance
**Concept:** Mechanism where a subclass inherits properties and behaviors from a superclass, fostering code reuse and hierarchical organization.

### Concrete Implementation:
- **Superclass:** `src/edu/nie/campusfix/model/User.java`
  - Encapsulates shared user attributes: `id`, `username`, `passwordHash`, `fullName`, `email`, `phone`, `role`, and `createdAt`.
- **Subclasses:**
  - `src/edu/nie/campusfix/model/StudentUser.java extends User`: Adds student-specific attributes (`studentIdNumber`, `department`).
  - `src/edu/nie/campusfix/model/AdminUser.java extends User`: Adds administrator attributes (`employeeId`, `department`).
  - `src/edu/nie/campusfix/model/TechnicianUser.java extends User`: Adds technician attributes (`technicianId`, `specialization`, `active`).

---

## 3. Polymorphism
**Concept:** The ability of different classes to respond to the same method call with subclass-specific behavior (dynamic dispatch).

### Concrete Implementation:
- **File:** `src/edu/nie/campusfix/model/User.java` declares abstract behavioral contracts:
  ```java
  public abstract boolean canManageTechnicians();
  public abstract boolean canAssignComplaints();
  public abstract boolean canUpdateStatus();
  ```
- **Subclass Overrides:**
  - In `StudentUser.java`:
    ```java
    @Override public boolean canAssignComplaints() { return false; }
    @Override public boolean canManageTechnicians() { return false; }
    ```
  - In `AdminUser.java`:
    ```java
    @Override public boolean canAssignComplaints() { return true; }
    @Override public boolean canManageTechnicians() { return true; }
    ```
  - In `TechnicianUser.java`:
    ```java
    @Override public boolean canUpdateStatus() { return true; }
    @Override public boolean canAssignComplaints() { return false; }
    ```
- **Dynamic Polymorphism in Action:** In `AssignmentService.java`:
  ```java
  if (!assigner.canAssignComplaints()) {
      throw new UnauthorizedException("Only administrators are authorized to assign complaints.");
  }
  ```
  The caller treats `assigner` polymorphically as a `User` without needing to check `instanceof AdminUser`.

---

## 4. Abstraction
**Concept:** Hiding implementation details and exposing only essential features through abstract classes and interfaces.

### Concrete Implementation:
- **Base Class Abstraction:** `User.java` is declared `public abstract class User`. Direct instantiation is forbidden; only concrete role representations (`StudentUser`, `AdminUser`, `TechnicianUser`) can be created.
- **Repository Abstraction:** `Repository<T, ID>` in `src/edu/nie/campusfix/repository/Repository.java` defines entity lifecycle contracts (`save`, `findById`, `findAll`, `deleteById`) without exposing underlying file system properties or directory paths.

---

## 5. Interfaces
**Concept:** Pure behavioral contracts that decouple callers from concrete implementations, facilitating testability and modularity.

### Concrete Implementation:
- **File:** `src/edu/nie/campusfix/repository/UserRepository.java`
  ```java
  public interface UserRepository extends Repository<User, String> {
      Optional<User> findByUsername(String username);
      List<TechnicianUser> findAllTechnicians();
      boolean hasAdminUser();
  }
  ```
- **File:** `src/edu/nie/campusfix/repository/ComplaintRepository.java`
  ```java
  public interface ComplaintRepository extends Repository<Complaint, String> {
      List<Complaint> findByReporterId(String reporterId);
      List<Complaint> findByTechnicianId(String technicianId);
      List<Complaint> findByStatus(ComplaintStatus status);
  }
  ```
- Services (`ComplaintService`, `AssignmentService`, `ReportService`) declare interface dependencies rather than coupling to `FileUserRepository` or `FileComplaintRepository`.

---

## 6. Generics
**Concept:** Parameterized types enabling compile-time type safety and eliminating explicit casting.

### Concrete Implementation:
- **File:** `src/edu/nie/campusfix/repository/Repository.java`
  ```java
  public interface Repository<T, ID> {
      Optional<T> findById(ID id);
      List<T> findAll();
      T save(T entity);
      boolean deleteById(ID id);
      boolean existsById(ID id);
  }
  ```
- In `FileUserRepository implements UserRepository` (which extends `Repository<User, String>`), type parameter `T` is resolved to `User` and `ID` to `String`.

---

## 7. Java Collections Framework
**Concept:** Standardized architecture for storing and manipulating groups of objects.

### Concrete Implementations:
- `List<Complaint>` and `ArrayList<Complaint>`: Used across `ComplaintService` for searching, filtering, and sorting.
- `Map<Category, Long>`, `Map<Priority, Long>`, `Map<ComplaintStatus, Long>`: Used in `ReportService.java` with specialized `EnumMap` collections for efficient constant-time metric aggregation.
- `LinkedHashMap<String, Long>`: Used in `ReportService.java` to preserve insertion order when computing technician active workloads.
- `Collections.unmodifiableList(...)`: Used in `Complaint.java` to ensure immutability of audit history trails.

---

## 8. Custom Exception Handling Hierarchy
**Concept:** Systematic domain error handling separating recoverable validation issues, unauthorized operations, and storage failures.

### Concrete Implementation:
- **Base:** `src/edu/nie/campusfix/exception/CampusFixException.java extends RuntimeException`
- **Subclasses:**
  - `ValidationException`: Input boundary violations (e.g. password too short, invalid email).
  - `AuthenticationException`: Invalid credentials during login.
  - `UnauthorizedException`: Attempted privilege escalation or prohibited role actions.
  - `ResourceNotFoundException`: Lookup failures for user or complaint identifiers.
  - `StorageException`: File I/O, property parsing, or persistence failures.

---

## 9. Modern Java Features (Java 17+)
- **Pattern Matching for `instanceof`:** `if (o instanceof User user)` eliminates boiler-plate casting.
- **Enhanced `switch` Expressions:** Used in `Priority.fromScore(...)` and `ComplaintStatus.canTransitionTo(...)` for exhaustive compiler-checked branches.
- **Records:** `ReportService.ComplaintSummaryReport` declared as a Java `record` for concise, immutable data-carrier semantics.
- **Date/Time API:** `java.time.Instant` and `java.time.Duration` for timezone-safe response target calculations.
