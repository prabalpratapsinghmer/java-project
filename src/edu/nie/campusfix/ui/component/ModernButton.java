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
 * Modern styled JButton supporting primary, secondary, danger, and success color tokens
 * with hover, press states, and anti-aliased rounded appearance.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ModernButton extends JButton {
    private static final long serialVersionUID = 1L;
    public enum Style {
        PRIMARY, SECONDARY, DANGER, SUCCESS, SIDEBAR
    }

    private final Style style;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean isActive = false;

    public ModernButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(UITheme.FONT_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (style == Style.SIDEBAR) {
            setHorizontalAlignment(LEADING);
            setPreferredSize(new Dimension(210, 40));
        } else {
            setPreferredSize(new Dimension(getPreferredSize().width + 24, 38));
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
            bg = new Color(0xE2, 0xE8, 0xF0);
            fg = new Color(0x94, 0xA3, 0xB8);
        } else {
            switch (style) {
                case PRIMARY -> {
                    bg = isPressed ? UITheme.PRIMARY_ACTIVE : (isHovered ? UITheme.PRIMARY_HOVER : UITheme.PRIMARY);
                    fg = Color.WHITE;
                }
                case SECONDARY -> {
                    bg = isPressed ? new Color(0xE2, 0xE8, 0xF0) : (isHovered ? new Color(0xF1, 0xF5, 0xF9) : Color.WHITE);
                    fg = UITheme.TEXT_PRIMARY;
                    border = UITheme.BORDER_INPUT;
                }
                case DANGER -> {
                    bg = isPressed ? new Color(0x99, 0x1B, 0x1B) : (isHovered ? UITheme.DANGER_HOVER : UITheme.DANGER);
                    fg = Color.WHITE;
                }
                case SUCCESS -> {
                    bg = isPressed ? new Color(0x04, 0x78, 0x57) : (isHovered ? UITheme.SUCCESS_HOVER : UITheme.SUCCESS);
                    fg = Color.WHITE;
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

        // Draw background
        int arc = (style == Style.SIDEBAR) ? 8 : 10;
        g2.setColor(bg);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        }

        // Draw text
        g2.setFont(getFont());
        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        int textX;
        if (style == Style.SIDEBAR) {
            textX = 18;
        } else {
            textX = (w - fm.stringWidth(getText())) / 2;
        }
        int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
