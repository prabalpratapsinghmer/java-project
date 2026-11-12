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
import edu.nie.campusfix.ui.dialog.NewComplaintDialog;
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

/**
 * Main application window featuring responsive top navigation, dark slate sidebar,
 * and role-tailored dashboard containers.
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
        setSize(1240, 800);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createSidebarPanel(), BorderLayout.WEST);
        add(createContentPanel(), BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(getWidth(), 64));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));

        // Brand
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        brandBox.setOpaque(false);

        JLabel brandLbl = new JLabel("CampusFix");
        brandLbl.setFont(UITheme.FONT_TITLE);
        brandLbl.setForeground(UITheme.PRIMARY);

        JLabel subBrand = new JLabel("Smart Campus Maintenance System");
        subBrand.setFont(UITheme.FONT_SMALL);
        subBrand.setForeground(UITheme.TEXT_MUTED);

        brandBox.add(brandLbl);
        brandBox.add(subBrand);
        header.add(brandBox, BorderLayout.WEST);

        // User profile pill + sign out
        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        userBox.setOpaque(false);

        JLabel userLbl = new JLabel(currentUser.getFullName());
        userLbl.setFont(UITheme.FONT_BOLD);
        userLbl.setForeground(UITheme.TEXT_PRIMARY);

        PillBadge roleBadge = PillBadge.createForRole(currentUser.getRole());

        ModernButton signOutBtn = new ModernButton("Sign Out", ModernButton.Style.SECONDARY);
        signOutBtn.addActionListener(e -> handleSignOut());

        userBox.add(userLbl);
        userBox.add(roleBadge);
        userBox.add(Box.createHorizontalStrut(8));
        userBox.add(signOutBtn);
        header.add(userBox, BorderLayout.EAST);

        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));

        JLabel navTitle = new JLabel("NAVIGATION");
        navTitle.setFont(UITheme.FONT_BADGE);
        navTitle.setForeground(UITheme.SIDEBAR_TEXT);
        sidebar.add(navTitle);
        sidebar.add(Box.createVerticalStrut(14));

        if (currentUser.getRole() == Role.STUDENT) {
            ModernButton myComplaintsBtn = new ModernButton("My Complaints", ModernButton.Style.SIDEBAR);
            myComplaintsBtn.setActive(true);
            sidebar.add(myComplaintsBtn);

            sidebar.add(Box.createVerticalStrut(8));
            ModernButton newCompBtn = new ModernButton("+ Report Issue", ModernButton.Style.SIDEBAR);
            newCompBtn.addActionListener(e -> {
                NewComplaintDialog dlg = new NewComplaintDialog(this, complaintService, priorityService, currentUser);
                dlg.setVisible(true);
                if (dlg.isComplaintSubmitted() && studentDashboard != null) {
                    studentDashboard.loadData();
                }
            });
            sidebar.add(newCompBtn);

        } else if (currentUser.getRole() == Role.ADMIN) {
            ModernButton overviewBtn = new ModernButton("Dashboard Overview", ModernButton.Style.SIDEBAR);
            overviewBtn.setActive(true);
            sidebar.add(overviewBtn);

        } else if (currentUser.getRole() == Role.TECHNICIAN) {
            ModernButton tasksBtn = new ModernButton("Assigned Tasks", ModernButton.Style.SIDEBAR);
            tasksBtn.setActive(true);
            sidebar.add(tasksBtn);
        }

        sidebar.add(Box.createVerticalGlue());

        // Role details footer in sidebar
        JLabel roleDesc = new JLabel("<html><body style='width: 170px; color: #94A3B8; font-size: 11px;'>"
                + currentUser.getRoleDescription() + "</body></html>");
        sidebar.add(roleDesc);

        return sidebar;
    }

    private JPanel createContentPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        if (currentUser.getRole() == Role.STUDENT) {
            studentDashboard = new StudentDashboardPanel(this, currentUser, complaintService, priorityService);
            container.add(studentDashboard, BorderLayout.CENTER);
        } else if (currentUser.getRole() == Role.ADMIN) {
            adminDashboard = new AdminDashboardPanel(this, currentUser, complaintService, priorityService, reportService, userRepository, authService);
            container.add(adminDashboard, BorderLayout.CENTER);
        } else if (currentUser.getRole() == Role.TECHNICIAN) {
            technicianDashboard = new TechnicianDashboardPanel(this, currentUser, complaintService, priorityService);
            container.add(technicianDashboard, BorderLayout.CENTER);
        }

        return container;
    }

    private void handleSignOut() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to sign out?", "Confirm Sign Out",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            LoginFrame loginFrame = new LoginFrame(authService, complaintService, priorityService, reportService, userRepository);
            loginFrame.setVisible(true);
            dispose();
        }
    }
}
