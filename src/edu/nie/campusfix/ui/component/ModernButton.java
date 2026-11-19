package edu.nie.campusfix.ui.component;

import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern styled JButton supporting primary, secondary, danger, success, ghost, and sidebar
 * color tokens with dynamic text sizing, hover/press feedback, and anti-aliased rendering.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ModernButton extends JButton {
    private static final long serialVersionUID = 1L;

    public enum Style {
        PRIMARY, SECONDARY, DANGER, SUCCESS, GHOST, SIDEBAR
    }

    private final Style style;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean isActive = false;

    public ModernButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(style == Style.SIDEBAR ? UITheme.FONT_BOLD : UITheme.FONT_SMALL_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (style == Style.SIDEBAR) {
            setHorizontalAlignment(LEADING);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.s(46)));
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setActive(boolean active) {
        this.isActive = active;
        repaint();
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(getText() != null ? getText() : "");
        if (style == Style.SIDEBAR) {
            return new Dimension(Math.max(UITheme.s(216), textWidth + UITheme.s(36)), UITheme.s(46));
        }
        return new Dimension(Math.max(UITheme.s(92), textWidth + UITheme.s(34)), UITheme.s(42));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color bg;
        Color fg;
        Color border = null;

        if (!isEnabled()) {
            if (style == Style.SIDEBAR) {
                bg = UITheme.SIDEBAR_BG;
                fg = UITheme.SIDEBAR_MUTED;
            } else {
                bg = UITheme.BG_SUBTLE;
                fg = UITheme.TEXT_SUBTLE;
                border = UITheme.BORDER_CARD;
            }
        } else {
            switch (style) {
                case PRIMARY -> {
                    bg = isPressed ? UITheme.PRIMARY_ACTIVE : (isHovered ? UITheme.PRIMARY_HOVER : UITheme.PRIMARY);
                    fg = Color.WHITE;
                }
                case SECONDARY -> {
                    bg = isPressed ? UITheme.BORDER_CARD : (isHovered ? UITheme.BG_SUBTLE : Color.WHITE);
                    fg = UITheme.TEXT_PRIMARY;
                    border = UITheme.BORDER_INPUT;
                }
                case DANGER -> {
                    bg = isPressed ? new Color(0x99, 0x1B, 0x1B) : (isHovered ? UITheme.DANGER_HOVER : UITheme.DANGER);
                    fg = Color.WHITE;
                }
                case SUCCESS -> {
                    bg = isPressed ? new Color(0x06, 0x5F, 0x46) : (isHovered ? UITheme.SUCCESS_HOVER : UITheme.SUCCESS);
                    fg = Color.WHITE;
                }
                case GHOST -> {
                    bg = isPressed ? UITheme.PRIMARY_SOFT : (isHovered ? UITheme.BG_SUBTLE : new Color(0, 0, 0, 0));
                    fg = UITheme.PRIMARY;
                }
                case SIDEBAR -> {
                    bg = isActive ? UITheme.SIDEBAR_ACTIVE : (isHovered ? UITheme.SIDEBAR_HOVER : UITheme.SIDEBAR_BG);
                    fg = isActive ? UITheme.SIDEBAR_TEXT_ACTIVE : (isHovered ? Color.WHITE : UITheme.SIDEBAR_TEXT);
                }
                default -> {
                    bg = UITheme.PRIMARY;
                    fg = Color.WHITE;
                }
            }
        }

        int arc = (style == Style.SIDEBAR) ? 8 : 8;
        if (bg.getAlpha() > 0) {
            g2.setColor(bg);
            g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
        }

        if (style == Style.SIDEBAR && isActive) {
            g2.setColor(UITheme.PRIMARY);
            g2.fillRoundRect(1, 7, 4, h - 14, 4, 4);
        }

        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        }

        g2.setFont(getFont());
        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        String txt = getText() != null ? getText() : "";
        int textX = (style == Style.SIDEBAR) ? 18 : (w - fm.stringWidth(txt)) / 2;
        int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(txt, textX, textY);

        g2.dispose();
    }
}
