package ui;

import ui.theme.CustomScrollBarUI;
import ui.theme.Theme;
import ui.theme.ThemeComboBoxRenderer;
import ui.theme.ThemeComboBoxUI;
import ui.theme.ThemeManager;
import ui.theme.ThemeMode;
import ui.theme.ThemeTabbedPaneUI;
import ui.theme.ThemeTableHeaderRenderer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Central styling hub and design system for SportGrid.
 *
 * Theme Engine:
 *  - Fully functional Light Mode & Dark Mode with instant runtime switching.
 *  - High-contrast, WCAG AA compliant colors across both themes.
 *  - State-aware interactive widgets (Normal, Hover, Pressed, Focused, and Disabled).
 *  - Drop-in theme toggle pill button for window headers.
 *  - Custom combo box and scrollbar renderers eliminating platform contrast issues on macOS.
 */
public final class StyleConstants {

    private StyleConstants() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Dynamic Theme Color Accessors (Backwards-compatible static fields)
    // ─────────────────────────────────────────────────────────────────────────
    public static Color BG_DARK        = ThemeManager.getTheme().getBgMain();
    public static Color BG_CARD        = ThemeManager.getTheme().getBgCard();
    public static Color BG_CARD_ALT    = ThemeManager.getTheme().getBgCardAlt();
    public static Color BG_FIELD       = ThemeManager.getTheme().getBgField();
    public static Color ACCENT_GREEN   = ThemeManager.getTheme().getAccentGreen();
    public static Color ACCENT_GREEN_D = ThemeManager.getTheme().getAccentGreenHover();
    public static Color TEXT_PRIMARY   = ThemeManager.getTheme().getTextPrimary();
    public static Color TEXT_MUTED     = ThemeManager.getTheme().getTextMuted();
    public static Color TEXT_ERROR     = ThemeManager.getTheme().getStatusError();
    public static Color BORDER_COLOR   = ThemeManager.getTheme().getBorder();
    public static Color TABLE_ROW_ALT  = ThemeManager.getTheme().getTableRowAlt();

    /**
     * Re-synchronizes the legacy static color references when the theme switches.
     */
    public static void updateStaticColors() {
        Theme t = ThemeManager.getTheme();
        BG_DARK        = t.getBgMain();
        BG_CARD        = t.getBgCard();
        BG_CARD_ALT    = t.getBgCardAlt();
        BG_FIELD       = t.getBgField();
        ACCENT_GREEN   = t.getAccentGreen();
        ACCENT_GREEN_D = t.getAccentGreenHover();
        TEXT_PRIMARY   = t.getTextPrimary();
        TEXT_MUTED     = t.getTextMuted();
        TEXT_ERROR     = t.getStatusError();
        BORDER_COLOR   = t.getBorder();
        TABLE_ROW_ALT  = t.getTableRowAlt();
    }

    public static Theme theme() {
        return ThemeManager.getTheme();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Typography
    // ─────────────────────────────────────────────────────────────────────────
    private static Font getSafeFont(int style, int size) {
        String[] preferredFamilies = {
                "Helvetica Neue", "Helvetica", "Arial", "Segoe UI", "DejaVu Sans", Font.DIALOG
        };
        try {
            Set<String> available = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String family : preferredFamilies) {
                if (available.contains(family)) {
                    return new Font(family, style, size);
                }
            }
        } catch (Exception ignored) {}
        return new Font(Font.DIALOG, style, size);
    }

    public static final Font FONT_TITLE   = getSafeFont(Font.BOLD,  22);
    public static final Font FONT_HEADING = getSafeFont(Font.BOLD,  15);
    public static final Font FONT_BODY    = getSafeFont(Font.PLAIN, 13);
    public static final Font FONT_SMALL   = getSafeFont(Font.PLAIN, 12);
    public static final Font FONT_BUTTON  = getSafeFont(Font.BOLD,  13);

    // Borders
    public static final Border PADDING_MEDIUM = new EmptyBorder(12, 16, 12, 16);
    public static final Border PADDING_LARGE  = new EmptyBorder(24, 32, 24, 32);

    // ─────────────────────────────────────────────────────────────────────────
    // Factory Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a state-aware primary button.
     * Displays proper disabled background/text, hover/pressed feedback, and focus ring.
     */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Theme t = ThemeManager.getTheme();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                int w = getWidth();
                int h = getHeight();

                if (!isEnabled()) {
                    g2.setColor(t.getBtnDisabledBg());
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    setForeground(t.getBtnDisabledText());
                } else {
                    Color fill = getModel().isPressed() ? t.getAccentGreenHover()
                               : getModel().isRollover() ? t.getAccentGreenHover() : t.getAccentGreen();
                    g2.setColor(fill);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    setForeground(t.getAccentText());

                    // Focus ring
                    if (hasFocus()) {
                        g2.setColor(t.getTextPrimary());
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
                    }
                }
                g2.dispose();

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paintComponent(g);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                FontMetrics fm = getFontMetrics(getFont());
                int textW = (getText() != null) ? fm.stringWidth(getText()) : 0;
                int width = Math.max(d.width + 12, textW + 42);
                int height = Math.max(d.height, 40);
                return new Dimension(width, height);
            }
        };

        btn.setFont(FONT_BUTTON);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.setMargin(new Insets(8, 18, 8, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Creates a state-aware secondary outlined button.
     * Displays proper disabled outline/text, hover fill, and focus ring.
     */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Theme t = ThemeManager.getTheme();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                int w = getWidth();
                int h = getHeight();

                if (!isEnabled()) {
                    g2.setColor(t.getBgCard());
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(t.getBorderSubtle());
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                    setForeground(t.getBtnDisabledText());
                } else {
                    g2.setColor(getModel().isRollover() ? t.getBgField() : t.getBgCard());
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(t.getAccentGreen());
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                    setForeground(t.getAccentGreen());

                    // Focus ring
                    if (hasFocus()) {
                        g2.setColor(t.getBorderFocus());
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
                    }
                }
                g2.dispose();

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paintComponent(g);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                FontMetrics fm = getFontMetrics(getFont());
                int textW = (getText() != null) ? fm.stringWidth(getText()) : 0;
                int width = Math.max(d.width + 12, textW + 42);
                int height = Math.max(d.height, 40);
                return new Dimension(width, height);
            }
        };

        btn.setFont(FONT_BUTTON);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.setMargin(new Insets(8, 18, 8, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Creates a state-aware danger / cancellation outlined button.
     * Displays error/red border and text when enabled, with hover feedback and focus ring.
     */
    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Theme t = ThemeManager.getTheme();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                int w = getWidth();
                int h = getHeight();

                if (!isEnabled()) {
                    g2.setColor(t.getBgCard());
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(t.getBorderSubtle());
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                    setForeground(t.getBtnDisabledText());
                } else {
                    Color errorCol = t.getStatusError();
                    Color bgHover = new Color(errorCol.getRed(), errorCol.getGreen(), errorCol.getBlue(), 35);
                    g2.setColor(getModel().isRollover() ? bgHover : t.getBgCard());
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(errorCol);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                    setForeground(errorCol);

                    // Focus ring
                    if (hasFocus()) {
                        g2.setColor(errorCol);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
                    }
                }
                g2.dispose();

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paintComponent(g);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                FontMetrics fm = getFontMetrics(getFont());
                int textW = (getText() != null) ? fm.stringWidth(getText()) : 0;
                int width = Math.max(d.width + 12, textW + 36);
                int height = Math.max(d.height, 38);
                return new Dimension(width, height);
            }
        };

        btn.setFont(FONT_BUTTON);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setMargin(new Insets(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Creates a self-contained Theme Toggle Button pill.
     * Ready to insert into any navbar or header. Displays "☀️ Light" when in Dark mode,
     * or "🌙 Dark" when in Light mode. Clicking immediately toggles the active theme.
     */
    public static JButton themeToggleButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Theme t = ThemeManager.getTheme();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(getModel().isRollover() ? t.getBgField() : t.getBgCard());
                g2.fillRoundRect(0, 0, w, h, 18, 18);
                g2.setColor(t.getBorder());
                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
                g2.dispose();

                setForeground(t.getTextPrimary());
                super.paintComponent(g);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(110, 36);
            }
        };

        Runnable updateLabel = () -> {
            boolean dark = ThemeManager.isDark();
            btn.setText(dark ? "☀️ Light" : "🌙 Dark");
            btn.setToolTipText(dark ? "Switch to Light Theme" : "Switch to Dark Theme");
        };
        updateLabel.run();

        btn.setFont(FONT_BUTTON);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            ThemeManager.toggleTheme();
            updateLabel.run();
        });

        // Register to update label whenever theme changes globally
        ThemeManager.addListener(newTheme -> updateLabel.run());

        return btn;
    }

    /**
     * Creates an accessible text field with hover feedback and prominent focus indicator.
     */
    public static JTextField styledField() {
        JTextField field = new JTextField() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.max(d.width, 180), Math.max(d.height, 38));
            }
        };

        applyFieldStyling(field);
        return field;
    }

    /**
     * Creates an accessible password field with hover feedback and focus indicator.
     */
    public static JPasswordField styledPasswordField() {
        JPasswordField field = new JPasswordField() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.max(d.width, 180), Math.max(d.height, 38));
            }
        };

        applyFieldStyling(field);
        return field;
    }

    private static void applyFieldStyling(JTextField field) {
        Theme t = ThemeManager.getTheme();
        field.setBackground(t.getBgField());
        field.setForeground(t.getTextPrimary());
        field.setCaretColor(t.getAccentGreen());
        field.setFont(FONT_BODY);

        Border normalBorder = new CompoundBorder(
                new LineBorder(t.getBorder(), 1, true),
                new EmptyBorder(6, 12, 6, 12));
        Border focusBorder = new CompoundBorder(
                new LineBorder(t.getBorderFocus(), 2, true),
                new EmptyBorder(5, 11, 5, 11));

        field.setBorder(normalBorder);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(new CompoundBorder(
                        new LineBorder(ThemeManager.getTheme().getBorderFocus(), 2, true),
                        new EmptyBorder(5, 11, 5, 11)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(new CompoundBorder(
                        new LineBorder(ThemeManager.getTheme().getBorder(), 1, true),
                        new EmptyBorder(6, 12, 6, 12)));
            }
        });

        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!field.hasFocus()) {
                    field.setBorder(new CompoundBorder(
                            new LineBorder(ThemeManager.getTheme().getBorderFocus(), 1, true),
                            new EmptyBorder(6, 12, 6, 12)));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!field.hasFocus()) {
                    field.setBorder(new CompoundBorder(
                            new LineBorder(ThemeManager.getTheme().getBorder(), 1, true),
                            new EmptyBorder(6, 12, 6, 12)));
                }
            }
        });
    }

    /**
     * Adds an accessible, subtle watermark placeholder to an input field.
     */
    public static void addPlaceholder(JTextField field, String placeholder) {
        field.putClientProperty("placeholder", placeholder);
    }

    /** Creates a standard form label with theme-aware muted text color. */
    public static JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(ThemeManager.getTheme().getTextMuted());
        return lbl;
    }

    /**
     * Styles a combo box and installs ThemeComboBoxUI & ThemeComboBoxRenderer to completely
     * eliminate native Aqua white fields and blue arrow controls.
     */
    public static void styleComboBox(JComboBox<?> combo) {
        Theme t = ThemeManager.getTheme();
        combo.setUI(new ThemeComboBoxUI());
        combo.setBackground(t.getBgField());
        combo.setForeground(t.getTextPrimary());
        combo.setFont(FONT_BODY);
        combo.setBorder(new EmptyBorder(2, 4, 2, 4));
        combo.setPreferredSize(new Dimension(Math.max(combo.getPreferredSize().width + 28, 160), 38));
        combo.setFocusable(false);
        combo.setOpaque(false);
        combo.setRenderer(new ThemeComboBoxRenderer());
    }

    /**
     * Creates a styled, theme-aware JComboBox that retains ThemeComboBoxUI across L&F updates.
     */
    public static <T> JComboBox<T> styledComboBox(T[] items) {
        JComboBox<T> combo = new JComboBox<T>(items) {
            @Override
            public void updateUI() {
                setUI(new ThemeComboBoxUI());
            }
        };
        styleComboBox(combo);
        return combo;
    }

    /**
     * Fully styles a JTabbedPane with ThemeTabbedPaneUI, ensuring high-contrast tabs in both themes.
     */
    public static void styleTabbedPane(JTabbedPane tab) {
        tab.setUI(new ThemeTabbedPaneUI());
        tab.setFont(FONT_HEADING);
        tab.setFocusable(false);
    }

    /**
     * Creates a styled, theme-aware JTabbedPane that retains ThemeTabbedPaneUI across L&F updates.
     */
    public static JTabbedPane styledTabbedPane() {
        JTabbedPane tab = new JTabbedPane() {
            @Override
            public void updateUI() {
                setUI(new ThemeTabbedPaneUI());
            }
        };
        styleTabbedPane(tab);
        return tab;
    }

    /**
     * Configures a scrollpane to use custom, theme-aware scrollbars and eliminate white background flash.
     */
    public static void styleScrollPane(JScrollPane scroll) {
        Theme t = ThemeManager.getTheme();
        scroll.setBorder(BorderFactory.createLineBorder(t.getBorder(), 1));
        scroll.getViewport().setBackground(t.getBgCard());
        scroll.setBackground(t.getBgMain());

        // Fill corner with header background so no blank gray box appears
        JPanel corner = new JPanel();
        corner.setBackground(t.isDark() ? t.getBgMain() : ThemeTableHeaderRenderer.LIGHT_HEADER_BG);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);

        if (scroll.getVerticalScrollBar() != null) {
            scroll.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        }
        if (scroll.getHorizontalScrollBar() != null) {
            scroll.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
        }
    }

    /**
     * Fully styles a JTable with theme colors, proper row heights, flat modern headers, and alternate row striping.
     */
    public static void styleTable(JTable table) {
        Theme t = ThemeManager.getTheme();
        table.setFont(FONT_BODY);
        table.setRowHeight(38);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(t.isDark() ? t.getBorderSubtle() : t.getBorder());
        table.setBackground(t.getBgCard());
        table.setForeground(t.getTextPrimary());
        table.setSelectionBackground(t.getAccentGreen());
        table.setSelectionForeground(t.isDark() ? t.getTableSelectionFg() : Color.WHITE);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_HEADING);
        header.setBackground(t.isDark() ? t.getBgMain() : ThemeTableHeaderRenderer.LIGHT_HEADER_BG);
        header.setForeground(t.isDark() ? t.getTextMuted() : ThemeTableHeaderRenderer.LIGHT_HEADER_FG);
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorder()));
        header.setDefaultRenderer(new ThemeTableHeaderRenderer());

        table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
    }

    /**
     * Updates an existing table and its header to match newly switched theme colors.
     */
    public static void updateTableTheme(JTable table) {
        if (table == null) return;
        Theme t = ThemeManager.getTheme();
        table.setGridColor(t.isDark() ? t.getBorderSubtle() : t.getBorder());
        table.setBackground(t.getBgCard());
        table.setForeground(t.getTextPrimary());
        table.setSelectionBackground(t.getAccentGreen());
        table.setSelectionForeground(t.isDark() ? t.getTableSelectionFg() : Color.WHITE);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(t.isDark() ? t.getBgMain() : ThemeTableHeaderRenderer.LIGHT_HEADER_BG);
            header.setForeground(t.isDark() ? t.getTextMuted() : ThemeTableHeaderRenderer.LIGHT_HEADER_FG);
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorder()));
            header.repaint();
        }
        table.repaint();
    }

    /**
     * Alternate row cell renderer ensuring high contrast, clean padding, and theme adaptability.
     */
    public static class StripedTableCellRenderer extends DefaultTableCellRenderer {
        private final int horizontalAlignment;

        public StripedTableCellRenderer() {
            this(SwingConstants.LEFT);
        }

        public StripedTableCellRenderer(int horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
        }

        public int getHorizontalAlignment() {
            return horizontalAlignment;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(horizontalAlignment);
            setFont(FONT_BODY);

            Theme t = ThemeManager.getTheme();
            if (isSelected) {
                c.setBackground(t.getAccentGreen());
                c.setForeground(t.isDark() ? t.getTableSelectionFg() : Color.WHITE);
            } else {
                c.setBackground((row % 2 == 1) ? t.getTableRowAlt() : t.getBgCard());
                c.setForeground(t.getTextPrimary());
            }

            if (c instanceof JComponent) {
                ((JComponent) c).setBorder(new EmptyBorder(0, 12, 0, 12));
            }
            return c;
        }
    }

    /**
     * High-contrast, theme-aware badge cell renderer for statuses (CONFIRMED, APPROVED, PENDING, CANCELLED, etc.).
     */
    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(FONT_HEADING);

            Theme t = ThemeManager.getTheme();
            String val = (value != null) ? value.toString().toUpperCase() : "";

            if (isSelected) {
                c.setBackground(t.getAccentGreen());
                c.setForeground(t.isDark() ? t.getTableSelectionFg() : Color.WHITE);
            } else {
                c.setBackground((row % 2 == 1) ? t.getTableRowAlt() : t.getBgCard());
                if (val.contains("CONFIRMED") || val.contains("APPROVED") || val.contains("AVAILABLE") || val.contains("SUCCESS") || val.contains("ACTIVE")) {
                    c.setForeground(t.getStatusSuccess());
                } else if (val.contains("CANCELLED") || val.contains("REJECTED") || val.contains("BLOCKED") || val.contains("FAILED") || val.contains("INACTIVE")) {
                    c.setForeground(t.getStatusError());
                } else if (val.contains("PENDING")) {
                    c.setForeground(t.getStatusWarning());
                } else {
                    c.setForeground(t.getTextMuted());
                }
            }
            if (c instanceof JComponent) {
                ((JComponent) c).setBorder(new EmptyBorder(0, 8, 0, 8));
            }
            return c;
        }
    }

    /**
     * Translates exceptions into user-friendly messages for the UI.
     */
    public static String formatErrorMessage(Throwable ex, String action) {
        if (ex == null) return action + " failed.";

        if (ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null) {
            ex = ex.getCause();
        }

        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        System.err.println("[" + action + "] Error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        if (root != ex) {
            System.err.println("[" + action + "] Root cause: " + root.getClass().getName() + " - " + root.getMessage());
        }

        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage();
        }

        String rootMsg = (root.getMessage() != null) ? root.getMessage().toLowerCase() : "";
        if (root instanceof java.net.ConnectException
                || rootMsg.contains("communications link failure")
                || rootMsg.contains("connection refused")
                || rootMsg.contains("failed to connect")) {
            return "Cannot connect to MySQL database. Make sure MySQL is running on port 3306.";
        }
        if (rootMsg.contains("access denied")) {
            return "Database access denied. Check db.user and db.password in db.properties.";
        }
        if (rootMsg.contains("unknown database")) {
            return "Database 'sportgrid' not found. Please run sql/schema.sql.";
        }
        if (rootMsg.contains("doesn't exist") || (rootMsg.contains("table") && rootMsg.contains("not found"))) {
            return "Database tables are missing. Please run sql/schema.sql.";
        }

        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage() : root.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : (action + " failed. Please try again.");
    }

    /**
     * Applies default theme settings on startup.
     */
    public static void applyGlobalDefaults() {
        ThemeManager.applyUIManagerDefaults();

        // Ensure font mappings are registered
        UIManager.put("OptionPane.messageFont",          FONT_BODY);
        UIManager.put("OptionPane.buttonFont",           FONT_BUTTON);
        UIManager.put("Button.font",                   FONT_BUTTON);
        UIManager.put("Label.font",                    FONT_BODY);
        UIManager.put("TextField.font",                FONT_BODY);
        UIManager.put("PasswordField.font",            FONT_BODY);
        UIManager.put("Table.font",                    FONT_BODY);
        UIManager.put("TableHeader.font",              FONT_HEADING);
        UIManager.put("TabbedPane.font",               FONT_HEADING);
        UIManager.put("ComboBox.font",                 FONT_BODY);
        UIManager.put("RadioButton.font",              FONT_BODY);
        UIManager.put("CheckBox.font",                 FONT_BODY);
        UIManager.put("TextArea.font",                 FONT_BODY);
        UIManager.put("ToolTip.font",                  FONT_SMALL);
        UIManager.put("TitledBorder.font",             FONT_HEADING);
    }
}
