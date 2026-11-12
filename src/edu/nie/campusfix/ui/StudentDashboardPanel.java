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
import edu.nie.campusfix.ui.dialog.NewComplaintDialog;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern dashboard panel for students/staff providing personal complaint tracking,
 * live search/filter, and instant complaint registration.
 */
@SuppressWarnings({"serial", "this-escape"})
public class StudentDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final JFrame parentFrame;
    private final User currentUser;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;

    private final StatCard totalCard = new StatCard("Total Submitted", 0, UITheme.PRIMARY);
    private final StatCard pendingCard = new StatCard("Pending", 0, UITheme.WARNING);
    private final StatCard inProgressCard = new StatCard("In Progress", 0, new Color(0x3B, 0x82, 0xF6));
    private final StatCard resolvedCard = new StatCard("Resolved", 0, UITheme.SUCCESS);

    private final JTextField searchField = new JTextField(16);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Statuses", "Pending", "Assigned", "In Progress", "Resolved"});

    private final DefaultTableModel tableModel;
    private final ModernTable complaintsTable;
    private List<Complaint> currentList = new ArrayList<>();

    public StudentDashboardPanel(JFrame parentFrame, User currentUser,
                                 ComplaintService complaintService, PriorityService priorityService) {
        this.parentFrame = parentFrame;
        this.currentUser = currentUser;
        this.complaintService = complaintService;
        this.priorityService = priorityService;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Row: Stats & Action
        JPanel topPanel = new JPanel(new BorderLayout(16, 0));
        topPanel.setOpaque(false);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        statsRow.setOpaque(false);
        statsRow.add(totalCard);
        statsRow.add(pendingCard);
        statsRow.add(inProgressCard);
        statsRow.add(resolvedCard);
        topPanel.add(statsRow, BorderLayout.CENTER);

        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        actionBox.setOpaque(false);
        ModernButton newBtn = new ModernButton("+ Report New Complaint", ModernButton.Style.PRIMARY);
        newBtn.addActionListener(e -> openNewComplaintDialog());
        actionBox.add(newBtn);
        topPanel.add(actionBox, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center: Complaints Table with Search/Filter Toolbar
        JPanel centerCard = UITheme.createCardPanel();
        centerCard.setLayout(new BorderLayout(0, 12));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBox.setOpaque(false);

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UITheme.FONT_BOLD);
        searchLbl.setForeground(UITheme.TEXT_PRIMARY);
        UITheme.styleTextField(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshTable(); }
            public void removeUpdate(DocumentEvent e) { refreshTable(); }
            public void changedUpdate(DocumentEvent e) { refreshTable(); }
        });

        UITheme.styleComboBox(statusFilter);
        statusFilter.addActionListener(e -> refreshTable());

        filterBox.add(searchLbl);
        filterBox.add(searchField);
        filterBox.add(Box.createHorizontalStrut(8));
        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(UITheme.FONT_BOLD);
        statusLbl.setForeground(UITheme.TEXT_PRIMARY);
        filterBox.add(statusLbl);
        filterBox.add(statusFilter);

        JPanel actionsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsBox.setOpaque(false);

        ModernButton viewBtn = new ModernButton("View Details", ModernButton.Style.SECONDARY);
        viewBtn.addActionListener(e -> handleViewDetails());

        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.Style.SECONDARY);
        refreshBtn.addActionListener(e -> loadData());

        actionsBox.add(viewBtn);
        actionsBox.add(refreshBtn);

        toolbar.add(filterBox, BorderLayout.WEST);
        toolbar.add(actionsBox, BorderLayout.EAST);
        centerCard.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Title", "Category", "Location", "Priority", "Status", "Technician", "Submitted"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) return Priority.class;
                if (columnIndex == 4) return ComplaintStatus.class;
                return String.class;
            }
        };

        complaintsTable = new ModernTable(tableModel);
        complaintsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        complaintsTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        complaintsTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        complaintsTable.getColumnModel().getColumn(3).setPreferredWidth(95);
        complaintsTable.getColumnModel().getColumn(4).setPreferredWidth(105);
        complaintsTable.getColumnModel().getColumn(5).setPreferredWidth(130);
        complaintsTable.getColumnModel().getColumn(6).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(complaintsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD));
        centerCard.add(scrollPane, BorderLayout.CENTER);

        add(centerCard, BorderLayout.CENTER);

        loadData();
    }

    public void loadData() {
        refreshTable();
    }

    private void refreshTable() {
        String query = searchField.getText().trim();
        String stFilter = (String) statusFilter.getSelectedItem();
        ComplaintStatus statusEnum = null;
        if (stFilter != null && !stFilter.startsWith("All")) {
            statusEnum = switch (stFilter) {
                case "Pending" -> ComplaintStatus.PENDING;
                case "Assigned" -> ComplaintStatus.ASSIGNED;
                case "In Progress" -> ComplaintStatus.IN_PROGRESS;
                case "Resolved" -> ComplaintStatus.RESOLVED;
                default -> null;
            };
        }

        List<Complaint> allMy = complaintService.getComplaintsForUser(currentUser);

        // Update KPI counts
        long total = allMy.size();
        long pending = 0;
        long inProgress = 0;
        long resolved = 0;
        for (Complaint c : allMy) {
            if (c.getStatus() == ComplaintStatus.PENDING || c.getStatus() == ComplaintStatus.ASSIGNED) pending++;
            else if (c.getStatus() == ComplaintStatus.IN_PROGRESS) inProgress++;
            else if (c.getStatus() == ComplaintStatus.RESOLVED) resolved++;
        }
        totalCard.setValue(total);
        pendingCard.setValue(pending);
        inProgressCard.setValue(inProgress);
        resolvedCard.setValue(resolved);

        // Filter for table
        currentList = complaintService.searchComplaints(currentUser, query, null, null, statusEnum, null);

        tableModel.setRowCount(0);
        for (Complaint c : currentList) {
            tableModel.addRow(new Object[]{
                    c.getTitle(),
                    c.getCategory().getDisplayName(),
                    c.getLocation(),
                    c.getPriority(),
                    c.getStatus(),
                    c.getAssignedTechnicianName().isBlank() ? "Unassigned" : c.getAssignedTechnicianName(),
                    FORMATTER.format(c.getCreatedAt())
            });
        }
    }

    private void openNewComplaintDialog() {
        NewComplaintDialog dlg = new NewComplaintDialog(parentFrame, complaintService, priorityService, currentUser);
        dlg.setVisible(true);
        if (dlg.isComplaintSubmitted()) {
            loadData();
        }
    }

    private void handleViewDetails() {
        int row = complaintsTable.getSelectedRow();
        if (row < 0 || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a complaint from the table to view details.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Complaint selected = currentList.get(row);
        ComplaintDetailDialog dlg = new ComplaintDetailDialog(parentFrame, selected, priorityService);
        dlg.setVisible(true);
    }
}
