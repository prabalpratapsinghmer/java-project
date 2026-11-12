package edu.nie.campusfix.ui.dialog;

import edu.nie.campusfix.exception.CampusFixException;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;

/**
 * Modal dialog for assigning or reassigning a maintenance complaint to a technician.
 * Displays real-time technician workload to facilitate balanced assignment.
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

    private record TechnicianItem(TechnicianUser tech, long activeTasks) {
        @Override
        public String toString() {
            return tech.getFullName() + " [" + tech.getSpecialization().getDisplayName() + "] – Active Tasks: " + activeTasks;
        }
    }

    public AssignTechnicianDialog(JFrame parent, ComplaintService complaintService,
                                  UserRepository userRepository, ReportService reportService,
                                  Complaint complaint, User admin) {
        super(parent, "Assign Maintenance Technician", true);
        this.complaintService = complaintService;
        this.complaint = complaint;
        this.admin = admin;

        setSize(540, 360);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel titleLbl = new JLabel("Assign Complaint");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subLbl = new JLabel("Task: " + complaint.getTitle() + " (" + complaint.getCategory().getDisplayName() + ")");
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

        // Form
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Populate technicians with workload counts
        ReportService.ComplaintSummaryReport report = reportService.generateSummary();
        Map<String, Long> workloads = report.technicianWorkload();
        List<TechnicianUser> technicians = userRepository.findAllTechnicians();

        for (TechnicianUser t : technicians) {
            long active = 0;
            for (Map.Entry<String, Long> entry : workloads.entrySet()) {
                if (entry.getKey().startsWith(t.getFullName())) {
                    active = entry.getValue();
                    break;
                }
            }
            technicianCombo.addItem(new TechnicianItem(t, active));
        }

        UITheme.styleComboBox(technicianCombo);
        UITheme.styleTextField(noteField);

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.3;
        JLabel techLbl = new JLabel("Technician *");
        techLbl.setFont(UITheme.FONT_BOLD);
        techLbl.setForeground(UITheme.TEXT_PRIMARY);
        formCard.add(techLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        formCard.add(technicianCombo, gbc);

        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0.3;
        JLabel noteLbl = new JLabel("Assignment Note");
        noteLbl.setFont(UITheme.FONT_BOLD);
        noteLbl.setForeground(UITheme.TEXT_PRIMARY);
        formCard.add(noteLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        formCard.add(noteField, gbc);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        centerWrapper.add(formCard, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.Style.SECONDARY);
        cancelBtn.addActionListener(e -> dispose());

        ModernButton assignBtn = new ModernButton("Assign", ModernButton.Style.PRIMARY);
        assignBtn.addActionListener(e -> handleAssign());

        footer.add(cancelBtn);
        footer.add(assignBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void handleAssign() {
        TechnicianItem selected = (TechnicianItem) technicianCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "No technician selected. Please register a technician first.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            complaintService.assignTechnician(admin, complaint.getId(), selected.tech().getId(), noteField.getText().trim());
            assigned = true;
            JOptionPane.showMessageDialog(this,
                    "Complaint assigned to " + selected.tech().getFullName() + " successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (CampusFixException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isAssigned() {
        return assigned;
    }
}
