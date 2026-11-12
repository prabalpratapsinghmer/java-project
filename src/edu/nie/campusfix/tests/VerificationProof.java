package edu.nie.campusfix.tests;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Role;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.exception.ValidationException;
import edu.nie.campusfix.model.AdminUser;
import edu.nie.campusfix.model.Complaint;
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
import edu.nie.campusfix.ui.AdminDashboardPanel;
import edu.nie.campusfix.ui.LoginFrame;
import edu.nie.campusfix.ui.MainFrame;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.Toolkit;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VerificationProof {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("Running Headless Typography & Public Filing Verification");
        System.out.println("==================================================");

        UITheme.setupSystemLookAndFeel();

        // 1. Verify Typography Tokens
        verifyTypography();

        // 2. Verify Backend Public Complaint Filing
        Path tempDir = Files.createTempDirectory("campusfix_proof_");
        Path userDir = tempDir.resolve("users");
        Path complaintDir = tempDir.resolve("complaints");
        Files.createDirectories(userDir);
        Files.createDirectories(complaintDir);

        UserRepository userRepo = new FileUserRepository(userDir);
        ComplaintRepository compRepo = new FileComplaintRepository(complaintDir);
        AuthenticationService authService = new AuthenticationService(userRepo);
        PriorityService priorityService = new PriorityService();
        DuplicateDetectionService duplicateService = new DuplicateDetectionService(compRepo);
        AssignmentService assignmentService = new AssignmentService(compRepo, userRepo);
        ComplaintService compService = new ComplaintService(compRepo, priorityService, duplicateService, assignmentService);
        ReportService reportService = new ReportService(compRepo, userRepo);
        // 2. Verify Backend Public Complaint Filing
        verifyPublicComplaintBackend(compService, compRepo);

        // 3. Register initial admin first so LoginFrame displays AUTH card (tabs)
        AdminUser admin = authService.registerInitialAdmin("admin_proof", "AdminPass@123".toCharArray(),
                "Proof Administrator", "admin@campus.edu", "9876543210", "ADM-PROOF", "Facilities");

        // 4. Verify LoginFrame structure and rendering
        verifyAndRenderLoginFrame(authService, compService, priorityService, reportService, userRepo);

        // 5. Verify AdminDashboardPanel structure and rendering
        verifyAndRenderAdminDashboard(admin, authService, compService, priorityService, reportService, userRepo);

        // 6. Verify MainFrame maximized window state
        verifyMainFrameMaximized(admin, authService, compService, priorityService, reportService, userRepo);
        System.out.println("\nAll Verification Assertions Passed Successfully!");
    }

    private static void verifyTypography() {
        System.out.print("[CHECK] UITheme Typography Tokens... ");
        assertEqual(UITheme.FONT_TITLE.getSize(), 26, "FONT_TITLE size must be 26");
        assertEqual(UITheme.FONT_TITLE.getStyle(), Font.BOLD, "FONT_TITLE style must be BOLD");

        assertEqual(UITheme.FONT_SUBTITLE.getSize(), 18, "FONT_SUBTITLE size must be 18");
        assertEqual(UITheme.FONT_SUBTITLE.getStyle(), Font.BOLD, "FONT_SUBTITLE style must be BOLD");

        assertEqual(UITheme.FONT_SECTION.getSize(), 16, "FONT_SECTION size must be 16");
        assertEqual(UITheme.FONT_SECTION.getStyle(), Font.BOLD, "FONT_SECTION style must be BOLD");

        assertEqual(UITheme.FONT_REGULAR.getSize(), 15, "FONT_REGULAR size must be 15");
        assertEqual(UITheme.FONT_REGULAR.getStyle(), Font.PLAIN, "FONT_REGULAR style must be PLAIN");

        assertEqual(UITheme.FONT_BOLD.getSize(), 15, "FONT_BOLD size must be 15");
        assertEqual(UITheme.FONT_BOLD.getStyle(), Font.BOLD, "FONT_BOLD style must be BOLD");

        assertEqual(UITheme.FONT_SMALL.getSize(), 13, "FONT_SMALL size must be 13");
        assertEqual(UITheme.FONT_SMALL.getStyle(), Font.PLAIN, "FONT_SMALL style must be PLAIN");

        assertEqual(UITheme.FONT_BADGE.getSize(), 12, "FONT_BADGE size must be 12");
        assertEqual(UITheme.FONT_BADGE.getStyle(), Font.BOLD, "FONT_BADGE style must be BOLD");

        assertEqual(UITheme.FONT_METRIC.getSize(), 32, "FONT_METRIC size must be 32");
        assertEqual(UITheme.FONT_METRIC.getStyle(), Font.BOLD, "FONT_METRIC style must be BOLD");

        assertEqual(Role.STUDENT.getDisplayName(), "Student / Community / Staff", "Role.STUDENT display name");
        System.out.println("PASSED");
    }

    private static void verifyPublicComplaintBackend(ComplaintService compService, ComplaintRepository compRepo) {
        System.out.print("[CHECK] Public Complaint Filing Backend... ");

        Complaint c = compService.submitPublicComplaint(
                "Aarav (Grade 4)", "parent@nie.ac.in", "Broken Swing",
                Category.SAFETY, "School Playground",
                "Swing chain broken and hanging dangerously", Severity.HIGH
        );

        assertNotNull(c, "Complaint should not be null");
        assertTrue(c.getReporterId().startsWith("public_"), "Reporter ID must start with public_");
        assertEqual(c.getReporterName(), "Aarav (Grade 4) (parent@nie.ac.in)", "Reporter formatted name");
        assertEqual(c.getPriority(), Priority.CRITICAL, "Priority must be CRITICAL (High severity + Safety high-risk)");
        assertTrue(compRepo.findById(c.getId()).isPresent(), "Complaint must exist in repository");

        // Without contact
        Complaint c2 = compService.submitPublicComplaint(
                "Aarav (Grade 4)", "", "Water Fountain Leak",
                Category.PLUMBING, "Primary Wing",
                "Fountain continuously leaking water on floor", Severity.MEDIUM
        );
        assertEqual(c2.getReporterName(), "Aarav (Grade 4)", "Reporter name without contact suffix");

        // Validation checks
        boolean nameRejected = false;
        try {
            compService.submitPublicComplaint("A", "parent@nie.ac.in", "Title", Category.SAFETY, "Loc", "Desc", Severity.LOW);
        } catch (ValidationException ex) {
            nameRejected = true;
        }
        assertTrue(nameRejected, "Short reporter name must be rejected");

        System.out.println("PASSED");
    }

    private static void verifyAndRenderLoginFrame(AuthenticationService authService, ComplaintService compService,
                                                 PriorityService priorityService, ReportService reportService,
                                                 UserRepository userRepo) throws Exception {
        System.out.print("[CHECK] LoginFrame 3-Tab Structure & Rendering (1920x1080 Fullscreen)... ");

        File reportsDir = new File("reports");
        if (!reportsDir.exists()) reportsDir.mkdirs();

        LoginFrame frame = new LoginFrame(authService, compService, priorityService, reportService, userRepo);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        boolean isMaximized = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0 || frame.getWidth() == screenSize.width;
        assertTrue(isMaximized, "LoginFrame must be maximized or match screen dimensions");

        Container contentPane = frame.getContentPane();
        List<JPanel> panels = findComponents(contentPane, JPanel.class);
        boolean found780Card = false;
        for (JPanel panel : panels) {
            if (panel.getPreferredSize() != null && panel.getPreferredSize().width == 780) {
                found780Card = true;
                break;
            }
        }
        assertTrue(found780Card, "Authentication card holder preferred width must be 780px");

        int renderWidth = 1920;
        int renderHeight = 1080;
        frame.setSize(renderWidth, renderHeight);
        contentPane.setSize(renderWidth, renderHeight);
        layoutComponent(contentPane, renderWidth, renderHeight);

        JTabbedPane tabbedPane = findComponent(contentPane, JTabbedPane.class);
        assertNotNull(tabbedPane, "LoginFrame must contain a JTabbedPane");
        assertEqual(tabbedPane.getTabCount(), 3, "JTabbedPane must have 3 tabs");
        assertEqual(tabbedPane.getTitleAt(0), "Sign In", "Tab 0 title");
        assertEqual(tabbedPane.getTitleAt(1), "Report Issue (Children & Public)", "Tab 1 title");
        assertEqual(tabbedPane.getTitleAt(2), "Create Account", "Tab 2 title");

        // Render each tab at 1920x1080
        // Tab 1: Report Issue (Children & Public)
        tabbedPane.setSelectedIndex(1);
        layoutComponent(contentPane, renderWidth, renderHeight);
        BufferedImage imgPublic = renderComponent(contentPane, renderWidth, renderHeight);
        ImageIO.write(imgPublic, "PNG", new File("reports/public_complaint_tab.png"));

        // Tab 0: Sign In
        tabbedPane.setSelectedIndex(0);
        layoutComponent(contentPane, renderWidth, renderHeight);
        BufferedImage imgSignIn = renderComponent(contentPane, renderWidth, renderHeight);
        ImageIO.write(imgSignIn, "PNG", new File("reports/sign_in_tab.png"));

        // Tab 2: Create Account
        tabbedPane.setSelectedIndex(2);
        layoutComponent(contentPane, renderWidth, renderHeight);
        BufferedImage imgRegister = renderComponent(contentPane, renderWidth, renderHeight);
        ImageIO.write(imgRegister, "PNG", new File("reports/create_account_tab.png"));

        frame.dispose();
        System.out.println("PASSED (Screenshots saved to reports/)");
    }

    private static void verifyMainFrameMaximized(User admin, AuthenticationService authService,
                                                ComplaintService compService, PriorityService priorityService,
                                                ReportService reportService, UserRepository userRepo) {
        System.out.print("[CHECK] MainFrame Maximized Window State... ");
        MainFrame mainFrame = new MainFrame(admin, authService, compService, priorityService, reportService, userRepo);
        assertTrue((mainFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0, "MainFrame must have MAXIMIZED_BOTH state");
        mainFrame.dispose();
        System.out.println("PASSED");
    }

    private static void verifyAndRenderAdminDashboard(User admin, AuthenticationService authService,
                                                     ComplaintService compService, PriorityService priorityService,
                                                     ReportService reportService, UserRepository userRepo) throws Exception {
        System.out.print("[CHECK] AdminDashboardPanel + Log New Complaint Button & Rendering... ");

        JFrame dummyFrame = new JFrame();
        AdminDashboardPanel adminPanel = new AdminDashboardPanel(dummyFrame, admin, compService, priorityService,
                reportService, userRepo, authService);

        List<ModernButton> buttons = findComponents(adminPanel, ModernButton.class);
        boolean foundLogComplaintBtn = false;
        boolean foundAssignBtn = false;
        for (ModernButton btn : buttons) {
            if ("+ Log New Complaint".equals(btn.getText())) {
                foundLogComplaintBtn = true;
            }
            if ("Assign / Reassign".equals(btn.getText())) {
                foundAssignBtn = true;
            }
        }
        assertTrue(foundLogComplaintBtn, "+ Log New Complaint button must exist in AdminDashboardPanel");
        assertTrue(foundAssignBtn, "Assign / Reassign button must exist in AdminDashboardPanel");

        dummyFrame.add(adminPanel);
        dummyFrame.setSize(1240, 800);
        adminPanel.setSize(1020, 730);
        layoutComponent(adminPanel, 1020, 730);
        adminPanel.refreshComplaints();
        layoutComponent(adminPanel, 1020, 730);

        BufferedImage imgAdmin = renderComponent(adminPanel, 1020, 730);
        ImageIO.write(imgAdmin, "PNG", new File("reports/admin_dashboard.png"));

        dummyFrame.dispose();
        System.out.println("PASSED (Screenshot saved to reports/admin_dashboard.png)");
    }

    private static void layoutComponent(Component c, int width, int height) {
        c.setSize(width, height);
        c.doLayout();
        if (c instanceof Container cont) {
            cont.doLayout();
            for (Component child : cont.getComponents()) {
                layoutComponent(child, child.getWidth(), child.getHeight());
            }
        }
    }

    private static BufferedImage renderComponent(Component c, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(UITheme.BG_CANVAS);
        g2.fillRect(0, 0, width, height);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.printAll(g2);
        g2.dispose();
        return img;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T findComponent(Container root, Class<T> clazz) {
        for (Component c : root.getComponents()) {
            if (clazz.isInstance(c)) {
                return (T) c;
            }
            if (c instanceof Container cont) {
                T found = findComponent(cont, clazz);
                if (found != null) return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> List<T> findComponents(Container root, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (clazz.isInstance(c)) {
                result.add((T) c);
            }
            if (c instanceof Container cont) {
                result.addAll(findComponents(cont, clazz));
            }
        }
        return result;
    }

    private static void assertEqual(Object actual, Object expected, String message) {
        if (!actual.equals(expected)) {
            throw new AssertionError(message + " - Expected: <" + expected + ">, but got: <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("Assertion failed (must not be null): " + message);
        }
    }
}
