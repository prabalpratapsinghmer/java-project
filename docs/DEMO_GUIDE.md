# CampusFix – Faculty Demonstration & Viva Evaluation Guide

This guide is designed for the 5-member student team presenting **CampusFix** during third-semester Java OOP laboratory evaluations and viva examinations.

---

## 1. Demonstration Setup & Pre-Flight Checklist
Before calling the faculty examiner to your terminal:
1. Open a terminal in the project root directory:
   ```bash
   cd "/home/prabal/Desktop/java project"
   ```
2. Clean compiled binaries and test storage (if fresh demo required):
   ```bash
   ./compile.sh
   ```
3. Run the automated test suite to prove system health:
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
2. **Examiner Observation:** Point out that on first launch, the system automatically detects that no administrator exists and opens the **"Initial Administrator Setup"** wizard.
3. Fill in the fields:
   - **Admin Username:** `admin`
   - **Password:** `Admin@123`
   - **Full Name:** `Dr. Ramesh Rao`
   - **Email:** `admin@nie.ac.in`
   - **Employee ID:** `ADM-001`
   - **Department:** `Campus Infrastructure`
4. Click **"Complete Administrator Initialization"**.
5. Explain to faculty: *"Public registration can never create administrative accounts; initial setup is sealed once the first administrator is registered."*

---

### Act 2: Technician Roster Setup
1. On the Admin Dashboard, click the **"Technician Roster"** tab.
2. Click **"+ Register New Technician"**.
3. Create an electrical technician:
   - **Username:** `tech_rajesh`
   - **Password:** `Tech@123`
   - **Full Name:** `Rajesh Kumar`
   - **Email:** `rajesh@campus.edu`
   - **Technician ID:** `TECH-101`
   - **Specialization Category:** `Electrical`
4. Click **"Register Technician"**. Point out that the roster updates immediately showing `Active Tasks: 0`.

---

### Act 3: Student Registration & Submission with Dynamic Priority
1. Click **"Sign Out"** at the top right. Confirm dialog.
2. Select the **"Register as Student"** tab:
   - **Username:** `student_arun`
   - **Password:** `Student@123`
   - **Full Name:** `Arun Patel`
   - **Email:** `arun@nie.ac.in`
   - **Student ID / USN:** `4NI22CS001`
   - **Department:** `Computer Science & Engineering`
3. Click **"Create Student Account"**. The application signs the student in directly.
4. On the Student Dashboard, click **"+ Report New Complaint"**.
5. Enter complaint details:
   - **Title:** `Ceiling Fan Sparking and Smoke`
   - **Category:** Select `Electrical`
   - **Location:** `Admin Block Room 102`
   - **Severity:** Select `HIGH`
   - **Description:** `Continuous sparking and burning smell from ceiling fan regulator.`
6. **Show Dynamic Priority Preview:** Point to the live pill badge at the bottom of the form displaying **`CRITICAL`** with explanation: *"Base score: 3 (High severity) + 1 high-risk bonus (Electrical) = 4 -> Priority: Critical (Response target: 24h)"*.
7. Click **"Submit Complaint"**.

---

### Act 4: Intelligent Duplicate Detection
1. While still logged in as `student_arun`, click **"+ Report New Complaint"** again.
2. Enter the same issue for the same location:
   - **Category:** `Electrical`
   - **Location:** `Admin Block, Room 102!` *(Note punctuation and casing)*
   - **Title:** `Fan switch smoking`
   - **Description:** `Smoke observed near switchboard.`
3. Click **"Submit Complaint"**.
4. **Examiner Observation:** A warning modal appears:
   > *"A similar unresolved complaint already exists for this location: 'Ceiling Fan Sparking and Smoke' (Pending, Critical Priority)... Do you still want to submit this complaint?"*
5. Explain to faculty: *"Our DuplicateDetectionService normalizes whitespace, strips punctuation, checks category matching, and checks the 7-day rolling window without blindly merging or dropping user requests."*
6. Click **"No"** to cancel the duplicate.

---

### Act 5: Administrative Workload-Informed Assignment
1. Sign out and sign back in as `admin` (`Admin@123`).
2. Point out that the KPI cards on top show:
   - **Total:** `1`
   - **Pending:** `1`
   - **Assigned:** `0`
3. Select the critical complaint row and click **"Assign / Reassign"**.
4. In the dialog, show that `Rajesh Kumar [Electrical]` displays `Active Tasks: 0`.
5. Enter assignment note: `Dispatched for immediate electrical isolation.`
6. Click **"Assign"**. Notice the status badge turns blue (**`Assigned`**) and the Assigned KPI increments.

---

### Act 6: Technician Execution & Resolution
1. Sign out and log in as `tech_rajesh` (`Tech@123`).
2. Point out that the Technician Dashboard displays only tasks assigned to Rajesh.
3. Select the complaint and click **"Start Work"**. Status transitions to yellow (**`In Progress`**).
4. Select the complaint and click **"Mark Resolved"**.
5. Enter resolution notes: `Replaced burnt capacitor, insulated wire joints, tested load.`
6. Click **"Mark as Resolved"**. The status updates to green (**`Resolved`**).

---

### Act 7: Full Audit Trail & CSV Export
1. Log in as `admin`.
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
