package edu.nie.campusfix.ui;

import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.exception.CampusFixException;
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
import javax.swing.Box;
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
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern technician dashboard allowing maintenance personnel to monitor assigned tasks,
 * transition status from ASSIGNED to IN_PROGRESS, and mark tasks as RESOLVED.
 */
@SuppressWarnings({"serial", "this-escape"})
public class TechnicianDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final JFrame parentFrame;
    private final User techUser;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;

    private final StatCard assignedCard = new StatCard("Assigned To Me", 0, UITheme.PRIMARY);
    private final StatCard inProgressCard = new StatCard("In Progress", 0, new Color(0x02, 0x84, 0xC7));
    private final StatCard resolvedCard = new StatCard("Resolved", 0, UITheme.SUCCESS);

    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Statuses", "Assigned", "In Progress", "Resolved"});

    private final DefaultTableModel tableModel;
    private final ModernTable complaintsTable;
    private List<Complaint> currentList = new ArrayList<>();

    public TechnicianDashboardPanel(JFrame parentFrame, User techUser,
                                    ComplaintService complaintService, PriorityService priorityService) {
        this.parentFrame = parentFrame;
        this.techUser = techUser;
        this.complaintService = complaintService;
        this.priorityService = priorityService;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Row: Stats
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        statsRow.setOpaque(false);
        statsRow.add(assignedCard);
        statsRow.add(inProgressCard);
        statsRow.add(resolvedCard);
        add(statsRow, BorderLayout.NORTH);

        // Center: Complaints Table
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 12));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftBox.setOpaque(false);

        JLabel sLbl = new JLabel("Search:");
        sLbl.setFont(UITheme.FONT_BOLD);
        sLbl.setForeground(UITheme.TEXT_PRIMARY);
        UITheme.styleTextField(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshTable(); }
            public void removeUpdate(DocumentEvent e) { refreshTable(); }
            public void changedUpdate(DocumentEvent e) { refreshTable(); }
        });

        UITheme.styleComboBox(statusFilter);
        statusFilter.addActionListener(e -> refreshTable());

        leftBox.add(sLbl);
        leftBox.add(searchField);
        leftBox.add(Box.createHorizontalStrut(8));
        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(UITheme.FONT_BOLD);
        statusLbl.setForeground(UITheme.TEXT_PRIMARY);
        leftBox.add(statusLbl);
        leftBox.add(statusFilter);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBox.setOpaque(false);

        ModernButton viewBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
        viewBtn.addActionListener(e -> handleViewDetails());

        ModernButton startBtn = new ModernButton("Start Work", ModernButton.Style.PRIMARY);
        startBtn.addActionListener(e -> handleStartWork());

        ModernButton resolveBtn = new ModernButton("Mark Resolved", ModernButton.Style.SUCCESS);
        resolveBtn.addActionListener(e -> handleMarkResolved());

        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> loadData());

        rightBox.add(viewBtn);
        rightBox.add(startBtn);
        rightBox.add(resolveBtn);
        rightBox.add(refreshBtn);

        toolbar.add(leftBox, BorderLayout.WEST);
        toolbar.add(rightBox, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Title", "Category", "Location", "Priority", "Status", "Reporter", "Assigned Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 3) return Priority.class;
                if (col == 4) return ComplaintStatus.class;
                return String.class;
            }
        };

        complaintsTable = new ModernTable(tableModel);
        complaintsTable.getColumnModel().getColumn(0).setPreferredWidth(190);
        complaintsTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        complaintsTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        complaintsTable.getColumnModel().getColumn(3).setPreferredWidth(95);
        complaintsTable.getColumnModel().getColumn(4).setPreferredWidth(105);
        complaintsTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        complaintsTable.getColumnModel().getColumn(6).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(complaintsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD));
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
        loadData();
    }

    public void loadData() {
        refreshTable();
    }

    private void refreshTable() {
        List<Complaint> myTasks = complaintService.getComplaintsForUser(techUser);

        long assigned = 0;
        long inProgress = 0;
        long resolved = 0;
        for (Complaint c : myTasks) {
            if (c.getStatus() == ComplaintStatus.ASSIGNED) assigned++;
            else if (c.getStatus() == ComplaintStatus.IN_PROGRESS) inProgress++;
            else if (c.getStatus() == ComplaintStatus.RESOLVED) resolved++;
        }

        assignedCard.setValue(assigned);
        inProgressCard.setValue(inProgress);
        resolvedCard.setValue(resolved);

        String q = searchField.getText().trim();
        String stFilter = (String) statusFilter.getSelectedItem();
        ComplaintStatus statusEnum = null;
        if (stFilter != null && !stFilter.startsWith("All")) {
            statusEnum = switch (stFilter) {
                case "Assigned" -> ComplaintStatus.ASSIGNED;
                case "In Progress" -> ComplaintStatus.IN_PROGRESS;
                case "Resolved" -> ComplaintStatus.RESOLVED;
                default -> null;
            };
        }

        currentList = complaintService.searchComplaints(techUser, q, null, null, statusEnum, null);

        tableModel.setRowCount(0);
        for (Complaint c : currentList) {
            tableModel.addRow(new Object[]{
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority(),
                    c.getStatus(),
                    c.getReporterName(),
                    FORMATTER.format(c.getUpdatedAt())
            });
        }
    }

    private void handleViewDetails() {
        int row = complaintsTable.getSelectedRow();
        if (row < 0 || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a task to view details.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = currentList.get(row);
        ComplaintDetailDialog dlg = new ComplaintDetailDialog(parentFrame, selected, priorityService);
        dlg.setVisible(true);
    }

    private void handleStartWork() {
        int row = complaintsTable.getSelectedRow();
        if (row < 0 || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a task to start work.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = currentList.get(row);
        if (selected.getStatus() != ComplaintStatus.ASSIGNED) {
            JOptionPane.showMessageDialog(this, "Can only start work on complaints in ASSIGNED status.",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            complaintService.startWork(techUser, selected.getId(), "Maintenance work commenced by " + techUser.getFullName());
            JOptionPane.showMessageDialog(this, "Status updated to In Progress.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadData();
        } catch (CampusFixException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleMarkResolved() {
        int row = complaintsTable.getSelectedRow();
        if (row < 0 || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a task to resolve.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = currentList.get(row);
        if (selected.getStatus() == ComplaintStatus.RESOLVED) {
            JOptionPane.showMessageDialog(this, "This task is already resolved.",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ResolveComplaintDialog dlg = new ResolveComplaintDialog(parentFrame, complaintService, selected, techUser);
        dlg.setVisible(true);
        if (dlg.isResolved()) {
            loadData();
        }
    }
}
