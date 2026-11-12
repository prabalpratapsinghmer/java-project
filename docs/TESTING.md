# CampusFix – Quality Assurance and Test Plan

## 1. Test Strategy Overview
The CampusFix test harness (`edu.nie.campusfix.tests.TestRunner`) executes without external testing frameworks (such as JUnit or TestNG), relying strictly on core Java language capabilities.

### Key Quality Principles:
1. **Zero Production Side-Effects:** Every test run creates a dedicated, isolated temporary directory (`Files.createTempDirectory("campusfix_test_env_")`), runs all setup, mutations, reloads, and assertions in that sandbox, and deletes the temporary files upon completion.
2. **Deterministic Preconditions:** Tests do not rely on existing data in `data/users/` or `data/complaints/`.
3. **Headless Execution:** All 30 tests run in CLI mode without requiring an active graphical X11/Wayland display server.

---

## 2. Test Execution Command
Compile and run the test harness:
```bash
./compile.sh
java -cp out edu.nie.campusfix.tests.TestRunner
```

---

## 3. Comprehensive 30-Test Matrix

| Test ID | Name | Primary Target | Verification Criteria | Status |
|---|---|---|---|---|
| **01** | Initial Admin Setup | `AuthenticationService`, `UserRepository` | Verifies `isInitialAdminSetupRequired()` transitions from `true` to `false` and persists admin record. | **PASS** |
| **02** | Student Account Registration | `AuthenticationService`, `StudentUser` | Creates student user and confirms role is `STUDENT` and record is retrievable. | **PASS** |
| **03** | Invalid Registration Rejection | `ValidationUtil`, `ValidationException` | Rejects passwords `< 6` characters and malformed email addresses without creating records. | **PASS** |
| **04** | Valid Login Accepted | `AuthenticationService`, `PasswordUtil` | Successfully verifies password using PBKDF2 salt-hash comparison and returns `User`. | **PASS** |
| **05** | Incorrect Credentials Rejected | `AuthenticationService` | Rejects incorrect password with `AuthenticationException` without throwing uncaught errors. | **PASS** |
| **06** | Student Complaint Creation | `ComplaintService`, `Complaint` | Verifies submission sets `PENDING` status, generates UUID, and links reporter ID. | **PASS** |
| **07** | Unique Complaint IDs | `Complaint`, `UUID` | Confirms multiple complaints filed sequentially receive distinct UUID identifiers. | **PASS** |
| **08** | Priority Calculation | `PriorityService` | Verifies deterministic priority scoring (Low, Medium, High, Critical) with category bonuses. | **PASS** |
| **09** | Duplicate Matching Warning | `DuplicateDetectionService` | Confirms duplicate warning triggers for identical normalized location and category within 7 days. | **PASS** |
| **10** | Non-Matching Location Safety | `DuplicateDetectionService` | Confirms different locations do not trigger false duplicate warnings. | **PASS** |
| **11** | Admin Can Assign Technicians | `AssignmentService`, `Complaint` | Transitions status to `ASSIGNED` and records `ASSIGNED` audit history entry. | **PASS** |
| **12** | Student Cannot Assign | `AssignmentService`, `UnauthorizedException` | Rejects assignment attempt by `StudentUser` with `UnauthorizedException`. | **PASS** |
| **13** | Technician Views Assigned | `ComplaintService` | Filters complaints visible to technician strictly to those matching assigned technician ID. | **PASS** |
| **14** | Technician Cannot Modify Others | `ComplaintService`, `UnauthorizedException` | Blocks technician from starting work on complaints assigned to another technician. | **PASS** |
| **15** | Valid Status Transitions | `Complaint`, `ComplaintStatus` | Exercises full valid progression: `PENDING` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED`. | **PASS** |
| **16** | Invalid Transitions Blocked | `ComplaintStatus.canTransitionTo` | Blocks starting work on an unassigned `PENDING` complaint. | **PASS** |
| **17** | Resolution Notes Saved | `Complaint.resolve` | Confirms mandatory resolution notes (min 5 chars) are persisted upon resolution. | **PASS** |
| **18** | Complete Audit Trail | `ComplaintHistoryEntry` | Confirms each lifecycle event (`CREATED`, `ASSIGNED`, `WORK_STARTED`, `RESOLVED`) is logged with actor name. | **PASS** |
| **19** | Overdue SLA Classification | `Complaint.isOverdue` | Confirms complaints past response target hours are overdue; resolved complaints are never overdue. | **PASS** |
| **20** | Dashboard Metrics Balance | `ReportService` | Asserts sum of `pending + assigned + inProgress + resolved` equals `totalComplaints`. | **PASS** |
| **21** | Search & Filter Precision | `ComplaintService.searchComplaints` | Verifies multi-predicate filtering (category, priority, status, query) returns exact matching subsets. | **PASS** |
| **22** | CSV Report Export | `CsvExportUtil`, `ReportService` | Generates RFC-4180 CSV file and verifies presence of standard column headers. | **PASS** |
| **23** | Restart / Reload Survival | `FileUserRepository`, `FileComplaintRepository` | Instantiates new repository instances pointing to existing disk directories; validates record retention. | **PASS** |
| **24** | Corrupted File Isolation | `FileStorageUtil.quarantineCorruptFile` | Verifies malformed `.properties` file is quarantined to `.corrupt` while valid records continue loading. | **PASS** |
| **25** | Empty Database Stability | `ReportService` | Verifies reporting and querying on clean directories returns 0 counts without null pointer exceptions. | **PASS** |
| **26** | Admin Privilege Escalation Block | `AuthenticationService` | Rejects secondary administrative registration attempts once initial admin is initialized. | **PASS** |
| **27** | Exhaustive Priority Matrix | `PriorityService` | Evaluates all 27 permutations (3 severities × 9 categories) against deterministic rule formula. | **PASS** |
| **28** | Accidental Overwrite Defense | `FileUserRepository` | Verifies user entity properties and password hashes cannot be corrupted by duplicate IDs. | **PASS** |
| **29** | Class Compilation Integrity | File System Check | Verifies all required class binaries are present in the `out/` build directory. | **PASS** |
| **30** | End-to-End Lifecycle Simulation | Full Subsystem Integration | Simulates complete lifecycle from admin setup and student filing through technician resolution. | **PASS** |

---

## 4. Test Results Summary
```
==================================================
Running CampusFix Test Suite (30 Tests)
==================================================
[PASS] Test 01: Initial administrator setup works.
[PASS] Test 02: Student account registration works.
[PASS] Test 03: Invalid registration input is rejected.
[PASS] Test 04: Correct login credentials are accepted.
[PASS] Test 05: Incorrect credentials are rejected.
[PASS] Test 06: Student complaint creation works.
[PASS] Test 07: Unique complaint IDs are generated.
[PASS] Test 08: Priority calculation is correct.
[PASS] Test 09: Duplicate complaint warnings are generated correctly.
[PASS] Test 10: Different locations do not cause false duplicate matches.
[PASS] Test 11: Administrator can assign technicians.
[PASS] Test 12: Student cannot assign technicians.
[PASS] Test 13: Technician can view assigned complaints.
[PASS] Test 14: Technician cannot modify unrelated complaints.
[PASS] Test 15: Valid status transitions succeed.
[PASS] Test 16: Invalid status transitions fail safely.
[PASS] Test 17: Resolution notes are saved.
[PASS] Test 18: Complaint history is recorded correctly.
[PASS] Test 19: Overdue classification is correct.
[PASS] Test 20: Dashboard totals are calculated correctly.
[PASS] Test 21: Complaint searches and filters work.
[PASS] Test 22: CSV report export works.
[PASS] Test 23: Application data survives restart/reload.
[PASS] Test 24: Corrupted data is reported safely.
[PASS] Test 25: Application handles an empty complaint database.
[PASS] Test 26: Administrator privileges cannot be obtained through public registration.
[PASS] Test 27: All priority combinations produce the expected result.
[PASS] Test 28: User and complaint records cannot be accidentally overwritten.
[PASS] Test 29: All generated Java classes compile successfully.
[PASS] Test 30: A complete complaint workflow can be executed from creation to resolution.
==================================================
RESULTS: 30 / 30 Passed (0 Failed)
==================================================
```
