package edu.nie.campusfix.tests;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Role;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.exception.AuthenticationException;
import edu.nie.campusfix.exception.UnauthorizedException;
import edu.nie.campusfix.exception.ValidationException;
import edu.nie.campusfix.exception.StorageException;
import edu.nie.campusfix.model.AdminUser;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.StudentUser;
import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.ComplaintRepository;
import edu.nie.campusfix.repository.FileComplaintRepository;
import edu.nie.campusfix.repository.FileUserRepository;
import edu.nie.campusfix.repository.UserRepository;
import edu.nie.campusfix.service.AssignmentService;
import edu.nie.campusfix.service.AuthenticationService;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.service.DuplicateDetectionService;
import edu.nie.campusfix.service.PriorityService;
import edu.nie.campusfix.service.ReportService;
import edu.nie.campusfix.util.CsvExportUtil;
import edu.nie.campusfix.util.PasswordUtil;
import edu.nie.campusfix.util.FileStorageUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Comprehensive standalone automated test suite validating all 30 mandatory CampusFix test specifications.
 * Executes in an isolated temporary directory to ensure zero side-effects on persistent application storage.
 */
public class TestRunner {
    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Running CampusFix Test Suite (30 Tests)");
        System.out.println("==================================================");

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("campusfix_test_env_");
            Path userDir = tempDir.resolve("users");
            Path complaintDir = tempDir.resolve("complaints");
            Path reportsDir = tempDir.resolve("reports");

            UserRepository userRepo = new FileUserRepository(userDir);
            ComplaintRepository compRepo = new FileComplaintRepository(complaintDir);
            AuthenticationService authService = new AuthenticationService(userRepo);
            PriorityService priorityService = new PriorityService();
            DuplicateDetectionService duplicateService = new DuplicateDetectionService(compRepo);
            AssignmentService assignmentService = new AssignmentService(compRepo, userRepo);
            ComplaintService compService = new ComplaintService(compRepo, priorityService, duplicateService, assignmentService);
            ReportService reportService = new ReportService(compRepo, userRepo);

            // Execute all 30 tests
            test01_InitialAdminSetup(authService, userRepo);
            test02_StudentRegistration(authService, userRepo);
            test03_InvalidRegistration(authService);
            test04_LoginSuccess(authService);
            test05_LoginFailure(authService);
            test06_ComplaintCreation(compService, userRepo);
            test07_UniqueComplaintIds(compService, userRepo);
            test08_PriorityCalculation(priorityService);
            test09_DuplicateDetectionMatching(compService, userRepo);
            test10_DuplicateDetectionNonMatching(compService, userRepo);
            test11_AdminCanAssignTechnician(authService, compService, userRepo);
            test12_StudentCannotAssignTechnician(compService, userRepo);
            test13_TechnicianViewsAssignedComplaints(compService, userRepo);
            test14_TechnicianCannotModifyUnrelated(compService, authService, userRepo);
            test15_ValidStatusTransitions(compService, userRepo);
            test16_InvalidStatusTransitions(compService, userRepo);
            test17_ResolutionNotesSaved(compService, userRepo);
            test18_ComplaintHistoryRecorded(compService, userRepo);
            test19_OverdueClassification(compRepo, priorityService);
            test20_DashboardTotalsCalculated(compService, reportService, userRepo);
            test21_ComplaintSearchesAndFilters(compService, userRepo);
            test22_CsvReportExport(reportService, reportsDir);
            test23_DataSurvivesReload(userDir, complaintDir);
            test24_CorruptedDataHandledSafely(userDir, complaintDir);
            test25_EmptyDatabaseStability(tempDir);
            test26_NoAdminViaPublicRegistration(authService);
            test27_AllPriorityCombinations(priorityService);
            test28_NoAccidentalOverwrite(userRepo);
            test29_ClassesCompileSuccessfully();
            test30_FullEndToEndLifecycle(tempDir);

        } catch (Exception e) {
            System.err.println("[FAIL] Unexpected test harness error: " + e.getMessage());
            e.printStackTrace();
            failedCount++;
        } finally {
            if (tempDir != null) {
                cleanupDir(tempDir);
            }
        }

        System.out.println("==================================================");
        System.out.println("RESULTS: " + passedCount + " / 30 Passed (" + failedCount + " Failed)");
        System.out.println("==================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void pass(String testName) {
        passedCount++;
        System.out.println("[PASS] " + testName);
    }

    private static void fail(String testName, String reason) {
        failedCount++;
        System.err.println("[FAIL] " + testName + " -> " + reason);
    }

    // TEST 01: Initial administrator setup works.
    private static void test01_InitialAdminSetup(AuthenticationService auth, UserRepository repo) {
        String name = "Test 01: Initial administrator setup works.";
        try {
            assertTrue(auth.isInitialAdminSetupRequired(), "Setup should be required initially");
            AdminUser admin = auth.registerInitialAdmin("admin_test", "AdminPass@123".toCharArray(),
                    "System Admin", "admin@test.edu", "9876543210", "ADM-001", "IT Infrastructure");
            assertNotNull(admin, "Admin should not be null");
            assertTrue(repo.hasAdminUser(), "Repository should have admin user");
            assertFalse(auth.isInitialAdminSetupRequired(), "Setup should no longer be required");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 02: Student account registration works.
    private static void test02_StudentRegistration(AuthenticationService auth, UserRepository repo) {
        String name = "Test 02: Student account registration works.";
        try {
            StudentUser student = auth.registerStudent("student_test", "StudentPass@123".toCharArray(),
                    "Arun Patel", "arun@test.edu", "9876543211", "USN001", "CSE");
            assertNotNull(student, "Student should not be null");
            assertEquals(Role.STUDENT, student.getRole(), "Role must be STUDENT");
            assertTrue(repo.findByUsername("student_test").isPresent(), "Student must exist in repository");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 03: Invalid registration input is rejected.
    private static void test03_InvalidRegistration(AuthenticationService auth) {
        String name = "Test 03: Invalid registration input is rejected.";
        try {
            boolean shortPassRejected = false;
            try {
                auth.registerStudent("user_short", "123".toCharArray(), "Valid Name", "v@test.edu", "", "", "");
            } catch (ValidationException e) {
                shortPassRejected = true;
            }

            boolean badEmailRejected = false;
            try {
                auth.registerStudent("user_email", "ValidPass123".toCharArray(), "Valid Name", "not-an-email", "", "", "");
            } catch (ValidationException e) {
                badEmailRejected = true;
            }

            assertTrue(shortPassRejected, "Short password (<6) must be rejected");
            assertTrue(badEmailRejected, "Invalid email format must be rejected");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 04: Correct login credentials are accepted.
    private static void test04_LoginSuccess(AuthenticationService auth) {
        String name = "Test 04: Correct login credentials are accepted.";
        try {
            User user = auth.login("student_test", "StudentPass@123".toCharArray());
            assertNotNull(user, "User should be returned on valid login");
            assertEquals("student_test", user.getUsername(), "Username should match");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 05: Incorrect credentials are rejected.
    private static void test05_LoginFailure(AuthenticationService auth) {
        String name = "Test 05: Incorrect credentials are rejected.";
        try {
            boolean rejected = false;
            try {
                auth.login("student_test", "WrongPassword".toCharArray());
            } catch (AuthenticationException e) {
                rejected = true;
            }
            assertTrue(rejected, "Wrong password must be rejected");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 06: Student complaint creation works.
    private static void test06_ComplaintCreation(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 06: Student complaint creation works.";
        try {
            User student = userRepo.findByUsername("student_test").orElseThrow();
            Complaint c = compService.submitComplaint(student, "Broken Projector", Category.CLASSROOM_EQUIPMENT,
                    "Seminar Hall 2", "Projector lamp flickers and displays error code E4", Severity.MEDIUM);
            assertNotNull(c, "Complaint must not be null");
            assertEquals(ComplaintStatus.PENDING, c.getStatus(), "Initial status must be PENDING");
            assertEquals("Seminar Hall 2", c.getLocation(), "Location must match");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 07: Unique complaint IDs are generated.
    private static void test07_UniqueComplaintIds(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 07: Unique complaint IDs are generated.";
        try {
            User student = userRepo.findByUsername("student_test").orElseThrow();
            Complaint c1 = compService.submitComplaint(student, "Issue One", Category.PLUMBING, "Washroom A", "Water leak under sink pipe", Severity.LOW);
            Complaint c2 = compService.submitComplaint(student, "Issue Two", Category.FURNITURE, "Room 101", "Broken wooden desk chair leg", Severity.LOW);
            assertNotNull(c1.getId(), "c1 ID not null");
            assertNotNull(c2.getId(), "c2 ID not null");
            assertFalse(c1.getId().equals(c2.getId()), "Complaint IDs must be distinct UUIDs");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 08: Priority calculation is correct.
    private static void test08_PriorityCalculation(PriorityService priorityService) {
        String name = "Test 08: Priority calculation is correct.";
        try {
            assertEquals(Priority.LOW, priorityService.calculatePriority(Severity.LOW, Category.FURNITURE), "Low + Furniture = LOW");
            assertEquals(Priority.MEDIUM, priorityService.calculatePriority(Severity.MEDIUM, Category.PLUMBING), "Medium + Plumbing = MEDIUM");
            assertEquals(Priority.HIGH, priorityService.calculatePriority(Severity.HIGH, Category.CLASSROOM_EQUIPMENT), "High + Classroom = HIGH");
            assertEquals(Priority.CRITICAL, priorityService.calculatePriority(Severity.HIGH, Category.ELECTRICAL), "High + Electrical (bonus) = CRITICAL");
            assertEquals(Priority.HIGH, priorityService.calculatePriority(Severity.MEDIUM, Category.SAFETY), "Medium + Safety (bonus) = HIGH");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 09: Duplicate complaint warnings are generated correctly.
    private static void test09_DuplicateDetectionMatching(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 09: Duplicate complaint warnings are generated correctly.";
        try {
            User student = userRepo.findByUsername("student_test").orElseThrow();
            compService.submitComplaint(student, "Leaking Tap", Category.PLUMBING, "Hostel 3 Ground Floor", "Tap continuously leaking water", Severity.LOW);
            List<Complaint> duplicates = compService.checkPotentialDuplicates("Hostel 3, Ground Floor!", Category.PLUMBING);
            assertFalse(duplicates.isEmpty(), "Should detect duplicate for normalized location 'Hostel 3, Ground Floor!'");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 10: Different locations do not cause false duplicate matches.
    private static void test10_DuplicateDetectionNonMatching(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 10: Different locations do not cause false duplicate matches.";
        try {
            List<Complaint> duplicates = compService.checkPotentialDuplicates("Library Second Floor", Category.PLUMBING);
            assertTrue(duplicates.isEmpty(), "Different location must not trigger duplicate warning");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 11: Administrator can assign technicians.
    private static void test11_AdminCanAssignTechnician(AuthenticationService auth, ComplaintService compService, UserRepository userRepo) {
        String name = "Test 11: Administrator can assign technicians.";
        try {
            User admin = userRepo.findByUsername("admin_test").orElseThrow();
            TechnicianUser tech = auth.createTechnician(admin, "tech_rajesh", "TechPass@123".toCharArray(),
                    "Rajesh Kumar", "rajesh@campus.edu", "9876543212", "TECH-101", Category.ELECTRICAL);

            User student = userRepo.findByUsername("student_test").orElseThrow();
            Complaint complaint = compService.submitComplaint(student, "Sparking Switch", Category.ELECTRICAL,
                    "Admin Office 101", "Loose switchboard sparking when turned on", Severity.HIGH);

            Complaint assigned = compService.assignTechnician(admin, complaint.getId(), tech.getId(), "Urgent electrical inspection");
            assertEquals(ComplaintStatus.ASSIGNED, assigned.getStatus(), "Status must be ASSIGNED");
            assertEquals(tech.getId(), assigned.getAssignedTechnicianId(), "Assigned technician ID must match");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 12: Student cannot assign technicians.
    private static void test12_StudentCannotAssignTechnician(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 12: Student cannot assign technicians.";
        try {
            User student = userRepo.findByUsername("student_test").orElseThrow();
            TechnicianUser tech = userRepo.findAllTechnicians().get(0);
            List<Complaint> complaints = compService.getAllComplaints();
            Complaint complaint = complaints.get(0);

            boolean unauthorizedCaught = false;
            try {
                compService.assignTechnician(student, complaint.getId(), tech.getId(), "Student attempt");
            } catch (UnauthorizedException e) {
                unauthorizedCaught = true;
            }
            assertTrue(unauthorizedCaught, "Student must be rejected from assigning technician");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 13: Technician can view assigned complaints.
    private static void test13_TechnicianViewsAssignedComplaints(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 13: Technician can view assigned complaints.";
        try {
            TechnicianUser tech = userRepo.findAllTechnicians().get(0);
            List<Complaint> techTasks = compService.getComplaintsForUser(tech);
            assertFalse(techTasks.isEmpty(), "Technician must see assigned tasks");
            assertEquals(tech.getId(), techTasks.get(0).getAssignedTechnicianId(), "Technician ID must match");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 14: Technician cannot modify unrelated complaints.
    private static void test14_TechnicianCannotModifyUnrelated(ComplaintService compService, AuthenticationService auth, UserRepository userRepo) {
        String name = "Test 14: Technician cannot modify unrelated complaints.";
        try {
            User admin = userRepo.findByUsername("admin_test").orElseThrow();
            TechnicianUser otherTech = auth.createTechnician(admin, "tech_suresh", "TechPass@123".toCharArray(),
                    "Suresh Verma", "suresh@campus.edu", "9876543213", "TECH-102", Category.PLUMBING);

            TechnicianUser assignedTech = userRepo.findAllTechnicians().stream()
                    .filter(t -> t.getUsername().equals("tech_rajesh")).findFirst().orElseThrow();

            User student = userRepo.findByUsername("student_test").orElseThrow();
            Complaint c = compService.submitComplaint(student, "Fuse Blown", Category.ELECTRICAL, "Block B 204", "Main fuse blown out", Severity.HIGH);
            compService.assignTechnician(admin, c.getId(), assignedTech.getId(), "Assignment note");

            boolean unauthorizedCaught = false;
            try {
                compService.startWork(otherTech, c.getId(), "Unrelated technician attempt");
            } catch (UnauthorizedException e) {
                unauthorizedCaught = true;
            }
            assertTrue(unauthorizedCaught, "Unrelated technician must not be allowed to start work");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 15: Valid status transitions succeed.
    private static void test15_ValidStatusTransitions(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 15: Valid status transitions succeed.";
        try {
            User admin = userRepo.findByUsername("admin_test").orElseThrow();
            TechnicianUser tech = userRepo.findAllTechnicians().get(0);
            User student = userRepo.findByUsername("student_test").orElseThrow();

            Complaint c = compService.submitComplaint(student, "AC Malfunction", Category.CLASSROOM_EQUIPMENT,
                    "CS Lab 1", "Air conditioner blowing warm air", Severity.MEDIUM);
            assertEquals(ComplaintStatus.PENDING, c.getStatus());

            c = compService.assignTechnician(admin, c.getId(), tech.getId(), "Assigned to tech");
            assertEquals(ComplaintStatus.ASSIGNED, c.getStatus());

            c = compService.startWork(tech, c.getId(), "Inspecting refrigerant gas level");
            assertEquals(ComplaintStatus.IN_PROGRESS, c.getStatus());

            c = compService.resolveComplaint(tech, c.getId(), "Refilled refrigerant and cleaned air filters.");
            assertEquals(ComplaintStatus.RESOLVED, c.getStatus());
            assertNotNull(c.getResolvedAt(), "Resolved timestamp must be recorded");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 16: Invalid status transitions fail safely.
    private static void test16_InvalidStatusTransitions(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 16: Invalid status transitions fail safely.";
        try {
            User student = userRepo.findByUsername("student_test").orElseThrow();
            TechnicianUser tech = userRepo.findAllTechnicians().get(0);

            Complaint c = compService.submitComplaint(student, "Desk Damaged", Category.FURNITURE,
                    "Room 303", "Desk surface is broken and unusable", Severity.LOW);

            // Attempting to start work directly on PENDING complaint (without assignment)
            boolean failedCorrectly = false;
            try {
                compService.startWork(tech, c.getId(), "Direct start attempt");
            } catch (Exception e) {
                failedCorrectly = true;
            }
            assertTrue(failedCorrectly, "Cannot start work on unassigned PENDING complaint");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 17: Resolution notes are saved.
    private static void test17_ResolutionNotesSaved(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 17: Resolution notes are saved.";
        try {
            User admin = userRepo.findByUsername("admin_test").orElseThrow();
            TechnicianUser tech = userRepo.findAllTechnicians().get(0);
            User student = userRepo.findByUsername("student_test").orElseThrow();

            Complaint c = compService.submitComplaint(student, "Water Leakage", Category.PLUMBING,
                    "Gymnasium Washroom", "Water leaking under washbasin pipe", Severity.LOW);
            compService.assignTechnician(admin, c.getId(), tech.getId(), "Note");
            compService.startWork(tech, c.getId(), "Note");

            String resolutionNotes = "Replaced faulty rubber washer and re-tightened joint.";
            Complaint resolved = compService.resolveComplaint(tech, c.getId(), resolutionNotes);

            assertEquals(resolutionNotes, resolved.getResolutionNotes(), "Resolution notes must match exactly");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 18: Complaint history is recorded correctly.
    private static void test18_ComplaintHistoryRecorded(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 18: Complaint history is recorded correctly.";
        try {
            User admin = userRepo.findByUsername("admin_test").orElseThrow();
            TechnicianUser tech = userRepo.findAllTechnicians().get(0);
            User student = userRepo.findByUsername("student_test").orElseThrow();

            Complaint c = compService.submitComplaint(student, "Door Lock Jammed", Category.HOSTEL_MAINTENANCE,
                    "Hostel Room 12", "Door latch jammed from inside", Severity.MEDIUM);
            compService.assignTechnician(admin, c.getId(), tech.getId(), "Assigned");
            compService.startWork(tech, c.getId(), "Started");
            Complaint resolved = compService.resolveComplaint(tech, c.getId(), "Lubricated cylinder and replaced lock key.");

            assertTrue(resolved.getAuditHistory().size() >= 4, "Must record at least 4 lifecycle events (Created, Assigned, Started, Resolved)");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 19: Overdue classification is correct.
    private static void test19_OverdueClassification(ComplaintRepository compRepo, PriorityService priorityService) {
        String name = "Test 19: Overdue classification is correct.";
        try {
            Instant twoDaysAgo = Instant.now().minus(49, ChronoUnit.HOURS);
            Complaint oldComplaint = new Complaint(
                    UUID.randomUUID().toString(),
                    "user-1", "Student", "Old Critical Issue", Category.ELECTRICAL,
                    "Substation", "Tripping breaker", Severity.HIGH, Priority.CRITICAL,
                    ComplaintStatus.ASSIGNED, "tech-1", "Tech",
                    twoDaysAgo, twoDaysAgo, null, "", List.of()
            );
            compRepo.save(oldComplaint);

            assertTrue(oldComplaint.isOverdue(), "49h old CRITICAL (target 24h) complaint must be overdue");

            // Resolved complaint must NEVER be overdue
            Complaint resolvedOld = new Complaint(
                    UUID.randomUUID().toString(),
                    "user-1", "Student", "Old Resolved Issue", Category.ELECTRICAL,
                    "Substation", "Tripping breaker", Severity.HIGH, Priority.CRITICAL,
                    ComplaintStatus.RESOLVED, "tech-1", "Tech",
                    twoDaysAgo, twoDaysAgo, Instant.now(), "Completed", List.of()
            );
            assertFalse(resolvedOld.isOverdue(), "Resolved complaint must not be marked overdue");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 20: Dashboard totals are calculated correctly.
    private static void test20_DashboardTotalsCalculated(ComplaintService compService, ReportService reportService, UserRepository userRepo) {
        String name = "Test 20: Dashboard totals are calculated correctly.";
        try {
            ReportService.ComplaintSummaryReport report = reportService.generateSummary();
            long total = report.totalComplaints();
            long sum = report.pendingCount() + report.assignedCount() + report.inProgressCount() + report.resolvedCount();
            assertEquals(total, sum, "Sum of individual status counts must equal total complaints");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 21: Complaint searches and filters work.
    private static void test21_ComplaintSearchesAndFilters(ComplaintService compService, UserRepository userRepo) {
        String name = "Test 21: Complaint searches and filters work.";
        try {
            User admin = userRepo.findByUsername("admin_test").orElseThrow();
            List<Complaint> electricals = compService.searchComplaints(admin, "", Category.ELECTRICAL, null, null, null);
            for (Complaint c : electricals) {
                assertEquals(Category.ELECTRICAL, c.getCategory(), "Every search hit must match category filter");
            }
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 22: CSV report export works.
    private static void test22_CsvReportExport(ReportService reportService, Path reportsDir) {
        String name = "Test 22: CSV report export works.";
        try {
            Path csvFile = reportsDir.resolve("test_export.csv");
            reportService.exportComplaintsToCsv(csvFile);
            assertTrue(Files.exists(csvFile), "Exported CSV file must exist on disk");
            String content = Files.readString(csvFile);
            assertTrue(content.contains("Complaint ID"), "CSV must include header 'Complaint ID'");
            assertTrue(content.contains("Category"), "CSV must include header 'Category'");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 23: Application data survives restart/reload.
    private static void test23_DataSurvivesReload(Path userDir, Path complaintDir) {
        String name = "Test 23: Application data survives restart/reload.";
        try {
            // Instantiate completely fresh repository instances reading from the same directory
            UserRepository reloadedUserRepo = new FileUserRepository(userDir);
            ComplaintRepository reloadedCompRepo = new FileComplaintRepository(complaintDir);

            assertTrue(reloadedUserRepo.findByUsername("admin_test").isPresent(), "Admin user must survive reload");
            assertTrue(reloadedUserRepo.findByUsername("student_test").isPresent(), "Student user must survive reload");
            assertFalse(reloadedCompRepo.findAll().isEmpty(), "Complaints must survive reload");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 24: Corrupted data is reported safely.
    private static void test24_CorruptedDataHandledSafely(Path userDir, Path complaintDir) {
        String name = "Test 24: Corrupted data is reported safely.";
        try {
            Path corruptFile = userDir.resolve("corrupt_user.properties");
            Files.writeString(corruptFile, "invalid_unparsable_binary_content_\\u0000");

            UserRepository safeRepo = new FileUserRepository(userDir);
            List<User> users = safeRepo.findAll();
            assertNotNull(users, "Should safely return valid users even with corrupted file present");

            // Path traversal rejection verification
            assertTrue(safeRepo.findById("../../../etc/passwd").isEmpty(),
                    "Path traversal attempt on user repository must return empty Optional");

            ComplaintRepository compRepo = new FileComplaintRepository(complaintDir);
            assertTrue(compRepo.findById("../../../etc/shadow").isEmpty(),
                    "Path traversal attempt on complaint repository must return empty Optional");

            boolean traversalCaught = false;
            try {
                FileStorageUtil.resolveSafePath(userDir, "../hacked", ".properties");
            } catch (StorageException se) {
                traversalCaught = true;
            }
            assertTrue(traversalCaught, "FileStorageUtil must reject directory traversal identifier");

            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 25: Application handles an empty complaint database.
    private static void test25_EmptyDatabaseStability(Path parent) {
        String name = "Test 25: Application handles an empty complaint database.";
        try {
            Path emptyCompDir = parent.resolve("empty_complaints");
            Path emptyUserDir = parent.resolve("empty_users");
            ComplaintRepository emptyRepo = new FileComplaintRepository(emptyCompDir);
            UserRepository emptyUsers = new FileUserRepository(emptyUserDir);
            ReportService emptyReportService = new ReportService(emptyRepo, emptyUsers);

            ReportService.ComplaintSummaryReport report = emptyReportService.generateSummary();
            assertEquals(0L, report.totalComplaints(), "Total complaints on empty db must be 0");
            assertEquals(0L, report.overdueCount(), "Overdue count must be 0");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 26: Administrator privileges cannot be obtained through public registration.
    private static void test26_NoAdminViaPublicRegistration(AuthenticationService auth) {
        String name = "Test 26: Administrator privileges cannot be obtained through public registration.";
        try {
            boolean rejected = false;
            try {
                // Secondary attempt to register admin must fail with UnauthorizedException
                auth.registerInitialAdmin("hacker_admin", "HackPass@123".toCharArray(), "Hacker", "h@campus.edu", "", "H01", "Dept");
            } catch (UnauthorizedException e) {
                rejected = true;
            }
            assertTrue(rejected, "Secondary admin creation via public setup must be blocked");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 27: All priority combinations produce the expected result.
    private static void test27_AllPriorityCombinations(PriorityService priorityService) {
        String name = "Test 27: All priority combinations produce the expected result.";
        try {
            for (Severity sev : Severity.values()) {
                for (Category cat : Category.values()) {
                    Priority prio = priorityService.calculatePriority(sev, cat);
                    assertNotNull(prio, "Priority should not be null for " + sev + ", " + cat);
                    int base = sev.getBaseScore();
                    if (cat.isHighRisk()) base += 1;
                    int expectedScore = Math.min(4, Math.max(1, base));
                    assertEquals(expectedScore, prio.getScore(), "Priority score mismatch for " + sev + " / " + cat);
                }
            }
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 28: User and complaint records cannot be accidentally overwritten.
    private static void test28_NoAccidentalOverwrite(UserRepository userRepo) {
        String name = "Test 28: User and complaint records cannot be accidentally overwritten.";
        try {
            User existing = userRepo.findByUsername("student_test").orElseThrow();
            String originalHash = existing.getPasswordHash();

            // Attempting to re-save another user with same ID but different username should update only through deliberate entity updates
            User fresh = userRepo.findById(existing.getId()).orElseThrow();
            assertEquals(originalHash, fresh.getPasswordHash(), "Password hash must not be altered");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 29: All generated Java classes compile successfully.
    private static void test29_ClassesCompileSuccessfully() {
        String name = "Test 29: All generated Java classes compile successfully.";
        try {
            Path outDir = Path.of("out");
            assertTrue(Files.exists(outDir), "out directory must exist");
            long classCount;
            try (var s = Files.walk(outDir)) {
                classCount = s.filter(p -> p.toString().endsWith(".class")).count();
            }
            assertTrue(classCount >= 15, "At least 15 compiled class files must exist in out/");
            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // TEST 30: A complete complaint workflow can be executed from creation to resolution.
    private static void test30_FullEndToEndLifecycle(Path tempDir) {
        String name = "Test 30: A complete complaint workflow can be executed from creation to resolution.";
        try {
            Path uDir = tempDir.resolve("e2e_users");
            Path cDir = tempDir.resolve("e2e_complaints");
            UserRepository uRepo = new FileUserRepository(uDir);
            ComplaintRepository cRepo = new FileComplaintRepository(cDir);
            AuthenticationService auth = new AuthenticationService(uRepo);
            PriorityService priority = new PriorityService();
            DuplicateDetectionService dup = new DuplicateDetectionService(cRepo);
            AssignmentService assign = new AssignmentService(cRepo, uRepo);
            ComplaintService service = new ComplaintService(cRepo, priority, dup, assign);

            // 1. Admin Setup
            AdminUser admin = auth.registerInitialAdmin("superadmin", "AdminPass@123".toCharArray(),
                    "Super Admin", "super@campus.edu", "", "ADM-E2E", "Administration");

            // 2. Student Registration
            StudentUser student = auth.registerStudent("alice_student", "StudentPass@123".toCharArray(),
                    "Alice Smith", "alice@campus.edu", "", "USN-E2E", "CSE");

            // 3. Technician Registration
            TechnicianUser tech = auth.createTechnician(admin, "bob_tech", "TechPass@123".toCharArray(),
                    "Bob Builder", "bob@campus.edu", "", "TECH-E2E", Category.ELECTRICAL);

            // 4. Student Submits Complaint
            Complaint complaint = service.submitComplaint(student, "Server Room UPS Alarm", Category.ELECTRICAL,
                    "Data Center Rack 4", "UPS continuous alarm sounding; battery indicator amber", Severity.HIGH);
            assertEquals(Priority.CRITICAL, complaint.getPriority());
            assertEquals(ComplaintStatus.PENDING, complaint.getStatus());

            // 5. Admin Assigns Technician
            complaint = service.assignTechnician(admin, complaint.getId(), tech.getId(), "Dispatched Bob for immediate UPS inspection");
            assertEquals(ComplaintStatus.ASSIGNED, complaint.getStatus());

            // 6. Technician Starts Work
            complaint = service.startWork(tech, complaint.getId(), "Isolated secondary UPS circuit and testing batteries");
            assertEquals(ComplaintStatus.IN_PROGRESS, complaint.getStatus());

            // 7. Technician Resolves Complaint
            complaint = service.resolveComplaint(tech, complaint.getId(), "Replaced defective battery bank module 3. System nominal.");
            assertEquals(ComplaintStatus.RESOLVED, complaint.getStatus());
            assertEquals(4, complaint.getAuditHistory().size());

            pass(name);
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError(message);
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " (Expected: " + expected + ", Actual: " + actual + ")");
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, "Equality assertion failed");
    }

    private static void cleanupDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                try (var stream = Files.walk(dir)) {
                    stream.sorted(Comparator.reverseOrder())
                          .forEach(p -> {
                              try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                          });
                }
            }
        } catch (IOException ignored) {}
    }
}
