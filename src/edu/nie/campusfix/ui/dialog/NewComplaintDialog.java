package edu.nie.campusfix.ui.dialog;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.exception.CampusFixException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.service.PriorityService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.component.PillBadge;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Modal form for reporting maintenance complaints featuring dynamic priority calculation preview
 * and duplicate complaint warnings.
 */
@SuppressWarnings({"serial", "this-escape"})
public class NewComplaintDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final ComplaintService complaintService;
    private final PriorityService priorityService;
    private final User reporter;
    private boolean complaintSubmitted = false;

    private final JTextField titleField = new JTextField();
    private final JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());
    private final JTextField locationField = new JTextField();
    private final JComboBox<Severity> severityCombo = new JComboBox<>(Severity.values());
    private final JTextArea descriptionArea = new JTextArea(5, 20);
    private final PillBadge priorityBadge = PillBadge.createForPriority(Priority.LOW);
    private final JLabel priorityExplanation = new JLabel();

    public NewComplaintDialog(JFrame parent, ComplaintService complaintService,
                              PriorityService priorityService, User reporter) {
        super(parent, "Report New Maintenance Complaint", true);
        this.complaintService = complaintService;
        this.priorityService = priorityService;
        this.reporter = reporter;

        setSize(640, 620);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        JLabel titleLbl = new JLabel("New Maintenance Request");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitleLbl = new JLabel("Submit infrastructure issue for automated prioritization and technician assignment");
        subtitleLbl.setFont(UITheme.FONT_SMALL);
        subtitleLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel headerBox = new JPanel();
        headerBox.setOpaque(false);
        headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
        headerBox.add(titleLbl);
        headerBox.add(Box.createVerticalStrut(4));
        headerBox.add(subtitleLbl);
        header.add(headerBox, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Form Panel
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        UITheme.styleTextField(titleField);
        UITheme.styleComboBox(categoryCombo);
        UITheme.styleTextField(locationField);
        UITheme.styleComboBox(severityCombo);

        descriptionArea.setFont(UITheme.FONT_REGULAR);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_INPUT, 1));
        descScroll.setPreferredSize(new Dimension(300, 100));

        priorityExplanation.setFont(UITheme.FONT_SMALL);
        priorityExplanation.setForeground(UITheme.TEXT_MUTED);

        categoryCombo.addActionListener(e -> updatePriorityPreview());
        severityCombo.addActionListener(e -> updatePriorityPreview());
        updatePriorityPreview();

        int row = 0;
        addFormField(formCard, gbc, row++, "Complaint Title *", titleField);
        addFormField(formCard, gbc, row++, "Infrastructure Category *", categoryCombo);
        addFormField(formCard, gbc, row++, "Campus Location * (e.g. Science Block Lab 3)", locationField);
        addFormField(formCard, gbc, row++, "Issue Severity *", severityCombo);

        // Priority preview row
        JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        previewPanel.setOpaque(false);
        previewPanel.add(priorityBadge);
        previewPanel.add(priorityExplanation);
        addFormField(formCard, gbc, row++, "Calculated Priority", previewPanel);

        addFormField(formCard, gbc, row++, "Detailed Description *", descScroll);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        centerWrapper.add(formCard, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.Style.SECONDARY);
        cancelBtn.addActionListener(e -> dispose());

        ModernButton submitBtn = new ModernButton("Submit Complaint", ModernButton.Style.PRIMARY);
        submitBtn.addActionListener(e -> handleSubmit());

        footer.add(cancelBtn);
        footer.add(submitBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String labelText, Object comp) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        if (comp instanceof java.awt.Component c) {
            panel.add(c, gbc);
        }
    }

    private void updatePriorityPreview() {
        Category cat = (Category) categoryCombo.getSelectedItem();
        Severity sev = (Severity) severityCombo.getSelectedItem();
        if (cat != null && sev != null) {
            Priority prio = priorityService.calculatePriority(sev, cat);
            priorityBadge.updateBadge(prio.getDisplayName(),
                    switch (prio) {
                        case CRITICAL -> UITheme.BADGE_CRITICAL_BG;
                        case HIGH -> UITheme.BADGE_HIGH_BG;
                        case MEDIUM -> UITheme.BADGE_MEDIUM_BG;
                        case LOW -> UITheme.BADGE_LOW_BG;
                    },
                    switch (prio) {
                        case CRITICAL -> UITheme.BADGE_CRITICAL_FG;
                        case HIGH -> UITheme.BADGE_HIGH_FG;
                        case MEDIUM -> UITheme.BADGE_MEDIUM_FG;
                        case LOW -> UITheme.BADGE_LOW_FG;
                    },
                    switch (prio) {
                        case CRITICAL -> UITheme.BADGE_CRITICAL_BORDER;
                        case HIGH -> UITheme.BADGE_HIGH_BORDER;
                        case MEDIUM -> UITheme.BADGE_MEDIUM_BORDER;
                        case LOW -> UITheme.BADGE_LOW_BORDER;
                    }
            );
            priorityExplanation.setText("Target response: " + prio.getResponseTargetHours() + "h" +
                    (cat.isHighRisk() ? " (Includes high-risk category bonus)" : ""));
        }
    }

    private void handleSubmit() {
        String title = titleField.getText().trim();
        Category cat = (Category) categoryCombo.getSelectedItem();
        String loc = locationField.getText().trim();
        Severity sev = (Severity) severityCombo.getSelectedItem();
        String desc = descriptionArea.getText().trim();

        // Check for duplicates before submission
        List<Complaint> potentialDuplicates = complaintService.checkPotentialDuplicates(loc, cat);
        if (!potentialDuplicates.isEmpty()) {
            Complaint dup = potentialDuplicates.get(0);
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "A similar unresolved complaint already exists for this location:\n\n" +
                            "\"" + dup.getTitle() + "\" (" + dup.getStatus().getDisplayName() + ", " + dup.getPriority().getDisplayName() + " Priority)\n" +
                            "Location: " + dup.getLocation() + "\n\n" +
                            "Do you still want to submit this complaint?",
                    "Potential Duplicate Complaint Detected",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            complaintService.submitComplaint(reporter, title, cat, loc, desc, sev);
            complaintSubmitted = true;
            JOptionPane.showMessageDialog(this,
                    "Complaint submitted successfully!\nPriority: " + priorityService.calculatePriority(sev, cat).getDisplayName(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (CampusFixException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isComplaintSubmitted() {
        return complaintSubmitted;
    }
}
