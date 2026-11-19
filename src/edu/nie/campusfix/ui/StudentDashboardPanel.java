package edu.nie.campusfix.ui;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.service.PriorityService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.component.ModernTable;
import edu.nie.campusfix.ui.component.StatCard;
import edu.nie.campusfix.ui.dialog.ComplaintDetailDialog;
import edu.nie.campusfix.ui.dialog.NewComplaintDialog;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Student / Staff workspace hosting both:
 * - Student Dashboard (KPI overview + recent submissions + quick actions)
 * - My Complaints (full searchable, filterable, sortable personal complaint tracker)
 */
@SuppressWarnings({"serial", "this-escape"})
public class StudentDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final String VIEW_DASHBOARD = "DASHBOARD";
    public static final String VIEW_MY_COMPLAINTS = "MY_COMPLAINTS";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final JFrame parentFrame;
    private final User currentUser;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;

    private final CardLayout viewLayout = new CardLayout();
    private final JPanel viewHost = new JPanel(viewLayout);

    // KPI Cards (Dashboard view)
    private final StatCard totalCard = new StatCard("Total Submitted", 0, UITheme.PRIMARY);
    private final StatCard openCard = new StatCard("Pending / Assigned", 0, UITheme.WARNING);
    private final StatCard inProgressCard = new StatCard("In Progress", 0, UITheme.INFO);
    private final StatCard resolvedCard = new StatCard("Resolved", 0, UITheme.SUCCESS);

    // Recent Table (Dashboard view)
    private final DefaultTableModel recentModel;
    private final ModernTable recentTable;
    private final CardLayout recentCardLayout = new CardLayout();
    private final JPanel recentTableHolder = new JPanel(recentCardLayout);
    private List<Complaint> recentList = new ArrayList<>();

    // My Complaints Filters & Table
    private final JTextField searchField = new JTextField(16);
    private final JComboBox<Object> categoryFilter = new JComboBox<>();
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final JLabel resultCountLabel = new JLabel("0 complaints");
    private final DefaultTableModel tableModel;
    private final ModernTable complaintsTable;
    private final CardLayout listCardLayout = new CardLayout();
    private final JPanel listTableHolder = new JPanel(listCardLayout);
    private List<Complaint> currentList = new ArrayList<>();

    private Runnable onOpenAllAction;

    public StudentDashboardPanel(JFrame parentFrame, User currentUser,
                                 ComplaintService complaintService, PriorityService priorityService) {
        this.parentFrame = parentFrame;
        this.currentUser = currentUser;
        this.complaintService = complaintService;
        this.priorityService = priorityService;

        setLayout(new BorderLayout());
        setBackground(UITheme.BG_CANVAS);

        // Recent table model
        String[] recentCols = {"ID", "Title", "Category", "Location", "Priority", "Status", "Submitted"};
        recentModel = new DefaultTableModel(recentCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 4) return Priority.class;
                if (col == 5) return ComplaintStatus.class;
                return String.class;
            }
        };
        recentTable = new ModernTable(recentModel);
        recentTable.enableDefaultSorting();
        configureRecentColumns();
        recentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailsFor(recentTable, recentList);
                }
            }
        });

        // Full personal complaints table model
        String[] fullCols = {"ID", "Title", "Category", "Location", "Priority", "Status", "Technician", "Submitted"};
        tableModel = new DefaultTableModel(fullCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 4) return Priority.class;
                if (col == 5) return ComplaintStatus.class;
                return String.class;
            }
        };
        complaintsTable = new ModernTable(tableModel);
        complaintsTable.enableDefaultSorting();
        configureFullColumns();
        complaintsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailsFor(complaintsTable, currentList);
                }
            }
        });

        viewHost.setOpaque(false);
        viewHost.add(createDashboardOverview(), VIEW_DASHBOARD);
        viewHost.add(createMyComplaintsView(), VIEW_MY_COMPLAINTS);
        add(viewHost, BorderLayout.CENTER);

        loadData();
    }

    public void setOnOpenAllAction(Runnable action) {
        this.onOpenAllAction = action;
    }

    public void showView(String viewName) {
        viewLayout.show(viewHost, viewName);
        loadData();
    }

    private JPanel createDashboardOverview() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Greeting + Primary Action
        JPanel headerRow = new JPanel(new BorderLayout(16, 0));
        headerRow.setOpaque(false);
        headerRow.add(UITheme.createPageHeader(
                "Welcome back, " + currentUser.getFullName(),
                "Track your reported campus maintenance issues and submit new requests."
        ), BorderLayout.WEST);

        JPanel ctaBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        ctaBox.setOpaque(false);
        ModernButton newBtn = new ModernButton("+ Submit New Complaint", ModernButton.Style.PRIMARY);
        newBtn.addActionListener(e -> openNewComplaintDialog());
        ctaBox.add(newBtn);
        headerRow.add(ctaBox, BorderLayout.EAST);

        // KPI Row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.add(totalCard);
        statsRow.add(openCard);
        statsRow.add(inProgressCard);
        statsRow.add(resolvedCard);

        JPanel northWrap = new JPanel(new BorderLayout(0, 16));
        northWrap.setOpaque(false);
        northWrap.add(headerRow, BorderLayout.NORTH);
        northWrap.add(statsRow, BorderLayout.CENTER);
        panel.add(northWrap, BorderLayout.NORTH);

        // Recent Activity Card
        JPanel recentCard = UITheme.createCardPanel(16, 18);
        recentCard.setLayout(new BorderLayout(0, 12));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        JLabel recentTitle = new JLabel("Recent Complaints");
        recentTitle.setFont(UITheme.FONT_SUBTITLE);
        recentTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel cardActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cardActions.setOpaque(false);
        ModernButton viewDetailBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
        viewDetailBtn.addActionListener(e -> openDetailsFor(recentTable, recentList));

        ModernButton viewAllBtn = new ModernButton("View All My Complaints", ModernButton.Style.GHOST);
        viewAllBtn.addActionListener(e -> {
            if (onOpenAllAction != null) {
                onOpenAllAction.run();
            } else {
                showView(VIEW_MY_COMPLAINTS);
            }
        });
        cardActions.add(viewDetailBtn);
        cardActions.add(viewAllBtn);

        cardHeader.add(recentTitle, BorderLayout.WEST);
        cardHeader.add(cardActions, BorderLayout.EAST);
        recentCard.add(cardHeader, BorderLayout.NORTH);

        JScrollPane recentScroll = new JScrollPane(recentTable);
        UITheme.styleScrollPane(recentScroll);

        recentTableHolder.setOpaque(false);
        recentTableHolder.add(recentScroll, "TABLE");
        recentTableHolder.add(UITheme.createEmptyStatePanel(
                "No Complaints Submitted Yet",
                "Click '+ Submit New Complaint' above to report an electrical, plumbing, classroom, lab, hostel, or safety issue on campus."
        ), "EMPTY");
        recentCard.add(recentTableHolder, BorderLayout.CENTER);

        panel.add(recentCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMyComplaintsView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UITheme.createPageHeader(
                "My Complaints",
                "Search, filter, and inspect the complete status history of your submissions."
        ), BorderLayout.WEST);

        ModernButton newBtn = new ModernButton("+ Submit New Complaint", ModernButton.Style.PRIMARY);
        newBtn.addActionListener(e -> openNewComplaintDialog());
        JPanel topBtnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        topBtnWrap.setOpaque(false);
        topBtnWrap.add(newBtn);
        headerRow.add(topBtnWrap, BorderLayout.EAST);
        panel.add(headerRow, BorderLayout.NORTH);

        // Main Table Card
        JPanel centerCard = UITheme.createCardPanel(14, 16);
        centerCard.setLayout(new BorderLayout(0, 12));

        // 2-row responsive toolbar
        JPanel toolbar = new JPanel(new BorderLayout(0, 8));
        toolbar.setOpaque(false);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filterRow.setOpaque(false);

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UITheme.FONT_SMALL_BOLD);
        UITheme.styleTextField(searchField);
        searchField.setPreferredSize(new Dimension(UITheme.s(210), UITheme.s(38)));
        searchField.setToolTipText("Search by ID, title, location, or description");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshFilteredTable(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshFilteredTable(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshFilteredTable(); }
        });

        categoryFilter.addItem("All Categories");
        for (Category c : Category.values()) categoryFilter.addItem(c);
        UITheme.styleComboBox(categoryFilter);
        categoryFilter.setPreferredSize(new Dimension(UITheme.s(182), UITheme.s(38)));
        categoryFilter.addActionListener(e -> refreshFilteredTable());

        statusFilter.addItem("All Statuses");
        for (ComplaintStatus s : ComplaintStatus.values()) statusFilter.addItem(s);
        UITheme.styleComboBox(statusFilter);
        statusFilter.setPreferredSize(new Dimension(UITheme.s(160), UITheme.s(38)));
        statusFilter.addActionListener(e -> refreshFilteredTable());

        ModernButton resetBtn = new ModernButton("Reset", ModernButton.Style.SECONDARY);
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            refreshFilteredTable();
        });

        filterRow.add(searchLbl);
        filterRow.add(searchField);
        filterRow.add(categoryFilter);
        filterRow.add(statusFilter);
        filterRow.add(resetBtn);

        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);
        resultCountLabel.setFont(UITheme.FONT_SMALL);
        resultCountLabel.setForeground(UITheme.TEXT_MUTED);
        actionRow.add(resultCountLabel, BorderLayout.WEST);

        JPanel actionsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsBox.setOpaque(false);
        ModernButton viewBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
        viewBtn.addActionListener(e -> openDetailsFor(complaintsTable, currentList));

        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> loadData());

        actionsBox.add(viewBtn);
        actionsBox.add(refreshBtn);
        actionRow.add(actionsBox, BorderLayout.EAST);

        toolbar.add(filterRow, BorderLayout.NORTH);
        toolbar.add(actionRow, BorderLayout.SOUTH);
        centerCard.add(toolbar, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(complaintsTable);
        UITheme.styleScrollPane(scrollPane);

        listTableHolder.setOpaque(false);
        listTableHolder.add(scrollPane, "TABLE");
        listTableHolder.add(UITheme.createEmptyStatePanel(
                "No Matching Complaints Found",
                "Try clearing your search or status filter, or submit a new maintenance complaint."
        ), "EMPTY");
        centerCard.add(listTableHolder, BorderLayout.CENTER);

        panel.add(centerCard, BorderLayout.CENTER);
        return panel;
    }

    private void configureRecentColumns() {
        recentTable.getColumnModel().getColumn(0).setPreferredWidth(UITheme.s(85));
        recentTable.getColumnModel().getColumn(1).setPreferredWidth(UITheme.s(210));
        recentTable.getColumnModel().getColumn(2).setPreferredWidth(UITheme.s(140));
        recentTable.getColumnModel().getColumn(3).setPreferredWidth(UITheme.s(150));
        recentTable.getColumnModel().getColumn(4).setPreferredWidth(UITheme.s(100));
        recentTable.getColumnModel().getColumn(5).setPreferredWidth(UITheme.s(110));
        recentTable.getColumnModel().getColumn(6).setPreferredWidth(UITheme.s(140));
    }

    private void configureFullColumns() {
        complaintsTable.getColumnModel().getColumn(0).setPreferredWidth(UITheme.s(85));
        complaintsTable.getColumnModel().getColumn(1).setPreferredWidth(UITheme.s(190));
        complaintsTable.getColumnModel().getColumn(2).setPreferredWidth(UITheme.s(135));
        complaintsTable.getColumnModel().getColumn(3).setPreferredWidth(UITheme.s(140));
        complaintsTable.getColumnModel().getColumn(4).setPreferredWidth(UITheme.s(100));
        complaintsTable.getColumnModel().getColumn(5).setPreferredWidth(UITheme.s(110));
        complaintsTable.getColumnModel().getColumn(6).setPreferredWidth(UITheme.s(130));
        complaintsTable.getColumnModel().getColumn(7).setPreferredWidth(UITheme.s(140));
    }

    public void loadData() {
        List<Complaint> allMy = complaintService.getComplaintsForUser(currentUser);

        long total = allMy.size();
        long open = 0;
        long inProgress = 0;
        long resolved = 0;
        for (Complaint c : allMy) {
            if (c.getStatus() == ComplaintStatus.PENDING || c.getStatus() == ComplaintStatus.ASSIGNED) open++;
            else if (c.getStatus() == ComplaintStatus.IN_PROGRESS) inProgress++;
            else if (c.getStatus() == ComplaintStatus.RESOLVED) resolved++;
        }
        totalCard.setValue(total);
        openCard.setValue(open);
        inProgressCard.setValue(inProgress);
        resolvedCard.setValue(resolved);

        recentList = allMy.size() > 8 ? new ArrayList<>(allMy.subList(0, 8)) : new ArrayList<>(allMy);
        recentModel.setRowCount(0);
        for (Complaint c : recentList) {
            recentModel.addRow(new Object[]{
                    shortId(c.getId()),
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority(),
                    c.getStatus(),
                    FORMATTER.format(c.getCreatedAt())
            });
        }
        recentCardLayout.show(recentTableHolder, recentList.isEmpty() ? "EMPTY" : "TABLE");

        refreshFilteredTable();
    }

    private void refreshFilteredTable() {
        String query = searchField.getText().trim();
        Category cat = categoryFilter.getSelectedItem() instanceof Category c ? c : null;
        ComplaintStatus statusEnum = statusFilter.getSelectedItem() instanceof ComplaintStatus s ? s : null;

        currentList = complaintService.searchComplaints(currentUser, query, cat, null, statusEnum, null);

        tableModel.setRowCount(0);
        for (Complaint c : currentList) {
            tableModel.addRow(new Object[]{
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
        resultCountLabel.setText("Showing " + currentList.size() + " complaint" + (currentList.size() == 1 ? "" : "s")
                + "  •  Click any column header to sort  •  Double-click a row for details");
        listCardLayout.show(listTableHolder, currentList.isEmpty() ? "EMPTY" : "TABLE");
    }

    public void openNewComplaintDialog() {
        NewComplaintDialog dlg = new NewComplaintDialog(parentFrame, complaintService, priorityService, currentUser);
        dlg.setVisible(true);
        if (dlg.isComplaintSubmitted()) {
            loadData();
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
        try {
            Complaint fresh = complaintService.getComplaintForUser(currentUser, selected.getId());
            ComplaintDetailDialog dlg = new ComplaintDetailDialog(parentFrame, fresh, priorityService);
            dlg.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Access Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) : id;
    }
}
