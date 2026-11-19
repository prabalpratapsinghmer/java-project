package edu.nie.campusfix.ui.dialog;

import edu.nie.campusfix.exception.CampusFixException;
import edu.nie.campusfix.model.Complaint;
import edu.nie.campusfix.model.User;
import edu.nie.campusfix.service.ComplaintService;
import edu.nie.campusfix.ui.component.ModernButton;
import edu.nie.campusfix.ui.theme.UITheme;
import edu.nie.campusfix.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutionException;

/**
 * Modal dialog prompting the assigned technician for mandatory resolution notes
 * when marking an IN_PROGRESS complaint as RESOLVED.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ResolveComplaintDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final ComplaintService complaintService;
    private final Complaint complaint;
    private final User actor;
    private boolean resolved = false;

    private final JTextArea notesArea = new JTextArea(6, 32);
    private final JLabel countLabel = new JLabel("0 / 1000 characters (min 5)");
    private final ModernButton resolveBtn = new ModernButton("Mark as Resolved", ModernButton.Style.SUCCESS);
    private final ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.Style.SECONDARY);

    public ResolveComplaintDialog(JFrame parent, ComplaintService complaintService,
                                  Complaint complaint, User actor) {
        super(parent, "Resolve Maintenance Complaint", true);
        this.complaintService = complaintService;
        this.complaint = complaint;
        this.actor = actor;

        setSize(UITheme.s(600), UITheme.s(440));
        setMinimumSize(new Dimension(UITheme.s(480), UITheme.s(360)));
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
        JPanel bodyCard = UITheme.createCardPanel(16, 18);
        bodyCard.setLayout(new BorderLayout(0, 8));

        JLabel instructLbl = new JLabel("Resolution Notes / Work Performed *");
        instructLbl.setFont(UITheme.FONT_SMALL_BOLD);
        instructLbl.setForeground(UITheme.TEXT_PRIMARY);
        bodyCard.add(instructLbl, BorderLayout.NORTH);

        UITheme.styleTextArea(notesArea);
        notesArea.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                int len = notesArea.getText().trim().length();
                countLabel.setText(len + " / 1000 characters (min 5)");
                countLabel.setForeground(len > 1000 ? UITheme.DANGER : UITheme.TEXT_MUTED);
            }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        JScrollPane scroll = new JScrollPane(notesArea);
        UITheme.styleScrollPane(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_INPUT, 1));
        bodyCard.add(scroll, BorderLayout.CENTER);

        countLabel.setFont(UITheme.FONT_SMALL);
        countLabel.setForeground(UITheme.TEXT_MUTED);
        bodyCard.add(countLabel, BorderLayout.SOUTH);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        centerWrapper.add(bodyCard, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        cancelBtn.addActionListener(e -> dispose());
        resolveBtn.addActionListener(e -> handleResolve());

        footer.add(cancelBtn);
        footer.add(resolveBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void handleResolve() {
        String notes = notesArea.getText().trim();
        try {
            ValidationUtil.validateResolutionNotes(notes);
        } catch (CampusFixException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        resolveBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        resolveBtn.setText("Resolving...");

        new SwingWorker<Complaint, Void>() {
            @Override
            protected Complaint doInBackground() {
                return complaintService.resolveComplaint(actor, complaint.getId(), notes);
            }

            @Override
            protected void done() {
                resolveBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                try {
                    get();
                    resolved = true;
                    JOptionPane.showMessageDialog(ResolveComplaintDialog.this,
                            "Complaint marked as RESOLVED and audit history updated.",
                            "Resolution Recorded", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (InterruptedException | ExecutionException ex) {
                    resolveBtn.setText("Mark as Resolved");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(ResolveComplaintDialog.this,
                            cause.getMessage(), "Resolution Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public boolean isResolved() {
        return resolved;
    }
}
