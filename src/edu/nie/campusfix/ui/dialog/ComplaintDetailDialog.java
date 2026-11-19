package edu.nie.campusfix.ui.dialog;

import edu.nie.campusfix.enums.ComplaintStatus;
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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modal dialog presenting comprehensive complaint details, visual 4-stage lifecycle stepper,
 * overdue SLA banner, priority rule explanation, resolution notes, and full audit trail.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ComplaintDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public ComplaintDetailDialog(JFrame parent, Complaint complaint, PriorityService priorityService) {
        super(parent, "Complaint Details – " + complaint.getTitle(), true);
        setSize(UITheme.s(900), UITheme.s(760));
        setMinimumSize(new Dimension(UITheme.s(740), UITheme.s(600)));
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
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

        JPanel idRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        idRow.setOpaque(false);
        idRow.setAlignmentX(LEFT_ALIGNMENT);

        JLabel idLbl = new JLabel("ID: " + complaint.getId() + "   ");
        idLbl.setFont(UITheme.FONT_MONO);
        idLbl.setForeground(UITheme.TEXT_MUTED);

        ModernButton copyBtn = new ModernButton("Copy ID", ModernButton.Style.GHOST);
        copyBtn.setPreferredSize(new Dimension(UITheme.s(88), UITheme.s(28)));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(complaint.getId()), null);
            copyBtn.setText("Copied!");
        });

        idRow.add(idLbl);
        idRow.add(copyBtn);

        titleLbl.setAlignmentX(LEFT_ALIGNMENT);
        titleBox.add(titleLbl);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(idRow);

        JPanel badgesBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        badgesBox.setOpaque(false);
        if (complaint.isOverdue()) {
            badgesBox.add(PillBadge.createForOverdue());
        }
        badgesBox.add(PillBadge.createForPriority(complaint.getPriority()));
        badgesBox.add(PillBadge.createForStatus(complaint.getStatus()));

        headerPanel.add(titleBox, BorderLayout.CENTER);
        headerPanel.add(badgesBox, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Scrollable Body tracking viewport width cleanly (zero horizontal scrollbar!)
        JPanel contentPanel = new ScrollableVerticalPanel();
        contentPanel.setBackground(UITheme.BG_CANVAS);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        // Overdue Banner (if overdue)
        if (complaint.isOverdue()) {
            JPanel overdueBanner = new JPanel(new BorderLayout(8, 0));
            overdueBanner.setBackground(UITheme.DANGER_SOFT);
            overdueBanner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BADGE_CRITICAL_BORDER, 1),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)
            ));
            JLabel warnLbl = new JLabel("SLA Response Target Exceeded — Elapsed: "
                    + complaint.getFormattedElapsedTime() + " (Target: "
                    + complaint.getPriority().getResponseTargetHours() + "h, "
                    + complaint.getFormattedOverdueStatus() + ")");
            warnLbl.setFont(UITheme.FONT_SMALL_BOLD);
            warnLbl.setForeground(UITheme.BADGE_CRITICAL_FG);
            overdueBanner.add(warnLbl, BorderLayout.CENTER);
            overdueBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            contentPanel.add(overdueBanner);
            contentPanel.add(Box.createVerticalStrut(12));
        }

        // 4-Step Lifecycle Stepper Card
        JPanel stepperCard = createLifecycleStepper(complaint.getStatus());
        stepperCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        contentPanel.add(stepperCard);
        contentPanel.add(Box.createVerticalStrut(12));

        // Metadata Card
        JPanel metaCard = UITheme.createCardPanel(14, 18);
        metaCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addMetaRow(metaCard, gbc, 0,
                "Category:", complaint.getCategory().getDisplayName(),
                "Location:", complaint.getLocation());
        addMetaRow(metaCard, gbc, 1,
                "Severity:", complaint.getSeverity().getDisplayName(),
                "Elapsed Time:", complaint.getFormattedElapsedTime()
                        + " (Target: " + complaint.getPriority().getResponseTargetHours() + "h)");
        addMetaRow(metaCard, gbc, 2,
                "Reporter:", complaint.getReporterName(),
                "Submitted:", FORMATTER.format(complaint.getCreatedAt()));
        addMetaRow(metaCard, gbc, 3,
                "Assigned Tech:", complaint.getAssignedTechnicianName().isBlank() ? "Unassigned" : complaint.getAssignedTechnicianName(),
                "Last Updated:", FORMATTER.format(complaint.getUpdatedAt()));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED && complaint.getResolvedAt() != null) {
            addMetaRow(metaCard, gbc, 4,
                    "Resolved At:", FORMATTER.format(complaint.getResolvedAt()),
                    "SLA Status:", "Resolved");
        }

        String explanation = priorityService.explainCalculation(complaint.getSeverity(), complaint.getCategory());
        JPanel ruleBanner = new JPanel(new BorderLayout());
        ruleBanner.setBackground(UITheme.PRIMARY_SOFT);
        ruleBanner.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        JLabel expLbl = new JLabel("<html><b>Priority Calculation Rule:</b> " + escapeHtml(explanation) + "</html>");
        expLbl.setFont(UITheme.FONT_SMALL);
        expLbl.setForeground(UITheme.PRIMARY);
        ruleBanner.add(expLbl, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        gbc.insets = new Insets(10, 8, 2, 8);
        metaCard.add(ruleBanner, gbc);

        contentPanel.add(metaCard);
        contentPanel.add(Box.createVerticalStrut(12));

        // Description Card
        JPanel descCard = UITheme.createCardPanel(14, 18);
        descCard.setLayout(new BorderLayout(0, 6));
        JLabel descTitle = new JLabel("COMPLAINT DESCRIPTION");
        descTitle.setFont(UITheme.FONT_BADGE);
        descTitle.setForeground(UITheme.TEXT_MUTED);
        JLabel descBody = new JLabel("<html><body style='width: 560px;'>" + escapeHtml(complaint.getDescription()) + "</body></html>");
        descBody.setFont(UITheme.FONT_REGULAR);
        descBody.setForeground(UITheme.TEXT_PRIMARY);
        descCard.add(descTitle, BorderLayout.NORTH);
        descCard.add(descBody, BorderLayout.CENTER);
        contentPanel.add(descCard);
        contentPanel.add(Box.createVerticalStrut(12));

        // Resolution Notes Card (if resolved)
        if (complaint.getStatus() == ComplaintStatus.RESOLVED && !complaint.getResolutionNotes().isBlank()) {
            JPanel resCard = UITheme.createCardPanel(14, 18);
            resCard.setLayout(new BorderLayout(0, 6));
            JLabel resTitle = new JLabel("RESOLUTION NOTES");
            resTitle.setFont(UITheme.FONT_BADGE);
            resTitle.setForeground(UITheme.SUCCESS);
            JLabel resBody = new JLabel("<html><body style='width: 560px;'>" + escapeHtml(complaint.getResolutionNotes()) + "</body></html>");
            resBody.setFont(UITheme.FONT_REGULAR);
            resBody.setForeground(UITheme.TEXT_PRIMARY);
            resCard.add(resTitle, BorderLayout.NORTH);
            resCard.add(resBody, BorderLayout.CENTER);
            contentPanel.add(resCard);
            contentPanel.add(Box.createVerticalStrut(12));
        }

        // Audit Trail Timeline Card
        JPanel auditCard = UITheme.createCardPanel(14, 18);
        auditCard.setLayout(new BorderLayout(0, 10));
        List<ComplaintHistoryEntry> history = complaint.getAuditHistory();
        JLabel auditTitle = new JLabel("LIFECYCLE AUDIT HISTORY (" + history.size() + " EVENTS)");
        auditTitle.setFont(UITheme.FONT_BADGE);
        auditTitle.setForeground(UITheme.TEXT_MUTED);
        auditCard.add(auditTitle, BorderLayout.NORTH);

        String[] cols = {"Timestamp", "Action", "Actor", "Actor ID", "Notes"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (ComplaintHistoryEntry entry : history) {
            String shortActorId = entry.getActorId().length() > 8
                    ? entry.getActorId().substring(0, 8)
                    : entry.getActorId();
            model.addRow(new Object[]{
                    FORMATTER.format(entry.getTimestamp()),
                    entry.getActionType().getDisplayName(),
                    entry.getActorName(),
                    shortActorId,
                    entry.getNotes()
            });
        }

        ModernTable auditTable = new ModernTable(model);
        auditTable.setRowHeight(UITheme.s(38));
        auditTable.getColumnModel().getColumn(0).setPreferredWidth(UITheme.s(145));
        auditTable.getColumnModel().getColumn(1).setPreferredWidth(UITheme.s(130));
        auditTable.getColumnModel().getColumn(2).setPreferredWidth(UITheme.s(120));
        auditTable.getColumnModel().getColumn(3).setPreferredWidth(UITheme.s(75));
        auditTable.getColumnModel().getColumn(4).setPreferredWidth(UITheme.s(230));

        JScrollPane scrollTable = new JScrollPane(auditTable);
        UITheme.styleScrollPane(scrollTable);
        scrollTable.setPreferredSize(new Dimension(UITheme.s(200), UITheme.s(175)));
        auditCard.add(scrollTable, BorderLayout.CENTER);
        contentPanel.add(auditCard);

        JScrollPane mainScroll = new JScrollPane(contentPanel);
        UITheme.styleScrollPane(mainScroll);
        mainScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(UITheme.BG_CANVAS);
        add(mainScroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        ModernButton closeBtn = new ModernButton("Close", ModernButton.Style.SECONDARY);
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createLifecycleStepper(ComplaintStatus currentStatus) {
        JPanel card = UITheme.createCardPanel(12, 18);
        card.setLayout(new GridLayout(1, 4, 8, 0));
        ComplaintStatus[] stages = ComplaintStatus.values();
        int currentIdx = currentStatus.ordinal();

        for (int i = 0; i < stages.length; i++) {
            final int stepNum = i + 1;
            final boolean completed = i < currentIdx;
            final boolean active = i == currentIdx;
            final ComplaintStatus stage = stages[i];

            JPanel stepPanel = new JPanel(new BorderLayout(0, 4)) {
                private static final long serialVersionUID = 1L;
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = active ? UITheme.PRIMARY_SOFT : (completed ? UITheme.BADGE_RESOLVED_BG : UITheme.BG_CANVAS);
                    Color border = active ? UITheme.PRIMARY : (completed ? UITheme.BADGE_RESOLVED_BORDER : UITheme.BORDER_CARD);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            stepPanel.setOpaque(false);
            stepPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

            String badge = completed ? "✓ Step " + stepNum : "Step " + stepNum;
            JLabel numLbl = new JLabel(badge, SwingConstants.CENTER);
            numLbl.setFont(UITheme.FONT_BADGE);
            numLbl.setForeground(active ? UITheme.PRIMARY : (completed ? UITheme.SUCCESS : UITheme.TEXT_MUTED));

            JLabel nameLbl = new JLabel(stage.getDisplayName(), SwingConstants.CENTER);
            nameLbl.setFont(UITheme.FONT_SMALL_BOLD);
            nameLbl.setForeground(active ? UITheme.TEXT_PRIMARY : (completed ? UITheme.BADGE_RESOLVED_FG : UITheme.TEXT_MUTED));

            stepPanel.add(numLbl, BorderLayout.NORTH);
            stepPanel.add(nameLbl, BorderLayout.CENTER);
            card.add(stepPanel);
        }
        return card;
    }

    private void addMetaRow(JPanel panel, GridBagConstraints gbc, int row,
                            String lbl1, String val1, String lbl2, String val2) {
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;

        gbc.gridx = 0;
        JLabel l1 = new JLabel(lbl1);
        l1.setFont(UITheme.FONT_SMALL_BOLD);
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
        l2.setFont(UITheme.FONT_SMALL_BOLD);
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

    /**
     * Vertical scrollable container that always stretches cleanly to the viewport width.
     */
    @SuppressWarnings("serial")
    private static class ScrollableVerticalPanel extends JPanel implements javax.swing.Scrollable {
        private static final long serialVersionUID = 1L;
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
