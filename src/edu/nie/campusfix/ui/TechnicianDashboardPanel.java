package edu.nie.campusfix.ui;

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
import edu.nie.campusfix.ui.dialog.ResolveComplaintDialog;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Maintenance Personnel workspace hosting:
 * - Technician Dashboard (personal workload KPIs & priority distribution)
 * - Assigned Tasks (strictly scoped assigned complaints with Start Work & Mark Resolved actions)
 */
@SuppressWarnings({"serial", "this-escape"})
public class TechnicianDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final String VIEW_DASHBOARD = "DASHBOARD";
    public static final String VIEW_TASKS = "TASKS";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final JFrame parentFrame;
    private final User techUser;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;

    private final CardLayout viewLayout = new CardLayout();
    private final JPanel viewHost = new JPanel(viewLayout);

    // KPI Cards
    private final StatCard assignedCard = new StatCard("Assigned To Me", 0, UITheme.PRIMARY);
    private final StatCard inProgressCard = new StatCard("In Progress", 0, UITheme.INFO);
    private final StatCard resolvedCard = new StatCard("Resolved", 0, UITheme.SUCCESS);
    private final StatCard overdueCard = new StatCard("Overdue Active", 0, UITheme.DANGER);

    // Overview Priority Breakdown
    private final JPanel priorityBarsPanel = new JPanel();
    private final DefaultTableModel urgentModel;
    private final ModernTable urgentTable;
    private final CardLayout urgentCardLayout = new CardLayout();
    private final JPanel urgentHolder = new JPanel(urgentCardLayout);
    private List<Complaint> activeTasksList = new ArrayList<>();

    // Tasks View Controls
    private final JTextField searchField = new JTextField(16);
    private final JComboBox<Object> priorityFilter = new JComboBox<>();
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final JLabel countLabel = new JLabel("0 tasks");
    private final ModernButton viewBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
    private final ModernButton startBtn = new ModernButton("Start Work", ModernButton.Style.PRIMARY);
    private final ModernButton resolveBtn = new ModernButton("Mark Resolved", ModernButton.Style.SUCCESS);

    private final DefaultTableModel tableModel;
    private final ModernTable complaintsTable;
    private final CardLayout tasksCardLayout = new CardLayout();
    private final JPanel tasksHolder = new JPanel(tasksCardLayout);
    private List<Complaint> currentList = new ArrayList<>();

    private Runnable onOpenTasksAction;

    public TechnicianDashboardPanel(JFrame parentFrame, User techUser,
                                    ComplaintService complaintService, PriorityService priorityService) {
        this.parentFrame = parentFrame;
        this.techUser = techUser;
        this.complaintService = complaintService;
        this.priorityService = priorityService;

        setLayout(new BorderLayout());
        setBackground(UITheme.BG_CANVAS);

        // Urgent/Active overview table
        String[] urgentCols = {"ID", "Title", "Category", "Location", "Priority", "Status", "Elapsed"};
        urgentModel = new DefaultTableModel(urgentCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 4) return Priority.class;
                if (col == 5) return ComplaintStatus.class;
                return String.class;
            }
        };
        urgentTable = new ModernTable(urgentModel);
        urgentTable.enableDefaultSorting();
        urgentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailsFor(urgentTable, activeTasksList);
                }
            }
        });

        // Full Assigned Tasks table
        String[] cols = {"ID", "Title", "Category", "Location", "Priority", "Status", "Elapsed", "Submitted"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 4) return Priority.class;
                if (col == 5) return ComplaintStatus.class;
                return String.class;
            }
        };
        complaintsTable = new ModernTable(tableModel);
        complaintsTable.enableDefaultSorting();
        complaintsTable.getColumnModel().getColumn(0).setPreferredWidth(UITheme.s(85));
        complaintsTable.getColumnModel().getColumn(1).setPreferredWidth(UITheme.s(190));
        complaintsTable.getColumnModel().getColumn(2).setPreferredWidth(UITheme.s(135));
        complaintsTable.getColumnModel().getColumn(3).setPreferredWidth(UITheme.s(145));
        complaintsTable.getColumnModel().getColumn(4).setPreferredWidth(UITheme.s(100));
        complaintsTable.getColumnModel().getColumn(5).setPreferredWidth(UITheme.s(110));
        complaintsTable.getColumnModel().getColumn(6).setPreferredWidth(UITheme.s(95));
        complaintsTable.getColumnModel().getColumn(7).setPreferredWidth(UITheme.s(140));

        complaintsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtonStates();
            }
        });
        complaintsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailsFor(complaintsTable, currentList);
                }
            }
        });

        viewHost.setOpaque(false);
        viewHost.add(createDashboardView(), VIEW_DASHBOARD);
        viewHost.add(createAssignedTasksView(), VIEW_TASKS);
        add(viewHost, BorderLayout.CENTER);

        loadData();
    }

    public void setOnOpenTasksAction(Runnable action) {
        this.onOpenTasksAction = action;
    }

    public void showView(String viewName) {
        viewLayout.show(viewHost, viewName);
        loadData();
    }

    private JPanel createDashboardView() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UITheme.createPageHeader(
                "Technician Dashboard",
                techUser.getRoleDescription() + " — Monitor assigned tasks and update repair progress."
        ), BorderLayout.WEST);

        ModernButton openQueueBtn = new ModernButton("Open Assigned Tasks Queue", ModernButton.Style.PRIMARY);
        openQueueBtn.addActionListener(e -> {
            if (onOpenTasksAction != null) {
                onOpenTasksAction.run();
            } else {
                showView(VIEW_TASKS);
            }
        });
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        btnWrap.setOpaque(false);
        btnWrap.add(openQueueBtn);
        headerRow.add(btnWrap, BorderLayout.EAST);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.add(assignedCard);
        statsRow.add(inProgressCard);
        statsRow.add(resolvedCard);
        statsRow.add(overdueCard);

        JPanel northWrap = new JPanel(new BorderLayout(0, 16));
        northWrap.setOpaque(false);
        northWrap.add(headerRow, BorderLayout.NORTH);
        northWrap.add(statsRow, BorderLayout.CENTER);
        panel.add(northWrap, BorderLayout.NORTH);

        // Center: Active Tasks + Priority Breakdown
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);

        JPanel activeCard = UITheme.createCardPanel(16, 18);
        activeCard.setLayout(new BorderLayout(0, 10));
        JLabel activeTitle = new JLabel("Active Assigned Work Orders");
        activeTitle.setFont(UITheme.FONT_SUBTITLE);
        activeTitle.setForeground(UITheme.TEXT_PRIMARY);
        activeCard.add(activeTitle, BorderLayout.NORTH);

        JScrollPane urgentScroll = new JScrollPane(urgentTable);
        UITheme.styleScrollPane(urgentScroll);

        urgentHolder.setOpaque(false);
        urgentHolder.add(urgentScroll, "TABLE");
        urgentHolder.add(UITheme.createEmptyStatePanel(
                "No Active Assignments",
                "You currently have no pending or in-progress maintenance tasks assigned to you."
        ), "EMPTY");
        activeCard.add(urgentHolder, BorderLayout.CENTER);

        JPanel breakdownCard = UITheme.createCardPanel(16, 18);
        breakdownCard.setPreferredSize(new Dimension(UITheme.s(290), UITheme.s(260)));
        breakdownCard.setLayout(new BorderLayout(0, 10));
        JLabel bdTitle = new JLabel("ACTIVE BY PRIORITY");
        bdTitle.setFont(UITheme.FONT_BADGE);
        bdTitle.setForeground(UITheme.TEXT_MUTED);
        breakdownCard.add(bdTitle, BorderLayout.NORTH);

        priorityBarsPanel.setOpaque(false);
        priorityBarsPanel.setLayout(new BoxLayout(priorityBarsPanel, BoxLayout.Y_AXIS));
        breakdownCard.add(priorityBarsPanel, BorderLayout.CENTER);

        body.add(activeCard, BorderLayout.CENTER);
        body.add(breakdownCard, BorderLayout.EAST);
        panel.add(body, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAssignedTasksView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        panel.add(UITheme.createPageHeader(
                "My Assigned Tasks",
                "Select an assigned task to begin work (ASSIGNED → IN PROGRESS) or record completion notes (IN PROGRESS → RESOLVED)."
        ), BorderLayout.NORTH);

        JPanel card = UITheme.createCardPanel(14, 16);
        card.setLayout(new BorderLayout(0, 12));

        // 2-row toolbar
        JPanel toolbar = new JPanel(new BorderLayout(0, 8));
        toolbar.setOpaque(false);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filterRow.setOpaque(false);

        JLabel sLbl = new JLabel("Search:");
        sLbl.setFont(UITheme.FONT_SMALL_BOLD);
        UITheme.styleTextField(searchField);
        searchField.setPreferredSize(new Dimension(UITheme.s(200), UITheme.s(38)));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshTasksTable(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshTasksTable(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshTasksTable(); }
        });

        priorityFilter.addItem("All Priorities");
        for (Priority p : Priority.values()) priorityFilter.addItem(p);
        UITheme.styleComboBox(priorityFilter);
        priorityFilter.setPreferredSize(new Dimension(UITheme.s(158), UITheme.s(38)));
        priorityFilter.addActionListener(e -> refreshTasksTable());

        statusFilter.addItem("All Statuses");
        statusFilter.addItem(ComplaintStatus.ASSIGNED);
        statusFilter.addItem(ComplaintStatus.IN_PROGRESS);
        statusFilter.addItem(ComplaintStatus.RESOLVED);
        UITheme.styleComboBox(statusFilter);
        statusFilter.setPreferredSize(new Dimension(UITheme.s(160), UITheme.s(38)));
        statusFilter.addActionListener(e -> refreshTasksTable());

        ModernButton resetBtn = new ModernButton("Reset", ModernButton.Style.SECONDARY);
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            priorityFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            refreshTasksTable();
        });

        filterRow.add(sLbl);
        filterRow.add(searchField);
        filterRow.add(priorityFilter);
        filterRow.add(statusFilter);
        filterRow.add(resetBtn);

        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);
        countLabel.setFont(UITheme.FONT_SMALL);
        countLabel.setForeground(UITheme.TEXT_MUTED);
        actionRow.add(countLabel, BorderLayout.WEST);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBox.setOpaque(false);

        viewBtn.addActionListener(e -> openDetailsFor(complaintsTable, currentList));
        startBtn.addActionListener(e -> handleStartWork());
        resolveBtn.addActionListener(e -> handleMarkResolved());

        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> loadData());

        rightBox.add(viewBtn);
        rightBox.add(startBtn);
        rightBox.add(resolveBtn);
        rightBox.add(refreshBtn);
        actionRow.add(rightBox, BorderLayout.EAST);

        toolbar.add(filterRow, BorderLayout.NORTH);
        toolbar.add(actionRow, BorderLayout.SOUTH);
        card.add(toolbar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(complaintsTable);
        UITheme.styleScrollPane(scroll);

        tasksHolder.setOpaque(false);
        tasksHolder.add(scroll, "TABLE");
        tasksHolder.add(UITheme.createEmptyStatePanel(
                "No Assigned Tasks Found",
                "You currently have no maintenance tasks matching these filters. Check back once an administrator assigns work."
        ), "EMPTY");
        card.add(tasksHolder, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    public void loadData() {
        List<Complaint> myTasks = complaintService.getComplaintsForUser(techUser);

        long assigned = 0;
        long inProgress = 0;
        long resolved = 0;
        long overdue = 0;
        Map<Priority, Long> activePrio = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) activePrio.put(p, 0L);

        activeTasksList = new ArrayList<>();
        for (Complaint c : myTasks) {
            if (c.getStatus() == ComplaintStatus.ASSIGNED) {
                assigned++;
                activeTasksList.add(c);
                activePrio.put(c.getPriority(), activePrio.get(c.getPriority()) + 1);
            } else if (c.getStatus() == ComplaintStatus.IN_PROGRESS) {
                inProgress++;
                activeTasksList.add(c);
                activePrio.put(c.getPriority(), activePrio.get(c.getPriority()) + 1);
            } else if (c.getStatus() == ComplaintStatus.RESOLVED) {
                resolved++;
            }
            if (c.isOverdue()) overdue++;
        }

        assignedCard.setValue(assigned);
        inProgressCard.setValue(inProgress);
        resolvedCard.setValue(resolved);
        overdueCard.setValue(overdue);

        // Update overview active table
        urgentModel.setRowCount(0);
        for (Complaint c : activeTasksList) {
            urgentModel.addRow(new Object[]{
                    shortId(c.getId()),
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority(),
                    c.getStatus(),
                    c.getFormattedElapsedTime()
            });
        }
        urgentCardLayout.show(urgentHolder, activeTasksList.isEmpty() ? "EMPTY" : "TABLE");

        // Update priority breakdown bars
        priorityBarsPanel.removeAll();
        long maxP = activePrio.values().stream().mapToLong(Long::longValue).max().orElse(1);
        priorityBarsPanel.add(UITheme.createMetricBarRow("Critical (24h target)", activePrio.get(Priority.CRITICAL), Math.max(1, maxP), UITheme.DANGER));
        priorityBarsPanel.add(UITheme.createMetricBarRow("High (48h target)", activePrio.get(Priority.HIGH), Math.max(1, maxP), UITheme.WARNING));
        priorityBarsPanel.add(UITheme.createMetricBarRow("Medium (72h target)", activePrio.get(Priority.MEDIUM), Math.max(1, maxP), UITheme.PRIMARY));
        priorityBarsPanel.add(UITheme.createMetricBarRow("Low (7d target)", activePrio.get(Priority.LOW), Math.max(1, maxP), UITheme.SUCCESS));
        priorityBarsPanel.revalidate();
        priorityBarsPanel.repaint();

        refreshTasksTable();
    }

    private void refreshTasksTable() {
        String q = searchField.getText().trim();
        Priority prioEnum = priorityFilter.getSelectedItem() instanceof Priority p ? p : null;
        ComplaintStatus statusEnum = statusFilter.getSelectedItem() instanceof ComplaintStatus s ? s : null;

        currentList = complaintService.searchComplaints(techUser, q, null, prioEnum, statusEnum, null);

        tableModel.setRowCount(0);
        for (Complaint c : currentList) {
            tableModel.addRow(new Object[]{
                    shortId(c.getId()),
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority(),
                    c.getStatus(),
                    c.getFormattedElapsedTime(),
                    FORMATTER.format(c.getCreatedAt())
            });
        }

        countLabel.setText("Showing " + currentList.size() + " assigned task" + (currentList.size() == 1 ? "" : "s")
                + "  •  Double-click a row for details");
        tasksCardLayout.show(tasksHolder, currentList.isEmpty() ? "EMPTY" : "TABLE");
        updateActionButtonStates();
    }

    private void updateActionButtonStates() {
        int modelRow = complaintsTable.getSelectedModelRow();
        if (modelRow < 0 || modelRow >= currentList.size()) {
            startBtn.setEnabled(false);
            resolveBtn.setEnabled(false);
            return;
        }
        Complaint selected = currentList.get(modelRow);
        startBtn.setEnabled(selected.getStatus() == ComplaintStatus.ASSIGNED);
        resolveBtn.setEnabled(selected.getStatus() == ComplaintStatus.IN_PROGRESS);
    }

    private void openDetailsFor(ModernTable table, List<Complaint> list) {
        int modelRow = table.getSelectedModelRow();
        if (modelRow < 0 || modelRow >= list.size()) {
            JOptionPane.showMessageDialog(this, "Please select a task to view details.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Complaint fresh = complaintService.getComplaintForUser(techUser, list.get(modelRow).getId());
            ComplaintDetailDialog dlg = new ComplaintDetailDialog(parentFrame, fresh, priorityService);
            dlg.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Access Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleStartWork() {
        int modelRow = complaintsTable.getSelectedModelRow();
        if (modelRow < 0 || modelRow >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select an ASSIGNED task to start work.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = currentList.get(modelRow);
        if (selected.getStatus() != ComplaintStatus.ASSIGNED) {
            JOptionPane.showMessageDialog(this, "Work can only be started on tasks in ASSIGNED status.",
                    "Invalid Transition", JOptionPane.WARNING_MESSAGE);
            return;
        }

        startBtn.setEnabled(false);
        startBtn.setText("Starting...");
        new SwingWorker<Complaint, Void>() {
            @Override
            protected Complaint doInBackground() {
                return complaintService.startWork(techUser, selected.getId(),
                        "Maintenance work started by " + techUser.getFullName());
            }

            @Override
            protected void done() {
                startBtn.setText("Start Work");
                try {
                    get();
                    JOptionPane.showMessageDialog(TechnicianDashboardPanel.this,
                            "Status updated to IN PROGRESS.", "Work Started", JOptionPane.INFORMATION_MESSAGE);
                    loadData();
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(TechnicianDashboardPanel.this,
                            cause.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
                    updateActionButtonStates();
                }
            }
        }.execute();
    }

    private void handleMarkResolved() {
        int modelRow = complaintsTable.getSelectedModelRow();
        if (modelRow < 0 || modelRow >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select an IN PROGRESS task to resolve.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = currentList.get(modelRow);
        if (selected.getStatus() != ComplaintStatus.IN_PROGRESS) {
            JOptionPane.showMessageDialog(this,
                    "Please click 'Start Work' first. Only IN PROGRESS tasks can be marked as RESOLVED.",
                    "Invalid Transition", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ResolveComplaintDialog dlg = new ResolveComplaintDialog(parentFrame, complaintService, selected, techUser);
        dlg.setVisible(true);
        if (dlg.isResolved()) {
            loadData();
        }
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) : id;
    }
}
