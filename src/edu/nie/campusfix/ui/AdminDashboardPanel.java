package edu.nie.campusfix.ui;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.UserRepository;
import edu.nie.campusfix.service.AuthenticationService;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.service.DuplicateDetectionService;
import edu.nie.campusfix.service.PriorityService;
import edu.nie.campusfix.service.ReportService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.component.ModernTable;
import edu.nie.campusfix.ui.component.StatCard;
import edu.nie.campusfix.ui.dialog.AssignTechnicianDialog;
import edu.nie.campusfix.ui.dialog.ComplaintDetailDialog;
import edu.nie.campusfix.ui.dialog.NewComplaintDialog;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Administrator workspace hosting all 6 dedicated administrative modules:
 * 1. Dashboard Overview (KPIs + Category/Priority charts + Attention summary)
 * 2. All Complaints (full multi-criteria search, technician filter, sorting, assignment)
 * 3. Potential Duplicates Review (7-day unresolved location+category clusters)
 * 4. Overdue Queue (SLA tracking, priority filter, submission date sorting, elapsed duration)
 * 5. Technician Roster (account creation, ID-accurate workloads, active/inactive toggle)
 * 6. Reports & Analytics (all 6 mandatory reports + JFileChooser CSV export)
 */
@SuppressWarnings({"serial", "this-escape"})
public class AdminDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public static final String VIEW_DASHBOARD = "DASHBOARD";
    public static final String VIEW_ALL_COMPLAINTS = "ALL_COMPLAINTS";
    public static final String VIEW_DUPLICATES = "DUPLICATES";
    public static final String VIEW_OVERDUE = "OVERDUE";
    public static final String VIEW_TECHNICIANS = "TECHNICIANS";
    public static final String VIEW_REPORTS = "REPORTS";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final JFrame parentFrame;
    private final User adminUser;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final AuthenticationService authService;
    private Consumer<String> navigationHandler;

    private final CardLayout viewLayout = new CardLayout();
    private final JPanel viewHost = new JPanel(viewLayout);

    // Dashboard Overview KPI Cards
    private final StatCard ovTotalCard = new StatCard("Total", 0, UITheme.PRIMARY);
    private final StatCard ovPendingCard = new StatCard("Pending", 0, UITheme.WARNING);
    private final StatCard ovAssignedCard = new StatCard("Assigned", 0, new Color(0x63, 0x66, 0xF1));
    private final StatCard ovInProgressCard = new StatCard("In Progress", 0, UITheme.INFO);
    private final StatCard ovResolvedCard = new StatCard("Resolved", 0, UITheme.SUCCESS);
    private final StatCard ovOverdueCard = new StatCard("Overdue", 0, UITheme.DANGER);
    private final JPanel ovCategoryPanel = new JPanel();
    private final JPanel ovPriorityPanel = new JPanel();
    private final JLabel ovAttentionLabel = new JLabel();

    // All Complaints Filter Controls & Table
    private final JTextField searchField = new JTextField(16);
    private final JComboBox<Object> categoryFilter = new JComboBox<>();
    private final JComboBox<Object> priorityFilter = new JComboBox<>();
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final JComboBox<TechFilterOption> techFilter = new JComboBox<>();
    private final JCheckBox overdueCheck = new JCheckBox("Overdue Only");
    private final JLabel allCountLabel = new JLabel("0 complaints");
    private boolean updatingTechFilter = false;

    private final DefaultTableModel complaintsModel;
    private final ModernTable complaintsTable;
    private final CardLayout allTableCardLayout = new CardLayout();
    private final JPanel allTableHolder = new JPanel(allTableCardLayout);
    private List<Complaint> currentComplaints = new ArrayList<>();

    // Duplicates View
    private final DefaultTableModel duplicatesModel;
    private final ModernTable duplicatesTable;
    private final CardLayout dupCardLayout = new CardLayout();
    private final JPanel dupHolder = new JPanel(dupCardLayout);
    private final JLabel dupSummaryLabel = new JLabel("0 duplicate clusters detected");
    private List<Complaint> currentDuplicateRows = new ArrayList<>();

    // Overdue Queue View
    private final JComboBox<Object> overduePriorityFilter = new JComboBox<>();
    private final JLabel overdueCountLabel = new JLabel("0 overdue complaints");
    private final DefaultTableModel overdueModel;
    private final ModernTable overdueTable;
    private final CardLayout overdueCardLayout = new CardLayout();
    private final JPanel overdueHolder = new JPanel(overdueCardLayout);
    private List<Complaint> currentOverdueList = new ArrayList<>();

    // Technicians Roster View
    private final DefaultTableModel techModel;
    private final ModernTable techTable;
    private final CardLayout techCardLayout = new CardLayout();
    private final JPanel techHolder = new JPanel(techCardLayout);
    private final ModernButton toggleActiveBtn = new ModernButton("Toggle Active Status", ModernButton.Style.SECONDARY);
    private List<ReportService.TechnicianWorkloadEntry> currentTechEntries = new ArrayList<>();

    // Reports View (all 6 required reports)
    private final StatCard repTotalCard = new StatCard("Total", 0, UITheme.PRIMARY);
    private final StatCard repPendingCard = new StatCard("Pending", 0, UITheme.WARNING);
    private final StatCard repAssignedCard = new StatCard("Assigned", 0, new Color(0x63, 0x66, 0xF1));
    private final StatCard repInProgressCard = new StatCard("In Progress", 0, UITheme.INFO);
    private final StatCard repResolvedCard = new StatCard("Resolved", 0, UITheme.SUCCESS);
    private final StatCard repOverdueCard = new StatCard("Overdue", 0, UITheme.DANGER);
    private final JPanel repCategoryPanel = new JPanel();
    private final JPanel repPriorityPanel = new JPanel();
    private final JPanel repStatusPanel = new JPanel();
    private final JPanel repWorkloadPanel = new JPanel();
    private final DefaultTableModel repOverdueModel;
    private final ModernTable repOverdueTable;
    private final CardLayout repOverdueCardLayout = new CardLayout();
    private final JPanel repOverdueHolder = new JPanel(repOverdueCardLayout);

    private record TechFilterOption(String id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

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
        setBackground(UITheme.BG_CANVAS);

        // 1. All Complaints Table Model (exact columns specified in idea.md Section 10)
        String[] compCols = {"Complaint ID", "Title", "Category", "Location", "Priority", "Status", "Assigned Technician", "Submitted Date"};
        complaintsModel = new DefaultTableModel(compCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 4) return Priority.class;
                if (col == 5) return ComplaintStatus.class;
                return String.class;
            }
        };
        complaintsTable = new ModernTable(complaintsModel);
        complaintsTable.enableDefaultSorting();
        complaintsTable.getColumnModel().getColumn(0).setPreferredWidth(95);
        complaintsTable.getColumnModel().getColumn(1).setPreferredWidth(185);
        complaintsTable.getColumnModel().getColumn(2).setPreferredWidth(125);
        complaintsTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        complaintsTable.getColumnModel().getColumn(4).setPreferredWidth(95);
        complaintsTable.getColumnModel().getColumn(5).setPreferredWidth(105);
        complaintsTable.getColumnModel().getColumn(6).setPreferredWidth(125);
        complaintsTable.getColumnModel().getColumn(7).setPreferredWidth(145);
        complaintsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailsFor(complaintsTable, currentComplaints);
                }
            }
        });

        // 2. Duplicates Table Model
        String[] dupCols = {"Cluster", "Category", "Location", "Complaint ID", "Title", "Priority", "Status", "Submitted"};
        duplicatesModel = new DefaultTableModel(dupCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 5) return Priority.class;
                if (col == 6) return ComplaintStatus.class;
                return String.class;
            }
        };
        duplicatesTable = new ModernTable(duplicatesModel);
        duplicatesTable.enableDefaultSorting();
        duplicatesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailsFor(duplicatesTable, currentDuplicateRows);
                }
            }
        });

        // 3. Overdue Queue Table Model
        String[] overdueCols = {"Complaint ID", "Title", "Category", "Location", "Priority", "Status", "Technician", "Submitted", "Elapsed", "SLA Status"};
        overdueModel = new DefaultTableModel(overdueCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 4) return Priority.class;
                if (col == 5) return ComplaintStatus.class;
                return String.class;
            }
        };
        overdueTable = new ModernTable(overdueModel);
        overdueTable.enableDefaultSorting();
        overdueTable.getColumnModel().getColumn(9).setCellRenderer(new ModernTable.SemanticTagRenderer());
        overdueTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailsFor(overdueTable, currentOverdueList);
                }
            }
        });

        // 4. Technician Roster Table Model
        String[] techCols = {"Tech ID", "Full Name", "Username", "Email", "Specialization", "Status", "Active Tasks", "Resolved"};
        techModel = new DefaultTableModel(techCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 6 || col == 7) return Long.class;
                return String.class;
            }
        };
        techTable = new ModernTable(techModel);
        techTable.enableDefaultSorting();
        techTable.getColumnModel().getColumn(5).setCellRenderer(new ModernTable.SemanticTagRenderer());

        // 5. Embedded Overdue Report Table Model
        String[] repOverdueCols = {"Complaint ID", "Title", "Priority", "Status", "Technician", "Submitted", "Elapsed", "SLA Status"};
        repOverdueModel = new DefaultTableModel(repOverdueCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 2) return Priority.class;
                if (col == 3) return ComplaintStatus.class;
                return String.class;
            }
        };
        repOverdueTable = new ModernTable(repOverdueModel);
        repOverdueTable.enableDefaultSorting();
        repOverdueTable.getColumnModel().getColumn(7).setCellRenderer(new ModernTable.SemanticTagRenderer());

        // Assemble views
        viewHost.setOpaque(false);
        viewHost.add(createDashboardOverviewView(), VIEW_DASHBOARD);
        viewHost.add(createAllComplaintsView(), VIEW_ALL_COMPLAINTS);
        viewHost.add(createDuplicatesView(), VIEW_DUPLICATES);
        viewHost.add(createOverdueView(), VIEW_OVERDUE);
        viewHost.add(createTechnicianView(), VIEW_TECHNICIANS);
        viewHost.add(createReportsView(), VIEW_REPORTS);
        add(viewHost, BorderLayout.CENTER);

        showView(VIEW_DASHBOARD);
    }

    public void setNavigationHandler(Consumer<String> handler) {
        this.navigationHandler = handler;
    }

    public void showView(String viewName) {
        viewLayout.show(viewHost, viewName);
        switch (viewName) {
            case VIEW_DASHBOARD -> refreshDashboardOverview();
            case VIEW_ALL_COMPLAINTS -> {
                reloadTechnicianFilterOptions();
                refreshComplaints();
            }
            case VIEW_DUPLICATES -> refreshDuplicates();
            case VIEW_OVERDUE -> refreshOverdue();
            case VIEW_TECHNICIANS -> refreshTechnicians();
            case VIEW_REPORTS -> refreshReports();
            default -> refreshDashboardOverview();
        }
    }

    private void navigateTo(String viewName) {
        if (navigationHandler != null) {
            navigationHandler.accept(viewName);
        } else {
            showView(viewName);
        }
    }

    // =========================================================================
    // VIEW 1: ADMINISTRATOR DASHBOARD OVERVIEW
    // =========================================================================
    private JPanel createDashboardOverviewView() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Header + Primary Action
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UITheme.createPageHeader(
                "Administrator Dashboard",
                "Real-time campus maintenance overview, priority distribution, and operational alerts."
        ), BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        topActions.setOpaque(false);
        ModernButton logBtn = new ModernButton("+ Log Complaint", ModernButton.Style.PRIMARY);
        logBtn.addActionListener(e -> handleLogComplaint());
        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> refreshDashboardOverview());
        topActions.add(logBtn);
        topActions.add(refreshBtn);
        headerRow.add(topActions, BorderLayout.EAST);

        // 6 KPI Cards
        JPanel statsRow = new JPanel(new GridLayout(1, 6, 10, 0));
        statsRow.setOpaque(false);
        statsRow.add(ovTotalCard);
        statsRow.add(ovPendingCard);
        statsRow.add(ovAssignedCard);
        statsRow.add(ovInProgressCard);
        statsRow.add(ovResolvedCard);
        statsRow.add(ovOverdueCard);

        JPanel northWrap = new JPanel(new BorderLayout(0, 14));
        northWrap.setOpaque(false);
        northWrap.add(headerRow, BorderLayout.NORTH);
        northWrap.add(statsRow, BorderLayout.CENTER);
        panel.add(northWrap, BorderLayout.NORTH);

        // Center: Category & Priority Breakdowns
        JPanel chartsGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsGrid.setOpaque(false);
        chartsGrid.add(wrapBreakdownCard("COMPLAINTS BY CATEGORY", ovCategoryPanel));
        chartsGrid.add(wrapBreakdownCard("COMPLAINTS BY PRIORITY (SLA TARGETS)", ovPriorityPanel));
        panel.add(chartsGrid, BorderLayout.CENTER);

        // Bottom: Quick Action Attention Bar
        JPanel attentionCard = UITheme.createCardPanel(12, 18);
        attentionCard.setLayout(new BorderLayout(0, 10));
        ovAttentionLabel.setFont(UITheme.FONT_REGULAR);
        ovAttentionLabel.setForeground(UITheme.TEXT_SECONDARY);
        attentionCard.add(ovAttentionLabel, BorderLayout.NORTH);

        JPanel quickBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        quickBtns.setOpaque(false);
        ModernButton allBtn = new ModernButton("Manage Complaints", ModernButton.Style.PRIMARY);
        allBtn.addActionListener(e -> navigateTo(VIEW_ALL_COMPLAINTS));
        ModernButton overdueBtn = new ModernButton("Overdue Queue", ModernButton.Style.DANGER);
        overdueBtn.addActionListener(e -> navigateTo(VIEW_OVERDUE));
        ModernButton dupBtn = new ModernButton("Duplicate Review", ModernButton.Style.SECONDARY);
        dupBtn.addActionListener(e -> navigateTo(VIEW_DUPLICATES));
        ModernButton repBtn = new ModernButton("Reports & CSV", ModernButton.Style.SUCCESS);
        repBtn.addActionListener(e -> navigateTo(VIEW_REPORTS));
        quickBtns.add(allBtn);
        quickBtns.add(overdueBtn);
        quickBtns.add(dupBtn);
        quickBtns.add(repBtn);
        attentionCard.add(quickBtns, BorderLayout.SOUTH);

        panel.add(attentionCard, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // VIEW 2: ALL COMPLAINTS MANAGEMENT
    // =========================================================================
    private JPanel createAllComplaintsView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        panel.add(UITheme.createPageHeader(
                "All Campus Complaints",
                "Search, filter by category/priority/status/technician/overdue, sort columns, and dispatch technicians."
        ), BorderLayout.NORTH);

        JPanel card = UITheme.createCardPanel(14, 16);
        card.setLayout(new BorderLayout(0, 10));

        // Clean 3-row responsive toolbar (zero clipping on 1024px+ laptop screens)
        JPanel toolbar = new JPanel();
        toolbar.setOpaque(false);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));

        // Row 1: Search + Category + Priority + Status
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row1.setOpaque(false);
        row1.setAlignmentX(LEFT_ALIGNMENT);

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UITheme.FONT_SMALL_BOLD);
        UITheme.styleTextField(searchField);
        searchField.setPreferredSize(new Dimension(200, 36));
        searchField.setToolTipText("Search by Complaint ID, title, location, reporter, or technician");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshComplaints(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshComplaints(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshComplaints(); }
        });

        categoryFilter.addItem("All Categories");
        for (Category c : Category.values()) categoryFilter.addItem(c);
        UITheme.styleComboBox(categoryFilter);
        categoryFilter.setPreferredSize(new Dimension(UITheme.s(182), UITheme.s(38)));
        categoryFilter.addActionListener(e -> refreshComplaints());

        priorityFilter.addItem("All Priorities");
        for (Priority p : Priority.values()) priorityFilter.addItem(p);
        UITheme.styleComboBox(priorityFilter);
        priorityFilter.setPreferredSize(new Dimension(UITheme.s(158), UITheme.s(38)));
        priorityFilter.addActionListener(e -> refreshComplaints());

        statusFilter.addItem("All Statuses");
        for (ComplaintStatus s : ComplaintStatus.values()) statusFilter.addItem(s);
        UITheme.styleComboBox(statusFilter);
        statusFilter.setPreferredSize(new Dimension(UITheme.s(160), UITheme.s(38)));
        statusFilter.addActionListener(e -> refreshComplaints());

        row1.add(searchLbl);
        row1.add(searchField);
        row1.add(categoryFilter);
        row1.add(priorityFilter);
        row1.add(statusFilter);

        // Row 2: Assigned Technician filter + Overdue checkbox + Reset
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row2.setOpaque(false);
        row2.setAlignmentX(LEFT_ALIGNMENT);

        JLabel techLbl = new JLabel("Technician:");
        techLbl.setFont(UITheme.FONT_SMALL_BOLD);
        UITheme.styleComboBox(techFilter);
        techFilter.setPreferredSize(new Dimension(210, 36));
        techFilter.addActionListener(e -> {
            if (!updatingTechFilter) {
                refreshComplaints();
            }
        });

        UITheme.styleCheckBox(overdueCheck, UITheme.DANGER);
        overdueCheck.addActionListener(e -> refreshComplaints());

        ModernButton resetBtn = new ModernButton("Reset Filters", ModernButton.Style.SECONDARY);
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilter.setSelectedIndex(0);
            priorityFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            if (techFilter.getItemCount() > 0) techFilter.setSelectedIndex(0);
            overdueCheck.setSelected(false);
            refreshComplaints();
        });

        row2.add(techLbl);
        row2.add(techFilter);
        row2.add(Box.createHorizontalStrut(6));
        row2.add(overdueCheck);
        row2.add(Box.createHorizontalStrut(6));
        row2.add(resetBtn);

        // Row 3: Count + Action Buttons
        JPanel row3 = new JPanel(new BorderLayout());
        row3.setOpaque(false);
        row3.setAlignmentX(LEFT_ALIGNMENT);
        row3.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));

        allCountLabel.setFont(UITheme.FONT_SMALL);
        allCountLabel.setForeground(UITheme.TEXT_MUTED);
        row3.add(allCountLabel, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);

        ModernButton logComplaintBtn = new ModernButton("+ Log Complaint", ModernButton.Style.PRIMARY);
        logComplaintBtn.addActionListener(e -> handleLogComplaint());

        ModernButton viewBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
        viewBtn.addActionListener(e -> openDetailsFor(complaintsTable, currentComplaints));

        ModernButton assignBtn = new ModernButton("Assign / Reassign", ModernButton.Style.SECONDARY);
        assignBtn.addActionListener(e -> handleAssignTechnician(complaintsTable, currentComplaints));

        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> {
            reloadTechnicianFilterOptions();
            refreshComplaints();
        });

        actionBtns.add(logComplaintBtn);
        actionBtns.add(viewBtn);
        actionBtns.add(assignBtn);
        actionBtns.add(refreshBtn);
        row3.add(actionBtns, BorderLayout.EAST);

        toolbar.add(row1);
        toolbar.add(Box.createVerticalStrut(4));
        toolbar.add(row2);
        toolbar.add(Box.createVerticalStrut(4));
        toolbar.add(row3);
        card.add(toolbar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(complaintsTable);
        UITheme.styleScrollPane(scroll);

        allTableHolder.setOpaque(false);
        allTableHolder.add(scroll, "TABLE");
        allTableHolder.add(UITheme.createEmptyStatePanel(
                "No Matching Complaints",
                "There are no complaints matching the current search and filter criteria."
        ), "EMPTY");
        card.add(allTableHolder, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // VIEW 3: POTENTIAL DUPLICATES REVIEW
    // =========================================================================
    private JPanel createDuplicatesView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        panel.add(UITheme.createPageHeader(
                "Potential Duplicate Complaints",
                "Unresolved complaints matching the same category and normalized location within a 7-day rolling window."
        ), BorderLayout.NORTH);

        JPanel card = UITheme.createCardPanel(14, 16);
        card.setLayout(new BorderLayout(0, 10));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        dupSummaryLabel.setFont(UITheme.FONT_SMALL_BOLD);
        dupSummaryLabel.setForeground(UITheme.TEXT_SECONDARY);
        bar.add(dupSummaryLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        ModernButton viewBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
        viewBtn.addActionListener(e -> openDetailsFor(duplicatesTable, currentDuplicateRows));
        ModernButton assignBtn = new ModernButton("Assign / Reassign", ModernButton.Style.PRIMARY);
        assignBtn.addActionListener(e -> handleAssignTechnician(duplicatesTable, currentDuplicateRows));
        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> refreshDuplicates());
        actions.add(viewBtn);
        actions.add(assignBtn);
        actions.add(refreshBtn);
        bar.add(actions, BorderLayout.EAST);
        card.add(bar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(duplicatesTable);
        UITheme.styleScrollPane(scroll);
        dupHolder.setOpaque(false);
        dupHolder.add(scroll, "TABLE");
        dupHolder.add(UITheme.createEmptyStatePanel(
                "No Potential Duplicates Detected",
                "All unresolved complaints from the past 7 days represent distinct campus locations and categories."
        ), "EMPTY");
        card.add(dupHolder, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // VIEW 4: OVERDUE COMPLAINT QUEUE
    // =========================================================================
    private JPanel createOverdueView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        panel.add(UITheme.createPageHeader(
                "Overdue Complaints Queue",
                "Unresolved complaints exceeding project-defined response targets (Critical 24h, High 48h, Medium 72h, Low 7d)."
        ), BorderLayout.NORTH);

        JPanel card = UITheme.createCardPanel(14, 16);
        card.setLayout(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel pLbl = new JLabel("Filter Priority:");
        pLbl.setFont(UITheme.FONT_SMALL_BOLD);
        overduePriorityFilter.addItem("All Priorities");
        for (Priority p : Priority.values()) overduePriorityFilter.addItem(p);
        UITheme.styleComboBox(overduePriorityFilter);
        overduePriorityFilter.setPreferredSize(new Dimension(150, 36));
        overduePriorityFilter.addActionListener(e -> refreshOverdue());

        overdueCountLabel.setFont(UITheme.FONT_SMALL_BOLD);
        overdueCountLabel.setForeground(UITheme.DANGER);

        left.add(pLbl);
        left.add(overduePriorityFilter);
        left.add(Box.createHorizontalStrut(10));
        left.add(overdueCountLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        ModernButton viewBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
        viewBtn.addActionListener(e -> openDetailsFor(overdueTable, currentOverdueList));
        ModernButton assignBtn = new ModernButton("Assign / Reassign", ModernButton.Style.PRIMARY);
        assignBtn.addActionListener(e -> handleAssignTechnician(overdueTable, currentOverdueList));
        ModernButton exportBtn = new ModernButton("Export Overdue CSV", ModernButton.Style.SUCCESS);
        exportBtn.addActionListener(e -> handleExportCsv(ExportType.OVERDUE));
        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> refreshOverdue());

        right.add(viewBtn);
        right.add(assignBtn);
        right.add(exportBtn);
        right.add(refreshBtn);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(overdueTable);
        UITheme.styleScrollPane(scroll);
        overdueHolder.setOpaque(false);
        overdueHolder.add(scroll, "TABLE");
        overdueHolder.add(UITheme.createEmptyStatePanel(
                "No Overdue Complaints",
                "All active maintenance complaints are currently within their response target windows."
        ), "EMPTY");
        card.add(overdueHolder, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // VIEW 5: TECHNICIAN ROSTER & MANAGEMENT
    // =========================================================================
    private JPanel createTechnicianView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(UITheme.createPageHeader(
                "Technician Management",
                "Create maintenance technician accounts, monitor active workloads, and manage availability."
        ), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);
        toggleActiveBtn.addActionListener(e -> handleToggleTechnicianActive());
        ModernButton addTechBtn = new ModernButton("+ Register Technician", ModernButton.Style.PRIMARY);
        addTechBtn.addActionListener(e -> openAddTechnicianDialog());
        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> refreshTechnicians());

        actions.add(toggleActiveBtn);
        actions.add(addTechBtn);
        actions.add(refreshBtn);
        topBar.add(actions, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        JPanel card = UITheme.createCardPanel(14, 16);
        card.setLayout(new BorderLayout());

        JScrollPane scroll = new JScrollPane(techTable);
        UITheme.styleScrollPane(scroll);
        techHolder.setOpaque(false);
        techHolder.add(scroll, "TABLE");
        techHolder.add(UITheme.createEmptyStatePanel(
                "No Technicians Registered Yet",
                "Click '+ Register Technician' above to onboard maintenance personnel and begin dispatching complaints."
        ), "EMPTY");
        card.add(techHolder, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // VIEW 6: COMPLAINT REPORTS & CSV EXPORT (ALL 6 REQUIRED REPORTS)
    // =========================================================================
    private JPanel createReportsView() {
        JPanel outer = new JPanel(new BorderLayout(0, 14));
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(UITheme.createPageHeader(
                "Complaint Reports & Analytics",
                "Live metrics from stored records with formula-safe UTF-8 CSV export."
        ), BorderLayout.WEST);

        JPanel exportBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        exportBtns.setOpaque(false);
        ModernButton expAllBtn = new ModernButton("Export Full CSV", ModernButton.Style.SUCCESS);
        expAllBtn.addActionListener(e -> handleExportCsv(ExportType.ALL_COMPLAINTS));
        ModernButton expSumBtn = new ModernButton("Export Summary CSV", ModernButton.Style.PRIMARY);
        expSumBtn.addActionListener(e -> handleExportCsv(ExportType.SUMMARY));
        ModernButton expOvdBtn = new ModernButton("Export Overdue CSV", ModernButton.Style.SECONDARY);
        expOvdBtn.addActionListener(e -> handleExportCsv(ExportType.OVERDUE));
        exportBtns.add(expAllBtn);
        exportBtns.add(expSumBtn);
        exportBtns.add(expOvdBtn);
        topBar.add(exportBtns, BorderLayout.EAST);
        outer.add(topBar, BorderLayout.NORTH);

        // Scrollable reports body containing all 6 mandatory reports
        JPanel body = new JPanel();
        body.setBackground(UITheme.BG_CANVAS);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // Report 1: Total Complaints Summary
        JPanel r1Row = new JPanel(new GridLayout(1, 6, 10, 0));
        r1Row.setOpaque(false);
        r1Row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        r1Row.add(repTotalCard);
        r1Row.add(repPendingCard);
        r1Row.add(repAssignedCard);
        r1Row.add(repInProgressCard);
        r1Row.add(repResolvedCard);
        r1Row.add(repOverdueCard);
        body.add(r1Row);
        body.add(Box.createVerticalStrut(16));

        // Reports 2, 3, 4, 6 in a 2x2 grid
        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(900, 440));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 460));
        grid.add(wrapBreakdownCard("CATEGORY-WISE COMPLAINT COUNTS", repCategoryPanel));
        grid.add(wrapBreakdownCard("PRIORITY-WISE COMPLAINT COUNTS", repPriorityPanel));
        grid.add(wrapBreakdownCard("STATUS-WISE COMPLAINT COUNTS", repStatusPanel));
        grid.add(wrapBreakdownCard("TECHNICIAN WORKLOAD REPORT (ACTIVE TASKS)", repWorkloadPanel));
        body.add(grid);
        body.add(Box.createVerticalStrut(16));

        // Report 5: Overdue Complaint Report
        JPanel overdueCard = UITheme.createCardPanel(14, 16);
        overdueCard.setLayout(new BorderLayout(0, 10));
        overdueCard.setPreferredSize(new Dimension(900, 240));
        overdueCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        JLabel ovdTitle = new JLabel("OVERDUE COMPLAINT REPORT (PROJECT-DEFINED RESPONSE TARGETS)");
        ovdTitle.setFont(UITheme.FONT_BADGE);
        ovdTitle.setForeground(UITheme.DANGER);
        overdueCard.add(ovdTitle, BorderLayout.NORTH);

        JScrollPane repOvdScroll = new JScrollPane(repOverdueTable);
        UITheme.styleScrollPane(repOvdScroll);
        repOverdueHolder.setOpaque(false);
        repOverdueHolder.add(repOvdScroll, "TABLE");
        repOverdueHolder.add(UITheme.createEmptyStatePanel(
                "Zero Overdue Complaints",
                "No unresolved complaints have exceeded their response target window."
        ), "EMPTY");
        overdueCard.add(repOverdueHolder, BorderLayout.CENTER);
        body.add(overdueCard);

        JScrollPane mainScroll = new JScrollPane(body);
        UITheme.styleScrollPane(mainScroll);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(UITheme.BG_CANVAS);
        outer.add(mainScroll, BorderLayout.CENTER);
        return outer;
    }

    private JPanel wrapBreakdownCard(String title, JPanel contentPanel) {
        JPanel card = UITheme.createCardPanel(14, 16);
        card.setLayout(new BorderLayout(0, 10));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_BADGE);
        titleLbl.setForeground(UITheme.TEXT_MUTED);
        card.add(titleLbl, BorderLayout.NORTH);

        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(contentPanel);
        UITheme.styleScrollPane(scroll);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // DATA REFRESH METHODS
    // =========================================================================
    public void refreshDashboardOverview() {
        ReportService.ComplaintSummaryReport report = reportService.generateSummary();
        ovTotalCard.setValue(report.totalComplaints());
        ovPendingCard.setValue(report.pendingCount());
        ovAssignedCard.setValue(report.assignedCount());
        ovInProgressCard.setValue(report.inProgressCount());
        ovResolvedCard.setValue(report.resolvedCount());
        ovOverdueCard.setValue(report.overdueCount());

        populateCategoryBars(ovCategoryPanel, report.categoryBreakdown());
        populatePriorityBars(ovPriorityPanel, report.priorityBreakdown());

        int dupGroups = complaintService.getPotentialDuplicateGroups(adminUser).size();
        ovAttentionLabel.setText("<html><b>Operational Summary:</b> "
                + report.pendingCount() + " pending unassigned &nbsp;•&nbsp; "
                + report.overdueCount() + " overdue &nbsp;•&nbsp; "
                + dupGroups + " duplicate cluster" + (dupGroups == 1 ? "" : "s")
                + " &nbsp;•&nbsp; " + report.technicianWorkloads().size() + " registered technician"
                + (report.technicianWorkloads().size() == 1 ? "" : "s") + "</html>");
    }

    private void reloadTechnicianFilterOptions() {
        updatingTechFilter = true;
        try {
            TechFilterOption previous = (TechFilterOption) techFilter.getSelectedItem();
            techFilter.removeAllItems();
            techFilter.addItem(new TechFilterOption("", "All Technicians"));
            techFilter.addItem(new TechFilterOption(ComplaintService.FILTER_UNASSIGNED, "Unassigned Only"));
            for (TechnicianUser t : userRepository.findAllTechnicians()) {
                techFilter.addItem(new TechFilterOption(t.getId(), t.getFullName() + " (" + t.getTechnicianId() + ")"));
            }
            if (previous != null) {
                for (int i = 0; i < techFilter.getItemCount(); i++) {
                    if (techFilter.getItemAt(i).id().equals(previous.id())) {
                        techFilter.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } finally {
            updatingTechFilter = false;
        }
    }

    public void refreshComplaints() {
        String query = searchField.getText().trim();
        Category cat = categoryFilter.getSelectedItem() instanceof Category c ? c : null;
        Priority prio = priorityFilter.getSelectedItem() instanceof Priority p ? p : null;
        ComplaintStatus status = statusFilter.getSelectedItem() instanceof ComplaintStatus s ? s : null;
        TechFilterOption techOpt = (TechFilterOption) techFilter.getSelectedItem();
        String techId = (techOpt != null && !techOpt.id().isEmpty()) ? techOpt.id() : null;
        Boolean overdue = overdueCheck.isSelected() ? Boolean.TRUE : null;

        currentComplaints = complaintService.searchComplaints(adminUser, query, cat, prio, status, techId, overdue);

        complaintsModel.setRowCount(0);
        for (Complaint c : currentComplaints) {
            complaintsModel.addRow(new Object[]{
                    shortId(c.getId()),
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority(),
                    c.getStatus(),
                    c.getAssignedTechnicianName().isBlank() ? "Unassigned" : c.getAssignedTechnicianName(),
                    FORMATTER.format(c.getCreatedAt())
            });
        }
        allCountLabel.setText("Showing " + currentComplaints.size() + " complaint"
                + (currentComplaints.size() == 1 ? "" : "s")
                + "  •  Double-click row for details");
        allTableCardLayout.show(allTableHolder, currentComplaints.isEmpty() ? "EMPTY" : "TABLE");
    }

    public void refreshDuplicates() {
        List<DuplicateDetectionService.DuplicateGroup> groups = complaintService.getPotentialDuplicateGroups(adminUser);
        duplicatesModel.setRowCount(0);
        currentDuplicateRows = new ArrayList<>();

        int groupNum = 1;
        for (DuplicateDetectionService.DuplicateGroup group : groups) {
            String clusterTag = "Cluster #" + groupNum + " (" + group.complaints().size() + ")";
            for (Complaint c : group.complaints()) {
                currentDuplicateRows.add(c);
                duplicatesModel.addRow(new Object[]{
                        clusterTag,
                        c.getCategory().getDisplayName(),
                        c.getLocation(),
                        shortId(c.getId()),
                        c.getTitle(),
                        c.getPriority(),
                        c.getStatus(),
                        FORMATTER.format(c.getCreatedAt())
                });
            }
            groupNum++;
        }

        dupSummaryLabel.setText(groups.size() + " potential duplicate cluster" + (groups.size() == 1 ? "" : "s")
                + " (" + currentDuplicateRows.size() + " unresolved complaints)");
        dupCardLayout.show(dupHolder, currentDuplicateRows.isEmpty() ? "EMPTY" : "TABLE");
    }

    public void refreshOverdue() {
        Priority prio = overduePriorityFilter.getSelectedItem() instanceof Priority p ? p : null;
        currentOverdueList = complaintService.getOverdueComplaints(adminUser, prio);

        overdueModel.setRowCount(0);
        for (Complaint c : currentOverdueList) {
            overdueModel.addRow(new Object[]{
                    shortId(c.getId()),
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority(),
                    c.getStatus(),
                    c.getAssignedTechnicianName().isBlank() ? "Unassigned" : c.getAssignedTechnicianName(),
                    FORMATTER.format(c.getCreatedAt()),
                    c.getFormattedElapsedTime(),
                    c.getFormattedOverdueStatus()
            });
        }
        overdueCountLabel.setText(currentOverdueList.size() + " overdue complaint"
                + (currentOverdueList.size() == 1 ? "" : "s"));
        overdueCardLayout.show(overdueHolder, currentOverdueList.isEmpty() ? "EMPTY" : "TABLE");
    }

    public void refreshTechnicians() {
        ReportService.ComplaintSummaryReport report = reportService.generateSummary();
        currentTechEntries = report.technicianWorkloads();
        techModel.setRowCount(0);

        for (ReportService.TechnicianWorkloadEntry entry : currentTechEntries) {
            techModel.addRow(new Object[]{
                    entry.technicianCode(),
                    entry.fullName(),
                    entry.username(),
                    userRepository.findById(entry.id()).map(User::getEmail).orElse(""),
                    entry.specialization().getDisplayName(),
                    entry.active() ? "Active" : "Inactive",
                    entry.activeTasks(),
                    entry.resolvedTasks()
            });
        }
        techCardLayout.show(techHolder, currentTechEntries.isEmpty() ? "EMPTY" : "TABLE");
    }

    public void refreshReports() {
        ReportService.ComplaintSummaryReport report = reportService.generateSummary();
        repTotalCard.setValue(report.totalComplaints());
        repPendingCard.setValue(report.pendingCount());
        repAssignedCard.setValue(report.assignedCount());
        repInProgressCard.setValue(report.inProgressCount());
        repResolvedCard.setValue(report.resolvedCount());
        repOverdueCard.setValue(report.overdueCount());

        populateCategoryBars(repCategoryPanel, report.categoryBreakdown());
        populatePriorityBars(repPriorityPanel, report.priorityBreakdown());
        populateStatusBars(repStatusPanel, report.statusBreakdown());

        // Technician workload bars
        repWorkloadPanel.removeAll();
        if (report.technicianWorkloads().isEmpty()) {
            JLabel empty = new JLabel("No technicians registered yet.");
            empty.setFont(UITheme.FONT_SMALL);
            empty.setForeground(UITheme.TEXT_MUTED);
            repWorkloadPanel.add(empty);
        } else {
            long maxTasks = report.technicianWorkloads().stream()
                    .mapToLong(ReportService.TechnicianWorkloadEntry::activeTasks)
                    .max().orElse(1);
            for (ReportService.TechnicianWorkloadEntry entry : report.technicianWorkloads()) {
                String lbl = entry.fullName() + " (" + entry.specialization().getDisplayName()
                        + " • " + entry.resolvedTasks() + " resolved)";
                repWorkloadPanel.add(UITheme.createMetricBarRow(lbl, entry.activeTasks(), Math.max(1, maxTasks), UITheme.PRIMARY));
            }
        }
        repWorkloadPanel.revalidate();
        repWorkloadPanel.repaint();

        // Embedded Overdue Report Table
        repOverdueModel.setRowCount(0);
        for (Complaint c : report.overdueComplaints()) {
            repOverdueModel.addRow(new Object[]{
                    shortId(c.getId()),
                    c.getTitle(),
                    c.getPriority(),
                    c.getStatus(),
                    c.getAssignedTechnicianName().isBlank() ? "Unassigned" : c.getAssignedTechnicianName(),
                    FORMATTER.format(c.getCreatedAt()),
                    c.getFormattedElapsedTime(),
                    c.getFormattedOverdueStatus()
            });
        }
        repOverdueCardLayout.show(repOverdueHolder, report.overdueComplaints().isEmpty() ? "EMPTY" : "TABLE");
    }

    private void populateCategoryBars(JPanel panel, Map<Category, Long> counts) {
        panel.removeAll();
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        for (Map.Entry<Category, Long> entry : counts.entrySet()) {
            Color c = entry.getKey().isHighRisk() ? UITheme.DANGER : UITheme.PRIMARY;
            panel.add(UITheme.createMetricBarRow(entry.getKey().getDisplayName(), entry.getValue(), Math.max(1, max), c));
        }
        panel.revalidate();
        panel.repaint();
    }

    private void populatePriorityBars(JPanel panel, Map<Priority, Long> counts) {
        panel.removeAll();
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        panel.add(UITheme.createMetricBarRow("Critical (24h target)", counts.getOrDefault(Priority.CRITICAL, 0L), Math.max(1, max), UITheme.DANGER));
        panel.add(UITheme.createMetricBarRow("High (48h target)", counts.getOrDefault(Priority.HIGH, 0L), Math.max(1, max), UITheme.WARNING));
        panel.add(UITheme.createMetricBarRow("Medium (72h target)", counts.getOrDefault(Priority.MEDIUM, 0L), Math.max(1, max), UITheme.PRIMARY));
        panel.add(UITheme.createMetricBarRow("Low (7d target)", counts.getOrDefault(Priority.LOW, 0L), Math.max(1, max), UITheme.SUCCESS));
        panel.revalidate();
        panel.repaint();
    }

    private void populateStatusBars(JPanel panel, Map<ComplaintStatus, Long> counts) {
        panel.removeAll();
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        panel.add(UITheme.createMetricBarRow("Pending", counts.getOrDefault(ComplaintStatus.PENDING, 0L), Math.max(1, max), UITheme.WARNING));
        panel.add(UITheme.createMetricBarRow("Assigned", counts.getOrDefault(ComplaintStatus.ASSIGNED, 0L), Math.max(1, max), UITheme.PRIMARY));
        panel.add(UITheme.createMetricBarRow("In Progress", counts.getOrDefault(ComplaintStatus.IN_PROGRESS, 0L), Math.max(1, max), UITheme.INFO));
        panel.add(UITheme.createMetricBarRow("Resolved", counts.getOrDefault(ComplaintStatus.RESOLVED, 0L), Math.max(1, max), UITheme.SUCCESS));
        panel.revalidate();
        panel.repaint();
    }

    // =========================================================================
    // ACTIONS & DIALOGS
    // =========================================================================
    private void handleLogComplaint() {
        NewComplaintDialog dlg = new NewComplaintDialog(parentFrame, complaintService, priorityService, adminUser);
        dlg.setVisible(true);
        if (dlg.isComplaintSubmitted()) {
            refreshDashboardOverview();
            refreshComplaints();
        }
    }

    private void openDetailsFor(ModernTable table, List<Complaint> backingList) {
        int modelRow = table.getSelectedModelRow();
        if (modelRow < 0 || modelRow >= backingList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to view details.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = backingList.get(modelRow);
        ComplaintDetailDialog dlg = new ComplaintDetailDialog(parentFrame, selected, priorityService);
        dlg.setVisible(true);
    }

    private void handleAssignTechnician(ModernTable table, List<Complaint> backingList) {
        int modelRow = table.getSelectedModelRow();
        if (modelRow < 0 || modelRow >= backingList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to assign or reassign.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = backingList.get(modelRow);
        if (selected.getStatus() == ComplaintStatus.RESOLVED) {
            JOptionPane.showMessageDialog(this, "Resolved complaints cannot be assigned or reassigned.",
                    "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AssignTechnicianDialog dlg = new AssignTechnicianDialog(parentFrame, complaintService,
                userRepository, reportService, selected, adminUser);
        dlg.setVisible(true);
        if (dlg.isAssigned()) {
            refreshDashboardOverview();
            refreshComplaints();
            refreshDuplicates();
            refreshOverdue();
        }
    }

    private void handleToggleTechnicianActive() {
        int modelRow = techTable.getSelectedModelRow();
        if (modelRow < 0 || modelRow >= currentTechEntries.size()) {
            JOptionPane.showMessageDialog(this, "Please select a technician from the roster first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ReportService.TechnicianWorkloadEntry entry = currentTechEntries.get(modelRow);
        boolean nextActive = !entry.active();
        String verb = nextActive ? "activate" : "deactivate";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + verb + " technician " + entry.fullName() + " (" + entry.technicianCode() + ")?",
                "Confirm Status Change", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<TechnicianUser, Void>() {
            @Override
            protected TechnicianUser doInBackground() {
                return authService.setTechnicianActive(adminUser, entry.id(), nextActive);
            }

            @Override
            protected void done() {
                try {
                    get();
                    refreshTechnicians();
                    reloadTechnicianFilterOptions();
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(AdminDashboardPanel.this,
                            cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openAddTechnicianDialog() {
        JDialog dlg = new JDialog(parentFrame, "Register New Technician", true);
        dlg.setSize(UITheme.s(600), UITheme.s(600));
        dlg.setMinimumSize(new Dimension(UITheme.s(500), UITheme.s(520)));
        dlg.setLocationRelativeTo(parentFrame);
        dlg.getContentPane().setBackground(UITheme.BG_CANVAS);
        dlg.setLayout(new BorderLayout());

        dlg.getRootPane().registerKeyboardAction(
                e -> dlg.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(14, 22, 14, 22)
        ));
        header.add(UITheme.createPageHeader("Register Maintenance Technician",
                "Only administrators can provision technician credentials."), BorderLayout.WEST);
        dlg.add(header, BorderLayout.NORTH);

        JPanel formCard = UITheme.createCardPanel(16, 20);
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JPasswordField confirmPassField = new JPasswordField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField techIdField = new JTextField("TECH-" + (100 + (System.currentTimeMillis() % 900)));
        JComboBox<Category> specCombo = new JComboBox<>(Category.values());

        UITheme.styleTextField(userField);
        UITheme.styleTextField(passField);
        UITheme.styleTextField(confirmPassField);
        UITheme.styleTextField(nameField);
        UITheme.styleTextField(emailField);
        UITheme.styleTextField(phoneField);
        UITheme.styleTextField(techIdField);
        UITheme.styleComboBox(specCombo);

        int row = 0;
        addTechRow(formCard, gbc, row++, "Username *", userField);
        addTechRow(formCard, gbc, row++, "Password (min 6) *", passField);
        addTechRow(formCard, gbc, row++, "Confirm Password *", confirmPassField);
        addTechRow(formCard, gbc, row++, "Full Name *", nameField);
        addTechRow(formCard, gbc, row++, "Email Address *", emailField);
        addTechRow(formCard, gbc, row++, "Phone", phoneField);
        addTechRow(formCard, gbc, row++, "Technician Code", techIdField);
        addTechRow(formCard, gbc, row++, "Specialization *", specCombo);

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
            char[] pass = passField.getPassword();
            char[] conf = confirmPassField.getPassword();
            if (!Arrays.equals(pass, conf)) {
                Arrays.fill(pass, '\0');
                Arrays.fill(conf, '\0');
                JOptionPane.showMessageDialog(dlg, "Passwords do not match.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Arrays.fill(conf, '\0');

            String u = userField.getText().trim();
            String n = nameField.getText().trim();
            String em = emailField.getText().trim();
            String ph = phoneField.getText().trim();
            String tid = techIdField.getText().trim();
            Category spec = (Category) specCombo.getSelectedItem();

            saveBtn.setEnabled(false);
            saveBtn.setText("Saving...");
            new SwingWorker<TechnicianUser, Void>() {
                @Override
                protected TechnicianUser doInBackground() {
                    return authService.createTechnician(adminUser, u, pass, n, em, ph, tid, spec);
                }

                @Override
                protected void done() {
                    saveBtn.setEnabled(true);
                    saveBtn.setText("Register Technician");
                    try {
                        TechnicianUser tech = get();
                        JOptionPane.showMessageDialog(dlg,
                                "Technician '" + tech.getFullName() + "' registered successfully.",
                                "Technician Created", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                        refreshTechnicians();
                        reloadTechnicianFilterOptions();
                    } catch (InterruptedException | ExecutionException ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(dlg, cause.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        footer.add(cancelBtn);
        footer.add(saveBtn);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void addTechRow(JPanel panel, GridBagConstraints gbc, int row, String lbl, JComponent comp) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.34;
        JLabel l = new JLabel(lbl);
        l.setFont(UITheme.FONT_SMALL_BOLD);
        l.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.66;
        panel.add(comp, gbc);
    }

    private enum ExportType {
        ALL_COMPLAINTS("complaints_full_report.csv"),
        SUMMARY("complaints_summary_report.csv"),
        OVERDUE("complaints_overdue_report.csv");

        final String defaultFileName;
        ExportType(String name) { this.defaultFileName = name; }
    }

    private void handleExportCsv(ExportType type) {
        JFileChooser chooser = new JFileChooser(new File("reports"));
        chooser.setDialogTitle("Save CSV Report");
        chooser.setSelectedFile(new File("reports", type.defaultFileName));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File chosen = chooser.getSelectedFile();
        if (!chosen.getName().toLowerCase().endsWith(".csv")) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + ".csv");
        }
        Path destPath = chosen.toPath();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                switch (type) {
                    case ALL_COMPLAINTS -> reportService.exportComplaintsToCsv(destPath);
                    case SUMMARY -> reportService.exportSummaryReportToCsv(destPath);
                    case OVERDUE -> reportService.exportOverdueReportToCsv(destPath);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(AdminDashboardPanel.this,
                            "CSV report exported successfully to:\n" + destPath.toAbsolutePath(),
                            "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(AdminDashboardPanel.this,
                            "Failed to export CSV: " + cause.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) : id;
    }
}
