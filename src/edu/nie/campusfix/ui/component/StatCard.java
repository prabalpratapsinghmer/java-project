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
 * Modern KPI statistic card with rounded borders, title, large metric number, and accent indicator.
 */
@SuppressWarnings({"serial", "this-escape"})
public class StatCard extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final Color accentColor;

    public StatCard(String title, long initialValue, Color accentColor) {
        this.accentColor = accentColor != null ? accentColor : UITheme.PRIMARY;
        setLayout(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setPreferredSize(new Dimension(170, 95));

        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(UITheme.FONT_BADGE);
        titleLabel.setForeground(UITheme.TEXT_MUTED);

        valueLabel = new JLabel(String.valueOf(initialValue));
        valueLabel.setFont(UITheme.FONT_METRIC);
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(long value) {
        valueLabel.setText(String.valueOf(value));
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
        g2.fillRoundRect(0, 0, w - 1, 4, 10, 10);
        g2.fillRect(0, 2, w - 1, 2);

        g2.dispose();
        super.paintComponent(g);
    }
}
