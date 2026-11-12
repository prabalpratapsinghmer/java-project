package edu.nie.campusfix.ui.dialog;

import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.ComplaintHistoryEntry;
import edu.nie.campusfix.service.PriorityService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.component.ModernTable;
import edu.nie.campusfix.ui.component.PillBadge;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modal dialog presenting comprehensive complaint metadata, calculated priority explanation,
 * and chronological audit history timeline.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ComplaintDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public ComplaintDetailDialog(JFrame parent, Complaint complaint, PriorityService priorityService) {
        super(parent, "Complaint Details – " + complaint.getTitle(), true);
        setSize(780, 680);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout(0, 16));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(complaint.getTitle());
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel idLbl = new JLabel("ID: " + complaint.getId());
        idLbl.setFont(UITheme.FONT_SMALL);
        idLbl.setForeground(UITheme.TEXT_MUTED);

        titleBox.add(titleLbl);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(idLbl);

        JPanel badgesBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badgesBox.setOpaque(false);
        badgesBox.add(PillBadge.createForPriority(complaint.getPriority()));
        badgesBox.add(PillBadge.createForStatus(complaint.getStatus()));

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(badgesBox, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Content Panel (scrollable)
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 24, 16, 24));

        // Metadata Card
        JPanel metaCard = UITheme.createCardPanel();
        metaCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addMetaRow(metaCard, gbc, 0, "Category:", complaint.getCategory().getDisplayName(),
                "Location:", complaint.getLocation());
        addMetaRow(metaCard, gbc, 1, "Reporter:", complaint.getReporterName(),
                "Submitted:", FORMATTER.format(complaint.getCreatedAt()));
        addMetaRow(metaCard, gbc, 2, "Assigned Tech:",
                complaint.getAssignedTechnicianName().isBlank() ? "Unassigned" : complaint.getAssignedTechnicianName(),
                "Last Updated:", FORMATTER.format(complaint.getUpdatedAt()));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            addMetaRow(metaCard, gbc, 3, "Resolved At:",
                    complaint.getResolvedAt() != null ? FORMATTER.format(complaint.getResolvedAt()) : "N/A",
                    "Resolution Notes:", complaint.getResolutionNotes());
        }

        // Priority calculation explanation banner
        String explanation = priorityService.explainCalculation(complaint.getSeverity(), complaint.getCategory());
        JLabel expLbl = new JLabel("Priority Rule: " + explanation);
        expLbl.setFont(UITheme.FONT_SMALL);
        expLbl.setForeground(UITheme.PRIMARY);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 4;
        gbc.insets = new Insets(10, 8, 4, 8);
        metaCard.add(expLbl, gbc);

        // Description Card
        JPanel descCard = UITheme.createCardPanel();
        descCard.setLayout(new BorderLayout(0, 6));
        JLabel descTitle = new JLabel("DESCRIPTION");
        descTitle.setFont(UITheme.FONT_BADGE);
        descTitle.setForeground(UITheme.TEXT_MUTED);
        JLabel descBody = new JLabel("<html><body style='width: 480px;'>" + escapeHtml(complaint.getDescription()) + "</body></html>");
        descBody.setFont(UITheme.FONT_REGULAR);
        descBody.setForeground(UITheme.TEXT_PRIMARY);
        descCard.add(descTitle, BorderLayout.NORTH);
        descCard.add(descBody, BorderLayout.CENTER);

        // Audit Trail Timeline Card
        JPanel auditCard = UITheme.createCardPanel();
        auditCard.setLayout(new BorderLayout(0, 10));
        JLabel auditTitle = new JLabel("LIFECYCLE AUDIT TRAIL (" + complaint.getAuditHistory().size() + " events)");
        auditTitle.setFont(UITheme.FONT_BADGE);
        auditTitle.setForeground(UITheme.TEXT_MUTED);
        auditCard.add(auditTitle, BorderLayout.NORTH);

        String[] cols = {"Timestamp", "Action", "Actor", "Notes"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        List<ComplaintHistoryEntry> history = complaint.getAuditHistory();
        for (ComplaintHistoryEntry entry : history) {
            model.addRow(new Object[]{
                    FORMATTER.format(entry.getTimestamp()),
                    entry.getActionType().getDisplayName(),
                    entry.getActorName(),
                    entry.getNotes()
            });
        }

        ModernTable auditTable = new ModernTable(model);
        auditTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        auditTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        auditTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        auditTable.getColumnModel().getColumn(3).setPreferredWidth(260);

        JScrollPane scrollTable = new JScrollPane(auditTable);
        scrollTable.setPreferredSize(new Dimension(680, 160));
        scrollTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD));
        auditCard.add(scrollTable, BorderLayout.CENTER);

        contentPanel.add(metaCard);
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(descCard);
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(auditCard);

        JScrollPane mainScroll = new JScrollPane(contentPanel);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(UITheme.BG_CANVAS);
        add(mainScroll, BorderLayout.CENTER);

        // Footer Action Panel
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        ModernButton closeBtn = new ModernButton("Close", ModernButton.Style.SECONDARY);
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void addMetaRow(JPanel panel, GridBagConstraints gbc, int row,
                            String lbl1, String val1, String lbl2, String val2) {
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;

        gbc.gridx = 0;
        JLabel l1 = new JLabel(lbl1);
        l1.setFont(UITheme.FONT_BOLD);
        l1.setForeground(UITheme.TEXT_MUTED);
        panel.add(l1, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        JLabel v1 = new JLabel(val1);
        v1.setFont(UITheme.FONT_REGULAR);
        v1.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(v1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel l2 = new JLabel(lbl2);
        l2.setFont(UITheme.FONT_BOLD);
        l2.setForeground(UITheme.TEXT_MUTED);
        panel.add(l2, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        JLabel v2 = new JLabel(val2);
        v2.setFont(UITheme.FONT_REGULAR);
        v2.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(v2, gbc);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\n", "<br/>");
    }
}
