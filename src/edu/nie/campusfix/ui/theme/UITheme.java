package edu.nie.campusfix.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Design token system providing modern typography, high-contrast accessible color palettes,
 * rounded borders, and UI styling utilities for CampusFix.
 */
public final class UITheme {

    // Primary Colors
    public static final Color PRIMARY = new Color(0x4F, 0x46, 0xE5);          // Indigo 600
    public static final Color PRIMARY_HOVER = new Color(0x43, 0x38, 0xCA);    // Indigo 700
    public static final Color PRIMARY_ACTIVE = new Color(0x37, 0x30, 0xA3);   // Indigo 800

    // Neutral / Canvas Colors
    public static final Color BG_CANVAS = new Color(0xF8, 0xFA, 0xFC);        // Slate 50
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BORDER_CARD = new Color(0xE2, 0xE8, 0xF0);      // Slate 200
    public static final Color BORDER_INPUT = new Color(0xCB, 0xD5, 0xE1);     // Slate 300

    // Dark Sidebar Palette
    public static final Color SIDEBAR_BG = new Color(0x0F, 0x17, 0x2A);       // Slate 900
    public static final Color SIDEBAR_HOVER = new Color(0x1E, 0x29, 0x3B);    // Slate 800
    public static final Color SIDEBAR_ACTIVE = new Color(0x33, 0x41, 0x55);   // Slate 700
    public static final Color SIDEBAR_TEXT = new Color(0x94, 0xA3, 0xB8);     // Slate 400
    public static final Color SIDEBAR_TEXT_ACTIVE = Color.WHITE;

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(0x0F, 0x17, 0x2A);     // Slate 900
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);       // Slate 500
    public static final Color TEXT_SUBTLE = new Color(0x94, 0xA3, 0xB8);      // Slate 400

    // Semantic Status Colors
    public static final Color DANGER = new Color(0xDC, 0x26, 0x26);           // Red 600
    public static final Color DANGER_HOVER = new Color(0xB9, 0x1C, 0x1C);     // Red 700
    public static final Color SUCCESS = new Color(0x10, 0xB9, 0x81);          // Emerald 500
    public static final Color SUCCESS_HOVER = new Color(0x05, 0x96, 0x69);    // Emerald 600
    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);          // Amber 500

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
    public static final Color BADGE_HIGH_BORDER = new Color(0xFF, 0xED, 0xD5);

    public static final Color BADGE_MEDIUM_BG = new Color(0xFE, 0xFC, 0xE8);
    public static final Color BADGE_MEDIUM_FG = new Color(0xA1, 0x62, 0x07);
    public static final Color BADGE_MEDIUM_BORDER = new Color(0xFE, 0xF0, 0x8A);

    public static final Color BADGE_LOW_BG = new Color(0xF0, 0xFD, 0xF4);
    public static final Color BADGE_LOW_FG = new Color(0x15, 0x80, 0x3D);
    public static final Color BADGE_LOW_BORDER = new Color(0xDC, 0xFC, 0xE7);

    // Typography
    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    public static final Font FONT_SUBTITLE = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    public static final Font FONT_SECTION = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font FONT_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    public static final Font FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font FONT_BADGE = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_METRIC = new Font(Font.SANS_SERIF, Font.BOLD, 32);

    private UITheme() {
        // Utility
    }

    /**
     * Initializes default look and feel with smooth anti-aliased font rendering hints.
     */
    public static void setupSystemLookAndFeel() {
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Label.foreground", TEXT_PRIMARY);
            UIManager.put("Label.font", FONT_REGULAR);
            UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
            UIManager.put("TabbedPane.selectedForeground", TEXT_PRIMARY);
            UIManager.put("TabbedPane.background", Color.WHITE);
            UIManager.put("TabbedPane.font", FONT_BOLD);
            UIManager.put("TextFieldUI", "javax.swing.plaf.basic.BasicTextFieldUI");
            UIManager.put("PasswordFieldUI", "javax.swing.plaf.basic.BasicPasswordFieldUI");
            UIManager.put("TextAreaUI", "javax.swing.plaf.basic.BasicTextAreaUI");
            UIManager.put("TextField.foreground", TEXT_PRIMARY);
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("TextField.font", FONT_REGULAR);
            UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
            UIManager.put("PasswordField.background", Color.WHITE);
            UIManager.put("PasswordField.font", FONT_REGULAR);
            UIManager.put("TextArea.foreground", TEXT_PRIMARY);
            UIManager.put("TextArea.background", Color.WHITE);
            UIManager.put("TextArea.font", FONT_REGULAR);
            UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
            UIManager.put("ComboBox.background", Color.WHITE);
            UIManager.put("ComboBox.font", FONT_REGULAR);
            UIManager.put("Button.font", FONT_BOLD);
            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("Table.font", FONT_REGULAR);
            UIManager.put("TableHeader.foreground", new Color(0x47, 0x55, 0x69));
            UIManager.put("TableHeader.background", BG_CANVAS);
            UIManager.put("TableHeader.font", FONT_BOLD);
            UIManager.put("CheckBox.font", FONT_REGULAR);
            UIManager.put("Panel.background", BG_CANVAS);
            UIManager.put("Panel.foreground", TEXT_PRIMARY);
        } catch (Exception ignored) {}
    }

    /**
     * Creates a card panel styled with rounded corners and light slate border.
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel() {
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
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return panel;
    }

    /**
     * Styles text field with padding, clean border, and modern font.
     */
    public static void styleTextField(JTextField tf) {
        if (tf instanceof javax.swing.JPasswordField) {
            tf.setUI(new javax.swing.plaf.basic.BasicPasswordFieldUI());
        } else {
            tf.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
        }
        tf.setFont(FONT_REGULAR);
        tf.setForeground(TEXT_PRIMARY);
        tf.setBackground(Color.WHITE);
        tf.setCaretColor(PRIMARY);
        Border line = BorderFactory.createLineBorder(BORDER_INPUT, 1);
        Border pad = BorderFactory.createEmptyBorder(8, 10, 8, 10);
        tf.setBorder(BorderFactory.createCompoundBorder(line, pad));
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 40));
    }

    /**
     * Styles combo box with modern font and dimensions.
     */
    public static void styleComboBox(JComboBox<?> cb) {
        cb.setFont(FONT_REGULAR);
        cb.setForeground(TEXT_PRIMARY);
        cb.setBackground(Color.WHITE);
        cb.setPreferredSize(new Dimension(cb.getPreferredSize().width, 40));
    }

    /**
     * Styles a label with the specified font and foreground color (defaults to FONT_REGULAR and TEXT_PRIMARY).
     */
    public static void styleLabel(JLabel label, Font font, Color fg) {
        if (label == null) return;
        label.setFont(font != null ? font : FONT_REGULAR);
        label.setForeground(fg != null ? fg : TEXT_PRIMARY);
    }

    /**
     * Styles text area with standard typography, high contrast colors, and primary caret.
     */
    public static void styleTextArea(JTextArea ta) {
        if (ta == null) return;
        ta.setFont(FONT_REGULAR);
        ta.setForeground(TEXT_PRIMARY);
        ta.setBackground(Color.WHITE);
        ta.setCaretColor(PRIMARY);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    /**
     * Styles tabbed pane with bold font, white background, and high contrast tab headers.
     */
    public static void styleTabbedPane(JTabbedPane tp) {
        if (tp == null) return;
        tp.setFont(FONT_BOLD);
        tp.setForeground(TEXT_PRIMARY);
        tp.setBackground(Color.WHITE);
        for (int i = 0; i < tp.getTabCount(); i++) {
            tp.setForegroundAt(i, TEXT_PRIMARY);
        }
    }
}
