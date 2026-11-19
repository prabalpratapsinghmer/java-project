package edu.nie.campusfix.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Unified cross-platform design token system and Swing component styling utilities.
 * Uses CrossPlatformLookAndFeel with explicit light-mode tokens to guarantee crisp,
 * high-contrast rendering across Linux (dark or light GTK), Windows, and macOS.
 */
public final class UITheme {

    // Primary Brand Colors
    public static final Color PRIMARY = new Color(0x4F, 0x46, 0xE5);          // Indigo 600
    public static final Color PRIMARY_HOVER = new Color(0x43, 0x38, 0xCA);    // Indigo 700
    public static final Color PRIMARY_ACTIVE = new Color(0x37, 0x30, 0xA3);   // Indigo 800
    public static final Color PRIMARY_SOFT = new Color(0xEE, 0xF2, 0xFF);     // Indigo 50

    // Neutral / Canvas Colors
    public static final Color BG_CANVAS = new Color(0xF8, 0xFA, 0xFC);        // Slate 50
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BG_SUBTLE = new Color(0xF1, 0xF5, 0xF9);        // Slate 100
    public static final Color BORDER_CARD = new Color(0xE2, 0xE8, 0xF0);      // Slate 200
    public static final Color BORDER_INPUT = new Color(0xCB, 0xD5, 0xE1);     // Slate 300

    // Dark Slate Sidebar & Brand Panel Palette
    public static final Color SIDEBAR_BG = new Color(0x0F, 0x17, 0x2A);       // Slate 900
    public static final Color SIDEBAR_HOVER = new Color(0x1E, 0x29, 0x3B);    // Slate 800
    public static final Color SIDEBAR_ACTIVE = new Color(0x1E, 0x29, 0x3B);   // Slate 800
    public static final Color SIDEBAR_BORDER = new Color(0x33, 0x41, 0x55);   // Slate 700
    public static final Color SIDEBAR_TEXT = new Color(0xCB, 0xD5, 0xE1);     // Slate 300
    public static final Color SIDEBAR_MUTED = new Color(0x94, 0xA3, 0xB8);    // Slate 400
    public static final Color SIDEBAR_TEXT_ACTIVE = Color.WHITE;

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(0x0F, 0x17, 0x2A);     // Slate 900
    public static final Color TEXT_SECONDARY = new Color(0x33, 0x41, 0x55);   // Slate 700
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);       // Slate 500
    public static final Color TEXT_SUBTLE = new Color(0x94, 0xA3, 0xB8);      // Slate 400

    // Semantic Status Colors
    public static final Color DANGER = new Color(0xDC, 0x26, 0x26);           // Red 600
    public static final Color DANGER_HOVER = new Color(0xB9, 0x1C, 0x1C);     // Red 700
    public static final Color DANGER_SOFT = new Color(0xFE, 0xF2, 0xF2);      // Red 50
    public static final Color SUCCESS = new Color(0x05, 0x96, 0x69);          // Emerald 600
    public static final Color SUCCESS_HOVER = new Color(0x04, 0x78, 0x57);    // Emerald 700
    public static final Color WARNING = new Color(0xD9, 0x77, 0x06);          // Amber 600
    public static final Color WARNING_SOFT = new Color(0xFF, 0xFB, 0xEB);     // Amber 50
    public static final Color INFO = new Color(0x02, 0x84, 0xC7);             // Sky 600

    // Semantic Badge Colors (Pills)
    public static final Color BADGE_PENDING_BG = new Color(0xF1, 0xF5, 0xF9);
    public static final Color BADGE_PENDING_FG = new Color(0x47, 0x55, 0x69);
    public static final Color BADGE_PENDING_BORDER = new Color(0xCB, 0xD5, 0xE1);

    public static final Color BADGE_ASSIGNED_BG = new Color(0xEF, 0xF6, 0xFF);
    public static final Color BADGE_ASSIGNED_FG = new Color(0x1D, 0x4E, 0xD8);
    public static final Color BADGE_ASSIGNED_BORDER = new Color(0xBF, 0xDB, 0xFE);

    public static final Color BADGE_IN_PROGRESS_BG = new Color(0xFF, 0xFB, 0xEB);
    public static final Color BADGE_IN_PROGRESS_FG = new Color(0xB4, 0x53, 0x09);
    public static final Color BADGE_IN_PROGRESS_BORDER = new Color(0xFD, 0xE6, 0x8A);

    public static final Color BADGE_RESOLVED_BG = new Color(0xEC, 0xFD, 0xF5);
    public static final Color BADGE_RESOLVED_FG = new Color(0x04, 0x78, 0x57);
    public static final Color BADGE_RESOLVED_BORDER = new Color(0xA7, 0xF3, 0xD0);

    public static final Color BADGE_CRITICAL_BG = new Color(0xFE, 0xF2, 0xF2);
    public static final Color BADGE_CRITICAL_FG = new Color(0xB9, 0x1C, 0x1C);
    public static final Color BADGE_CRITICAL_BORDER = new Color(0xFE, 0xCA, 0xCA);

    public static final Color BADGE_HIGH_BG = new Color(0xFF, 0xF7, 0xED);
    public static final Color BADGE_HIGH_FG = new Color(0xC2, 0x41, 0x0C);
    public static final Color BADGE_HIGH_BORDER = new Color(0xFE, 0xD7, 0xAA);

    public static final Color BADGE_MEDIUM_BG = new Color(0xFE, 0xFC, 0xE8);
    public static final Color BADGE_MEDIUM_FG = new Color(0xA1, 0x62, 0x07);
    public static final Color BADGE_MEDIUM_BORDER = new Color(0xFD, 0xE0, 0x47);

    public static final Color BADGE_LOW_BG = new Color(0xF0, 0xFD, 0xF4);
    public static final Color BADGE_LOW_FG = new Color(0x15, 0x80, 0x3D);
    public static final Color BADGE_LOW_BORDER = new Color(0xBB, 0xF7, 0xD0);

    // Automatic fallback scale factor for unscaled HiDPI monitors when launched without -Dsun.java2d.uiScale
    public static final double UI_SCALE = detectFallbackScale();

    private static double detectFallbackScale() {
        try {
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                return 1.0;
            }
            java.awt.GraphicsDevice gd = java.awt.GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice();
            double sysScale = gd.getDefaultConfiguration().getDefaultTransform().getScaleX();
            if (sysScale > 1.05) {
                // JVM/OS HiDPI scaling is already active; avoid double-scaling
                return 1.0;
            }
            int screenWidth = gd.getDisplayMode().getWidth();
            if (screenWidth >= 3400) {
                return 1.65;
            } else if (screenWidth >= 2400) {
                return 1.45;
            } else if (screenWidth >= 2000) {
                return 1.25;
            }
        } catch (Exception e) {
            System.err.println("[CampusFix] Display scale detection fallback: " + e.getMessage());
        }
        return 1.0;
    }

    /**
     * Scales a base pixel dimension cleanly for unscaled HiDPI displays.
     */
    public static int s(int px) {
        return (int) Math.round(px * UI_SCALE);
    }

    // Typography Scale (generous, high-readability desktop sizes)
    public static final Font FONT_HERO = new Font(Font.SANS_SERIF, Font.BOLD, s(32));
    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, s(24));
    public static final Font FONT_SUBTITLE = new Font(Font.SANS_SERIF, Font.BOLD, s(18));
    public static final Font FONT_SECTION = new Font(Font.SANS_SERIF, Font.BOLD, s(16));
    public static final Font FONT_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, s(15));
    public static final Font FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, s(15));
    public static final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, s(13));
    public static final Font FONT_SMALL_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, s(13));
    public static final Font FONT_BADGE = new Font(Font.SANS_SERIF, Font.BOLD, s(12));
    public static final Font FONT_METRIC = new Font(Font.SANS_SERIF, Font.BOLD, s(30));
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, s(13));

    private UITheme() {
        // Utility
    }

    /**
     * Initializes Cross-Platform Look & Feel with explicit light-mode defaults and anti-aliased fonts.
     * Avoids Linux GTK dark-theme collisions that cause unreadable tabs and dark controls.
     */
    public static void setupSystemLookAndFeel() {
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

            UIManager.put("control", BG_CANVAS);
            UIManager.put("Panel.background", BG_CANVAS);
            UIManager.put("Panel.foreground", TEXT_PRIMARY);
            UIManager.put("Viewport.background", Color.WHITE);
            UIManager.put("ScrollPane.background", Color.WHITE);

            UIManager.put("Label.foreground", TEXT_PRIMARY);
            UIManager.put("Label.font", FONT_REGULAR);

            UIManager.put("OptionPane.background", Color.WHITE);
            UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
            UIManager.put("OptionPane.messageFont", FONT_REGULAR);
            UIManager.put("OptionPane.buttonFont", FONT_BOLD);

            UIManager.put("Button.background", Color.WHITE);
            UIManager.put("Button.foreground", TEXT_PRIMARY);
            UIManager.put("Button.font", FONT_BOLD);
            UIManager.put("Button.select", BG_SUBTLE);
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));

            UIManager.put("TextField.foreground", TEXT_PRIMARY);
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("TextField.caretForeground", PRIMARY);
            UIManager.put("TextField.font", FONT_REGULAR);
            UIManager.put("TextField.selectionBackground", PRIMARY_SOFT);
            UIManager.put("TextField.selectionForeground", TEXT_PRIMARY);

            UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
            UIManager.put("PasswordField.background", Color.WHITE);
            UIManager.put("PasswordField.caretForeground", PRIMARY);
            UIManager.put("PasswordField.font", FONT_REGULAR);

            UIManager.put("TextArea.foreground", TEXT_PRIMARY);
            UIManager.put("TextArea.background", Color.WHITE);
            UIManager.put("TextArea.caretForeground", PRIMARY);
            UIManager.put("TextArea.font", FONT_REGULAR);

            UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
            UIManager.put("ComboBox.background", Color.WHITE);
            UIManager.put("ComboBox.selectionBackground", PRIMARY_SOFT);
            UIManager.put("ComboBox.selectionForeground", PRIMARY);
            UIManager.put("ComboBox.buttonBackground", Color.WHITE);
            UIManager.put("ComboBox.font", FONT_REGULAR);

            UIManager.put("List.background", Color.WHITE);
            UIManager.put("List.foreground", TEXT_PRIMARY);
            UIManager.put("List.selectionBackground", PRIMARY_SOFT);
            UIManager.put("List.selectionForeground", PRIMARY);
            UIManager.put("List.font", FONT_REGULAR);

            UIManager.put("CheckBox.background", Color.WHITE);
            UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
            UIManager.put("CheckBox.font", FONT_REGULAR);

            UIManager.put("TabbedPane.background", BG_CANVAS);
            UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
            UIManager.put("TabbedPane.selected", Color.WHITE);
            UIManager.put("TabbedPane.contentAreaColor", Color.WHITE);
            UIManager.put("TabbedPane.font", FONT_BOLD);

            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("Table.font", FONT_REGULAR);
            UIManager.put("Table.selectionBackground", PRIMARY_SOFT);
            UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
            UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());

            UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
            UIManager.put("TableHeader.background", BG_SUBTLE);
            UIManager.put("TableHeader.font", FONT_SMALL_BOLD);

            UIManager.put("ToolTip.background", SIDEBAR_BG);
            UIManager.put("ToolTip.foreground", Color.WHITE);
            UIManager.put("ToolTip.font", FONT_SMALL);
            UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(4, 8, 4, 8));
        } catch (Exception e) {
            System.err.println("[CampusFix] Warning: Could not configure Swing Look & Feel: " + e.getMessage());
        }
    }

    /**
     * Creates a white surface card panel with rounded corners and crisp slate border.
     */
    public static JPanel createCardPanel() {
        return createCardPanel(16, 18);
    }

    /**
     * Creates a white surface card panel with customizable inner padding.
     */
    public static JPanel createCardPanel(int padVertical, int padHorizontal) {
        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(BORDER_CARD);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(padVertical, padHorizontal, padVertical, padHorizontal));
        return panel;
    }

    /**
     * Styles a text field or password field with clean padding and focus-ring feedback.
     */
    public static void styleTextField(JTextField tf) {
        if (tf instanceof JPasswordField) {
            tf.setUI(new BasicPasswordFieldUI());
        } else {
            tf.setUI(new BasicTextFieldUI());
        }
        tf.setFont(FONT_REGULAR);
        tf.setForeground(TEXT_PRIMARY);
        tf.setBackground(Color.WHITE);
        tf.setCaretColor(PRIMARY);
        tf.setOpaque(true);

        Border unfocused = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_INPUT, 1),
                BorderFactory.createEmptyBorder(s(8), s(12), s(8), s(12))
        );
        Border focused = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 2),
                BorderFactory.createEmptyBorder(s(7), s(11), s(7), s(11))
        );
        tf.setBorder(unfocused);
        tf.setPreferredSize(new Dimension(Math.max(s(140), tf.getPreferredSize().width), s(44)));

        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.setBorder(focused);
            }

            @Override
            public void focusLost(FocusEvent e) {
                tf.setBorder(unfocused);
            }
        });
    }

    /**
     * Styles a combo box with crisp white background, slate border, and custom chevron arrow.
     */
    public static void styleComboBox(JComboBox<?> cb) {
        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Color.WHITE);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        Polygon tri = new Polygon(
                                new int[]{cx - 4, cx + 4, cx},
                                new int[]{cy - 2, cy - 2, cy + 3},
                                3
                        );
                        g2.setColor(TEXT_MUTED);
                        g2.fillPolygon(tri);
                        g2.dispose();
                    }
                };
                arrow.setBorder(BorderFactory.createEmptyBorder());
                arrow.setFocusable(false);
                arrow.setContentAreaFilled(false);
                arrow.setPreferredSize(new Dimension(26, 36));
                return arrow;
            }
        });
        cb.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(FONT_REGULAR);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                if (isSelected) {
                    setBackground(PRIMARY_SOFT);
                    setForeground(PRIMARY);
                } else {
                    setBackground(Color.WHITE);
                    setForeground(TEXT_PRIMARY);
                }
                return this;
            }
        });
        cb.setFont(FONT_REGULAR);
        cb.setForeground(TEXT_PRIMARY);
        cb.setBackground(Color.WHITE);
        cb.setOpaque(true);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_INPUT, 1));
        cb.setPreferredSize(new Dimension(Math.max(s(140), cb.getPreferredSize().width + s(20)), s(42)));
    }

    /**
     * Styles a checkbox cleanly for light card surfaces.
     */
    public static void styleCheckBox(JCheckBox cb, Color fg) {
        cb.setFont(FONT_SMALL_BOLD);
        cb.setForeground(fg != null ? fg : TEXT_PRIMARY);
        cb.setOpaque(false);
        cb.setFocusPainted(false);
    }

    /**
     * Styles a text area with standard typography, high contrast colors, and primary caret.
     */
    public static void styleTextArea(JTextArea ta) {
        if (ta == null) return;
        ta.setFont(FONT_REGULAR);
        ta.setForeground(TEXT_PRIMARY);
        ta.setBackground(Color.WHITE);
        ta.setCaretColor(PRIMARY);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    /**
     * Styles a scroll pane with a white viewport, crisp border, and sleek thin scrollbars.
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        if (scrollPane == null) return;
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_CARD, 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        applyModernScrollBar(scrollPane.getVerticalScrollBar());
        applyModernScrollBar(scrollPane.getHorizontalScrollBar());
    }

    private static void applyModernScrollBar(JScrollBar bar) {
        if (bar == null) return;
        bar.setPreferredSize(new Dimension(10, 10));
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(0xCB, 0xD5, 0xE1);
                trackColor = BG_CANVAS;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics g, javax.swing.JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragging ? TEXT_MUTED : thumbColor);
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                        thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, javax.swing.JComponent c, Rectangle trackBounds) {
                g.setColor(trackColor);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
        });
    }

    /**
     * Styles a label with the specified font and foreground color.
     */
    public static void styleLabel(JLabel label, Font font, Color fg) {
        if (label == null) return;
        label.setFont(font != null ? font : FONT_REGULAR);
        label.setForeground(fg != null ? fg : TEXT_PRIMARY);
    }

    /**
     * Styles tabbed pane with bold font and high-contrast light colors.
     */
    public static void styleTabbedPane(JTabbedPane tp) {
        if (tp == null) return;
        tp.setFont(FONT_BOLD);
        tp.setForeground(TEXT_PRIMARY);
        tp.setBackground(Color.WHITE);
        for (int i = 0; i < tp.getTabCount(); i++) {
            tp.setForegroundAt(i, TEXT_PRIMARY);
            tp.setBackgroundAt(i, BG_SUBTLE);
        }
    }

    /**
     * Creates a consistent page section header with title and muted subtitle.
     */
    public static JPanel createPageHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout(0, 3));
        header.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_TITLE);
        titleLbl.setForeground(TEXT_PRIMARY);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(FONT_SMALL);
        subLbl.setForeground(TEXT_MUTED);

        header.add(titleLbl, BorderLayout.NORTH);
        header.add(subLbl, BorderLayout.SOUTH);
        return header;
    }

    /**
     * Creates a clean centered empty-state panel shown when tables or lists have no rows.
     */
    public static JPanel createEmptyStatePanel(String headline, String description) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createLineBorder(BORDER_CARD, 1));

        JPanel inner = new JPanel(new BorderLayout(0, 6));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JLabel headLbl = new JLabel(headline, SwingConstants.CENTER);
        headLbl.setFont(FONT_SUBTITLE);
        headLbl.setForeground(TEXT_SECONDARY);

        JLabel descLbl = new JLabel("<html><div style='text-align:center; width:380px;'>" + description + "</div></html>", SwingConstants.CENTER);
        descLbl.setFont(FONT_SMALL);
        descLbl.setForeground(TEXT_MUTED);

        inner.add(headLbl, BorderLayout.NORTH);
        inner.add(descLbl, BorderLayout.CENTER);
        outer.add(inner);
        return outer;
    }

    /**
     * Creates a clean metric bar row for visual breakdowns in reports and dashboards.
     */
    public static JPanel createMetricBarRow(String label, long count, long maxCount, Color barColor) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(FONT_SMALL_BOLD);
        nameLbl.setForeground(TEXT_SECONDARY);

        JLabel valLbl = new JLabel(String.valueOf(count));
        valLbl.setFont(FONT_SMALL_BOLD);
        valLbl.setForeground(TEXT_PRIMARY);

        top.add(nameLbl, BorderLayout.WEST);
        top.add(valLbl, BorderLayout.EAST);

        double ratio = maxCount > 0 ? Math.min(1.0, (double) count / maxCount) : 0.0;
        Color fill = barColor != null ? barColor : PRIMARY;

        JPanel barTrack = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(BG_SUBTLE);
                g2.fillRoundRect(0, 0, w, h, h, h);
                if (ratio > 0) {
                    int fillW = Math.max(h, (int) Math.round(w * ratio));
                    g2.setColor(fill);
                    g2.fillRoundRect(0, 0, Math.min(w, fillW), h, h, h);
                }
                g2.dispose();
            }
        };
        barTrack.setOpaque(false);
        barTrack.setPreferredSize(new Dimension(100, 7));

        row.add(top, BorderLayout.NORTH);
        row.add(barTrack, BorderLayout.SOUTH);
        return row;
    }
}
