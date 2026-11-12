
You are a senior Java software engineer, Java desktop application architect, QA engineer, and Object-Oriented Programming mentor.

Your task is to design, develop, test, debug, and document a COMPLETE, FULLY FUNCTIONAL Java desktop application for a third-semester Computer Science Experiential Learning project.

PROJECT NAME:
CampusFix – Smart Campus Maintenance and Complaint Management System

IMPORTANT:
This is an academic project for the subject "Object-Oriented Programming with Java".

The application MUST be implemented entirely using Java and its standard library.

Do not create a prototype, mock application, incomplete demonstration, or disconnected modules. Implement a complete application whose advertised features actually work.

You are responsible for the entire development lifecycle, from initial architecture to final verification.

==================================================
1. STRICT TECHNOLOGY REQUIREMENTS
==================================================

Programming language:
- Java 17 or later
- Pure Java source code

Graphical user interface:
- Java Swing
- Java AWT where necessary

Core technologies:
- Java Collections Framework
- Java File I/O and NIO
- Java exception handling
- Java generics
- Java interfaces
- Java inheritance and polymorphism
- Java encapsulation and abstraction
- Java time API
- Java security APIs

Persistence:
- Use built-in Java file handling.
- Use java.util.Properties and java.nio.file.
- Store application data locally.
- Data must survive application restarts.
- Use safe, atomic file updates where supported.
- Implement recovery or clear error reporting for corrupted files.

STRICTLY PROHIBITED:
- Python
- JavaScript
- TypeScript
- React
- HTML/CSS web interfaces
- Spring Boot
- JavaFX
- Node.js
- Flask
- Django
- External APIs
- Cloud services
- MySQL or other database installations
- Third-party dependencies
- Maven/Gradle plugins required for runtime

The final application must compile and run with only a compatible JDK.

Do not rely on internet connectivity during execution.

==================================================
2. PROJECT PROBLEM STATEMENT
==================================================

Educational institutions frequently encounter infrastructure-related problems such as malfunctioning classroom equipment, damaged electrical fittings, leaking water taps, faulty laboratory systems, and hostel maintenance issues.

In many institutions, these complaints are communicated verbally, through handwritten registers, or through informal messaging platforms.

This leads to delayed maintenance responses, duplicate complaints, poor communication, lack of accountability, and limited visibility into complaint resolution.

For example, a student who reports a damaged classroom fan may not know whether the complaint has been received, assigned to maintenance personnel, or resolved.

Similarly, urgent electrical safety complaints may not receive appropriate priority.

CampusFix will address these problems by providing a centralized Java desktop application for reporting, organizing, prioritizing, assigning, tracking, and resolving campus maintenance complaints.

The application will demonstrate practical use of Object-Oriented Programming concepts through a modular, maintainable architecture.

==================================================
3. PROJECT OBJECTIVES
==================================================

Implement all the following objectives:

1. Allow students and staff to register maintenance complaints.

2. Implement rule-based complaint prioritization based on issue severity and category.

3. Provide tracking of complaint status.

4. Allow administrators to assign maintenance tasks to appropriate personnel.

5. Identify potentially duplicate complaints from the same location and category.

6. Generate summaries of pending, resolved, and overdue complaints.

7. Implement the application using core Java OOP principles.

8. Ensure reliable local data persistence.

9. Develop a clear and easy-to-use desktop interface.

10. Ensure application stability, input validation, and correct role-based permissions.

All objectives must correspond to real working application functionality.

==================================================
4. USER ROLES AND PERMISSIONS
==================================================

Implement three logical roles.

ROLE 1: STUDENT / STAFF

Capabilities:
- Create a new account.
- Log into the application.
- Submit a maintenance complaint.
- Select complaint category.
- Specify campus location.
- Enter detailed complaint description.
- Select severity.
- View complaints submitted by the logged-in user.
- Search personal complaints.
- Track complaint status.
- View complaint history.
- Log out.

ROLE 2: ADMINISTRATOR

Capabilities:
- Log into the administrator dashboard.
- View all complaints.
- Search, filter, and sort complaints.
- View priority classifications.
- Review potentially duplicate complaints.
- Assign complaints to maintenance personnel.
- Reassign complaints when necessary.
- View technician workloads.
- Monitor pending complaints.
- Monitor overdue complaints.
- Review resolved complaints.
- Generate complaint summary reports.
- Manage technician accounts.
- Log out.

ROLE 3: MAINTENANCE PERSONNEL

Capabilities:
- Log into the technician dashboard.
- View assigned complaints only.
- See category, location, description, and priority.
- Change assigned complaints from ASSIGNED to IN_PROGRESS.
- Mark completed complaints as RESOLVED.
- Add resolution notes.
- View relevant complaint history.
- Log out.

SECURITY RULES:

- Students cannot assign complaints.
- Students cannot update maintenance status.
- Technicians cannot edit unrelated complaints.
- Technicians cannot assign complaints to themselves.
- Only administrators can assign or reassign technicians.
- Authorization must be validated in the service layer, not only by hiding GUI buttons.
- Users must not obtain administrator privileges through registration.

ACCOUNT SETUP:

On first launch, provide a secure initial administrator setup screen.

After administrator initialization, normal public registration may create STUDENT/STAFF accounts only.

Technician accounts must be created by administrators.

Never hardcode permanent administrator passwords.

==================================================
5. COMPLAINT MANAGEMENT MODULE
==================================================

Create a complete complaint registration system.

Every complaint must contain:

- Unique complaint ID
- Reporting user ID
- Reporter name
- Complaint title
- Category
- Campus location
- Description
- Severity
- Calculated priority
- Submission timestamp
- Current status
- Assigned technician ID, if applicable
- Last updated timestamp
- Resolution timestamp, if resolved
- Resolution notes, if resolved

Use UUID for unique identifiers.

Required complaint categories:

1. Electrical
2. Plumbing
3. Classroom Equipment
4. Laboratory Equipment
5. Hostel Maintenance
6. Furniture
7. Sanitation
8. Safety
9. Other

Severity levels:

LOW
MEDIUM
HIGH

Priorities:

LOW
MEDIUM
HIGH
CRITICAL

Complaint status values:

PENDING
ASSIGNED
IN_PROGRESS
RESOLVED

Add input validation:

- Title cannot be empty.
- Description cannot be empty.
- Location cannot be empty.
- Category and severity are mandatory.
- Enforce sensible maximum text lengths.
- Reject invalid IDs.
- Prevent invalid status transitions.
- Show understandable validation messages.

==================================================
6. RULE-BASED PRIORITY ENGINE
==================================================

Develop a priority calculation module using Java.

Do not use artificial intelligence or machine learning.

Define deterministic priority rules:

Severity base scores:
LOW = 1
MEDIUM = 2
HIGH = 3

For Electrical or Safety complaints, add 1 point.

Cap the score at 4.

Final priority:
1 = LOW
2 = MEDIUM
3 = HIGH
4 = CRITICAL

Examples:

- Low-severity furniture issue -> LOW
- Medium-severity plumbing issue -> MEDIUM
- High-severity laboratory issue -> HIGH
- High-severity electrical issue -> CRITICAL
- Medium-severity safety issue -> HIGH

Use a dedicated PriorityService class.

Requirements:
- Priority calculated automatically on submission.
- Priority visible to students, administrators, and technicians.
- Administrators can sort complaints by priority.
- Preserve the calculation result in persistent records.
- Unit-test every possible severity/category combination.

Explain the algorithm clearly in the project documentation.

==================================================
7. DUPLICATE COMPLAINT DETECTION
==================================================

Implement a simple, explainable duplicate detection system.

When a student submits a complaint:

1. Normalize the entered location.
2. Compare the complaint category.
3. Search existing unresolved complaints.
4. Consider only complaints submitted during the preceding 7 days.
5. Show potentially matching complaints.

For the first version, complaints are potential duplicates when:
- Normalized location matches.
- Category matches.
- The existing complaint is unresolved.
- The existing complaint is within the 7-day window.

Show a warning such as:

"A similar unresolved complaint already exists for this location. Do you still want to submit this complaint?"

Allow submission if the user explicitly confirms.

This module must:
- Never silently delete complaints.
- Never automatically merge different complaints.
- Never disclose private complaint information to unauthorized users.
- Avoid unnecessary duplicate warnings.
- Include automated tests for duplicate matching.

Use a dedicated DuplicateDetectionService class.

Do not falsely describe this feature as AI-powered.

==================================================
8. COMPLAINT STATUS TRACKING
==================================================

Implement the following workflow:

PENDING -> ASSIGNED -> IN_PROGRESS -> RESOLVED

Rules:

- New complaints start as PENDING.
- Admin assigns an appropriate technician.
- Assignment changes status to ASSIGNED.
- Assigned technician starts work.
- Status changes to IN_PROGRESS.
- Technician resolves the complaint and enters resolution notes.
- Status changes to RESOLVED.

Allow administrators to reassign an unresolved complaint without violating the workflow.

Prevent:
- Resolving a complaint without assignment.
- Students changing status.
- Unassigned technicians updating complaints.
- Moving RESOLVED complaints backwards.
- Invalid or unsupported status values.

Maintain a timestamped audit history for:

- Complaint creation
- Technician assignment
- Technician reassignment
- Work started
- Complaint resolution

Each history entry should contain:
- Timestamp
- Actor ID
- Action type
- Optional note

Show this history in the complaint details view.

==================================================
9. OVERDUE COMPLAINT DETECTION
==================================================

Identify unresolved complaints that exceed defined response targets.

Use the following demonstrative targets:

CRITICAL: 24 hours
HIGH: 48 hours
MEDIUM: 72 hours
LOW: 7 days

Calculate overdue status based on the time elapsed since complaint submission.

Use java.time for all calculations.

Make it clear in the documentation that these are project-defined response targets, not official college regulations.

Administrators should be able to:
- View all overdue complaints.
- Filter by priority.
- Sort by submission date.
- See how long each complaint has remained unresolved.

Do not mark resolved complaints as currently overdue.

==================================================
10. ADMINISTRATION MODULE
==================================================

Build a functional administrator dashboard containing:

- Total complaints
- Pending complaints
- Assigned complaints
- In-progress complaints
- Resolved complaints
- Overdue complaints
- Complaints grouped by category
- Complaints grouped by priority

Include a complaint management table.

Table columns:

Complaint ID
Title
Category
Location
Priority
Status
Assigned Technician
Submitted Date

Provide:
- Search
- Status filter
- Category filter
- Priority filter
- Sorting
- Refresh
- View Details
- Assign Technician
- Reassign Technician

Handle empty datasets correctly.

Counts must always reflect actual stored complaint data.

No hardcoded report values.

==================================================
11. TECHNICIAN MANAGEMENT
==================================================

Administrators must be able to:

- Create technician accounts.
- View available technicians.
- View their assigned unresolved complaint count.
- Assign or reassign complaints to technicians.

Prevent assigning complaints to nonexistent accounts.

Display technician workload to support informed assignment.

If a technician has no assignments, show an empty-state message rather than an error.

==================================================
12. REPORT GENERATION
==================================================

Implement reports using Java.

Required reports:

1. Total complaints summary
2. Category-wise complaint counts
3. Priority-wise complaint counts
4. Status-wise complaint counts
5. Overdue complaint report
6. Technician workload report

Display reports in the Java Swing interface.

Also provide export functionality to CSV using core Java file handling.

CSV output must:
- Include appropriate headers.
- Correctly escape commas, quotes, and line breaks.
- Use UTF-8.
- Avoid malformed records.
- Prevent spreadsheet formula injection for untrusted text fields.

Do not use external reporting libraries.

==================================================
13. GRAPHICAL USER INTERFACE
==================================================

Use Java Swing exclusively.

Design a clean and professional desktop application.

Suggested appearance:
- Modern academic application
- Consistent typography
- Clear navigation
- Readable tables
- Balanced spacing
- Accessible contrast
- Appropriate status indicators
- Consistent button styles

Implement these screens:

1. Welcome / Login
2. Account Registration
3. Initial Administrator Setup
4. Student Dashboard
5. New Complaint Form
6. My Complaints
7. Complaint Details
8. Administrator Dashboard
9. All Complaints
10. Technician Assignment
11. Technician Dashboard
12. Complaint Reports

GUI REQUIREMENTS:

- Use JFrame, JPanel, JTable, JScrollPane, JButton, JTextField, JComboBox, and appropriate Swing components.
- Use layout managers properly.
- Avoid absolute positioning.
- Do not use null layouts.
- Avoid overlapping text and controls.
- Ensure windows display correctly on ordinary laptop screens.
- Use JTable sorting/filtering where suitable.
- Show feedback after successful actions.
- Show meaningful error dialogs.
- Confirm sensitive actions where appropriate.
- Keep the interface responsive.
- Perform GUI updates on the Event Dispatch Thread.
- Move potentially slow file operations off the Event Dispatch Thread.
- Do not let invalid input crash the interface.

Every visible button must have working functionality.

Never create decorative buttons that do nothing.

==================================================
14. OBJECT-ORIENTED PROGRAMMING REQUIREMENTS
==================================================

This project is specifically for a Java OOP subject.

Demonstrate the following concepts clearly:

ENCAPSULATION:
- Use private instance variables.
- Provide appropriate methods for controlled access.
- Protect internal collections from unintended modifications.
- Enforce model invariants.

INHERITANCE:
- Create a suitable User hierarchy.
- StudentUser, AdminUser, and TechnicianUser may extend User.
- Avoid unnecessary inheritance.

POLYMORPHISM:
- Use overridden methods where different role behavior is genuinely required.
- Demonstrate polymorphism through meaningful application behavior.

ABSTRACTION:
- Use an abstract User class where appropriate.
- Define repository interfaces.
- Separate business logic from implementation details.

INTERFACES:
- Implement repository interfaces such as UserRepository and ComplaintRepository.
- Use interfaces for suitable service boundaries where useful.

COMPOSITION:
- Use composition between services and repositories.
- Prefer composition over unnecessary deep inheritance.

COLLECTIONS:
- ArrayList
- HashMap
- List
- Map
- Set where useful

EXCEPTION HANDLING:
- Handle validation errors.
- Handle file I/O failures.
- Handle corrupted records.
- Handle invalid operations.

GENERICS:
- Use generics correctly in collection and repository operations.

Make sure every OOP concept is genuinely used in functioning code.

Include documentation explaining which Java classes demonstrate each concept.

==================================================
15. PROJECT PACKAGE STRUCTURE
==================================================

Organize the application into a clean Java package structure.

Suggested layout:

CampusFix/
  src/
    edu/
      nie/
        campusfix/
          Main.java

          model/
            User.java
            StudentUser.java
            AdminUser.java
            TechnicianUser.java
            Complaint.java
            ComplaintHistoryEntry.java

          enums/
            Role.java
            Category.java
            Severity.java
            Priority.java
            ComplaintStatus.java

          repository/
            UserRepository.java
            ComplaintRepository.java
            FileUserRepository.java
            FileComplaintRepository.java

          service/
            AuthenticationService.java
            ComplaintService.java
            PriorityService.java
            DuplicateDetectionService.java
            AssignmentService.java
            ReportService.java

          ui/
            LoginFrame.java
            RegistrationPanel.java
            MainFrame.java
            StudentDashboard.java
            AdminDashboard.java
            TechnicianDashboard.java
            ComplaintFormPanel.java
            ComplaintTablePanel.java
            ReportPanel.java

          util/
            ValidationUtil.java
            PasswordUtil.java
            FileStorageUtil.java

          tests/
            TestRunner.java

  data/
  docs/
  README.md
  .gitignore

This is a suggested structure, not a requirement to create unused classes.

Adjust the architecture when needed to avoid duplication and improve correctness.

Follow normal Java naming conventions and package conventions.

==================================================
16. DATA PERSISTENCE
==================================================

Do not use an external database.

Implement persistence using Java's built-in APIs.

Use:
- java.util.Properties
- java.nio.file.Files
- java.nio.file.Path
- java.nio.file.StandardCopyOption

Store user records, complaint records, and complaint history in organized local directories.

Requirements:

1. Create required directories automatically.
2. Persist newly registered users.
3. Persist submitted complaints.
4. Persist technician assignments.
5. Persist status updates.
6. Persist audit history.
7. Restore records correctly after restarting.
8. Handle missing files and directories.
9. Detect invalid or corrupted records.
10. Avoid silently overwriting existing valid data.
11. Use temporary files and atomic replacement where supported.
12. Never silently discard a valid complaint.

Use synchronized repository operations where necessary.

Document limitations of local single-computer storage.

Do not claim multi-device synchronization.

SECURITY:

- Never store plaintext passwords.
- Use PBKDF2WithHmacSHA256 with a unique random salt per password.
- Use a secure iteration count appropriate to the chosen JDK.
- Verify passwords without exposing stored secrets.
- Validate authorization before modifying records.
- Avoid logging passwords.
- Ignore local application data in Git.
- Use safe parsing for record IDs and paths.

==================================================
17. SEARCHING AND FILTERING
==================================================

Implement functional complaint searching.

Users should be able to search or filter according to their permissions.

Supported search fields:
- Complaint ID
- Title
- Category
- Location
- Status

Administrators should also be able to filter by:
- Assigned technician
- Priority
- Overdue status

Make text searching case-insensitive where appropriate.

Ensure filters work correctly when multiple conditions are applied.

Handle no-match results gracefully.

==================================================
18. TESTING AND QUALITY ASSURANCE
==================================================

Testing is mandatory.

Do not assume the program works merely because code was generated.

Create a Java-based test runner without third-party testing frameworks.

Test at minimum:

TEST 1:
Initial administrator setup works.

TEST 2:
Student account registration works.

TEST 3:
Invalid registration input is rejected.

TEST 4:
Correct login credentials are accepted.

TEST 5:
Incorrect credentials are rejected.

TEST 6:
Student complaint creation works.

TEST 7:
Unique complaint IDs are generated.

TEST 8:
Priority calculation is correct.

TEST 9:
Duplicate complaint warnings are generated correctly.

TEST 10:
Different locations do not cause false duplicate matches.

TEST 11:
Administrator can assign technicians.

TEST 12:
Student cannot assign technicians.

TEST 13:
Technician can view assigned complaints.

TEST 14:
Technician cannot modify unrelated complaints.

TEST 15:
Valid status transitions succeed.

TEST 16:
Invalid status transitions fail safely.

TEST 17:
Resolution notes are saved.

TEST 18:
Complaint history is recorded correctly.

TEST 19:
Overdue classification is correct.

TEST 20:
Dashboard totals are calculated correctly.

TEST 21:
Complaint searches and filters work.

TEST 22:
CSV report export works.

TEST 23:
Application data survives restart/reload.

TEST 24:
Corrupted data is reported safely.

TEST 25:
Application handles an empty complaint database.

TEST 26:
Administrator privileges cannot be obtained through public registration.

TEST 27:
All priority combinations produce the expected result.

TEST 28:
User and complaint records cannot be accidentally overwritten.

TEST 29:
All generated Java classes compile successfully.

TEST 30:
A complete complaint workflow can be executed from creation to resolution.

Run tests using isolated temporary directories so that tests never damage real user data.

Test boundary conditions.

For GUI testing, perform a manual verification checklist where automated interaction is unavailable.

Do not falsely report that UI tests passed if they were not executed.

==================================================
19. IMPLEMENTATION PHASES
==================================================

Complete the project in the following order.

PHASE A: REQUIREMENTS AND ARCHITECTURE

- Analyze the complete requirements.
- Define class responsibilities.
- Define object relationships.
- Design the package structure.
- Define user roles.
- Define complaint lifecycle.
- Define persistence format.
- Identify validation and error-handling rules.

PHASE B: CORE JAVA MODELS

- Implement enums.
- Implement User hierarchy.
- Implement Complaint model.
- Implement complaint history.
- Implement validation utilities.
- Verify basic object behavior.

PHASE C: PERSISTENCE AND AUTHENTICATION

- Implement file repositories.
- Implement secure password storage.
- Implement user registration.
- Implement login.
- Implement administrator initialization.
- Implement persistent record loading.

PHASE D: BUSINESS LOGIC

- Implement complaint registration.
- Implement priority calculation.
- Implement duplicate detection.
- Implement assignment.
- Implement status tracking.
- Implement overdue detection.
- Implement complaint searching.
- Implement report calculations.

PHASE E: SWING INTERFACE

- Implement login and registration.
- Implement student dashboard.
- Implement administrator dashboard.
- Implement technician dashboard.
- Implement complaint management forms.
- Implement tables, filters, and reports.
- Connect all screens to real service methods.

PHASE F: INTEGRATION

- Connect the interface to the service layer.
- Connect services to repositories.
- Verify authorization.
- Verify persistence.
- Verify complete user workflows.

PHASE G: TESTING AND DEBUGGING

- Compile every Java source file.
- Run automated tests.
- Inspect failing tests.
- Fix actual root causes.
- Compile again.
- Rerun tests.
- Manually inspect the GUI where possible.
- Confirm that controls and navigation work.

PHASE H: DOCUMENTATION

- Write README.
- Document project architecture.
- Document OOP concepts.
- Document priority rules.
- Create a UML class diagram or textual UML specification.
- Explain how to compile and run.
- Provide example workflows.
- Prepare a short demonstration guide.

Do not skip implementation phases.

Do not stop after producing only the project skeleton.

==================================================
20. FIVE-MEMBER TEAM DIVISION
==================================================

The project will be developed and presented by five students.

Organize responsibilities as follows:

MEMBER 1:
User Management and Authentication
- User models
- Registration
- Authentication
- Role-based permissions

MEMBER 2:
Complaint Management
- Complaint model
- Registration
- Validation
- Searching
- Complaint details

MEMBER 3:
Priority and Duplicate Detection
- PriorityService
- DuplicateDetectionService
- Overdue calculations
- Related testing

MEMBER 4:
Assignment and Tracking
- Technician assignment
- Status transitions
- Complaint history
- Technician dashboard

MEMBER 5:
GUI, Reports, Integration, and Testing
- Java Swing integration
- Administrator dashboard
- Reports
- Build verification
- Final integration

Ensure the whole application is integrated.

Do not create five independent applications.

Document which classes and concepts each member can explain during evaluation.

==================================================
21. DEVELOPMENT AND CODING STANDARDS
==================================================

Follow these rules:

- Write readable Java code.
- Use meaningful class and method names.
- Avoid unnecessarily large classes.
- Follow single-responsibility principles.
- Avoid duplicated business logic.
- Separate UI from core logic.
- Avoid hardcoded sample reports.
- Avoid hardcoded local filesystem paths.
- Use appropriate Java enums.
- Avoid global mutable state where possible.
- Validate every user input.
- Handle checked and unchecked exceptions appropriately.
- Never swallow exceptions silently.
- Do not use empty catch blocks.
- Avoid unnecessary dependencies.
- Do not introduce unfinished TODO placeholders.
- Do not leave methods returning dummy values.
- Do not create buttons without functionality.
- Do not use fabricated data to make features appear operational.
- Do not add unnecessary complexity just to increase the number of classes.

Prioritize correctness, maintainability, and demonstrable OOP concepts over unnecessary features.

==================================================
22. BUILD AND EXECUTION
==================================================

The application must run on a normal laptop with a compatible JDK.

Target environment:
- Java 17+
- Linux, including Kali Linux
- Windows compatibility where practical
- 8 GB RAM or higher
- No dedicated GPU required
- No internet connection required

Make the application runnable using javac and java commands.

For Linux, document commands equivalent to:

javac -encoding UTF-8 -d out $(find src -name "*.java")

java -cp out edu.nie.campusfix.Main

Ensure these commands match the actual implemented package structure.

Also provide clear instructions for Windows users.

Do not require IntelliJ, Eclipse, Maven, Gradle, or third-party software for execution.

IDE support is optional.

==================================================
23. FINAL DOCUMENTATION
==================================================

Create a professional README.md containing:

1. Project title
2. Project overview
3. Real-world problem statement
4. Project objectives
5. Features
6. Technology stack
7. System architecture
8. Java OOP concepts demonstrated
9. Package structure
10. Installation instructions
11. Compilation instructions
12. Execution instructions
13. User roles
14. Complete workflow
15. Priority calculation rules
16. Data storage mechanism
17. Security limitations
18. Testing instructions
19. Sample demonstration scenario
20. Team member responsibilities

Also create:

docs/ARCHITECTURE.md
docs/OOP_CONCEPTS.md
docs/TESTING.md
docs/DEMO_GUIDE.md

The OOP documentation must explain real implementation examples from the source code, not generic textbook definitions.

==================================================
24. FINAL QUALITY CHECK
==================================================

Before declaring the project complete:

[ ] Every required Java file exists.
[ ] No core functionality is missing.
[ ] The project compiles successfully.
[ ] All executable automated tests pass.
[ ] Any unexecuted tests are clearly identified.
[ ] GUI can launch in a supported graphical environment.
[ ] Student registration works.
[ ] Administrator initialization works.
[ ] Authentication works.
[ ] Complaint submission works.
[ ] Priority assignment works.
[ ] Duplicate warnings work.
[ ] Technician assignment works.
[ ] Status tracking works.
[ ] History tracking works.
[ ] Overdue detection works.
[ ] Administrator reports work.
[ ] CSV export works.
[ ] Data survives application restart.
[ ] Permissions are enforced.
[ ] Invalid input is handled gracefully.
[ ] No plaintext passwords are stored.
[ ] No external dependencies are required.
[ ] All documentation matches the implemented code.
[ ] All OOP concepts can be demonstrated.
[ ] Build and run instructions are correct.

If an item fails, fix it and rerun the relevant checks.

Do not claim success without evidence.

==================================================
25. FINAL DELIVERABLES
==================================================

Provide:

1. Complete Java source code.
2. Functional Java Swing desktop application.
3. Local file-based persistence.
4. Automated Java test runner.
5. Compilation and execution instructions.
6. README.md.
7. Architecture documentation.
8. Java OOP explanation.
9. UML class design.
10. Team responsibility document.
11. Demonstration instructions.
12. Test results and remaining limitations.

In the final development summary, explicitly report:

- Implemented features
- Successful build command
- Automated test results
- GUI verification status
- Known defects or limitations
- Actual project folder structure
- Instructions to launch the application

==================================================
26. FINAL EXECUTION INSTRUCTIONS
==================================================

You are the project's implementation agent.

Begin by examining the current working directory.

If existing CampusFix files are present:
- Inspect them first.
- Preserve working functionality.
- Improve or complete the implementation.
- Do not overwrite user work blindly.

If no project exists:
- Create the application from scratch.

Work systematically.

Implement the actual code, not just a plan.

Use available tools to create, compile, execute, test, and debug the application.

After each major development phase:
- Check compilation.
- Run relevant tests.
- Fix failures.
- Continue to the next phase.

Do not ask unnecessary clarification questions.

Make reasonable, documented implementation decisions where requirements are underspecified.

If an execution tool is unavailable, do not invent test results. Clearly explain what could not be verified.

Keep all essential application functionality strictly within standard Java.

The ultimate goal is a complete, stable, understandable, and professional Java OOP academic project suitable for a five-member third-semester CSE team.

START DEVELOPMENT NOW.
