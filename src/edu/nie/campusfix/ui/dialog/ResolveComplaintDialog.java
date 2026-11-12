package edu.nie.campusfix.ui.dialog;

import edu.nie.campusfix.exception.CampusFixException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Modal dialog prompting technician or administrator for mandatory resolution notes
 * when marking a complaint as resolved.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ResolveComplaintDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final ComplaintService complaintService;
    private final Complaint complaint;
    private final User actor;
    private boolean resolved = false;

    private final JTextArea notesArea = new JTextArea(6, 30);

    public ResolveComplaintDialog(JFrame parent, ComplaintService complaintService,
                                  Complaint complaint, User actor) {
        super(parent, "Resolve Maintenance Complaint", true);
        this.complaintService = complaintService;
        this.complaint = complaint;
        this.actor = actor;

        setSize(520, 380);
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

        JLabel titleLbl = new JLabel("Complete & Resolve Task");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subLbl = new JLabel("Task: " + complaint.getTitle() + " (" + complaint.getLocation() + ")");
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

        // Body
        JPanel bodyCard = UITheme.createCardPanel();
        bodyCard.setLayout(new BorderLayout(0, 8));

        JLabel instructLbl = new JLabel("Resolution Summary / Action Taken (Minimum 5 characters) *");
        instructLbl.setFont(UITheme.FONT_BOLD);
        instructLbl.setForeground(UITheme.TEXT_PRIMARY);
        bodyCard.add(instructLbl, BorderLayout.NORTH);

        UITheme.styleTextArea(notesArea);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(notesArea);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_INPUT, 1));
        bodyCard.add(scroll, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        centerWrapper.add(bodyCard, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.Style.SECONDARY);
        cancelBtn.addActionListener(e -> dispose());

        ModernButton resolveBtn = new ModernButton("Mark as Resolved", ModernButton.Style.SUCCESS);
        resolveBtn.addActionListener(e -> handleResolve());

        footer.add(cancelBtn);
        footer.add(resolveBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void handleResolve() {
        String notes = notesArea.getText().trim();
        try {
            complaintService.resolveComplaint(actor, complaint.getId(), notes);
            resolved = true;
            JOptionPane.showMessageDialog(this, "Complaint resolved successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (CampusFixException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isResolved() {
        return resolved;
    }
}
