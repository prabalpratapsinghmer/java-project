package edu.nie.campusfix.ui.component;

import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Role;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Modern rounded pill badge component for semantic status, priority, and role visualization.
 */
@SuppressWarnings({"serial", "this-escape"})
public class PillBadge extends JComponent {
    private static final long serialVersionUID = 1L;
    private String text;
    private Color bgColor;
    private Color fgColor;
    private Color borderColor;

    public PillBadge(String text, Color bgColor, Color fgColor, Color borderColor) {
        this.text = text == null ? "" : text;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        this.borderColor = borderColor;
        setFont(UITheme.FONT_BADGE);
        setOpaque(false);
        updatePreferredSize();
    }

    public static PillBadge createForStatus(ComplaintStatus status) {
        if (status == null) return createCustom("Unknown", Color.LIGHT_GRAY, Color.DARK_GRAY, Color.GRAY);
        return switch (status) {
            case PENDING -> createCustom(status.getDisplayName(), UITheme.BADGE_PENDING_BG, UITheme.BADGE_PENDING_FG, UITheme.BADGE_PENDING_BORDER);
            case ASSIGNED -> createCustom(status.getDisplayName(), UITheme.BADGE_ASSIGNED_BG, UITheme.BADGE_ASSIGNED_FG, UITheme.BADGE_ASSIGNED_BORDER);
            case IN_PROGRESS -> createCustom(status.getDisplayName(), UITheme.BADGE_IN_PROGRESS_BG, UITheme.BADGE_IN_PROGRESS_FG, UITheme.BADGE_IN_PROGRESS_BORDER);
            case RESOLVED -> createCustom(status.getDisplayName(), UITheme.BADGE_RESOLVED_BG, UITheme.BADGE_RESOLVED_FG, UITheme.BADGE_RESOLVED_BORDER);
        };
    }

    public static PillBadge createForPriority(Priority priority) {
        if (priority == null) return createCustom("Unknown", Color.LIGHT_GRAY, Color.DARK_GRAY, Color.GRAY);
        return switch (priority) {
            case CRITICAL -> createCustom(priority.getDisplayName(), UITheme.BADGE_CRITICAL_BG, UITheme.BADGE_CRITICAL_FG, UITheme.BADGE_CRITICAL_BORDER);
            case HIGH -> createCustom(priority.getDisplayName(), UITheme.BADGE_HIGH_BG, UITheme.BADGE_HIGH_FG, UITheme.BADGE_HIGH_BORDER);
            case MEDIUM -> createCustom(priority.getDisplayName(), UITheme.BADGE_MEDIUM_BG, UITheme.BADGE_MEDIUM_FG, UITheme.BADGE_MEDIUM_BORDER);
            case LOW -> createCustom(priority.getDisplayName(), UITheme.BADGE_LOW_BG, UITheme.BADGE_LOW_FG, UITheme.BADGE_LOW_BORDER);
        };
    }

    public static PillBadge createForSeverity(Severity severity) {
        if (severity == null) return createCustom("Unknown", Color.LIGHT_GRAY, Color.DARK_GRAY, Color.GRAY);
        return switch (severity) {
            case HIGH -> createCustom(severity.getDisplayName(), UITheme.BADGE_CRITICAL_BG, UITheme.BADGE_CRITICAL_FG, UITheme.BADGE_CRITICAL_BORDER);
            case MEDIUM -> createCustom(severity.getDisplayName(), UITheme.BADGE_MEDIUM_BG, UITheme.BADGE_MEDIUM_FG, UITheme.BADGE_MEDIUM_BORDER);
            case LOW -> createCustom(severity.getDisplayName(), UITheme.BADGE_LOW_BG, UITheme.BADGE_LOW_FG, UITheme.BADGE_LOW_BORDER);
        };
    }

    public static PillBadge createForRole(Role role) {
        if (role == null) return createCustom("User", Color.LIGHT_GRAY, Color.DARK_GRAY, Color.GRAY);
        return switch (role) {
            case ADMIN -> createCustom("Admin", new Color(0xFE, 0xF3, 0xC7), new Color(0x92, 0x40, 0x0E), new Color(0xFD, 0xE6, 0x8A));
            case TECHNICIAN -> createCustom("Technician", new Color(0xE0, 0xE7, 0xFF), new Color(0x37, 0x30, 0xA3), new Color(0xC7, 0xD2, 0xFE));
            case STUDENT -> createCustom("Student", new Color(0xEC, 0xFD, 0xF5), new Color(0x06, 0x5F, 0x46), new Color(0xA7, 0xF3, 0xD0));
        };
    }

    public static PillBadge createCustom(String text, Color bg, Color fg, Color border) {
        return new PillBadge(text, bg, fg, border);
    }

    public void updateBadge(String text, Color bg, Color fg, Color border) {
        this.text = text;
        this.bgColor = bg;
        this.fgColor = fg;
        this.borderColor = border;
        updatePreferredSize();
        repaint();
    }

    private void updatePreferredSize() {
        FontMetrics fm = getFontMetrics(getFont() != null ? getFont() : UITheme.FONT_BADGE);
        int width = fm.stringWidth(text) + 20;
        int height = fm.getHeight() + 8;
        setPreferredSize(new Dimension(Math.max(60, width), Math.max(24, height)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = h - 2;

        g2.setColor(bgColor);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        g2.setColor(borderColor);
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

        g2.setColor(fgColor);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, tx, ty);

        g2.dispose();
    }
}
