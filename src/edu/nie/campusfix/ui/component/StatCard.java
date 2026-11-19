package edu.nie.campusfix.ui.component;

import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Modern KPI statistic card with colored top indicator bar, non-truncating title,
 * prominent metric figure, and optional muted caption.
 */
@SuppressWarnings({"serial", "this-escape"})
public class StatCard extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel captionLabel;
    private final Color accentColor;

    public StatCard(String title, long initialValue, Color accentColor) {
        this(title, initialValue, accentColor, "");
    }

    public StatCard(String title, long initialValue, Color accentColor, String caption) {
        this.accentColor = accentColor != null ? accentColor : UITheme.PRIMARY;
        setLayout(new BorderLayout(0, 4));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(UITheme.s(14), UITheme.s(14), UITheme.s(12), UITheme.s(14)));
        setPreferredSize(new Dimension(UITheme.s(150), UITheme.s(90)));
        setMinimumSize(new Dimension(UITheme.s(120), UITheme.s(84)));

        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(UITheme.FONT_BADGE);
        titleLabel.setForeground(UITheme.TEXT_MUTED);

        valueLabel = new JLabel(String.valueOf(initialValue));
        valueLabel.setFont(UITheme.FONT_METRIC);
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);

        captionLabel = new JLabel(caption == null ? "" : caption);
        captionLabel.setFont(UITheme.FONT_SMALL);
        captionLabel.setForeground(UITheme.TEXT_SUBTLE);

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        if (caption != null && !caption.isEmpty()) {
            add(captionLabel, BorderLayout.SOUTH);
        }
    }

    public void setValue(long value) {
        valueLabel.setText(String.valueOf(value));
        repaint();
    }

    public void setCaption(String caption) {
        captionLabel.setText(caption == null ? "" : caption);
        if (captionLabel.getParent() == null && caption != null && !caption.isEmpty()) {
            add(captionLabel, BorderLayout.SOUTH);
            revalidate();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Card background
        g2.setColor(UITheme.BG_CARD);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

        // Card border
        g2.setColor(UITheme.BORDER_CARD);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

        // Accent top indicator bar
        g2.setColor(accentColor);
        g2.fillRoundRect(1, 1, w - 2, 4, 8, 8);
        g2.fillRect(1, 3, w - 2, 2);

        g2.dispose();
        super.paintComponent(g);
    }
}
