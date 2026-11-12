package edu.nie.campusfix;

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
import edu.nie.campusfix.ui.LoginFrame;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * CampusFix – Smart Campus Maintenance and Complaint Management System
 * Application entry point wiring dependency injection, persistence layers, and desktop GUI.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("CampusFix – Smart Campus Maintenance System");
        System.out.println("Object-Oriented Programming with Java (Experiential Learning)");
        System.out.println("==================================================");

        // Initialize Repositories (Data Persistence)
        UserRepository userRepository = new FileUserRepository();
        ComplaintRepository complaintRepository = new FileComplaintRepository();

        // Initialize Domain Services (Business Logic)
        PriorityService priorityService = new PriorityService();
        DuplicateDetectionService duplicateService = new DuplicateDetectionService(complaintRepository);
        AssignmentService assignmentService = new AssignmentService(complaintRepository, userRepository);
        ComplaintService complaintService = new ComplaintService(complaintRepository, priorityService, duplicateService, assignmentService);
        ReportService reportService = new ReportService(complaintRepository, userRepository);
        AuthenticationService authService = new AuthenticationService(userRepository);

        // Check graphical environment availability
        boolean headless = GraphicsEnvironment.isHeadless();
        for (String arg : args) {
            if ("--headless".equalsIgnoreCase(arg) || "--cli".equalsIgnoreCase(arg) || "--status".equalsIgnoreCase(arg)) {
                headless = true;
                break;
            }
        }

        if (headless) {
            System.out.println("[CampusFix] Running in headless / CLI mode.");
            ReportService.ComplaintSummaryReport report = reportService.generateSummary();
            System.out.println("Status Summary:");
            System.out.println(" - Total Complaints: " + report.totalComplaints());
            System.out.println(" - Pending: " + report.pendingCount());
            System.out.println(" - Assigned: " + report.assignedCount());
            System.out.println(" - In Progress: " + report.inProgressCount());
            System.out.println(" - Resolved: " + report.resolvedCount());
            System.out.println(" - Overdue: " + report.overdueCount());
            System.out.println("Admin initialized: " + !authService.isInitialAdminSetupRequired());
            System.out.println("To launch desktop GUI, run in an X11/Wayland display environment.");
            return;
        }

        // Initialize Swing Look & Feel and launch LoginFrame on EDT
        UITheme.setupSystemLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame(authService, complaintService, priorityService, reportService, userRepository);
                loginFrame.setVisible(true);
            } catch (Exception e) {
                System.err.println("[CampusFix] Failed to launch graphical user interface: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
