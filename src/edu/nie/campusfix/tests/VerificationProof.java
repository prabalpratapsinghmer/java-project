package edu.nie.campusfix.tests;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.model.AdminUser;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.StudentUser;
import edu.nie.campusfix.model.TechnicianUser;
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
import edu.nie.campusfix.ui.AdminDashboardPanel;
import edu.nie.campusfix.ui.LoginFrame;
import edu.nie.campusfix.ui.MainFrame;
import edu.nie.campusfix.ui.StudentDashboardPanel;
import edu.nie.campusfix.ui.TechnicianDashboardPanel;
import edu.nie.campusfix.ui.dialog.ComplaintDetailDialog;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Renders offscreen high-resolution screenshots of all core CampusFix Swing views
 * in an isolated temporary sandbox to visually verify layout, typography, pill badges,
 * contrast, and zero-clipping across standard laptop dimensions (1280x800).
 */
public class VerificationProof {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("Running CampusFix Visual & Component Verification");
        System.out.println("==================================================");

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[SKIP] Headless environment detected (no X11/Wayland display). Run under Xvfb or desktop to render screenshots.");
            return;
        }

        UITheme.setupSystemLookAndFeel();

        Path tempDir = Files.createTempDirectory("campusfix_visual_proof_");
        try {
            Path userDir = tempDir.resolve("users");
            Path complaintDir = tempDir.resolve("complaints");
            File reportsDir = new File("reports");
            if (!reportsDir.exists() && !reportsDir.mkdirs()) {
                throw new IOException("Could not create reports directory");
            }

            UserRepository userRepo = new FileUserRepository(userDir);
            ComplaintRepository compRepo = new FileComplaintRepository(complaintDir);
            AuthenticationService authService = new AuthenticationService(userRepo);
            PriorityService priorityService = new PriorityService();
            DuplicateDetectionService duplicateService = new DuplicateDetectionService(compRepo);
            AssignmentService assignmentService = new AssignmentService(compRepo, userRepo);
            ComplaintService compService = new ComplaintService(compRepo, priorityService, duplicateService, assignmentService);
            ReportService reportService = new ReportService(compRepo, userRepo);

            // 1. Capture Initial Admin Setup before any admin exists
            SwingUtilities.invokeAndWait(() -> {
                try {
                    LoginFrame initFrame = new LoginFrame(authService, compService, priorityService, reportService, userRepo);
                    renderFrameToFile(initFrame, 1120, 720, new File("reports/01_initial_admin_setup.png"));
                    initFrame.dispose();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // 2. Seed rich realistic scenario in temp sandbox
            AdminUser admin = authService.registerInitialAdmin("admin", "AdminPass@123".toCharArray(),
                    "Dr. Priya Nair", "admin@nie.ac.in", "9876543210", "ADM-001", "Campus Infrastructure");
            StudentUser student = authService.registerStudent("arun_cs", "StudentPass@123".toCharArray(),
                    "Arun Patel", "arun.patel@nie.ac.in", "9876543211", "4NI23CS042", "Computer Science");
            TechnicianUser techElec = authService.createTechnician(admin, "rajesh_elec", "TechPass@123".toCharArray(),
                    "Rajesh Kumar", "rajesh@nie.ac.in", "9876543212", "TECH-101", Category.ELECTRICAL);
            TechnicianUser techPlumb = authService.createTechnician(admin, "suresh_plumb", "TechPass@123".toCharArray(),
                    "Suresh Verma", "suresh@nie.ac.in", "9876543213", "TECH-102", Category.PLUMBING);

            Complaint c1 = compService.submitComplaint(student, "Exposed Switchboard Sparking", Category.ELECTRICAL,
                    "CS Block - Lab 3", "Power outlet near row 2 sparks loudly whenever projector plug is inserted.", Severity.HIGH);
            compService.assignTechnician(admin, c1.getId(), techElec.getId(), "Immediate safety dispatch");
            c1 = compService.startWork(techElec, c1.getId(), "Isolated circuit breaker and inspecting wiring harness");

            Complaint c2 = compService.submitComplaint(student, "Leaking Overhead Pipe", Category.PLUMBING,
                    "Hostel B - 2nd Floor", "Constant water dripping onto hallway corridor near Room 214.", Severity.MEDIUM);
            compService.assignTechnician(admin, c2.getId(), techPlumb.getId(), "Check main valve and joint seal");

            Complaint c3 = compService.submitComplaint(student, "Broken Projector Mount", Category.CLASSROOM_EQUIPMENT,
                    "Golden Jubilee Block - LH 104", "Ceiling bracket bolt is loose and projector tilts downward.", Severity.HIGH);

            Complaint c4 = compService.submitComplaint(student, "Damaged Bench Frame", Category.FURNITURE,
                    "Mechanical Block - Room 108", "Metal welding on third row desk frame snapped.", Severity.LOW);
            compService.assignTechnician(admin, c4.getId(), techPlumb.getId(), "Assigned for repair");
            compService.startWork(techPlumb, c4.getId(), "Welding frame joint");
            c4 = compService.resolveComplaint(techPlumb, c4.getId(), "Re-welded steel joint and applied anti-rust primer.");

            // Add one overdue complaint for Overdue Queue & Reports screenshots
            Instant overdueTime = Instant.now().minus(38, ChronoUnit.HOURS);
            Complaint overdue = new Complaint(
                    UUID.randomUUID().toString(), student.getId(), student.getFullName(),
                    "Emergency Exit Light Outage", Category.SAFETY, "Central Library - Ground Floor",
                    "Main corridor emergency exit sign and backup battery light are completely dead.",
                    Severity.HIGH, Priority.CRITICAL, ComplaintStatus.PENDING, "", "",
                    overdueTime, overdueTime, null, "", List.of()
            );
            compRepo.create(overdue);

            final Complaint detailSample = c4;

            SwingUtilities.invokeAndWait(() -> {
                try {
                    // Sign In & Register screens
                    LoginFrame loginFrame = new LoginFrame(authService, compService, priorityService, reportService, userRepo);
                    loginFrame.showCard(LoginFrame.CARD_SIGN_IN);
                    renderFrameToFile(loginFrame, 1120, 720, new File("reports/02_sign_in.png"));
                    loginFrame.showCard(LoginFrame.CARD_REGISTER);
                    renderFrameToFile(loginFrame, 1120, 720, new File("reports/03_register_student.png"));
                    loginFrame.dispose();

                    // Student Dashboard
                    MainFrame studentFrame = new MainFrame(student, authService, compService, priorityService, reportService, userRepo);
                    studentFrame.switchStudentView(StudentDashboardPanel.VIEW_MY_COMPLAINTS, "My Complaints");
                    renderFrameToFile(studentFrame, 1280, 780, new File("reports/04_student_complaints.png"));
                    studentFrame.dispose();

                    // Admin Dashboard views
                    MainFrame adminFrame = new MainFrame(admin, authService, compService, priorityService, reportService, userRepo);
                    adminFrame.switchAdminView(AdminDashboardPanel.VIEW_DASHBOARD, "Administrator Dashboard");
                    renderFrameToFile(adminFrame, 1280, 780, new File("reports/05_admin_dashboard.png"));

                    adminFrame.switchAdminView(AdminDashboardPanel.VIEW_ALL_COMPLAINTS, "All Complaints");
                    renderFrameToFile(adminFrame, 1280, 780, new File("reports/06_admin_all_complaints.png"));

                    adminFrame.switchAdminView(AdminDashboardPanel.VIEW_REPORTS, "Reports & Analytics");
                    renderFrameToFile(adminFrame, 1280, 820, new File("reports/07_admin_reports.png"));

                    // Complaint Details Dialog
                    ComplaintDetailDialog detailDlg = new ComplaintDetailDialog(adminFrame, detailSample, priorityService);
                    renderContainerToFile(detailDlg.getContentPane(), 860, 740, new File("reports/09_complaint_details.png"));
                    detailDlg.dispose();
                    adminFrame.dispose();

                    // Technician Assigned Tasks
                    MainFrame techFrame = new MainFrame(techElec, authService, compService, priorityService, reportService, userRepo);
                    techFrame.switchTechView(TechnicianDashboardPanel.VIEW_TASKS, "Assigned Tasks");
                    renderFrameToFile(techFrame, 1280, 780, new File("reports/08_technician_tasks.png"));
                    techFrame.dispose();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            System.out.println("[PASS] Generated all 9 verification screenshots in reports/");
        } finally {
            cleanupDir(tempDir);
        }
    }

    private static void renderFrameToFile(javax.swing.JFrame frame, int width, int height, File outFile) throws IOException {
        frame.setSize(width, height);
        frame.addNotify();
        frame.validate();
        layoutTree(frame.getContentPane(), width, height);
        renderContainerToFile(frame.getContentPane(), width, height, outFile);
    }

    private static void renderContainerToFile(Container cont, int width, int height, File outFile) throws IOException {
        cont.setSize(width, height);
        layoutTree(cont, width, height);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(UITheme.BG_CANVAS);
        g2.fillRect(0, 0, width, height);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        cont.printAll(g2);
        g2.dispose();
        ImageIO.write(img, "PNG", outFile);
    }

    private static void layoutTree(Component comp, int width, int height) {
        if (width > 0 && height > 0) {
            comp.setSize(width, height);
        }
        comp.validate();
        comp.doLayout();
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                layoutTree(child, child.getWidth(), child.getHeight());
            }
        }
    }

    private static void cleanupDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                try (var s = Files.walk(dir)) {
                    s.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            System.err.println("[VerificationProof] Cleanup warning: " + e.getMessage());
                        }
                    });
                }
            }
        } catch (IOException e) {
            System.err.println("[VerificationProof] Cleanup error: " + e.getMessage());
        }
    }
}
