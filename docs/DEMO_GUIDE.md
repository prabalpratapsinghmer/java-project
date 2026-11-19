# CampusFix – Faculty Demonstration & Viva Evaluation Guide

This guide is designed for the 5-member student team presenting **CampusFix** during third-semester Java OOP laboratory evaluations and viva examinations.

---

## 1. Demonstration Setup & Pre-Flight Checklist
Before calling the faculty examiner to your terminal:
1. Open a terminal in the CampusFix project root directory:
   ```bash
   cd path/to/CampusFix
   ```
2. Compile all Java sources:
   ```bash
   ./compile.sh
   ```
3. Run the automated 30-test suite to verify system health:
   ```bash
   java -cp out edu.nie.campusfix.tests.TestRunner
   ```
   *(Verify 30 / 30 tests pass)*

---

## 2. Step-by-Step Viva Walkthrough Script

### Act 1: First Launch & Initial Administrator Setup
1. Launch the application:
   ```bash
   ./run.sh
   ```
2. **Examiner Observation:** On first launch, CampusFix detects that no administrator exists and opens the **"Initial Administrator Setup"** screen.
3. Fill in the fields (entering and confirming your own secure password — no password is ever hardcoded):
   - **Admin Username:** `admin`
   - **Full Name:** `Dr. Priya Nair`
   - **Email:** `admin@nie.ac.in`
   - **Employee ID:** `ADM-001`
   - **Department:** `Campus Infrastructure`
   - **Admin Password / Confirm Password:** `Admin@123`
4. Click **"Complete Administrator Setup"**.
5. Explain to faculty: *"Public registration can only create Student/Staff accounts; initial administrator setup is permanently disabled once the first administrator is initialized."*

---

### Act 2: Technician Roster Setup
1. On the Administrator sidebar, click **"Technicians"**.
2. Click **"+ Register Technician"**.
3. Create an electrical technician:
   - **Username:** `tech_rajesh`
   - **Password / Confirm:** `Tech@123`
   - **Full Name:** `Rajesh Kumar`
   - **Email:** `rajesh@campus.edu`
   - **Technician Code:** `TECH-101`
   - **Specialization:** `Electrical`
4. Click **"Register Technician"**. Point out that the roster updates immediately showing `Active Tasks: 0` and `Status: Active`.

---

### Act 3: Student Registration & Submission with Dynamic Priority
1. Click **"Sign Out"** in the top-right header and confirm.
2. On the Sign In screen, click **"Create Account"** to switch to Student / Staff Registration:
   - **Username:** `student_arun`
   - **Full Name:** `Arun Patel`
   - **Email:** `arun@nie.ac.in`
   - **Student / Staff ID:** `4NI23CS042`
   - **Department:** `Computer Science & Engineering`
   - **Password / Confirm:** `Student@123`
3. Click **"Create Account & Sign In"**.
4. On the Student Dashboard, click **"+ Submit New Complaint"** (or **"+ New Complaint"** in the sidebar).
5. Enter complaint details:
   - **Title:** `Ceiling Fan Sparking and Smoke`
   - **Category:** `Electrical`
   - **Campus Location:** `Admin Block Room 102`
   - **Issue Severity:** `High`
   - **Description:** `Continuous sparking and burning smell from ceiling fan regulator.`
6. **Show Dynamic Priority Preview:** Point to the live pill badge displaying **`Critical`** with rule breakdown: *"Base score: 3 (High severity) + 1 high-risk bonus (Electrical) = 4 -> Priority: Critical (Response target: 24h)"*.
7. Click **"Submit Complaint"**.

---

### Act 4: Privacy-Safe Duplicate Detection
1. Click **"+ New Complaint"** again.
2. Enter a similar issue for the same location (with different casing/punctuation):
   - **Title:** `Fan switch smoking`
   - **Category:** `Electrical`
   - **Campus Location:** `Admin Block, Room 102!`
   - **Severity:** `Medium`
   - **Description:** `Smoke observed near classroom switchboard panel.`
3. Click **"Submit Complaint"**.
4. **Examiner Observation:** Input validation runs first, followed by `DuplicateDetectionService`, showing the privacy-safe warning:
   > *"A similar unresolved complaint already exists for this location... Do you still want to submit this complaint?"*
   *(Notice private reporter details and descriptions of other users' complaints are never exposed.)*
5. Click **"No"** to cancel (or **"Yes"** to confirm and show how it appears in Admin → **Duplicate Review**).

---

### Act 5: Administrative Filtering, Sorting & Workload-Informed Assignment
1. Sign out and sign back in as `admin` (`Admin@123`).
2. Review the **Administrator Dashboard** (6 live KPI cards + Category & Priority charts).
3. Click **"All Complaints"** in the sidebar to show search, Category/Priority/Status/**Technician**/Overdue filters, and clickable column header sorting.
4. Select the critical complaint row and click **"Assign / Reassign"**.
5. In the dialog, point out `Rajesh Kumar [Electrical] ★ Match — Active Tasks: 0`.
6. Enter dispatch note `Dispatched for immediate electrical isolation.` and click **"Assign Technician"**.

---

### Act 6: Technician Execution & Resolution
1. Sign out and log in as `tech_rajesh` (`Tech@123`).
2. Click **"Assigned Tasks"** in the sidebar — notice Rajesh sees *only* complaints assigned to him.
3. Select the assigned complaint and click **"Start Work"** (`ASSIGNED` → `IN_PROGRESS`). Notice **"Mark Resolved"** now activates.
4. Click **"Mark Resolved"**, enter resolution notes (`Replaced burnt capacitor, insulated wire joints, tested load.`), and click **"Mark as Resolved"** (`IN_PROGRESS` → `RESOLVED`).

---

### Act 7: Audit Trail, Overdue Monitoring & CSV Export
1. Sign back in as `admin`.
2. Double-click the resolved complaint to open **Complaint Details** — show the 4-step lifecycle stepper, priority calculation rule, resolution notes, and 4-event chronological audit trail.
3. Click **"Overdue Queue"** and **"Reports & Export"** in the sidebar to show all 6 live reports and export CSV via the file chooser.
2. Select the resolved complaint and click **"View Details"**.
3. **Show Audit Trail:** Point out the 4 chronological entries:
   1. Created by `Arun Patel`
   2. Assigned to `Rajesh Kumar` by `Dr. Ramesh Rao`
   3. Work Started by `Rajesh Kumar`
   4. Resolved by `Rajesh Kumar` with resolution notes.
4. Close dialog and navigate to **"Reports & Analytics"** tab.
5. Click **"Export All to CSV"**.
6. Open terminal and verify exported CSV:
   ```bash
   cat reports/complaints_summary.csv
   ```
7. Point out formula injection prevention (`'` prepended to formula characters) and RFC-4180 escaping.

---

## 3. Frequently Asked Viva Questions & Model Answers

### Question 1 (For Member 1 - User Management & Security):
**Q: How do you store passwords securely without an external database?**  
*A: "We use PBKDF2WithHmacSHA256 from the standard Java cryptography library (`javax.crypto`). Each user password gets a unique 16-byte random salt generated via `SecureRandom` and undergoes 65,536 hash iterations. We store `<saltHex>:<hashHex>` in the user's `.properties` file. When verifying, we perform constant-time comparison with `MessageDigest.isEqual` to prevent timing attacks, and we wipe password `char[]` arrays from memory immediately after hashing."*

### Question 2 (For Member 2 - Complaint Aggregate & Encapsulation):
**Q: How did you ensure encapsulation in the Complaint model?**  
*A: "All fields in `Complaint.java` are private. Status transitions cannot be set directly; they must go through domain methods like `assignTechnician()`, `startWork()`, and `resolve()`, which enforce state machine rules via `ComplaintStatus.canTransitionTo()`. Additionally, `getAuditHistory()` returns `Collections.unmodifiableList(auditHistory)` to prevent external callers from adding or clearing entries directly."*

### Question 3 (For Member 3 - Priority & Duplicate Detection):
**Q: Is your priority engine using AI or machine learning?**  
*A: "No, Sir/Madam. It is an explainable, deterministic rule-based engine implemented in `PriorityService.java`. It assigns base scores (Low=1, Medium=2, High=3) and adds a +1 point bonus for high-risk categories like Electrical and Safety, clamping the score at 4 (Critical). This ensures total transparency and predictable response targets (24 hours to 7 days)."*

### Question 4 (For Member 4 - Assignment & Tracking):
**Q: How do you prevent a student from assigning a technician using GUI hacking?**  
*A: "Authorization is enforced in the service layer (`AssignmentService.java`), not just by hiding UI buttons. The service method checks `if (!assigner.canAssignComplaints()) throw new UnauthorizedException(...)`. Because `StudentUser.canAssignComplaints()` polymorphically returns `false`, any unauthorized call is rejected immediately."*

### Question 5 (For Member 5 - UI & Testing):
**Q: How does your application survive crashes or file corruption?**  
*A: "We implement atomic file persistence in `FileStorageUtil.java`. All writes are staged to a `.tmp` file and swapped atomically using `StandardCopyOption.ATOMIC_MOVE`. If a file is ever corrupted, our repository isolates it to `<filename>.corrupt.<timestamp>` and continues loading remaining valid files. Furthermore, our standalone `TestRunner.java` verifies all 30 test scenarios in an isolated temporary directory with 100% pass rate."*
