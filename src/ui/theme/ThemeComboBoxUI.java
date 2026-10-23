package ui.theme;

import ui.StyleConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Custom Theme-Aware ComboBoxUI for SportGrid.
 *
 * Completely replaces native OS / Aqua ComboBox UI rendering:
 *  - Custom rounded field background matching active Theme (Dark & Light).
 *  - Eliminates native white background and blue arrow controls.
 *  - Modern vector chevron arrow button styled with theme accents.
 *  - High-contrast, theme-aware dropdown popup list and scrollbar.
 *  - Instant adaptation to ThemeManager runtime theme toggling.
 */
public class ThemeComboBoxUI extends BasicComboBoxUI {

    private boolean isHovered = false;
    private MouseListener hoverListener;

    public static ComponentUI createUI(JComponent c) {
        return new ThemeComboBoxUI();
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        hoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                if (comboBox != null) comboBox.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                if (comboBox != null) comboBox.repaint();
            }
        };
        if (comboBox != null) {
            comboBox.addMouseListener(hoverListener);
        }
    }

    @Override
    protected void uninstallListeners() {
        if (comboBox != null && hoverListener != null) {
            comboBox.removeMouseListener(hoverListener);
        }
        hoverListener = null;
        super.uninstallListeners();
    }

    @Override
    protected JButton createArrowButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Theme t = ThemeManager.getTheme();
                int w = getWidth();
                int h = getHeight();

                // Seamless background blending with field
                g2.setColor(t.getBgField());
                g2.fillRect(0, 0, w, h);

                // Chevron arrow color (accent green on hover/focus/open, textMuted normally)
                boolean active = (getModel().isRollover()
                        || (comboBox != null && comboBox.hasFocus())
                        || (comboBox != null && isPopupVisible(comboBox)));

                g2.setColor(active ? t.getAccentGreen() : t.getTextMuted());
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = w / 2 - 2;
                int cy = h / 2;
                if (comboBox != null && isPopupVisible(comboBox)) {
                    // Upward chevron when popup is open
                    g2.drawLine(cx - 4, cy + 2, cx, cy - 2);
                    g2.drawLine(cx, cy - 2, cx + 4, cy + 2);
                } else {
                    // Downward chevron
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                }

                g2.dispose();
            }
        };

        btn.setBorder(new EmptyBorder(0, 0, 0, 8));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox) {
            @Override
            protected JScrollPane createScroller() {
                JScrollPane scroller = super.createScroller();
                Theme t = ThemeManager.getTheme();
                scroller.setBorder(BorderFactory.createEmptyBorder());
                scroller.getViewport().setBackground(t.getBgCard());
                scroller.setBackground(t.getBgCard());
                if (scroller.getVerticalScrollBar() != null) {
                    scroller.getVerticalScrollBar().setUI(new CustomScrollBarUI());
                }
                return scroller;
            }

            @Override
            public void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme t = ThemeManager.getTheme();
                g2.setColor(t.getBorder());
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }

            @Override
            public void show() {
                Theme t = ThemeManager.getTheme();
                if (list != null) {
                    list.setBackground(t.getBgCard());
                    list.setForeground(t.getTextPrimary());
                    list.setSelectionBackground(t.getAccentGreen());
                    list.setSelectionForeground(t.getAccentText());
                }
                super.show();
            }
        };

        Theme t = ThemeManager.getTheme();
        popup.setBorder(BorderFactory.createLineBorder(t.getBorder(), 1));
        if (popup.getList() != null) {
            popup.getList().setBackground(t.getBgCard());
            popup.getList().setForeground(t.getTextPrimary());
            popup.getList().setSelectionBackground(t.getAccentGreen());
            popup.getList().setSelectionForeground(t.getAccentText());
        }
        return popup;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Theme t = ThemeManager.getTheme();
        int w = c.getWidth();
        int h = c.getHeight();

        // 1. Field Background
        g2.setColor(t.getBgField());
        g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

        // 2. State-aware border (Focus > Hover > Normal)
        if (c.hasFocus() || isPopupVisible(comboBox)) {
            g2.setColor(t.getBorderFocus());
            g2.setStroke(new BasicStroke(1.8f));
        } else if (isHovered) {
            g2.setColor(t.getBorderFocus());
            g2.setStroke(new BasicStroke(1.2f));
        } else {
            g2.setColor(t.getBorder());
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
        g2.dispose();

        // 3. Paint Current Selected Value
        hasFocus = comboBox.hasFocus();
        Rectangle r = rectangleForCurrentValue();
        paintCurrentValue(g, r, hasFocus);
    }

    @Override
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        ListCellRenderer<Object> renderer = (ListCellRenderer<Object>) comboBox.getRenderer();
        Component comp = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
        Theme t = ThemeManager.getTheme();
        comp.setFont(StyleConstants.FONT_BODY);
        comp.setForeground(t.getTextPrimary());
        comp.setBackground(t.getBgField());
        if (comp instanceof JComponent) {
            ((JComponent) comp).setOpaque(false);
        }
        currentValuePane.paintComponent(g, comp, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, false);
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // Prevent default BasicComboBoxUI from painting gray background rectangle
    }
}
