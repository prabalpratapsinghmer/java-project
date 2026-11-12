package edu.nie.campusfix.ui.component;

import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Modern styled JTable with 38px row heights, custom header styling, alternating row colors,
 * and automatic pill badge renderers for status, priority, and severity columns.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ModernTable extends JTable {
    private static final long serialVersionUID = 1L;

    public ModernTable(TableModel model) {
        super(model);
        initStyle();
    }

    public ModernTable() {
        super();
        initStyle();
    }

    private void initStyle() {
        setRowHeight(44);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setGridColor(new Color(0xF1, 0xF5, 0xF9));
        setSelectionBackground(new Color(0xEE, 0xF2, 0xFF));
        setSelectionForeground(UITheme.TEXT_PRIMARY);
        setFont(UITheme.FONT_REGULAR);
        setIntercellSpacing(new Dimension(0, 1));
        setFillsViewportHeight(true);

        // Header style
        JTableHeader header = getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        header.setForeground(new Color(0x47, 0x55, 0x69));
        header.setBackground(UITheme.BG_CANVAS);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD));
        header.setReorderingAllowed(false);

        // Register custom renderers
        setDefaultRenderer(ComplaintStatus.class, new StatusBadgeRenderer());
        setDefaultRenderer(Priority.class, new PriorityBadgeRenderer());
        setDefaultRenderer(Severity.class, new SeverityBadgeRenderer());
        setDefaultRenderer(Object.class, new ModernDefaultCellRenderer());
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (!isRowSelected(row)) {
            c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFA, 0xFC));
        }
        return c;
    }

    /**
     * Default cell renderer with clean padding and text colors.
     */
    @SuppressWarnings({"serial", "this-escape"})
    public static class ModernDefaultCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public ModernDefaultCellRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(UITheme.FONT_REGULAR);
            if (!isSelected) {
                setForeground(UITheme.TEXT_PRIMARY);
            }
            return this;
        }
    }

    /**
     * Renders ComplaintStatus as an anti-aliased PillBadge.
     */
    public static class StatusBadgeRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new BorderLayout());

        public StatusBadgeRenderer() {
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            panel.removeAll();
            panel.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFA, 0xFC)));
            if (value instanceof ComplaintStatus status) {
                PillBadge badge = PillBadge.createForStatus(status);
                panel.add(badge, BorderLayout.WEST);
            } else if (value != null) {
                JLabel fallback = new JLabel(value.toString());
                fallback.setFont(UITheme.FONT_REGULAR);
                fallback.setForeground(UITheme.TEXT_PRIMARY);
                panel.add(fallback, BorderLayout.WEST);
            }
            return panel;
        }
    }

    /**
     * Renders Priority as an anti-aliased PillBadge.
     */
    public static class PriorityBadgeRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new BorderLayout());

        public PriorityBadgeRenderer() {
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            panel.removeAll();
            panel.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFA, 0xFC)));
            if (value instanceof Priority priority) {
                PillBadge badge = PillBadge.createForPriority(priority);
                panel.add(badge, BorderLayout.WEST);
            } else if (value != null) {
                JLabel fallback = new JLabel(value.toString());
                fallback.setFont(UITheme.FONT_REGULAR);
                fallback.setForeground(UITheme.TEXT_PRIMARY);
                panel.add(fallback, BorderLayout.WEST);
            }
            return panel;
        }
    }

    /**
     * Renders Severity as an anti-aliased PillBadge.
     */
    public static class SeverityBadgeRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new BorderLayout());

        public SeverityBadgeRenderer() {
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            panel.removeAll();
            panel.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFA, 0xFC)));
            if (value instanceof Severity severity) {
                PillBadge badge = PillBadge.createForSeverity(severity);
                panel.add(badge, BorderLayout.WEST);
            } else if (value != null) {
                JLabel fallback = new JLabel(value.toString());
                fallback.setFont(UITheme.FONT_REGULAR);
                fallback.setForeground(UITheme.TEXT_PRIMARY);
                panel.add(fallback, BorderLayout.WEST);
            }
            return panel;
        }
    }
}
