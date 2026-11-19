package edu.nie.campusfix.ui.component;

import edu.nie.campusfix.enums.ComplaintStatus;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Severity;
import edu.nie.campusfix.ui.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Comparator;
import java.util.List;

/**
 * Modern styled JTable featuring crisp headers, alternating row shading, direct-painted
 * anti-aliased pill badges for Priority/Status/Severity, and domain-aware column sorting.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ModernTable extends JTable {
    private static final long serialVersionUID = 1L;
    private static final Color ROW_ALT = new Color(0xF8, 0xFA, 0xFC);

    public ModernTable(TableModel model) {
        super(model);
        initStyle();
    }

    public ModernTable() {
        super();
        initStyle();
    }

    private void initStyle() {
        setRowHeight(UITheme.s(46));
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setGridColor(new Color(0xE2, 0xE8, 0xF0));
        setBackground(Color.WHITE);
        setForeground(UITheme.TEXT_PRIMARY);
        setSelectionBackground(UITheme.PRIMARY_SOFT);
        setSelectionForeground(UITheme.TEXT_PRIMARY);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFont(UITheme.FONT_REGULAR);
        setIntercellSpacing(new Dimension(0, 1));
        setFillsViewportHeight(true);
        setOpaque(true);

        // Explicit cross-platform header styling with sort indicator arrows
        JTableHeader header = getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, UITheme.s(42)));
        header.setBackground(UITheme.BG_SUBTLE);
        header.setForeground(UITheme.TEXT_SECONDARY);
        header.setFont(UITheme.FONT_SMALL_BOLD);
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new ModernHeaderRenderer());

        // Register direct-painted renderers (no broken child-panel layout!)
        setDefaultRenderer(ComplaintStatus.class, new DirectPillBadgeRenderer());
        setDefaultRenderer(Priority.class, new DirectPillBadgeRenderer());
        setDefaultRenderer(Severity.class, new DirectPillBadgeRenderer());
        setDefaultRenderer(Object.class, new ModernDefaultCellRenderer());
        setDefaultRenderer(String.class, new ModernDefaultCellRenderer());
        setDefaultRenderer(Long.class, new NumericCellRenderer());
        setDefaultRenderer(Integer.class, new NumericCellRenderer());
    }

    /**
     * Installs a domain-aware TableRowSorter sorting Priority by score and Status by lifecycle order.
     */
    public TableRowSorter<TableModel> enableDefaultSorting() {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(getModel());
        for (int col = 0; col < getModel().getColumnCount(); col++) {
            Class<?> colClass = getModel().getColumnClass(col);
            if (colClass == Priority.class) {
                sorter.setComparator(col, Comparator.comparingInt(p -> ((Priority) p).getScore()));
            } else if (colClass == ComplaintStatus.class) {
                sorter.setComparator(col, Comparator.comparingInt(s -> ((ComplaintStatus) s).ordinal()));
            } else if (colClass == Severity.class) {
                sorter.setComparator(col, Comparator.comparingInt(s -> ((Severity) s).getBaseScore()));
            }
        }
        setRowSorter(sorter);
        return sorter;
    }

    /**
     * Converts the currently selected view row index to the underlying model row index, or -1 if none.
     */
    public int getSelectedModelRow() {
        int viewRow = getSelectedRow();
        if (viewRow < 0) {
            return -1;
        }
        return convertRowIndexToModel(viewRow);
    }

    /**
     * Clean header cell renderer with sort indicator and bottom hairline border.
     */
    @SuppressWarnings("serial")
    private static class ModernHeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setOpaque(true);
            setBackground(UITheme.BG_SUBTLE);
            setForeground(UITheme.TEXT_SECONDARY);
            setFont(UITheme.FONT_SMALL_BOLD);
            setHorizontalAlignment(JLabel.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)
            ));

            String label = value == null ? "" : value.toString();
            if (table != null && table.getRowSorter() != null) {
                int modelCol = table.convertColumnIndexToModel(column);
                List<? extends RowSorter.SortKey> keys = table.getRowSorter().getSortKeys();
                for (RowSorter.SortKey key : keys) {
                    if (key.getColumn() == modelCol) {
                        if (key.getSortOrder() == SortOrder.ASCENDING) {
                            label += " ▲";
                        } else if (key.getSortOrder() == SortOrder.DESCENDING) {
                            label += " ▼";
                        }
                        break;
                    }
                }
            }
            setText(label);
            return this;
        }
    }

    /**
     * Default text cell renderer with clean padding, alternating row backgrounds, and tooltips.
     */
    @SuppressWarnings("serial")
    public static class ModernDefaultCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setFont(UITheme.FONT_REGULAR);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setOpaque(true);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(UITheme.TEXT_PRIMARY);
            } else {
                setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                setForeground(UITheme.TEXT_PRIMARY);
            }
            String text = value == null ? "" : value.toString();
            setText(text);
            setToolTipText(text.isEmpty() ? null : text);
            return this;
        }
    }

    /**
     * Clean numeric cell renderer (centered bold count).
     */
    @SuppressWarnings("serial")
    private static class NumericCellRenderer extends ModernDefaultCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(UITheme.FONT_BOLD);
            setForeground(UITheme.PRIMARY);
            setHorizontalAlignment(JLabel.LEFT);
            return this;
        }
    }

    /**
     * Direct-painted pill badge renderer for Priority, ComplaintStatus, and Severity cells.
     * Draws directly in paintComponent without child components so badges always render cleanly.
     */
    @SuppressWarnings("serial")
    public static class DirectPillBadgeRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private String badgeText = "";
        private PillBadge.BadgeColors badgeColors = new PillBadge.BadgeColors(
                UITheme.BG_SUBTLE, UITheme.TEXT_MUTED, UITheme.BORDER_CARD);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
            setOpaque(true);
            setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : ROW_ALT));

            if (value instanceof Priority prio) {
                badgeText = prio.getDisplayName();
                badgeColors = PillBadge.colorsForPriority(prio);
            } else if (value instanceof ComplaintStatus st) {
                badgeText = st.getDisplayName();
                badgeColors = PillBadge.colorsForStatus(st);
            } else if (value instanceof Severity sev) {
                badgeText = sev.getDisplayName();
                badgeColors = PillBadge.colorsForSeverity(sev);
            } else if (value != null) {
                badgeText = value.toString();
                badgeColors = new PillBadge.BadgeColors(UITheme.BG_SUBTLE, UITheme.TEXT_SECONDARY, UITheme.BORDER_CARD);
            } else {
                badgeText = "";
            }
            setToolTipText(badgeText.isEmpty() ? null : badgeText);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (!badgeText.isEmpty()) {
                g2.setFont(UITheme.FONT_BADGE);
                FontMetrics fm = g2.getFontMetrics();
                int pillW = Math.min(getWidth() - UITheme.s(14), Math.max(UITheme.s(66), fm.stringWidth(badgeText) + UITheme.s(22)));
                int pillH = UITheme.s(26);
                int x = UITheme.s(10);
                int y = (getHeight() - pillH) / 2;
                PillBadge.paintPill(g2, x, y, pillW, pillH, badgeText,
                        badgeColors.bg(), badgeColors.fg(), badgeColors.border(), UITheme.FONT_BADGE);
            }
            g2.dispose();
        }
    }

    /**
     * Direct-painted renderer for status text columns like Active/Inactive or Overdue status.
     */
    @SuppressWarnings("serial")
    public static class SemanticTagRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private String tagText = "";
        private PillBadge.BadgeColors colors = new PillBadge.BadgeColors(
                UITheme.BG_SUBTLE, UITheme.TEXT_SECONDARY, UITheme.BORDER_CARD);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
            setOpaque(true);
            setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : ROW_ALT));
            tagText = value == null ? "" : value.toString();

            String lower = tagText.toLowerCase();
            if (lower.startsWith("overdue") || lower.equals("inactive")) {
                colors = new PillBadge.BadgeColors(UITheme.BADGE_CRITICAL_BG, UITheme.BADGE_CRITICAL_FG, UITheme.BADGE_CRITICAL_BORDER);
            } else if (lower.equals("active") || lower.equals("resolved") || lower.equals("on track")) {
                colors = new PillBadge.BadgeColors(UITheme.BADGE_RESOLVED_BG, UITheme.BADGE_RESOLVED_FG, UITheme.BADGE_RESOLVED_BORDER);
            } else {
                colors = new PillBadge.BadgeColors(UITheme.BG_SUBTLE, UITheme.TEXT_SECONDARY, UITheme.BORDER_CARD);
            }
            setToolTipText(tagText.isEmpty() ? null : tagText);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (!tagText.isEmpty()) {
                g2.setFont(UITheme.FONT_BADGE);
                FontMetrics fm = g2.getFontMetrics();
                int pillW = Math.min(getWidth() - 16, Math.max(64, fm.stringWidth(tagText) + 18));
                int pillH = 24;
                int x = 10;
                int y = (getHeight() - pillH) / 2;
                PillBadge.paintPill(g2, x, y, pillW, pillH, tagText,
                        colors.bg(), colors.fg(), colors.border(), UITheme.FONT_BADGE);
            }
            g2.dispose();
        }
    }
}
