package edu.nie.campusfix.ui;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.exception.CampusFixException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.UserRepository;
import edu.nie.campusfix.service.AuthenticationService;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.service.PriorityService;
import edu.nie.campusfix.service.ReportService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.theme.UITheme;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.List;
/**
 * Modern entry window providing secure login, first-launch administrator initialization,
 * and student account self-registration.
 */
@SuppressWarnings({"serial", "this-escape"})
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final AuthenticationService authService;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;
    private final ReportService reportService;
    private final UserRepository userRepository;

    private final CardLayout rootCardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootCardLayout);

    public LoginFrame(AuthenticationService authService, ComplaintService complaintService,
                      PriorityService priorityService, ReportService reportService,
                      UserRepository userRepository) {
        super("CampusFix – Smart Campus Maintenance & Complaint Management");
        this.authService = authService;
        this.complaintService = complaintService;
        this.priorityService = priorityService;
        this.reportService = reportService;
        this.userRepository = userRepository;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(720, 680));
        setLocationRelativeTo(null);
        setResizable(true);
        getContentPane().setBackground(UITheme.BG_CANVAS);

        rootPanel.add(createScrollableCenterWrapper(createAuthCard(), 780), "AUTH");
        rootPanel.add(createScrollableCenterWrapper(createAdminInitCard(), 780), "ADMIN_INIT");

        add(rootPanel);

        if (authService.isInitialAdminSetupRequired()) {
            rootCardLayout.show(rootPanel, "ADMIN_INIT");
        } else {
            rootCardLayout.show(rootPanel, "AUTH");
        }
    }

    private JPanel createHeaderPanel(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("CampusFix");
        brand.setFont(UITheme.FONT_TITLE);
        brand.setForeground(UITheme.PRIMARY);
        brand.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        header.add(brand);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);
        header.add(Box.createVerticalStrut(16));
        return header;
    }

    private JPanel createAuthCard() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        wrapper.add(createHeaderPanel("CampusFix", "Smart Campus Maintenance System"), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(UITheme.TEXT_PRIMARY);

        tabbedPane.addTab("Sign In", createSignInTab());
        tabbedPane.addTab("Report Issue (Children & Public)", createPublicComplaintTab());
        tabbedPane.addTab("Create Account", createRegisterTab());
        UITheme.styleTabbedPane(tabbedPane);

        wrapper.add(tabbedPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createSignInTab() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        UITheme.styleTextField(usernameField);
        UITheme.styleTextField(passwordField);

        int row = 0;
        gbc.gridy = row++;
        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(UITheme.FONT_BOLD);
        userLbl.setForeground(UITheme.TEXT_PRIMARY);
        card.add(userLbl, gbc);

        gbc.gridy = row++;
        card.add(usernameField, gbc);

        gbc.gridy = row++;
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(UITheme.FONT_BOLD);
        passLbl.setForeground(UITheme.TEXT_PRIMARY);
        card.add(passLbl, gbc);

        gbc.gridy = row++;
        card.add(passwordField, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(18, 8, 8, 8);
        ModernButton loginBtn = new ModernButton("Sign In to CampusFix", ModernButton.Style.PRIMARY);
        loginBtn.addActionListener(e -> {
            String u = usernameField.getText().trim();
            char[] p = passwordField.getPassword();
            try {
                User user = authService.login(u, p);
                openMainFrame(user);
            } catch (CampusFixException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        card.add(loginBtn, gbc);

        return card;
    }

    private JPanel createRegisterTab() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField usnField = new JTextField();
        JTextField deptField = new JTextField();

        UITheme.styleTextField(userField);
        UITheme.styleTextField(passField);
        UITheme.styleTextField(nameField);
        UITheme.styleTextField(emailField);
        UITheme.styleTextField(phoneField);
        UITheme.styleTextField(usnField);
        UITheme.styleTextField(deptField);

        int[] row = new int[]{0};
        addStackedField(card, gbc, row, "Username *", userField);
        addStackedField(card, gbc, row, "Password (min 6 chars) *", passField);
        addStackedField(card, gbc, row, "Full Name *", nameField);
        addStackedField(card, gbc, row, "Email Address *", emailField);
        addStackedField(card, gbc, row, "Phone", phoneField);
        addStackedField(card, gbc, row, "Student ID / School Roll No (Optional)", usnField);
        addStackedField(card, gbc, row, "Department / School Grade / Community (Optional)", deptField);

        gbc.gridy = row[0]++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(14, 6, 8, 6);
        ModernButton regBtn = new ModernButton("Create Account & Sign In", ModernButton.Style.PRIMARY);
        regBtn.addActionListener(e -> {
            try {
                User student = authService.registerStudent(
                        userField.getText().trim(),
                        passField.getPassword(),
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        usnField.getText().trim(),
                        deptField.getText().trim()
                );
                JOptionPane.showMessageDialog(this,
                        "Account created successfully for " + student.getFullName() + "! You are now signed in.",
                        "Registration Successful", JOptionPane.INFORMATION_MESSAGE);
                openMainFrame(student);
            } catch (CampusFixException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        card.add(regBtn, gbc);

        return card;
    }
    private JPanel createPublicComplaintTab() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel sub = new JLabel("<html>Open to children, students, visitors, parents & community &mdash; no account required</html>");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_MUTED);

        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();
        JTextField titleField = new JTextField();
        JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());
        JTextField locationField = new JTextField();
        JComboBox<Severity> severityCombo = new JComboBox<>(Severity.values());
        JTextArea descArea = new JTextArea(4, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        UITheme.styleTextArea(descArea);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setPreferredSize(new Dimension(200, 88));
        descScroll.setMinimumSize(new Dimension(150, 75));
        descScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_INPUT, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        UITheme.styleTextField(nameField);
        UITheme.styleTextField(contactField);
        UITheme.styleTextField(titleField);
        UITheme.styleComboBox(categoryCombo);
        UITheme.styleTextField(locationField);
        UITheme.styleComboBox(severityCombo);

        int[] row = new int[]{0};
        gbc.gridy = row[0]++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(2, 6, 8, 6);
        card.add(sub, gbc);

        addStackedField(card, gbc, row, "Your Name *", nameField);
        addStackedField(card, gbc, row, "Contact Phone / Email", contactField);
        addStackedField(card, gbc, row, "Issue Title *", titleField);
        addStackedField(card, gbc, row, "Category *", categoryCombo);
        addStackedField(card, gbc, row, "Campus Location *", locationField);
        addStackedField(card, gbc, row, "Severity Level *", severityCombo);
        addStackedField(card, gbc, row, "Detailed Description *", descScroll);

        gbc.gridy = row[0]++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 6, 8, 6);
        ModernButton submitBtn = new ModernButton("Submit Complaint Immediately", ModernButton.Style.PRIMARY);
        submitBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String contact = contactField.getText().trim();
            String title = titleField.getText().trim();
            Category cat = (Category) categoryCombo.getSelectedItem();
            String loc = locationField.getText().trim();
            Severity sev = (Severity) severityCombo.getSelectedItem();
            String desc = descArea.getText().trim();

            List<Complaint> potentialDuplicates = complaintService.checkPotentialDuplicates(loc, cat);
            if (!potentialDuplicates.isEmpty()) {
                Complaint dup = potentialDuplicates.get(0);
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "A similar unresolved issue has already been reported for this location:\n\n" +
                                "\"" + dup.getTitle() + "\" (" + dup.getStatus().getDisplayName() + ", " + dup.getPriority().getDisplayName() + " Priority)\n" +
                                "Location: " + dup.getLocation() + "\n\n" +
                                "Do you still want to submit?",
                        "Potential Duplicate Detected",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                Complaint complaint = complaintService.submitPublicComplaint(name, contact, title, cat, loc, desc, sev);
                JOptionPane.showMessageDialog(this,
                        "Complaint Submitted Successfully!\n\n" +
                                "Complaint ID: " + complaint.getId() + "\n" +
                                "Priority: " + complaint.getPriority().getDisplayName() + "\n" +
                                "Estimated Response: " + complaint.getPriority().getResponseTargetHours() + "h\n\n" +
                                "Please save your Complaint ID to track progress below.",
                        "Submission Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                nameField.setText("");
                contactField.setText("");
                titleField.setText("");
                locationField.setText("");
                descArea.setText("");
            } catch (CampusFixException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Submission Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        card.add(submitBtn, gbc);

        // Quick Status Tracking section
        gbc.gridy = row[0]++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 6, 4, 6);
        JPanel trackSection = new JPanel(new BorderLayout(8, 8));
        trackSection.setOpaque(false);
        trackSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(12, 0, 4, 0)
        ));
        JLabel trackHeader = new JLabel("Track Existing Complaint Status");
        trackHeader.setFont(UITheme.FONT_BOLD);
        trackHeader.setForeground(UITheme.TEXT_PRIMARY);
        trackSection.add(trackHeader, BorderLayout.NORTH);

        JPanel trackInputRow = new JPanel(new BorderLayout(8, 0));
        trackInputRow.setOpaque(false);
        JTextField trackField = new JTextField();
        UITheme.styleTextField(trackField);
        trackField.setToolTipText("Enter Complaint ID...");
        ModernButton trackBtn = new ModernButton("Check Status", ModernButton.Style.SECONDARY);
        trackBtn.addActionListener(e -> {
            String id = trackField.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Complaint ID to check status.", "Track Status", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Complaint c = complaintService.getComplaintById(id);
                StringBuilder sb = new StringBuilder();
                sb.append("Complaint ID: ").append(c.getId()).append("\n");
                sb.append("Title: ").append(c.getTitle()).append("\n");
                sb.append("Category: ").append(c.getCategory().getDisplayName()).append("\n");
                sb.append("Location: ").append(c.getLocation()).append("\n");
                sb.append("Status: ").append(c.getStatus().getDisplayName()).append("\n");
                sb.append("Priority: ").append(c.getPriority().getDisplayName())
                        .append(" (Target: ").append(c.getPriority().getResponseTargetHours()).append("h)\n");
                String techName = c.getAssignedTechnicianName();
                sb.append("Assigned Technician: ").append(techName != null && !techName.isEmpty() ? techName : "Unassigned").append("\n");
                if (c.getResolutionNotes() != null && !c.getResolutionNotes().isBlank()) {
                    sb.append("Resolution Notes: ").append(c.getResolutionNotes()).append("\n");
                }
                JOptionPane.showMessageDialog(this, sb.toString(), "Complaint Status: " + c.getId(), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No complaint found with ID. Please check the ID and try again.", "Not Found", JOptionPane.WARNING_MESSAGE);
            }
        });
        trackInputRow.add(trackField, BorderLayout.CENTER);
        trackInputRow.add(trackBtn, BorderLayout.EAST);
        trackSection.add(trackInputRow, BorderLayout.CENTER);
        card.add(trackSection, gbc);

        return card;
    }

    private void addRegField(JPanel panel, GridBagConstraints gbc, int row, String label, Object comp) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.35;
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(l, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.65;
        if (comp instanceof java.awt.Component c) {
            panel.add(c, gbc);
        }
    }
    private void addStackedField(JPanel panel, GridBagConstraints gbc, int[] row, String label, Object comp) {
        gbc.gridy = row[0]++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 6, 2, 6);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(l, gbc);

        gbc.gridy = row[0]++;
        gbc.insets = new Insets(2, 6, 6, 6);
        if (comp instanceof java.awt.Component c) {
            panel.add(c, gbc);
        }
    }


    private JPanel createAdminInitCard() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("Initial Administrator Setup");
        brand.setFont(UITheme.FONT_TITLE);
        brand.setForeground(UITheme.PRIMARY);
        brand.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("First launch detected. Please configure the primary administrative account.");
        sub.setFont(UITheme.FONT_REGULAR);
        sub.setForeground(UITheme.TEXT_MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        header.add(brand);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);
        header.add(Box.createVerticalStrut(14));
        wrapper.add(header, BorderLayout.NORTH);

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = new JTextField("admin");
        JPasswordField passField = new JPasswordField();
        JTextField nameField = new JTextField("Campus Administrator");
        JTextField emailField = new JTextField("admin@nie.ac.in");
        JTextField phoneField = new JTextField();
        JTextField empIdField = new JTextField("ADM-001");
        JTextField deptField = new JTextField("Campus Infrastructure");

        UITheme.styleTextField(userField);
        UITheme.styleTextField(passField);
        UITheme.styleTextField(nameField);
        UITheme.styleTextField(emailField);
        UITheme.styleTextField(phoneField);
        UITheme.styleTextField(empIdField);
        UITheme.styleTextField(deptField);

        int row = 0;
        addRegField(card, gbc, row++, "Admin Username *", userField);
        addRegField(card, gbc, row++, "Admin Password *", passField);
        addRegField(card, gbc, row++, "Full Name *", nameField);
        addRegField(card, gbc, row++, "Email Address *", emailField);
        addRegField(card, gbc, row++, "Phone", phoneField);
        addRegField(card, gbc, row++, "Employee ID", empIdField);
        addRegField(card, gbc, row++, "Department", deptField);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 6, 4, 6);
        ModernButton initBtn = new ModernButton("Complete Administrator Initialization", ModernButton.Style.PRIMARY);
        initBtn.addActionListener(e -> {
            try {
                User admin = authService.registerInitialAdmin(
                        userField.getText().trim(),
                        passField.getPassword(),
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        empIdField.getText().trim(),
                        deptField.getText().trim()
                );
                JOptionPane.showMessageDialog(this,
                        "Administrator account initialized successfully!\nWelcome, " + admin.getFullName(),
                        "Setup Complete", JOptionPane.INFORMATION_MESSAGE);
                openMainFrame(admin);
            } catch (CampusFixException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        card.add(initBtn, gbc);

        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private void openMainFrame(User user) {
        MainFrame mainFrame = new MainFrame(user, authService, complaintService, priorityService, reportService, userRepository);
        mainFrame.setVisible(true);
        dispose();
    }

    private JScrollPane createScrollableCenterWrapper(JPanel card, int maxWidth) {
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        JPanel contentHolder = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(maxWidth, super.getPreferredSize().height);
            }
        };
        contentHolder.setOpaque(false);
        contentHolder.add(card, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(contentHolder, gbc);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }
}
