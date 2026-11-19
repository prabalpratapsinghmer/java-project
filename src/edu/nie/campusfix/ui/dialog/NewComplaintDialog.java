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
import edu.nie.campusfix.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Modal form for reporting maintenance complaints featuring live deterministic priority calculation,
 * upfront input validation, privacy-safe duplicate warnings, and off-EDT persistence.
 */
@SuppressWarnings({"serial", "this-escape"})
public class NewComplaintDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ComplaintService complaintService;
    private final PriorityService priorityService;
    private final User reporter;
    private boolean complaintSubmitted = false;
    private Complaint submittedComplaint;

    private final JTextField titleField = new JTextField();
    private final JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());
    private final JTextField locationField = new JTextField();
    private final JComboBox<Severity> severityCombo = new JComboBox<>(Severity.values());
    private final JTextArea descriptionArea = new JTextArea(5, 24);
    private final JLabel charCountLabel = new JLabel("0 / 1000 characters (min 10)");
    private final PillBadge priorityBadge = PillBadge.createForPriority(Priority.LOW);
    private final JLabel priorityExplanation = new JLabel();
    private final ModernButton submitBtn = new ModernButton("Submit Complaint", ModernButton.Style.PRIMARY);
    private final ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.Style.SECONDARY);

    public NewComplaintDialog(JFrame parent, ComplaintService complaintService,
                              PriorityService priorityService, User reporter) {
        super(parent, "Report New Maintenance Complaint", true);
        this.complaintService = complaintService;
        this.priorityService = priorityService;
        this.reporter = reporter;

        setSize(UITheme.s(760), UITheme.s(680));
        setMinimumSize(new Dimension(UITheme.s(600), UITheme.s(580)));
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        JLabel titleLbl = new JLabel("New Maintenance Complaint");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitleLbl = new JLabel("Provide accurate location and details for automatic priority scoring and dispatch");
        subtitleLbl.setFont(UITheme.FONT_SMALL);
        subtitleLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel headerBox = new JPanel();
        headerBox.setOpaque(false);
        headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
        headerBox.add(titleLbl);
        headerBox.add(Box.createVerticalStrut(3));
        headerBox.add(subtitleLbl);
        header.add(headerBox, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Form Card
        JPanel formCard = UITheme.createCardPanel(18, 22);
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        UITheme.styleTextField(titleField);
        titleField.setToolTipText("Short summary (3-100 characters), e.g., Flickering ceiling fan");
        UITheme.styleComboBox(categoryCombo);
        UITheme.styleTextField(locationField);
        locationField.setToolTipText("Specific campus location, e.g., Academic Block A - Room 204");
        UITheme.styleComboBox(severityCombo);
        severityCombo.setSelectedItem(Severity.MEDIUM);
        UITheme.styleTextArea(descriptionArea);

        descriptionArea.getDocument().addDocumentListener(new DocumentListener() {
            private void updateCount() {
                int len = descriptionArea.getText().trim().length();
                charCountLabel.setText(len + " / 1000 characters (min 10)");
                charCountLabel.setForeground(len > 1000 ? UITheme.DANGER : UITheme.TEXT_MUTED);
            }
            @Override public void insertUpdate(DocumentEvent e) { updateCount(); }
            @Override public void removeUpdate(DocumentEvent e) { updateCount(); }
            @Override public void changedUpdate(DocumentEvent e) { updateCount(); }
        });

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        UITheme.styleScrollPane(descScroll);
        descScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_INPUT, 1));
        descScroll.setPreferredSize(new Dimension(320, 110));

        priorityExplanation.setFont(UITheme.FONT_SMALL);
        priorityExplanation.setForeground(UITheme.TEXT_SECONDARY);

        categoryCombo.addActionListener(e -> updatePriorityPreview());
        severityCombo.addActionListener(e -> updatePriorityPreview());
        updatePriorityPreview();

        int row = 0;
        addFormField(formCard, gbc, row++, "Complaint Title *", titleField);
        addFormField(formCard, gbc, row++, "Category *", categoryCombo);
        addFormField(formCard, gbc, row++, "Campus Location *", locationField);
        addFormField(formCard, gbc, row++, "Issue Severity *", severityCombo);

        // Priority preview banner
        JPanel previewBanner = new JPanel(new BorderLayout(10, 0));
        previewBanner.setBackground(UITheme.PRIMARY_SOFT);
        previewBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC7, 0xD2, 0xFE), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        previewBanner.add(priorityBadge, BorderLayout.WEST);
        previewBanner.add(priorityExplanation, BorderLayout.CENTER);
        addFormField(formCard, gbc, row++, "Calculated Priority", previewBanner);

        // Description + character counter
        JPanel descPanel = new JPanel(new BorderLayout(0, 4));
        descPanel.setOpaque(false);
        descPanel.add(descScroll, BorderLayout.CENTER);
        charCountLabel.setFont(UITheme.FONT_SMALL);
        charCountLabel.setForeground(UITheme.TEXT_MUTED);
        descPanel.add(charCountLabel, BorderLayout.SOUTH);

        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.28;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel descLbl = new JLabel("Detailed Description *");
        descLbl.setFont(UITheme.FONT_SMALL_BOLD);
        descLbl.setForeground(UITheme.TEXT_PRIMARY);
        descLbl.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        formCard.add(descLbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.72;
        gbc.fill = GridBagConstraints.BOTH;
        formCard.add(descPanel, gbc);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        centerWrapper.add(formCard, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        cancelBtn.addActionListener(e -> dispose());
        submitBtn.addActionListener(e -> handleSubmit());

        footer.add(cancelBtn);
        footer.add(submitBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent comp) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.28;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_SMALL_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.72;
        panel.add(comp, gbc);
    }

    private void updatePriorityPreview() {
        Category cat = (Category) categoryCombo.getSelectedItem();
        Severity sev = (Severity) severityCombo.getSelectedItem();
        if (cat != null && sev != null) {
            Priority prio = priorityService.calculatePriority(sev, cat);
            priorityBadge.updateForPriority(prio);
            priorityExplanation.setText("<html>" + priorityService.explainCalculation(sev, cat) + "</html>");
        }
    }

    private void handleSubmit() {
        String title = titleField.getText().trim();
        Category cat = (Category) categoryCombo.getSelectedItem();
        String loc = locationField.getText().trim();
        Severity sev = (Severity) severityCombo.getSelectedItem();
        String desc = descriptionArea.getText().trim();

        // Validate inputs BEFORE running duplicate detection
        try {
            ValidationUtil.validateComplaintFields(title, loc, desc);
        } catch (CampusFixException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setBusy(true);
        new SwingWorker<List<Complaint>, Void>() {
            @Override
            protected List<Complaint> doInBackground() {
                return complaintService.checkPotentialDuplicates(loc, cat);
            }

            @Override
            protected void done() {
                try {
                    List<Complaint> potentialDuplicates = get();
                    if (!potentialDuplicates.isEmpty()) {
                        setBusy(false);
                        StringBuilder msg = new StringBuilder();
                        msg.append("A similar unresolved complaint already exists for this location.\n\n");
                        msg.append("Matching active report(s) within the last 7 days (").append(potentialDuplicates.size()).append("):\n");
                        int shown = Math.min(3, potentialDuplicates.size());
                        for (int i = 0; i < shown; i++) {
                            Complaint dup = potentialDuplicates.get(i);
                            // Privacy-safe summary: never disclose reporter name/ID or private description
                            msg.append(" • Category: ").append(dup.getCategory().getDisplayName())
                               .append(" | Location: ").append(dup.getLocation())
                               .append(" | Status: ").append(dup.getStatus().getDisplayName())
                               .append(" | Priority: ").append(dup.getPriority().getDisplayName())
                               .append(" | Submitted: ").append(SHORT_DATE.format(dup.getCreatedAt()))
                               .append("\n");
                        }
                        if (potentialDuplicates.size() > shown) {
                            msg.append("   ...and ").append(potentialDuplicates.size() - shown).append(" more.\n");
                        }
                        msg.append("\nDo you still want to submit this complaint?");

                        int choice = JOptionPane.showConfirmDialog(
                                NewComplaintDialog.this,
                                msg.toString(),
                                "Potential Duplicate Complaint Detected",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        );
                        if (choice != JOptionPane.YES_OPTION) {
                            return;
                        }
                        setBusy(true);
                    }
                    persistComplaint(title, cat, loc, desc, sev);
                } catch (InterruptedException | ExecutionException ex) {
                    setBusy(false);
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(NewComplaintDialog.this,
                            "Error checking duplicates: " + cause.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void persistComplaint(String title, Category cat, String loc, String desc, Severity sev) {
        new SwingWorker<Complaint, Void>() {
            @Override
            protected Complaint doInBackground() {
                return complaintService.submitComplaint(reporter, title, cat, loc, desc, sev);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    submittedComplaint = get();
                    complaintSubmitted = true;
                    JOptionPane.showMessageDialog(NewComplaintDialog.this,
                            "Complaint submitted successfully!\n\n"
                                    + "Complaint ID: " + submittedComplaint.getId() + "\n"
                                    + "Calculated Priority: " + submittedComplaint.getPriority().getDisplayName()
                                    + " (Response Target: " + submittedComplaint.getPriority().getResponseTargetHours() + "h)\n"
                                    + "Status: " + submittedComplaint.getStatus().getDisplayName(),
                            "Complaint Registered", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(NewComplaintDialog.this,
                            cause.getMessage(), "Submission Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        submitBtn.setEnabled(!busy);
        cancelBtn.setEnabled(!busy);
        submitBtn.setText(busy ? "Submitting..." : "Submit Complaint");
    }

    public boolean isComplaintSubmitted() {
        return complaintSubmitted;
    }

    public Complaint getSubmittedComplaint() {
        return submittedComplaint;
    }
}
