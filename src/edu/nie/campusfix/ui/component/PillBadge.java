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
 * Modern rounded pill badge component for semantic status, priority, severity, overdue, and role display.
 */
@SuppressWarnings({"serial", "this-escape"})
public class PillBadge extends JComponent {
    private static final long serialVersionUID = 1L;
    private String text;
    private Color bgColor;
    private Color fgColor;
    private Color borderColor;

    public record BadgeColors(Color bg, Color fg, Color border) {}

    public PillBadge(String text, Color bgColor, Color fgColor, Color borderColor) {
        this.text = text == null ? "" : text;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        this.borderColor = borderColor;
        setFont(UITheme.FONT_BADGE);
        setOpaque(false);
        updatePreferredSize();
    }

    public static BadgeColors colorsForStatus(ComplaintStatus status) {
        if (status == null) {
            return new BadgeColors(UITheme.BG_SUBTLE, UITheme.TEXT_MUTED, UITheme.BORDER_CARD);
        }
        return switch (status) {
            case PENDING -> new BadgeColors(UITheme.BADGE_PENDING_BG, UITheme.BADGE_PENDING_FG, UITheme.BADGE_PENDING_BORDER);
            case ASSIGNED -> new BadgeColors(UITheme.BADGE_ASSIGNED_BG, UITheme.BADGE_ASSIGNED_FG, UITheme.BADGE_ASSIGNED_BORDER);
            case IN_PROGRESS -> new BadgeColors(UITheme.BADGE_IN_PROGRESS_BG, UITheme.BADGE_IN_PROGRESS_FG, UITheme.BADGE_IN_PROGRESS_BORDER);
            case RESOLVED -> new BadgeColors(UITheme.BADGE_RESOLVED_BG, UITheme.BADGE_RESOLVED_FG, UITheme.BADGE_RESOLVED_BORDER);
        };
    }

    public static BadgeColors colorsForPriority(Priority priority) {
        if (priority == null) {
            return new BadgeColors(UITheme.BG_SUBTLE, UITheme.TEXT_MUTED, UITheme.BORDER_CARD);
        }
        return switch (priority) {
            case CRITICAL -> new BadgeColors(UITheme.BADGE_CRITICAL_BG, UITheme.BADGE_CRITICAL_FG, UITheme.BADGE_CRITICAL_BORDER);
            case HIGH -> new BadgeColors(UITheme.BADGE_HIGH_BG, UITheme.BADGE_HIGH_FG, UITheme.BADGE_HIGH_BORDER);
            case MEDIUM -> new BadgeColors(UITheme.BADGE_MEDIUM_BG, UITheme.BADGE_MEDIUM_FG, UITheme.BADGE_MEDIUM_BORDER);
            case LOW -> new BadgeColors(UITheme.BADGE_LOW_BG, UITheme.BADGE_LOW_FG, UITheme.BADGE_LOW_BORDER);
        };
    }

    public static BadgeColors colorsForSeverity(Severity severity) {
        if (severity == null) {
            return new BadgeColors(UITheme.BG_SUBTLE, UITheme.TEXT_MUTED, UITheme.BORDER_CARD);
        }
        return switch (severity) {
            case HIGH -> new BadgeColors(UITheme.BADGE_CRITICAL_BG, UITheme.BADGE_CRITICAL_FG, UITheme.BADGE_CRITICAL_BORDER);
            case MEDIUM -> new BadgeColors(UITheme.BADGE_MEDIUM_BG, UITheme.BADGE_MEDIUM_FG, UITheme.BADGE_MEDIUM_BORDER);
            case LOW -> new BadgeColors(UITheme.BADGE_LOW_BG, UITheme.BADGE_LOW_FG, UITheme.BADGE_LOW_BORDER);
        };
    }

    public static PillBadge createForStatus(ComplaintStatus status) {
        BadgeColors c = colorsForStatus(status);
        return createCustom(status != null ? status.getDisplayName() : "Unknown", c.bg(), c.fg(), c.border());
    }

    public static PillBadge createForPriority(Priority priority) {
        BadgeColors c = colorsForPriority(priority);
        return createCustom(priority != null ? priority.getDisplayName() : "Unknown", c.bg(), c.fg(), c.border());
    }

    public static PillBadge createForSeverity(Severity severity) {
        BadgeColors c = colorsForSeverity(severity);
        return createCustom(severity != null ? severity.getDisplayName() : "Unknown", c.bg(), c.fg(), c.border());
    }

    public static PillBadge createForRole(Role role) {
        if (role == null) return createCustom("User", UITheme.BG_SUBTLE, UITheme.TEXT_MUTED, UITheme.BORDER_CARD);
        return switch (role) {
            case ADMIN -> createCustom("Administrator", new Color(0xFE, 0xF3, 0xC7), new Color(0x92, 0x40, 0x0E), new Color(0xFD, 0xE6, 0x8A));
            case TECHNICIAN -> createCustom("Technician", new Color(0xE0, 0xE7, 0xFF), new Color(0x37, 0x30, 0xA3), new Color(0xC7, 0xD2, 0xFE));
            case STUDENT -> createCustom("Student / Staff", new Color(0xEC, 0xFD, 0xF5), new Color(0x06, 0x5F, 0x46), new Color(0xA7, 0xF3, 0xD0));
        };
    }

    public static PillBadge createForOverdue() {
        return createCustom("OVERDUE", UITheme.BADGE_CRITICAL_BG, UITheme.BADGE_CRITICAL_FG, UITheme.BADGE_CRITICAL_BORDER);
    }

    public static PillBadge createCustom(String text, Color bg, Color fg, Color border) {
        return new PillBadge(text, bg, fg, border);
    }

    public void updateBadge(String text, Color bg, Color fg, Color border) {
        this.text = text == null ? "" : text;
        this.bgColor = bg;
        this.fgColor = fg;
        this.borderColor = border;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    public void updateForPriority(Priority priority) {
        BadgeColors c = colorsForPriority(priority);
        updateBadge(priority != null ? priority.getDisplayName() : "Unknown", c.bg(), c.fg(), c.border());
    }

    private void updatePreferredSize() {
        FontMetrics fm = getFontMetrics(getFont() != null ? getFont() : UITheme.FONT_BADGE);
        int width = fm.stringWidth(text) + 22;
        int height = fm.getHeight() + 8;
        Dimension dim = new Dimension(Math.max(58, width), Math.max(24, height));
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        paintPill(g2, 0, 0, getWidth(), getHeight(), text, bgColor, fgColor, borderColor, getFont());
        g2.dispose();
    }

    /**
     * Reusable direct pill-painting helper shared with ModernTable cell renderers.
     */
    public static void paintPill(Graphics2D g2, int x, int y, int w, int h,
                                 String label, Color bg, Color fg, Color border, java.awt.Font font) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int arc = h - 2;
        g2.setColor(bg);
        g2.fillRoundRect(x + 1, y + 1, w - 2, h - 2, arc, arc);

        g2.setColor(border);
        g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, arc, arc);

        g2.setColor(fg);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(label, tx, ty);
    }
}
