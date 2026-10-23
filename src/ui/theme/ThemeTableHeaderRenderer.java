package ui.theme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import ui.StyleConstants;

/**
 * Modern, theme-aware table header renderer for SportGrid.
 *
 * Completely eliminates default Swing / macOS Aqua heavy 3D beveled borders:
 * - Light Mode: Clean light header background (#F1F5F9) with dark, bold readable column labels (#0F172A, 14.5:1 contrast).
 * - Dark Mode: Deep navy header (#0D1121) with readable muted slate labels (#8E9DC0, 7.1:1 contrast).
 * - Subtle 1px bottom separator (#E2E8F0 in light, #32416E in dark) replacing heavy grid lines.
 * - Dynamic column alignment matching underlying cell data (left, center, right).
 * - Sort order indicators (▲ / ▼) when sorting is active.
 */
public class ThemeTableHeaderRenderer extends DefaultTableCellRenderer {

    public static final Color LIGHT_HEADER_BG = new Color(241, 245, 249); // Tailwind slate-100
    public static final Color LIGHT_HEADER_FG = new Color(15, 23, 42);    // Tailwind slate-900 (dark, readable)

    public ThemeTableHeaderRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Theme t = ThemeManager.getTheme();
        setFont(StyleConstants.FONT_HEADING);

        // Header colors
        if (t.isDark()) {
            setBackground(t.getBgMain());
            setForeground(t.getTextMuted());
        } else {
            setBackground(LIGHT_HEADER_BG);
            setForeground(LIGHT_HEADER_FG);
        }

        // Clean flat separator at the bottom and comfortable padding (no vertical 3D bevels)
        setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorder()),
                new EmptyBorder(8, 12, 8, 12)
        ));

        // Determine column alignment to match data column
        int alignment = SwingConstants.LEFT;
        if (table != null && column >= 0 && column < table.getColumnCount()) {
            try {
                TableCellRenderer cellRenderer = table.getColumnModel().getColumn(column).getCellRenderer();
                if (cellRenderer == null && table.getRowCount() > 0) {
                    cellRenderer = table.getCellRenderer(0, column);
                }
                if (cellRenderer instanceof StyleConstants.StripedTableCellRenderer) {
                    alignment = ((StyleConstants.StripedTableCellRenderer) cellRenderer).getHorizontalAlignment();
                } else if (cellRenderer instanceof JLabel) {
                    alignment = ((JLabel) cellRenderer).getHorizontalAlignment();
                }
            } catch (Exception ignored) {}
        }
        setHorizontalAlignment(alignment);

        // Append sort indicator arrow if TableRowSorter is sorting this column
        String text = (value != null) ? value.toString() : "";
        if (table != null && table.getRowSorter() != null) {
            try {
                List<? extends RowSorter.SortKey> sortKeys = table.getRowSorter().getSortKeys();
                if (sortKeys != null && !sortKeys.isEmpty()) {
                    RowSorter.SortKey key = sortKeys.get(0);
                    int modelCol = table.convertColumnIndexToModel(column);
                    if (key.getColumn() == modelCol) {
                        text += (key.getSortOrder() == SortOrder.ASCENDING) ? "  ▲" : "  ▼";
                    }
                }
            } catch (Exception ignored) {}
        }
        setText(text);

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paintComponent(g);
    }
}
