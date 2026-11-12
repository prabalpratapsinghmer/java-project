package edu.nie.campusfix.ui;

import edu.nie.campusfix.exception.CampusFixException;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
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
        setSize(560, 680);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_CANVAS);

        rootPanel.add(createAuthCard(), "AUTH");
        rootPanel.add(createAdminInitCard(), "ADMIN_INIT");

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
        wrapper.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        wrapper.add(createHeaderPanel("CampusFix", "Smart Campus Maintenance System"), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("Sign In", createSignInTab());
        tabbedPane.addTab("Register as Student", createRegisterTab());

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
        card.add(userLbl, gbc);

        gbc.gridy = row++;
        card.add(usernameField, gbc);

        gbc.gridy = row++;
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(UITheme.FONT_BOLD);
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

        int row = 0;
        addRegField(card, gbc, row++, "Username *", userField);
        addRegField(card, gbc, row++, "Password (min 6 chars) *", passField);
        addRegField(card, gbc, row++, "Full Name *", nameField);
        addRegField(card, gbc, row++, "Email Address *", emailField);
        addRegField(card, gbc, row++, "Phone", phoneField);
        addRegField(card, gbc, row++, "Student ID / USN", usnField);
        addRegField(card, gbc, row++, "Department (e.g. CSE)", deptField);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 6, 4, 6);
        ModernButton regBtn = new ModernButton("Create Student Account", ModernButton.Style.PRIMARY);
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

    private void addRegField(JPanel panel, GridBagConstraints gbc, int row, String label, Object comp) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.35;
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BOLD);
        panel.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
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
}
