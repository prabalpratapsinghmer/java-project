package edu.nie.campusfix.ui;

import edu.nie.campusfix.enums.Role;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.UserRepository;
import edu.nie.campusfix.service.AuthenticationService;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.service.PriorityService;
import edu.nie.campusfix.service.ReportService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.component.PillBadge;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main application window featuring responsive top navigation bar, fully wired dark-slate
 * sidebar navigation (zero decorative buttons), and role-specific workspaces.
 */
@SuppressWarnings({"serial", "this-escape"})
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final User currentUser;
    private final AuthenticationService authService;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;
    private final ReportService reportService;
    private final UserRepository userRepository;

    private final Map<String, ModernButton> navButtons = new LinkedHashMap<>();
    private final JLabel headerContextLabel = new JLabel();

    private StudentDashboardPanel studentDashboard;
    private AdminDashboardPanel adminDashboard;
    private TechnicianDashboardPanel technicianDashboard;

    public MainFrame(User currentUser, AuthenticationService authService,
                     ComplaintService complaintService, PriorityService priorityService,
                     ReportService reportService, UserRepository userRepository) {
        super("CampusFix – Smart Campus Maintenance System (" + currentUser.getRole().getDisplayName() + ")");
        this.currentUser = currentUser;
        this.authService = authService;
        this.complaintService = complaintService;
        this.priorityService = priorityService;
        this.reportService = reportService;
        this.userRepository = userRepository;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(screen.width - 40, Math.max(UITheme.s(1280), (int) (screen.width * 0.88)));
        int height = Math.min(screen.height - 60, Math.max(UITheme.s(820), (int) (screen.height * 0.88)));
        setSize(width, height);
        setMinimumSize(new Dimension(Math.min(screen.width - 40, UITheme.s(1024)), Math.min(screen.height - 60, UITheme.s(680))));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        // Create content panel before sidebar so sidebar handlers can switch views cleanly
        JPanel content = createContentPanel();
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createSidebarPanel(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(getWidth(), 62));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(10, 22, 10, 22)
        ));

        // Left: Brand + current section breadcrumb
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        brandBox.setOpaque(false);

        JLabel brandLbl = new JLabel("CampusFix");
        brandLbl.setFont(UITheme.FONT_TITLE);
        brandLbl.setForeground(UITheme.PRIMARY);

        JLabel sep = new JLabel("|");
        sep.setFont(UITheme.FONT_REGULAR);
        sep.setForeground(UITheme.BORDER_INPUT);

        headerContextLabel.setFont(UITheme.FONT_BOLD);
        headerContextLabel.setForeground(UITheme.TEXT_SECONDARY);

        brandBox.add(brandLbl);
        brandBox.add(sep);
        brandBox.add(headerContextLabel);
        header.add(brandBox, BorderLayout.WEST);

        // Right: User profile + Role badge + Sign Out
        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        userBox.setOpaque(false);

        JLabel userLbl = new JLabel(currentUser.getFullName());
        userLbl.setFont(UITheme.FONT_BOLD);
        userLbl.setForeground(UITheme.TEXT_PRIMARY);

        PillBadge roleBadge = PillBadge.createForRole(currentUser.getRole());

        ModernButton signOutBtn = new ModernButton("Sign Out", ModernButton.Style.SECONDARY);
        signOutBtn.addActionListener(e -> handleSignOut());

        userBox.add(userLbl);
        userBox.add(roleBadge);
        userBox.add(Box.createHorizontalStrut(4));
        userBox.add(signOutBtn);
        header.add(userBox, BorderLayout.EAST);

        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(UITheme.s(252), getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 14, 20, 14));

        JLabel navTitle = new JLabel("WORKSPACE");
        navTitle.setFont(UITheme.FONT_BADGE);
        navTitle.setForeground(UITheme.SIDEBAR_MUTED);
        navTitle.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        navTitle.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(navTitle);
        sidebar.add(Box.createVerticalStrut(12));

        if (currentUser.getRole() == Role.STUDENT) {
            addNavButton(sidebar, StudentDashboardPanel.VIEW_DASHBOARD, "Dashboard", () -> switchStudentView(StudentDashboardPanel.VIEW_DASHBOARD, "Student Dashboard"));
            addNavButton(sidebar, StudentDashboardPanel.VIEW_MY_COMPLAINTS, "My Complaints", () -> switchStudentView(StudentDashboardPanel.VIEW_MY_COMPLAINTS, "My Complaints"));

            sidebar.add(Box.createVerticalStrut(10));
            ModernButton newCompBtn = new ModernButton("+ New Complaint", ModernButton.Style.PRIMARY);
            newCompBtn.setAlignmentX(LEFT_ALIGNMENT);
            newCompBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            newCompBtn.addActionListener(e -> {
                if (studentDashboard != null) {
                    studentDashboard.openNewComplaintDialog();
                }
            });
            sidebar.add(newCompBtn);

            setActiveNav(StudentDashboardPanel.VIEW_DASHBOARD, "Student Dashboard");

        } else if (currentUser.getRole() == Role.ADMIN) {
            addNavButton(sidebar, AdminDashboardPanel.VIEW_DASHBOARD, "Dashboard Overview", () -> switchAdminView(AdminDashboardPanel.VIEW_DASHBOARD, "Administrator Dashboard"));
            addNavButton(sidebar, AdminDashboardPanel.VIEW_ALL_COMPLAINTS, "All Complaints", () -> switchAdminView(AdminDashboardPanel.VIEW_ALL_COMPLAINTS, "All Complaints"));
            addNavButton(sidebar, AdminDashboardPanel.VIEW_DUPLICATES, "Duplicate Review", () -> switchAdminView(AdminDashboardPanel.VIEW_DUPLICATES, "Potential Duplicates"));
            addNavButton(sidebar, AdminDashboardPanel.VIEW_OVERDUE, "Overdue Queue", () -> switchAdminView(AdminDashboardPanel.VIEW_OVERDUE, "Overdue Complaints"));
            addNavButton(sidebar, AdminDashboardPanel.VIEW_TECHNICIANS, "Technicians", () -> switchAdminView(AdminDashboardPanel.VIEW_TECHNICIANS, "Technician Roster"));
            addNavButton(sidebar, AdminDashboardPanel.VIEW_REPORTS, "Reports & Export", () -> switchAdminView(AdminDashboardPanel.VIEW_REPORTS, "Reports & Analytics"));

            setActiveNav(AdminDashboardPanel.VIEW_DASHBOARD, "Administrator Dashboard");

        } else if (currentUser.getRole() == Role.TECHNICIAN) {
            addNavButton(sidebar, TechnicianDashboardPanel.VIEW_DASHBOARD, "Technician Dashboard", () -> switchTechView(TechnicianDashboardPanel.VIEW_DASHBOARD, "Technician Dashboard"));
            addNavButton(sidebar, TechnicianDashboardPanel.VIEW_TASKS, "Assigned Tasks", () -> switchTechView(TechnicianDashboardPanel.VIEW_TASKS, "Assigned Tasks"));

            setActiveNav(TechnicianDashboardPanel.VIEW_DASHBOARD, "Technician Dashboard");
        }

        sidebar.add(Box.createVerticalGlue());

        // Account footer card in sidebar
        JPanel footerCard = new JPanel(new BorderLayout(0, 4));
        footerCard.setBackground(UITheme.SIDEBAR_HOVER);
        footerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.SIDEBAR_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        footerCard.setAlignmentX(LEFT_ALIGNMENT);
        footerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        JLabel signedInAs = new JLabel("SIGNED IN AS @" + currentUser.getUsername());
        signedInAs.setFont(UITheme.FONT_BADGE);
        signedInAs.setForeground(new Color(0xA5, 0xB4, 0xFC));

        JLabel roleDesc = new JLabel("<html><body style='width: 175px; color: #CBD5E1; font-size: 11px;'>"
                + currentUser.getRoleDescription() + "</body></html>");

        footerCard.add(signedInAs, BorderLayout.NORTH);
        footerCard.add(roleDesc, BorderLayout.CENTER);
        sidebar.add(footerCard);

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String key, String label, Runnable action) {
        ModernButton btn = new ModernButton(label, ModernButton.Style.SIDEBAR);
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.addActionListener(e -> action.run());
        navButtons.put(key, btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private void setActiveNav(String key, String breadcrumbTitle) {
        for (Map.Entry<String, ModernButton> entry : navButtons.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(key));
        }
        headerContextLabel.setText(breadcrumbTitle);
    }

    public void switchStudentView(String view, String title) {
        if (studentDashboard != null) {
            studentDashboard.showView(view);
            setActiveNav(view, title);
        }
    }

    public void switchAdminView(String view, String title) {
        if (adminDashboard != null) {
            adminDashboard.showView(view);
            setActiveNav(view, title);
        }
    }

    public void switchTechView(String view, String title) {
        if (technicianDashboard != null) {
            technicianDashboard.showView(view);
            setActiveNav(view, title);
        }
    }

    private JPanel createContentPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(UITheme.BG_CANVAS);

        if (currentUser.getRole() == Role.STUDENT) {
            studentDashboard = new StudentDashboardPanel(this, currentUser, complaintService, priorityService);
            studentDashboard.setOnOpenAllAction(() -> switchStudentView(StudentDashboardPanel.VIEW_MY_COMPLAINTS, "My Complaints"));
            container.add(studentDashboard, BorderLayout.CENTER);
        } else if (currentUser.getRole() == Role.ADMIN) {
            adminDashboard = new AdminDashboardPanel(this, currentUser, complaintService, priorityService, reportService, userRepository, authService);
            adminDashboard.setNavigationHandler(view -> {
                String title = switch (view) {
                    case AdminDashboardPanel.VIEW_ALL_COMPLAINTS -> "All Complaints";
                    case AdminDashboardPanel.VIEW_DUPLICATES -> "Potential Duplicates";
                    case AdminDashboardPanel.VIEW_OVERDUE -> "Overdue Complaints";
                    case AdminDashboardPanel.VIEW_TECHNICIANS -> "Technician Roster";
                    case AdminDashboardPanel.VIEW_REPORTS -> "Reports & Analytics";
                    default -> "Administrator Dashboard";
                };
                switchAdminView(view, title);
            });
            container.add(adminDashboard, BorderLayout.CENTER);
        } else if (currentUser.getRole() == Role.TECHNICIAN) {
            technicianDashboard = new TechnicianDashboardPanel(this, currentUser, complaintService, priorityService);
            technicianDashboard.setOnOpenTasksAction(() -> switchTechView(TechnicianDashboardPanel.VIEW_TASKS, "Assigned Tasks"));
            container.add(technicianDashboard, BorderLayout.CENTER);
        }

        return container;
    }

    private void handleSignOut() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to sign out of CampusFix?", "Confirm Sign Out",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            LoginFrame loginFrame = new LoginFrame(authService, complaintService, priorityService, reportService, userRepository);
            loginFrame.setVisible(true);
            dispose();
        }
    }
}
