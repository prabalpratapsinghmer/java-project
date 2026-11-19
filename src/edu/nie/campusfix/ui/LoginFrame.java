package edu.nie.campusfix.ui;

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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Entry window strictly implementing CampusFix's authenticated 3-role model:
 * 1. Welcome / Sign In
 * 2. Student / Staff Account Registration
 * 3. First-Launch Initial Administrator Setup
 */
@SuppressWarnings({"serial", "this-escape"})
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public static final String CARD_SIGN_IN = "SIGN_IN";
    public static final String CARD_REGISTER = "REGISTER";
    public static final String CARD_ADMIN_INIT = "ADMIN_INIT";

    private final AuthenticationService authService;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;
    private final ReportService reportService;
    private final UserRepository userRepository;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardHost = new JPanel(cardLayout);

    private ModernButton signInSubmitBtn;
    private ModernButton registerSubmitBtn;
    private ModernButton adminInitSubmitBtn;

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
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int initW = Math.min(screen.width - 40, UITheme.s(1240));
        int initH = Math.min(screen.height - 60, UITheme.s(780));
        setSize(initW, initH);
        setMinimumSize(new Dimension(Math.min(screen.width - 40, UITheme.s(980)), Math.min(screen.height - 60, UITheme.s(660))));
        setLocationRelativeTo(null);
        setResizable(true);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        add(createBrandSidebar(), BorderLayout.WEST);

        cardHost.setBackground(UITheme.BG_CANVAS);
        cardHost.add(wrapCenteredCard(createSignInCard(), UITheme.s(520), UITheme.s(660)), CARD_SIGN_IN);
        cardHost.add(wrapCenteredCard(createRegisterCard(), UITheme.s(580), UITheme.s(820)), CARD_REGISTER);
        cardHost.add(wrapCenteredCard(createAdminInitCard(), UITheme.s(580), UITheme.s(820)), CARD_ADMIN_INIT);

        add(cardHost, BorderLayout.CENTER);

        if (authService.isInitialAdminSetupRequired()) {
            showCard(CARD_ADMIN_INIT);
        } else {
            showCard(CARD_SIGN_IN);
        }
    }

    /**
     * Switches the visible authentication screen and updates the default Enter button.
     */
    public void showCard(String cardName) {
        cardLayout.show(cardHost, cardName);
        if (CARD_SIGN_IN.equals(cardName) && signInSubmitBtn != null) {
            getRootPane().setDefaultButton(signInSubmitBtn);
        } else if (CARD_REGISTER.equals(cardName) && registerSubmitBtn != null) {
            getRootPane().setDefaultButton(registerSubmitBtn);
        } else if (CARD_ADMIN_INIT.equals(cardName) && adminInitSubmitBtn != null) {
            getRootPane().setDefaultButton(adminInitSubmitBtn);
        }
    }

    private JPanel createBrandSidebar() {
        JPanel brand = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, UITheme.SIDEBAR_BG,
                        getWidth(), getHeight(), new Color(0x1E, 0x1B, 0x4B)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle decorative accent circles
                g2.setColor(new Color(0x4F, 0x46, 0xE5, 35));
                g2.fillOval(-60, getHeight() - 220, 280, 280);
                g2.fillOval(getWidth() - 140, -80, 220, 220);
                g2.dispose();
            }
        };
        brand.setPreferredSize(new Dimension(UITheme.s(410), UITheme.s(760)));
        brand.setLayout(new BorderLayout());
        brand.setBorder(BorderFactory.createEmptyBorder(UITheme.s(44), UITheme.s(36), UITheme.s(36), UITheme.s(36)));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel badge = new JLabel("CAMPUS INFRASTRUCTURE PORTAL");
        badge.setFont(UITheme.FONT_BADGE);
        badge.setForeground(new Color(0xA5, 0xB4, 0xFC));
        badge.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("CampusFix");
        title.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, UITheme.s(38)));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("<html><body style='width: " + UITheme.s(260) + "px;'>Smart Campus Maintenance and Complaint Management System</body></html>");
        subtitle.setFont(UITheme.FONT_REGULAR);
        subtitle.setForeground(UITheme.SIDEBAR_TEXT);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        top.add(badge);
        top.add(Box.createVerticalStrut(10));
        top.add(title);
        top.add(Box.createVerticalStrut(8));
        top.add(subtitle);
        top.add(Box.createVerticalStrut(32));

        top.add(createFeatureBullet("Deterministic Priority Engine",
                "Automatic severity & high-risk scoring for rapid hazard response."));
        top.add(Box.createVerticalStrut(16));
        top.add(createFeatureBullet("7-Day Duplicate Detection",
                "Prevents redundant dispatches across normalized campus locations."));
        top.add(Box.createVerticalStrut(16));
        top.add(createFeatureBullet("Role-Based Accountability",
                "Dedicated workflows for Students/Staff, Admins, and Technicians."));
        top.add(Box.createVerticalStrut(16));
        top.add(createFeatureBullet("Immutable Audit Trail & Reports",
                "Timestamped lifecycle tracking, SLA monitoring, and CSV export."));

        brand.add(top, BorderLayout.NORTH);

        JLabel footer = new JLabel("<html><body style='width: 290px;'>Pure Java 17+ Desktop Application<br/>PBKDF2-SHA256 Auth • Atomic Local Persistence</body></html>");
        footer.setFont(UITheme.FONT_SMALL);
        footer.setForeground(UITheme.SIDEBAR_MUTED);
        brand.add(footer, BorderLayout.SOUTH);

        return brand;
    }

    private JPanel createFeatureBullet(String heading, String detail) {
        JPanel p = new JPanel(new BorderLayout(10, 2));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(UITheme.s(340), UITheme.s(78)));

        JLabel dot = new JLabel("●");
        dot.setFont(UITheme.FONT_SMALL);
        dot.setForeground(new Color(0x81, 0x8C, 0xF8));
        dot.setVerticalAlignment(JLabel.TOP);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel h = new JLabel(heading);
        h.setFont(UITheme.FONT_BOLD);
        h.setForeground(Color.WHITE);

        JLabel d = new JLabel("<html><body style='width: " + UITheme.s(235) + "px;'>" + detail + "</body></html>");
        d.setFont(UITheme.FONT_SMALL);
        d.setForeground(UITheme.SIDEBAR_MUTED);

        text.add(h);
        text.add(Box.createVerticalStrut(2));
        text.add(d);

        p.add(dot, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private JPanel createSignInCard() {
        JPanel card = UITheme.createCardPanel(28, 30);
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        gbc.gridy = row++;
        JLabel heading = new JLabel("Sign in to your account");
        heading.setFont(UITheme.FONT_TITLE);
        heading.setForeground(UITheme.TEXT_PRIMARY);
        card.add(heading, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 4, 18, 4);
        JLabel sub = new JLabel("Enter your CampusFix credentials to access your role dashboard.");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_MUTED);
        card.add(sub, gbc);

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        UITheme.styleTextField(usernameField);
        UITheme.styleTextField(passwordField);

        gbc.insets = new Insets(4, 4, 2, 4);
        gbc.gridy = row++;
        card.add(createFieldLabel("Username"), gbc);

        gbc.insets = new Insets(2, 4, 10, 4);
        gbc.gridy = row++;
        card.add(usernameField, gbc);

        gbc.insets = new Insets(4, 4, 2, 4);
        gbc.gridy = row++;
        card.add(createFieldLabel("Password"), gbc);

        gbc.insets = new Insets(2, 4, 6, 4);
        gbc.gridy = row++;
        card.add(passwordField, gbc);

        JCheckBox showPass = new JCheckBox("Show password");
        UITheme.styleCheckBox(showPass, UITheme.TEXT_MUTED);
        char defaultEcho = passwordField.getEchoChar();
        showPass.addActionListener(e -> passwordField.setEchoChar(showPass.isSelected() ? (char) 0 : defaultEcho));
        gbc.insets = new Insets(0, 4, 14, 4);
        gbc.gridy = row++;
        card.add(showPass, gbc);

        signInSubmitBtn = new ModernButton("Sign In to CampusFix", ModernButton.Style.PRIMARY);
        signInSubmitBtn.addActionListener(e -> {
            String u = usernameField.getText().trim();
            char[] p = passwordField.getPassword();
            if (u.isEmpty() || p.length == 0) {
                Arrays.fill(p, '\0');
                JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                        "Missing Credentials", JOptionPane.WARNING_MESSAGE);
                return;
            }
            signInSubmitBtn.setEnabled(false);
            signInSubmitBtn.setText("Signing in...");
            new SwingWorker<User, Void>() {
                @Override
                protected User doInBackground() {
                    return authService.login(u, p);
                }

                @Override
                protected void done() {
                    signInSubmitBtn.setEnabled(true);
                    signInSubmitBtn.setText("Sign In to CampusFix");
                    try {
                        User user = get();
                        passwordField.setText("");
                        openMainFrame(user);
                    } catch (InterruptedException | ExecutionException ex) {
                        passwordField.setText("");
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(LoginFrame.this, cause.getMessage(),
                                "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        gbc.insets = new Insets(4, 4, 14, 4);
        gbc.gridy = row++;
        card.add(signInSubmitBtn, gbc);

        // Footer switch to registration
        JPanel switchRow = new JPanel(new BorderLayout());
        switchRow.setOpaque(false);
        switchRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(14, 0, 0, 0)
        ));
        JLabel promptLbl = new JLabel("New student or staff member?");
        promptLbl.setFont(UITheme.FONT_SMALL);
        promptLbl.setForeground(UITheme.TEXT_MUTED);

        ModernButton goRegBtn = new ModernButton("Create Account", ModernButton.Style.GHOST);
        goRegBtn.addActionListener(e -> showCard(CARD_REGISTER));

        switchRow.add(promptLbl, BorderLayout.WEST);
        switchRow.add(goRegBtn, BorderLayout.EAST);

        gbc.insets = new Insets(4, 4, 0, 4);
        gbc.gridy = row;
        card.add(switchRow, gbc);

        return card;
    }

    private JPanel createRegisterCard() {
        JPanel card = UITheme.createCardPanel(24, 28);
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 4, 2, 4);
        JLabel heading = new JLabel("Create Student / Staff Account");
        heading.setFont(UITheme.FONT_TITLE);
        heading.setForeground(UITheme.TEXT_PRIMARY);
        card.add(heading, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 4, 14, 4);
        JLabel sub = new JLabel("Public registration creates standard Student / Staff accounts only.");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_MUTED);
        card.add(sub, gbc);

        JTextField userField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField idField = new JTextField();
        JTextField deptField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JPasswordField confirmPassField = new JPasswordField();

        UITheme.styleTextField(userField);
        UITheme.styleTextField(nameField);
        UITheme.styleTextField(emailField);
        UITheme.styleTextField(phoneField);
        UITheme.styleTextField(idField);
        UITheme.styleTextField(deptField);
        UITheme.styleTextField(passField);
        UITheme.styleTextField(confirmPassField);

        row = addTwoColumnRow(card, gbc, row, "Username *", userField, "Full Name *", nameField);
        row = addTwoColumnRow(card, gbc, row, "Email Address *", emailField, "Phone (Optional)", phoneField);
        row = addTwoColumnRow(card, gbc, row, "Student / Staff ID", idField, "Department", deptField);
        row = addTwoColumnRow(card, gbc, row, "Password (min 6 chars) *", passField, "Confirm Password *", confirmPassField);

        JCheckBox showPass = new JCheckBox("Show passwords");
        UITheme.styleCheckBox(showPass, UITheme.TEXT_MUTED);
        char defaultEcho = passField.getEchoChar();
        showPass.addActionListener(e -> {
            char echo = showPass.isSelected() ? (char) 0 : defaultEcho;
            passField.setEchoChar(echo);
            confirmPassField.setEchoChar(echo);
        });
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 4, 12, 4);
        card.add(showPass, gbc);

        registerSubmitBtn = new ModernButton("Create Account & Sign In", ModernButton.Style.PRIMARY);
        registerSubmitBtn.addActionListener(e -> {
            char[] pass = passField.getPassword();
            char[] confirm = confirmPassField.getPassword();
            if (!Arrays.equals(pass, confirm)) {
                Arrays.fill(pass, '\0');
                Arrays.fill(confirm, '\0');
                JOptionPane.showMessageDialog(this, "Passwords do not match. Please re-enter.",
                        "Password Mismatch", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Arrays.fill(confirm, '\0');

            String u = userField.getText().trim();
            String n = nameField.getText().trim();
            String em = emailField.getText().trim();
            String ph = phoneField.getText().trim();
            String sid = idField.getText().trim();
            String dep = deptField.getText().trim();

            registerSubmitBtn.setEnabled(false);
            registerSubmitBtn.setText("Creating account...");

            new SwingWorker<User, Void>() {
                @Override
                protected User doInBackground() {
                    return authService.registerStudent(u, pass, n, em, ph, sid, dep);
                }

                @Override
                protected void done() {
                    registerSubmitBtn.setEnabled(true);
                    registerSubmitBtn.setText("Create Account & Sign In");
                    try {
                        User student = get();
                        passField.setText("");
                        confirmPassField.setText("");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Account created successfully for " + student.getFullName() + "!\nYou are now signed in.",
                                "Registration Successful", JOptionPane.INFORMATION_MESSAGE);
                        openMainFrame(student);
                    } catch (InterruptedException | ExecutionException ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(LoginFrame.this, cause.getMessage(),
                                "Registration Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 4, 14, 4);
        card.add(registerSubmitBtn, gbc);

        JPanel backRow = new JPanel(new BorderLayout());
        backRow.setOpaque(false);
        backRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(12, 0, 0, 0)
        ));
        JLabel alreadyLbl = new JLabel("Already have a CampusFix account?");
        alreadyLbl.setFont(UITheme.FONT_SMALL);
        alreadyLbl.setForeground(UITheme.TEXT_MUTED);
        ModernButton backBtn = new ModernButton("Back to Sign In", ModernButton.Style.GHOST);
        backBtn.addActionListener(e -> showCard(CARD_SIGN_IN));
        backRow.add(alreadyLbl, BorderLayout.WEST);
        backRow.add(backBtn, BorderLayout.EAST);

        gbc.gridy = row;
        gbc.insets = new Insets(2, 4, 0, 4);
        card.add(backRow, gbc);

        return card;
    }

    private JPanel createAdminInitCard() {
        JPanel card = UITheme.createCardPanel(24, 28);
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 4, 2, 4);
        JLabel heading = new JLabel("Initial Administrator Setup");
        heading.setFont(UITheme.FONT_TITLE);
        heading.setForeground(UITheme.TEXT_PRIMARY);
        card.add(heading, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 4, 14, 4);
        JLabel sub = new JLabel("First launch detected — configure your primary administrator account.");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_MUTED);
        card.add(sub, gbc);

        JTextField userField = new JTextField("admin");
        JTextField nameField = new JTextField("Campus Administrator");
        JTextField emailField = new JTextField("admin@campus.edu");
        JTextField phoneField = new JTextField();
        JTextField empIdField = new JTextField("ADM-001");
        JTextField deptField = new JTextField("Campus Infrastructure");
        JPasswordField passField = new JPasswordField();
        JPasswordField confirmPassField = new JPasswordField();

        UITheme.styleTextField(userField);
        UITheme.styleTextField(nameField);
        UITheme.styleTextField(emailField);
        UITheme.styleTextField(phoneField);
        UITheme.styleTextField(empIdField);
        UITheme.styleTextField(deptField);
        UITheme.styleTextField(passField);
        UITheme.styleTextField(confirmPassField);

        row = addTwoColumnRow(card, gbc, row, "Admin Username *", userField, "Full Name *", nameField);
        row = addTwoColumnRow(card, gbc, row, "Email Address *", emailField, "Phone (Optional)", phoneField);
        row = addTwoColumnRow(card, gbc, row, "Employee ID", empIdField, "Department", deptField);
        row = addTwoColumnRow(card, gbc, row, "Admin Password (min 6) *", passField, "Confirm Password *", confirmPassField);

        JCheckBox showPass = new JCheckBox("Show passwords");
        UITheme.styleCheckBox(showPass, UITheme.TEXT_MUTED);
        char defaultEcho = passField.getEchoChar();
        showPass.addActionListener(e -> {
            char echo = showPass.isSelected() ? (char) 0 : defaultEcho;
            passField.setEchoChar(echo);
            confirmPassField.setEchoChar(echo);
        });
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 4, 12, 4);
        card.add(showPass, gbc);

        adminInitSubmitBtn = new ModernButton("Complete Administrator Setup", ModernButton.Style.PRIMARY);
        adminInitSubmitBtn.addActionListener(e -> {
            char[] pass = passField.getPassword();
            char[] confirm = confirmPassField.getPassword();
            if (!Arrays.equals(pass, confirm)) {
                Arrays.fill(pass, '\0');
                Arrays.fill(confirm, '\0');
                JOptionPane.showMessageDialog(this, "Passwords do not match. Please re-enter.",
                        "Password Mismatch", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Arrays.fill(confirm, '\0');

            String u = userField.getText().trim();
            String n = nameField.getText().trim();
            String em = emailField.getText().trim();
            String ph = phoneField.getText().trim();
            String eid = empIdField.getText().trim();
            String dep = deptField.getText().trim();

            adminInitSubmitBtn.setEnabled(false);
            adminInitSubmitBtn.setText("Initializing...");

            new SwingWorker<User, Void>() {
                @Override
                protected User doInBackground() {
                    return authService.registerInitialAdmin(u, pass, n, em, ph, eid, dep);
                }

                @Override
                protected void done() {
                    adminInitSubmitBtn.setEnabled(true);
                    adminInitSubmitBtn.setText("Complete Administrator Setup");
                    try {
                        User admin = get();
                        passField.setText("");
                        confirmPassField.setText("");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Administrator account initialized successfully!\nWelcome, " + admin.getFullName() + ".",
                                "Setup Complete", JOptionPane.INFORMATION_MESSAGE);
                        openMainFrame(admin);
                    } catch (InterruptedException | ExecutionException ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(LoginFrame.this, cause.getMessage(),
                                "Initialization Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        gbc.gridy = row;
        gbc.insets = new Insets(6, 4, 4, 4);
        card.add(adminInitSubmitBtn, gbc);

        return card;
    }

    private int addTwoColumnRow(JPanel panel, GridBagConstraints gbc, int startRow,
                                String lbl1, JComponent comp1, String lbl2, JComponent comp2) {
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;

        gbc.gridy = startRow;
        gbc.gridx = 0;
        gbc.insets = new Insets(4, 4, 2, 8);
        panel.add(createFieldLabel(lbl1), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(4, 8, 2, 4);
        panel.add(createFieldLabel(lbl2), gbc);

        gbc.gridy = startRow + 1;
        gbc.gridx = 0;
        gbc.insets = new Insets(2, 4, 8, 8);
        panel.add(comp1, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(2, 8, 8, 4);
        panel.add(comp2, gbc);

        return startRow + 2;
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }

    private JScrollPane wrapCenteredCard(JPanel card, int minWidth, int maxWidth) {
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(UITheme.BG_CANVAS);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(UITheme.s(28), UITheme.s(32), UITheme.s(28), UITheme.s(32)));

        JPanel holder = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                int avail = centerPanel.getWidth() > 0 ? centerPanel.getWidth() - UITheme.s(64) : minWidth;
                int targetW = Math.max(minWidth, Math.min(maxWidth, (int) (avail * 0.82)));
                return new Dimension(targetW, super.getPreferredSize().height);
            }
        };
        holder.setOpaque(false);
        holder.add(card, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(holder, gbc);

        JScrollPane scroll = new JScrollPane(centerPanel);
        UITheme.styleScrollPane(scroll);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.BG_CANVAS);
        scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                holder.revalidate();
                centerPanel.revalidate();
            }
        });
        return scroll;
    }

    private void openMainFrame(User user) {
        MainFrame mainFrame = new MainFrame(user, authService, complaintService, priorityService, reportService, userRepository);
        mainFrame.setVisible(true);
        dispose();
    }
}
