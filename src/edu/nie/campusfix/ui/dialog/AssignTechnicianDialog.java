package edu.nie.campusfix.ui.dialog;

import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.TechnicianUser;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.repository.UserRepository;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.service.ReportService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.theme.UITheme;

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
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Modal dialog for assigning or reassigning a maintenance complaint to an active technician.
 * Uses exact ID-based workload lookup and highlights specialization matches.
 */
@SuppressWarnings({"serial", "this-escape"})
public class AssignTechnicianDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final ComplaintService complaintService;
    private final Complaint complaint;
    private final User admin;
    private boolean assigned = false;

    private final JComboBox<TechnicianItem> technicianCombo = new JComboBox<>();
    private final JTextField noteField = new JTextField();
    private final ModernButton assignBtn;
    private final ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.Style.SECONDARY);

    private record TechnicianItem(TechnicianUser tech, long activeTasks, boolean specialtyMatch) {
        @Override
        public String toString() {
            String rec = specialtyMatch ? " ★ Match" : "";
            return tech.getFullName() + " [" + tech.getSpecialization().getDisplayName() + "]"
                    + rec + " — Active Tasks: " + activeTasks;
        }
    }

    public AssignTechnicianDialog(JFrame parent, ComplaintService complaintService,
                                  UserRepository userRepository, ReportService reportService,
                                  Complaint complaint, User admin) {
        super(parent, complaint.getStatus() == ComplaintStatus.PENDING
                ? "Assign Maintenance Technician" : "Reassign Maintenance Technician", true);
        this.complaintService = complaintService;
        this.complaint = complaint;
        this.admin = admin;

        boolean isReassign = complaint.getStatus() != ComplaintStatus.PENDING;
        this.assignBtn = new ModernButton(isReassign ? "Confirm Reassignment" : "Assign Technician", ModernButton.Style.PRIMARY);

        setSize(UITheme.s(640), UITheme.s(420));
        setMinimumSize(new Dimension(UITheme.s(520), UITheme.s(360)));
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
                BorderFactory.createEmptyBorder(16, 22, 16, 22)
        ));

        JLabel titleLbl = new JLabel(isReassign ? "Reassign Complaint" : "Assign Complaint");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subLbl = new JLabel("Complaint: " + complaint.getTitle()
                + " (" + complaint.getCategory().getDisplayName() + " • " + complaint.getPriority().getDisplayName() + " Priority)");
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel headerBox = new JPanel();
        headerBox.setOpaque(false);
        headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
        headerBox.add(titleLbl);
        headerBox.add(Box.createVerticalStrut(4));
        headerBox.add(subLbl);
        header.add(headerBox, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Form Card
        JPanel formCard = UITheme.createCardPanel(18, 20);
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        ReportService.ComplaintSummaryReport report = reportService.generateSummary();
        Map<String, Long> activeByTechId = report.activeWorkloadByTechId();
        List<TechnicianUser> activeTechs = userRepository.findAllTechnicians().stream()
                .filter(TechnicianUser::isActive)
                .sorted(Comparator
                        .comparing((TechnicianUser t) -> t.getSpecialization() != complaint.getCategory())
                        .thenComparingLong(t -> activeByTechId.getOrDefault(t.getId(), 0L))
                        .thenComparing(TechnicianUser::getFullName))
                .toList();

        for (TechnicianUser t : activeTechs) {
            long active = activeByTechId.getOrDefault(t.getId(), 0L);
            boolean match = t.getSpecialization() == complaint.getCategory();
            technicianCombo.addItem(new TechnicianItem(t, active, match));
        }

        UITheme.styleComboBox(technicianCombo);
        UITheme.styleTextField(noteField);
        noteField.setToolTipText("Optional instructions or dispatch notes for the technician");

        int row = 0;
        if (isReassign && !complaint.getAssignedTechnicianName().isBlank()) {
            gbc.gridy = row++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            JLabel currentLbl = new JLabel("Currently assigned to: " + complaint.getAssignedTechnicianName()
                    + " (Status: " + complaint.getStatus().getDisplayName() + ")");
            currentLbl.setFont(UITheme.FONT_SMALL_BOLD);
            currentLbl.setForeground(UITheme.WARNING);
            formCard.add(currentLbl, gbc);
            gbc.gridwidth = 1;
        }

        if (activeTechs.isEmpty()) {
            gbc.gridy = row++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            JLabel emptyLbl = new JLabel("<html>No active technicians are registered yet. Please register or activate a technician in the <b>Technicians</b> section first.</html>");
            emptyLbl.setFont(UITheme.FONT_REGULAR);
            emptyLbl.setForeground(UITheme.DANGER);
            formCard.add(emptyLbl, gbc);
            assignBtn.setEnabled(false);
        } else {
            gbc.gridy = row;
            gbc.gridx = 0;
            gbc.weightx = 0.28;
            JLabel techLbl = new JLabel("Select Technician *");
            techLbl.setFont(UITheme.FONT_SMALL_BOLD);
            techLbl.setForeground(UITheme.TEXT_PRIMARY);
            formCard.add(techLbl, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.72;
            formCard.add(technicianCombo, gbc);

            row++;
            gbc.gridy = row;
            gbc.gridx = 0;
            gbc.weightx = 0.28;
            JLabel noteLbl = new JLabel("Dispatch Note");
            noteLbl.setFont(UITheme.FONT_SMALL_BOLD);
            noteLbl.setForeground(UITheme.TEXT_PRIMARY);
            formCard.add(noteLbl, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.72;
            formCard.add(noteField, gbc);
        }

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        centerWrapper.add(formCard, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        cancelBtn.addActionListener(e -> dispose());
        assignBtn.addActionListener(e -> handleAssign());

        footer.add(cancelBtn);
        footer.add(assignBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void handleAssign() {
        TechnicianItem selected = (TechnicianItem) technicianCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "No active technician selected.", "Assignment Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (complaint.getStatus() == ComplaintStatus.IN_PROGRESS) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "This complaint is currently IN PROGRESS with " + complaint.getAssignedTechnicianName() + ".\n"
                            + "Reassigning it to " + selected.tech().getFullName() + " will reset status to ASSIGNED.\n\nProceed?",
                    "Confirm Reassignment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String note = noteField.getText().trim();
        assignBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        assignBtn.setText("Saving...");

        new SwingWorker<Complaint, Void>() {
            @Override
            protected Complaint doInBackground() {
                return complaintService.assignTechnician(admin, complaint.getId(), selected.tech().getId(), note);
            }

            @Override
            protected void done() {
                assignBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                try {
                    get();
                    assigned = true;
                    JOptionPane.showMessageDialog(AssignTechnicianDialog.this,
                            "Complaint assigned to " + selected.tech().getFullName() + " successfully.",
                            "Assignment Confirmed", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (InterruptedException | ExecutionException ex) {
                    assignBtn.setText("Assign Technician");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(AssignTechnicianDialog.this,
                            cause.getMessage(), "Assignment Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public boolean isAssigned() {
        return assigned;
    }
}
