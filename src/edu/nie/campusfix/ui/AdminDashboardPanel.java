package edu.nie.campusfix.ui;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.exception.CampusFixException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.UserRepository;
import edu.nie.campusfix.service.AuthenticationService;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.service.PriorityService;
import edu.nie.campusfix.service.ReportService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.component.ModernTable;
import edu.nie.campusfix.ui.component.PillBadge;
import edu.nie.campusfix.ui.component.StatCard;
import edu.nie.campusfix.ui.dialog.AssignTechnicianDialog;
import edu.nie.campusfix.ui.dialog.ComplaintDetailDialog;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modern central administration dashboard providing multi-criteria complaint oversight,
 * technician management, and detailed reports generation with CSV export.
 */
@SuppressWarnings({"serial", "this-escape"})
public class AdminDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final JFrame parentFrame;
    private final User adminUser;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final AuthenticationService authService;

    // KPI Cards
    private final StatCard totalCard = new StatCard("Total Complaints", 0, UITheme.PRIMARY);
    private final StatCard pendingCard = new StatCard("Pending", 0, UITheme.WARNING);
    private final StatCard assignedCard = new StatCard("Assigned", 0, new Color(0x63, 0x66, 0xF1));
    private final StatCard inProgressCard = new StatCard("In Progress", 0, new Color(0x02, 0x84, 0xC7));
    private final StatCard resolvedCard = new StatCard("Resolved", 0, UITheme.SUCCESS);
    private final StatCard overdueCard = new StatCard("Overdue", 0, UITheme.DANGER);

    // Filter controls
    private final JTextField searchField = new JTextField(12);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JComboBox<String> priorityFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JCheckBox overdueCheck = new JCheckBox("Overdue Only");

    // Table
    private final DefaultTableModel complaintsModel;
    private final ModernTable complaintsTable;
    private List<Complaint> currentComplaints = new ArrayList<>();

    // Technicians Tab Table
    private final DefaultTableModel techModel;
    private final ModernTable techTable;

    // Reports UI Components
    private final JPanel categoryBreakdownPanel = new JPanel();
    private final JPanel priorityBreakdownPanel = new JPanel();
    private final JPanel statusBreakdownPanel = new JPanel();
    private final JPanel techWorkloadPanel = new JPanel();

    public AdminDashboardPanel(JFrame parentFrame, User adminUser,
                               ComplaintService complaintService, PriorityService priorityService,
                               ReportService reportService, UserRepository userRepository,
                               AuthenticationService authService) {
        this.parentFrame = parentFrame;
        this.adminUser = adminUser;
        this.complaintService = complaintService;
        this.priorityService = priorityService;
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.authService = authService;

        setLayout(new BorderLayout());
        setOpaque(false);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        // Complaints Table Model
        String[] compCols = {"Title", "Category", "Location", "Priority", "Status", "Reporter", "Technician", "Submitted"};
        complaintsModel = new DefaultTableModel(compCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 3) return Priority.class;
                if (col == 4) return ComplaintStatus.class;
                return String.class;
            }
        };
        complaintsTable = new ModernTable(complaintsModel);

        // Tech Table Model
        String[] techCols = {"Technician ID", "Full Name", "Username", "Specialization", "Active Status", "Active Tasks"};
        techModel = new DefaultTableModel(techCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        techTable = new ModernTable(techModel);

        tabbedPane.addTab("Complaints Overview", createComplaintsTab());
        tabbedPane.addTab("Technician Roster", createTechnicianTab());
        tabbedPane.addTab("Reports & Analytics", createReportsTab());

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 0) refreshComplaints();
            else if (idx == 1) refreshTechnicians();
            else if (idx == 2) refreshReports();
        });

        add(tabbedPane, BorderLayout.CENTER);
        refreshComplaints();
    }

    private JPanel createComplaintsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Stats Row
        JPanel statsRow = new JPanel(new GridLayout(1, 6, 10, 0));
        statsRow.setOpaque(false);
        statsRow.add(totalCard);
        statsRow.add(pendingCard);
        statsRow.add(assignedCard);
        statsRow.add(inProgressCard);
        statsRow.add(resolvedCard);
        statsRow.add(overdueCard);
        panel.add(statsRow, BorderLayout.NORTH);

        // Table Card
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        // Filter Bar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftFilters.setOpaque(false);

        UITheme.styleTextField(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshComplaints(); }
            public void removeUpdate(DocumentEvent e) { refreshComplaints(); }
            public void changedUpdate(DocumentEvent e) { refreshComplaints(); }
        });

        categoryFilter.addItem("All Categories");
        for (Category c : Category.values()) categoryFilter.addItem(c.name());
        UITheme.styleComboBox(categoryFilter);
        categoryFilter.addActionListener(e -> refreshComplaints());

        priorityFilter.addItem("All Priorities");
        for (Priority p : Priority.values()) priorityFilter.addItem(p.name());
        UITheme.styleComboBox(priorityFilter);
        priorityFilter.addActionListener(e -> refreshComplaints());

        statusFilter.addItem("All Statuses");
        for (ComplaintStatus s : ComplaintStatus.values()) statusFilter.addItem(s.name());
        UITheme.styleComboBox(statusFilter);
        statusFilter.addActionListener(e -> refreshComplaints());

        overdueCheck.setFont(UITheme.FONT_BOLD);
        overdueCheck.setForeground(UITheme.DANGER);
        overdueCheck.setOpaque(false);
        overdueCheck.addActionListener(e -> refreshComplaints());

        ModernButton resetBtn = new ModernButton("Reset", ModernButton.Style.SECONDARY);
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilter.setSelectedIndex(0);
            priorityFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            overdueCheck.setSelected(false);
            refreshComplaints();
        });

        leftFilters.add(new JLabel("Search:"));
        leftFilters.add(searchField);
        leftFilters.add(categoryFilter);
        leftFilters.add(priorityFilter);
        leftFilters.add(statusFilter);
        leftFilters.add(overdueCheck);
        leftFilters.add(resetBtn);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        ModernButton viewBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
        viewBtn.addActionListener(e -> handleViewDetails());

        ModernButton assignBtn = new ModernButton("Assign / Reassign", ModernButton.Style.PRIMARY);
        assignBtn.addActionListener(e -> handleAssignTechnician());

        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> refreshComplaints());

        rightActions.add(viewBtn);
        rightActions.add(assignBtn);
        rightActions.add(refreshBtn);

        toolbar.add(leftFilters, BorderLayout.WEST);
        toolbar.add(rightActions, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        // Table
        complaintsTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        complaintsTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        complaintsTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        complaintsTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        complaintsTable.getColumnModel().getColumn(4).setPreferredWidth(95);
        complaintsTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        complaintsTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        complaintsTable.getColumnModel().getColumn(7).setPreferredWidth(110);

        JScrollPane scroll = new JScrollPane(complaintsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD));
        card.add(scroll, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTechnicianTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel title = new JLabel("Maintenance Technicians");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        ModernButton addTechBtn = new ModernButton("+ Register New Technician", ModernButton.Style.PRIMARY);
        addTechBtn.addActionListener(e -> openAddTechnicianDialog());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(addTechBtn, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout());

        JScrollPane scroll = new JScrollPane(techTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD));
        card.add(scroll, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel title = new JLabel("Complaint Reports & Workload Analytics");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        ModernButton exportBtn = new ModernButton("Export All to CSV", ModernButton.Style.SUCCESS);
        exportBtn.addActionListener(e -> handleExportCsv());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(exportBtn, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);

        grid.add(wrapReportCard("CATEGORY BREAKDOWN", categoryBreakdownPanel));
        grid.add(wrapReportCard("PRIORITY BREAKDOWN", priorityBreakdownPanel));
        grid.add(wrapReportCard("STATUS BREAKDOWN", statusBreakdownPanel));
        grid.add(wrapReportCard("TECHNICIAN WORKLOAD", techWorkloadPanel));

        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel wrapReportCard(String title, JPanel contentPanel) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_BADGE);
        titleLbl.setForeground(UITheme.TEXT_MUTED);
        card.add(titleLbl, BorderLayout.NORTH);

        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    public void refreshComplaints() {
        ReportService.ComplaintSummaryReport report = reportService.generateSummary();
        totalCard.setValue(report.totalComplaints());
        pendingCard.setValue(report.pendingCount());
        assignedCard.setValue(report.assignedCount());
        inProgressCard.setValue(report.inProgressCount());
        resolvedCard.setValue(report.resolvedCount());
        overdueCard.setValue(report.overdueCount());

        String query = searchField.getText().trim();
        Category cat = categoryFilter.getSelectedIndex() > 0 ? Category.valueOf((String) categoryFilter.getSelectedItem()) : null;
        Priority prio = priorityFilter.getSelectedIndex() > 0 ? Priority.valueOf((String) priorityFilter.getSelectedItem()) : null;
        ComplaintStatus status = statusFilter.getSelectedIndex() > 0 ? ComplaintStatus.valueOf((String) statusFilter.getSelectedItem()) : null;
        Boolean overdue = overdueCheck.isSelected() ? Boolean.TRUE : null;

        currentComplaints = complaintService.searchComplaints(adminUser, query, cat, prio, status, overdue);

        complaintsModel.setRowCount(0);
        for (Complaint c : currentComplaints) {
            complaintsModel.addRow(new Object[]{
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority(),
                    c.getStatus(),
                    c.getReporterName(),
                    c.getAssignedTechnicianName().isBlank() ? "Unassigned" : c.getAssignedTechnicianName(),
                    FORMATTER.format(c.getCreatedAt())
            });
        }
    }

    public void refreshTechnicians() {
        techModel.setRowCount(0);
        ReportService.ComplaintSummaryReport report = reportService.generateSummary();
        Map<String, Long> workloads = report.technicianWorkload();

        for (TechnicianUser t : userRepository.findAllTechnicians()) {
            long active = 0;
            for (Map.Entry<String, Long> entry : workloads.entrySet()) {
                if (entry.getKey().startsWith(t.getFullName())) {
                    active = entry.getValue();
                    break;
                }
            }
            techModel.addRow(new Object[]{
                    t.getTechnicianId(),
                    t.getFullName(),
                    t.getUsername(),
                    t.getSpecialization().getDisplayName(),
                    t.isActive() ? "Active" : "Inactive",
                    active
            });
        }
    }

    public void refreshReports() {
        ReportService.ComplaintSummaryReport report = reportService.generateSummary();

        populateBreakdown(categoryBreakdownPanel, report.categoryBreakdown(), null);
        populateBreakdown(priorityBreakdownPanel, report.priorityBreakdown(), null);
        populateBreakdown(statusBreakdownPanel, report.statusBreakdown(), null);

        // Tech workload
        techWorkloadPanel.removeAll();
        for (Map.Entry<String, Long> entry : report.technicianWorkload().entrySet()) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            JLabel nameLbl = new JLabel(entry.getKey());
            nameLbl.setFont(UITheme.FONT_REGULAR);
            nameLbl.setForeground(UITheme.TEXT_PRIMARY);

            JLabel countLbl = new JLabel(String.valueOf(entry.getValue()));
            countLbl.setFont(UITheme.FONT_BOLD);
            countLbl.setForeground(UITheme.PRIMARY);

            row.add(nameLbl, BorderLayout.WEST);
            row.add(countLbl, BorderLayout.EAST);
            techWorkloadPanel.add(row);
        }
        techWorkloadPanel.revalidate();
        techWorkloadPanel.repaint();
    }

    private <T extends Enum<T>> void populateBreakdown(JPanel panel, Map<T, Long> counts, String prefix) {
        panel.removeAll();
        for (Map.Entry<T, Long> entry : counts.entrySet()) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            JLabel keyLbl = new JLabel(entry.getKey().toString());
            keyLbl.setFont(UITheme.FONT_REGULAR);
            keyLbl.setForeground(UITheme.TEXT_PRIMARY);

            JLabel valLbl = new JLabel(String.valueOf(entry.getValue()));
            valLbl.setFont(UITheme.FONT_BOLD);
            valLbl.setForeground(UITheme.PRIMARY);

            row.add(keyLbl, BorderLayout.WEST);
            row.add(valLbl, BorderLayout.EAST);
            panel.add(row);
        }
        panel.revalidate();
        panel.repaint();
    }

    private void handleViewDetails() {
        int row = complaintsTable.getSelectedRow();
        if (row < 0 || row >= currentComplaints.size()) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to view.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = currentComplaints.get(row);
        ComplaintDetailDialog dlg = new ComplaintDetailDialog(parentFrame, selected, priorityService);
        dlg.setVisible(true);
    }

    private void handleAssignTechnician() {
        int row = complaintsTable.getSelectedRow();
        if (row < 0 || row >= currentComplaints.size()) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to assign.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = currentComplaints.get(row);
        if (selected.getStatus() == ComplaintStatus.RESOLVED) {
            JOptionPane.showMessageDialog(this, "Resolved complaints cannot be reassigned.",
                    "Action Prohibited", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AssignTechnicianDialog dlg = new AssignTechnicianDialog(parentFrame, complaintService,
                userRepository, reportService, selected, adminUser);
        dlg.setVisible(true);
        if (dlg.isAssigned()) {
            refreshComplaints();
        }
    }

    private void openAddTechnicianDialog() {
        JDialog dlg = new JDialog(parentFrame, "Register New Technician", true);
        dlg.setSize(480, 480);
        dlg.setLocationRelativeTo(parentFrame);
        dlg.getContentPane().setBackground(UITheme.BG_CANVAS);
        dlg.setLayout(new BorderLayout());

        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField techIdField = new JTextField("TECH-" + (System.currentTimeMillis() % 1000));
        JComboBox<Category> specCombo = new JComboBox<>(Category.values());

        UITheme.styleTextField(userField);
        UITheme.styleTextField(passField);
        UITheme.styleTextField(nameField);
        UITheme.styleTextField(emailField);
        UITheme.styleTextField(phoneField);
        UITheme.styleTextField(techIdField);
        UITheme.styleComboBox(specCombo);

        int row = 0;
        addTechRow(formCard, gbc, row++, "Username *", userField);
        addTechRow(formCard, gbc, row++, "Password (min 6 chars) *", passField);
        addTechRow(formCard, gbc, row++, "Full Name *", nameField);
        addTechRow(formCard, gbc, row++, "Email Address *", emailField);
        addTechRow(formCard, gbc, row++, "Phone", phoneField);
        addTechRow(formCard, gbc, row++, "Technician ID", techIdField);
        addTechRow(formCard, gbc, row++, "Specialization Category *", specCombo);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        centerWrapper.add(formCard, BorderLayout.CENTER);
        dlg.add(centerWrapper, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.Style.SECONDARY);
        cancelBtn.addActionListener(e -> dlg.dispose());

        ModernButton saveBtn = new ModernButton("Register Technician", ModernButton.Style.PRIMARY);
        saveBtn.addActionListener(e -> {
            try {
                authService.createTechnician(
                        adminUser,
                        userField.getText().trim(),
                        passField.getPassword(),
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        techIdField.getText().trim(),
                        (Category) specCombo.getSelectedItem()
                );
                JOptionPane.showMessageDialog(dlg, "Technician registered successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
                refreshTechnicians();
            } catch (CampusFixException ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        footer.add(cancelBtn);
        footer.add(saveBtn);
        dlg.add(footer, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void addTechRow(JPanel panel, GridBagConstraints gbc, int row, String lbl, Object comp) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.35;
        JLabel l = new JLabel(lbl);
        l.setFont(UITheme.FONT_BOLD);
        panel.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        if (comp instanceof java.awt.Component c) {
            panel.add(c, gbc);
        }
    }

    private void handleExportCsv() {
        Path defaultPath = Path.of("reports/complaints_summary.csv");
        try {
            reportService.exportComplaintsToCsv(defaultPath);
            JOptionPane.showMessageDialog(this,
                    "Complaints CSV exported successfully to:\n" + defaultPath.toAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (CampusFixException ex) {
            JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
